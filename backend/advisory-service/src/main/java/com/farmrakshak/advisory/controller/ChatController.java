package com.farmrakshak.advisory.controller;

import com.farmrakshak.advisory.service.AiChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/advisories/chat")
@RequiredArgsConstructor
public class ChatController {

    private final AiChatService chatService;

    /**
     * POST /api/v1/advisories/chat
     * Body: { "question": "...", "sessionId": "optional-uuid", "lat": 19.0, "lon": 72.0 }
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> chat(
            @RequestHeader("X-User-Id") String userId,
            @RequestBody Map<String, Object> body
    ) {
        String question = (String) body.get("question");
        String sessionIdStr = (String) body.get("sessionId");
        UUID sessionId = sessionIdStr != null && !sessionIdStr.isBlank() ? UUID.fromString(sessionIdStr) : null;

        double lat = body.containsKey("lat") ? ((Number) body.get("lat")).doubleValue() : 19.0;
        double lon = body.containsKey("lon") ? ((Number) body.get("lon")).doubleValue() : 72.0;
        String language = body.containsKey("language") ? (String) body.get("language") : "en";

        Map<String, Object> result = chatService.chat(UUID.fromString(userId), sessionId, question, lat, lon, language);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", result,
                "message", "Chat response generated"
        ));
    }

    /**
     * GET /api/v1/advisories/chat/sessions
     */
    @GetMapping("/sessions")
    public ResponseEntity<Map<String, Object>> getSessions(@RequestHeader("X-User-Id") String userId) {
        List<Map<String, Object>> sessions = chatService.getSessions(UUID.fromString(userId));
        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", sessions
        ));
    }

    /**
     * GET /api/v1/advisories/chat/sessions/{sessionId}
     */
    @GetMapping("/sessions/{sessionId}")
    public ResponseEntity<Map<String, Object>> getMessages(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable UUID sessionId
    ) {
        List<Map<String, Object>> messages = chatService.getSessionMessages(UUID.fromString(userId), sessionId);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", messages
        ));
    }

    /**
     * DELETE /api/v1/advisories/chat/sessions/{sessionId}
     */
    @DeleteMapping("/sessions/{sessionId}")
    public ResponseEntity<Map<String, Object>> deleteSession(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable UUID sessionId
    ) {
        chatService.deleteSession(UUID.fromString(userId), sessionId);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Session deleted"
        ));
    }
}
