package com.farmrakshak.advisory.service;

import com.farmrakshak.advisory.dto.CropContext;
import com.farmrakshak.advisory.dto.Suggestion;
import com.farmrakshak.advisory.dto.WeatherContext;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Pure Java rule engine that applies weather-based and stage-based rules.
 * These rules run without any AI call and serve as the fallback.
 */
@Service
public class RuleEngine {

    public List<Suggestion> generateSuggestions(CropContext crop, WeatherContext weather) {
        List<Suggestion> suggestions = new ArrayList<>();

        // ── Weather-based rules ──
        if (weather != null) {
            if (weather.isRainForecast()) {
                suggestions.add(Suggestion.builder()
                        .category("WEATHER")
                        .message("Rain expected" + (weather.getRainVolume() > 0 ? " (" + weather.getRainVolume() + " mm)" : "") + ". Prepare drainage to avoid waterlogging. Delay any spray applications.")
                        .riskLevel("HIGH").build());
            }

            if (weather.getHumidity() > 80) {
                suggestions.add(Suggestion.builder()
                        .category("DISEASE_RISK")
                        .message("High humidity (" + (int) weather.getHumidity() + "%) increases fungal disease risk. Monitor crops for blight, mildew, and rust. Apply preventive fungicide if needed.")
                        .riskLevel("HIGH").build());
            } else if (weather.getHumidity() > 60) {
                suggestions.add(Suggestion.builder()
                        .category("DISEASE_RISK")
                        .message("Moderate humidity levels. Keep monitoring for early signs of fungal infections.")
                        .riskLevel("LOW").build());
            }

            if (weather.getTemperature() > 38) {
                suggestions.add(Suggestion.builder()
                        .category("IRRIGATION")
                        .message("Extreme heat (" + (int) weather.getTemperature() + "°C). Increase irrigation frequency. Apply mulching to retain soil moisture. Avoid midday field work.")
                        .riskLevel("HIGH").build());
            } else if (weather.getTemperature() > 35) {
                suggestions.add(Suggestion.builder()
                        .category("IRRIGATION")
                        .message("High temperature (" + (int) weather.getTemperature() + "°C). Provide adequate irrigation to prevent heat stress. Water early morning or late evening.")
                        .riskLevel("MEDIUM").build());
            }

            if (weather.getWindSpeed() > 30) {
                suggestions.add(Suggestion.builder()
                        .category("WEATHER")
                        .message("Strong winds (" + (int) weather.getWindSpeed() + " km/h). Protect young plants with windbreakers. Stake tall crops. Avoid spraying.")
                        .riskLevel("HIGH").build());
            } else if (weather.getWindSpeed() > 20) {
                suggestions.add(Suggestion.builder()
                        .category("WEATHER")
                        .message("Moderate winds. Delay any spray applications to avoid drift. Check crop supports.")
                        .riskLevel("MEDIUM").build());
            }
        }

        // ── Stage-based rules ──
        if (crop != null && crop.getCropStage() != null) {
            String stage = crop.getCropStage().toUpperCase();
            switch (stage) {
                case "GERMINATION":
                    suggestions.add(Suggestion.builder().category("IRRIGATION")
                            .message("Keep soil consistently moist but not waterlogged. Light watering 2-3 times daily.").riskLevel("MEDIUM").build());
                    suggestions.add(Suggestion.builder().category("GENERAL")
                            .message("Protect seedbed from birds and pests. Use netting if necessary.").riskLevel("LOW").build());
                    break;
                case "SEEDLING":
                    suggestions.add(Suggestion.builder().category("FERTILIZER")
                            .message("Apply starter fertilizer (DAP) at recommended dose to boost early root development.").riskLevel("LOW").build());
                    suggestions.add(Suggestion.builder().category("PEST_RISK")
                            .message("Watch for cutworms and damping-off disease in seedlings.").riskLevel("MEDIUM").build());
                    break;
                case "VEGETATIVE":
                    suggestions.add(Suggestion.builder().category("FERTILIZER")
                            .message("Apply nitrogen-rich fertilizer (urea/20-20-20 NPK) for leaf growth. Follow recommended dosage.").riskLevel("LOW").build());
                    suggestions.add(Suggestion.builder().category("IRRIGATION")
                            .message("Maintain regular irrigation schedule. Vegetative stage requires consistent moisture.").riskLevel("LOW").build());
                    break;
                case "FLOWERING":
                    suggestions.add(Suggestion.builder().category("FERTILIZER")
                            .message("Apply potassium-rich fertilizer to support flower and fruit development. Reduce nitrogen.").riskLevel("MEDIUM").build());
                    suggestions.add(Suggestion.builder().category("PEST_RISK")
                            .message("Flowering stage attracts pollinators but also pests. Monitor for aphids and thrips.").riskLevel("MEDIUM").build());
                    suggestions.add(Suggestion.builder().category("IRRIGATION")
                            .message("Increase irrigation slightly during flowering. Water stress can cause flower drop.").riskLevel("MEDIUM").build());
                    break;
                case "FRUITING":
                    suggestions.add(Suggestion.builder().category("IRRIGATION")
                            .message("Consistent watering is critical. Irregular watering causes fruit cracking and blossom end rot.").riskLevel("HIGH").build());
                    suggestions.add(Suggestion.builder().category("FERTILIZER")
                            .message("Apply calcium-based foliar spray to prevent blossom end rot.").riskLevel("MEDIUM").build());
                    break;
                case "MATURITY":
                    suggestions.add(Suggestion.builder().category("IRRIGATION")
                            .message("Reduce watering gradually. Allow natural ripening.").riskLevel("LOW").build());
                    suggestions.add(Suggestion.builder().category("GENERAL")
                            .message("Start planning harvest logistics. Check market prices for optimal selling time.").riskLevel("LOW").build());
                    break;
                case "HARVEST":
                    suggestions.add(Suggestion.builder().category("GENERAL")
                            .message("Harvest during cool morning hours for best quality. Handle produce carefully to avoid bruising.").riskLevel("LOW").build());
                    break;
            }
        }

        // ── Disease history-based rules ──
        if (crop != null && crop.getDiseaseHistory() != null && !crop.getDiseaseHistory().isEmpty()) {
            for (CropContext.DiseaseRecord disease : crop.getDiseaseHistory()) {
                if (disease.getDiseaseName() != null && !disease.getDiseaseName().equalsIgnoreCase("Healthy")) {
                    suggestions.add(Suggestion.builder()
                            .category("DISEASE_RISK")
                            .message("Previous detection: " + disease.getDiseaseName() + " (confidence: " + (int)(disease.getConfidence() * 100) + "%). Continue monitoring for recurrence. Apply follow-up treatment if symptoms return.")
                            .riskLevel("HIGH").build());
                }
            }
        }

        return suggestions;
    }
}
