package com.example.expensestracker.service;

import com.example.expensestracker.model.*;
import com.example.expensestracker.repository.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;

import java.util.*;

@Service
public class AiInsightService {

    private final TrackerAppRepository trackerAppRepository;
    private final TrackerRepository trackerRepository;
    private final TrackerEntryRepository trackerEntryRepository;
    private final UserRepository userRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${gemini.api.key:}")
    private String defaultApiKey;

    public AiInsightService(TrackerAppRepository trackerAppRepository,
                            TrackerRepository trackerRepository,
                            TrackerEntryRepository trackerEntryRepository,
                            UserRepository userRepository,
                            RestTemplate restTemplate,
                            ObjectMapper objectMapper) {
        this.trackerAppRepository = trackerAppRepository;
        this.trackerRepository = trackerRepository;
        this.trackerEntryRepository = trackerEntryRepository;
        this.userRepository = userRepository;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    public List<SmartInsight> getInsightsForApp(Long appId, Long userId) {
        TrackerApp app = trackerAppRepository.findById(appId).orElse(null);
        if (app == null || !app.getUserId().equals(userId)) {
            System.out.println("AI Insight: App not found or access denied for appId " + appId + ", userId " + userId);
            return Collections.emptyList();
        }

        // Fetch user-specific API key
        String userApiKey = userRepository.findById(userId)
                .map(User::getGeminiApiKey)
                .orElse(null);

        String activeApiKey = (userApiKey != null && !userApiKey.isEmpty()) ? userApiKey.trim() : defaultApiKey;

        System.out.println("AI Insight: Using API Key (start): " + (activeApiKey != null && activeApiKey.length() > 4 ? activeApiKey.substring(0, 4) + "..." : "EMPTY"));

        if (activeApiKey == null || activeApiKey.isEmpty()) {
            return getFallbackInsights(app);
        }

        List<Tracker> trackers = trackerRepository.findByUserIdAndAppId(userId, appId);
        String dataContext = buildDataContext(app, trackers);
        
        System.out.println("AI Insight: Data context length: " + dataContext.length());

        try {
            return callGemini(dataContext, activeApiKey);
        } catch (Exception e) {
            String errorMsg = e.getMessage();
            System.err.println("AI Insight Generation Failed: " + errorMsg);
            return getErrorFallbackInsights(app, errorMsg);
        }
    }

    private String buildDataContext(TrackerApp app, List<Tracker> trackers) {
        StringBuilder sb = new StringBuilder();
        sb.append("App Name: ").append(app.getName()).append("\n");
        sb.append("Trackers in this app:\n");

        for (Tracker t : trackers) {
            sb.append("- Tracker: ").append(t.getName()).append(" (Type: ").append(t.getType()).append(")\n");
            List<TrackerEntry> entries = trackerEntryRepository.findByTrackerIdOrderByDateAsc(t.getId());
            // Limit to last 10 entries for context
            int start = Math.max(0, entries.size() - 10);
            List<TrackerEntry> recentEntries = entries.subList(start, entries.size());
            
            sb.append("  Recent Data:\n");
            for (TrackerEntry e : recentEntries) {
                sb.append("    Date: ").append(e.getDate()).append(", Data: ").append(e.getFieldValues()).append("\n");
            }
        }
        return sb.toString();
    }

    private List<SmartInsight> callGemini(String context, String activeApiKey) throws Exception {
        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash-latest:generateContent?key=" + activeApiKey;

        String prompt = "You are an AI Smart Dashboard engine for a Personal OS platform called Omni Tracker. " +
                "Your goal is to analyze user tracking data and provide 3-4 highly relevant, actionable insights or metrics. " +
                "Format your response strictly as a JSON array of SmartInsight objects. " +
                "Each object must have: type (METRIC, ADVICE, ALERT), title, value, subtitle, icon (emoji), color (success, warning, danger, primary, magic), priority (1-5), and reasoning. " +
                "\n\nUSER DATA CONTEXT:\n" + context +
                "\n\nRESPONSE FORMAT:\n" +
                "[{\"type\":\"...\", \"title\":\"...\", \"value\":\"...\", \"subtitle\":\"...\", \"icon\":\"...\", \"color\":\"...\", \"priority\":1, \"reasoning\":\"...\"}]";

        Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(
                                Map.of("text", prompt)
                        ))
                )
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
        
        if (response.getStatusCode() == HttpStatus.OK) {
            JsonNode root = objectMapper.readTree(response.getBody());
            String aiText = root.path("candidates").get(0).path("content").path("parts").get(0).path("text").asText();
            
            // Handle Gemini markdown wrapping if present
            if (aiText.contains("```json")) {
                aiText = aiText.substring(aiText.indexOf("```json") + 7, aiText.lastIndexOf("```"));
            } else if (aiText.contains("```")) {
                aiText = aiText.substring(aiText.indexOf("```") + 3, aiText.lastIndexOf("```"));
            }

            return objectMapper.readValue(aiText, objectMapper.getTypeFactory().constructCollectionType(List.class, SmartInsight.class));
        }

        throw new RuntimeException("Gemini API call failed with status: " + response.getStatusCode());
    }

    private List<SmartInsight> getFallbackInsights(TrackerApp app) {
        return List.of(
            new SmartInsight("ADVICE", "Action Required", "AI Insights Pending", "Please ensure your Gemini API Key is set in Profile Settings.", "💡", "primary", 1, "No active API key found."),
            new SmartInsight("METRIC", app.getName() + " Status", "Active", "Operational", "🚀", "success", 2, "System is ready for data analysis.")
        );
    }

    private List<SmartInsight> getErrorFallbackInsights(TrackerApp app, String error) {
        return List.of(
            new SmartInsight("ALERT", "AI Error", "Generation Failed", "There was an issue communicating with Gemini.", "⚠️", "danger", 1, error),
            new SmartInsight("ADVICE", "Check API Key", "Verify Settings", "Ensure your API key is valid and has Gemini 1.5 Flash enabled.", "🔑", "warning", 2, "API Call Result: " + error)
        );
    }
}
