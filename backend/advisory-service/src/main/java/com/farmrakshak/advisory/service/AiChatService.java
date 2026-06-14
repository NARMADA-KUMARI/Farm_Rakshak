package com.farmrakshak.advisory.service;

import com.farmrakshak.advisory.entity.ChatMessage;
import com.farmrakshak.advisory.entity.ChatSession;
import com.farmrakshak.advisory.repository.ChatMessageRepository;
import com.farmrakshak.advisory.repository.ChatSessionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiChatService {

    private final ChatSessionRepository sessionRepo;
    private final ChatMessageRepository messageRepo;
    private final ChatContextBuilder contextBuilder;
    private final ObjectMapper objectMapper;

    @Value("${mistral.api-key:}")
    private String apiKey;

    @Value("${mistral.model:mistral-small-latest}")
    private String model;

    @Value("${mistral.url:https://api.mistral.ai/v1/chat/completions}")
    private String apiUrl;

    private final WebClient.Builder webClientBuilder;

    private static final int MAX_HISTORY = 10;
    private static final int MAX_QUESTION_LENGTH = 500;

    private static final Map<String, String> LANGUAGE_NAMES = Map.of(
            "en", "English", "hi", "Hindi", "te", "Telugu",
            "mr", "Marathi", "kn", "Kannada", "ta", "Tamil"
    );

    private static final String SYSTEM_PROMPT_TEMPLATE = """
            You are FarmRakshak AI, an expert agriculture assistant for Indian farmers.
            You have access to the farmer's REAL farm and crop data below. Always base your answers on this data.

            CONTEXT AWARENESS:
            - The farmer may own MULTIPLE FARMS in different locations with different soil types.
            - Each farm has its own crops with allocated area, sowing dates, and growth stages.
            - Farms have a total area capacity — some area may be unused (available for new crops).
            - When advising, consider the SPECIFIC FARM's location, soil type, and area for each crop.
            - If the farmer asks about a specific farm or crop, refer to the correct one from the data.
            - If the farmer asks what to plant next, consider the remaining unused area and the farm's soil/location.

            RULES:
            - Answer in simple, practical language a farmer can follow.
            - Keep answers under 200 words unless the farmer asks for detail.
            - Give actionable advice with specific steps.
            - When discussing area, use the farm's unit (acres, hectares, bigha, etc.).
            - Reference the farm name and location when giving location-specific advice.
            - For disease/pest advice, consider the farm's region and current weather.
            - If you're unsure, recommend consulting a local Krishi Vigyan Kendra or agriculture expert.
            - Never make up data — only use what is provided below.
            - Be warm, supportive, and encouraging.
            - IMPORTANT: You MUST respond in %s (%s). All your responses must be in this language.

            """;

    // ─── PUBLIC API ───

    @Transactional
    public Map<String, Object> chat(UUID userId, UUID sessionId, String question, double lat, double lon, String language) {
        // Validate language code
        if (language == null || !LANGUAGE_NAMES.containsKey(language)) {
            language = "en";
        }
        // Input validation
        if (question == null || question.isBlank()) {
            throw new IllegalArgumentException("Question cannot be empty");
        }
        question = sanitize(question);
        if (question.length() > MAX_QUESTION_LENGTH) {
            question = question.substring(0, MAX_QUESTION_LENGTH);
        }

        // Get or create session
        ChatSession session;
        if (sessionId != null) {
            session = sessionRepo.findById(sessionId)
                    .orElseThrow(() -> new RuntimeException("Session not found"));
            if (!session.getUserId().equals(userId)) {
                throw new RuntimeException("Access denied");
            }
        } else {
            session = sessionRepo.save(ChatSession.builder().userId(userId).title("New Chat").build());
        }

        // Build farm context
        String farmContext = contextBuilder.buildContext(userId, lat, lon);

        // Auto-title session from first message
        if ("New Chat".equals(session.getTitle())) {
            String title = question.length() > 50 ? question.substring(0, 47) + "..." : question;
            session.setTitle(title);
            sessionRepo.save(session);
        }

        // Save user message
        Instant now = Instant.now();
        messageRepo.save(ChatMessage.builder()
                .sessionId(session.getId())
                .role("user")
                .content(question)
                .createdAt(now)
                .build());

        // Load conversation history (last N messages)
        List<ChatMessage> history = messageRepo.findBySessionIdOrderByCreatedAtAsc(session.getId());
        if (history.size() > MAX_HISTORY) {
            history = history.subList(history.size() - MAX_HISTORY, history.size());
        }

        // Call AI
        String answer;
        try {
            answer = callMistral(farmContext, history, language);
        } catch (Exception e) {
            log.error("Mistral chat failed: {}", e.getMessage());
            answer = "AI assistant is temporarily unavailable. Please try again in a moment.";
        }

        // Save AI response
        Instant aiTime = Instant.now();
        ChatMessage aiMsg = messageRepo.save(ChatMessage.builder()
                .sessionId(session.getId())
                .role("assistant")
                .content(answer)
                .contextSnapshot(farmContext.length() > 5000 ? farmContext.substring(0, 5000) : farmContext)
                .createdAt(aiTime)
                .build());

        // Update session timestamp
        session.setUpdatedAt(aiTime);
        sessionRepo.save(session);

        return Map.of(
                "sessionId", session.getId().toString(),
                "answer", answer,
                "messageId", aiMsg.getId().toString(),
                "timestamp", aiTime.toString()
        );
    }

    public List<Map<String, Object>> getSessions(UUID userId) {
        return sessionRepo.findByUserIdOrderByUpdatedAtDesc(userId).stream()
                .map(s -> Map.<String, Object>of(
                        "id", s.getId().toString(),
                        "title", s.getTitle(),
                        "createdAt", s.getCreatedAt().toString(),
                        "updatedAt", s.getUpdatedAt().toString()
                ))
                .collect(Collectors.toList());
    }

    public List<Map<String, Object>> getSessionMessages(UUID userId, UUID sessionId) {
        ChatSession session = sessionRepo.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));
        if (!session.getUserId().equals(userId)) {
            throw new RuntimeException("Access denied");
        }

        return messageRepo.findBySessionIdOrderByCreatedAtAsc(sessionId).stream()
                .map(m -> Map.<String, Object>of(
                        "id", m.getId().toString(),
                        "role", m.getRole(),
                        "content", m.getContent(),
                        "createdAt", m.getCreatedAt().toString()
                ))
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteSession(UUID userId, UUID sessionId) {
        ChatSession session = sessionRepo.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));
        if (!session.getUserId().equals(userId)) {
            throw new RuntimeException("Access denied");
        }
        messageRepo.deleteBySessionId(sessionId);
        sessionRepo.delete(session);
    }

    // ─── PRIVATE ───

    @SuppressWarnings("unchecked")
    private String callMistral(String farmContext, List<ChatMessage> history, String language) {
        if (apiKey == null || apiKey.isBlank()) {
            return "AI is not configured. Please contact the administrator.";
        }

        String langName = LANGUAGE_NAMES.getOrDefault(language, "English");
        String systemPrompt = String.format(SYSTEM_PROMPT_TEMPLATE, langName, language) + farmContext;

        // Build messages array: system + history
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", systemPrompt));

        for (ChatMessage msg : history) {
            messages.add(Map.of("role", msg.getRole(), "content", msg.getContent()));
        }

        Map<String, Object> body = Map.of(
                "model", model,
                "messages", messages,
                "temperature", 0.4,
                "max_tokens", 800
        );

        Map<?, ?> response = webClientBuilder.build().post()
                .uri(apiUrl)
                .header("Authorization", "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        if (response == null) return "No response from AI.";

        List<?> choices = (List<?>) response.get("choices");
        if (choices == null || choices.isEmpty()) return "No response from AI.";

        Map<?, ?> choice = (Map<?, ?>) choices.get(0);
        Map<?, ?> message = (Map<?, ?>) choice.get("message");
        return message != null ? (String) message.get("content") : "No response from AI.";
    }

    private String sanitize(String input) {
        // Basic prompt injection prevention
        return input
                .replaceAll("(?i)(ignore|forget|disregard)\\s+(all\\s+)?(previous|above|prior)\\s+(instructions?|prompts?|rules?)", "[filtered]")
                .replaceAll("(?i)you\\s+are\\s+now", "[filtered]")
                .replaceAll("(?i)system\\s*prompt", "[filtered]")
                .trim();
    }
}
