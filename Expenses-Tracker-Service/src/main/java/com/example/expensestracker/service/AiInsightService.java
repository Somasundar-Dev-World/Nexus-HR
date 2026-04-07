package com.example.expensestracker.service;

import com.example.expensestracker.model.*;
import com.example.expensestracker.repository.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;

import java.util.*;
import java.time.LocalDateTime;

@Service
public class AiInsightService {

    private final TrackerAppRepository trackerAppRepository;
    private final TrackerRepository trackerRepository;
    private final TrackerEntryRepository trackerEntryRepository;
    private final UserRepository userRepository;
    private final AiInsightCacheRepository aiInsightCacheRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${anthropic.api.key:}")
    private String defaultApiKey;

    public AiInsightService(TrackerAppRepository trackerAppRepository,
                            TrackerRepository trackerRepository,
                            TrackerEntryRepository trackerEntryRepository,
                            UserRepository userRepository,
                            AiInsightCacheRepository aiInsightCacheRepository,
                            RestTemplate restTemplate,
                            ObjectMapper objectMapper) {
        this.trackerAppRepository = trackerAppRepository;
        this.trackerRepository = trackerRepository;
        this.trackerEntryRepository = trackerEntryRepository;
        this.userRepository = userRepository;
        this.aiInsightCacheRepository = aiInsightCacheRepository;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    public List<SmartInsight> getInsightsForApp(Long appId, Long userId, boolean forceRefresh) {
        TrackerApp app = trackerAppRepository.findById(appId).orElse(null);
        if (app == null || !app.getUserId().equals(userId)) {
            System.out.println("AI Insight: App not found or access denied for appId " + appId + ", userId " + userId);
            return Collections.emptyList();
        }

        // Check Persistence Cache
        if (!forceRefresh) {
            Optional<AiInsightCache> cached = aiInsightCacheRepository.findById(appId);
            if (cached.isPresent()) {
                System.out.println("AI Insight: Serving from DB cache for appId " + appId);
                try {
                    return objectMapper.readValue(cached.get().getInsightsJson(), 
                        objectMapper.getTypeFactory().constructCollectionType(List.class, SmartInsight.class));
                } catch (Exception e) {
                    System.err.println("AI Insight: Cache deserialization failed: " + e.getMessage());
                }
            }
        } else {
            System.out.println("AI Insight: Force refresh requested for appId " + appId);
        }

        // Fetch user-specific API key
        String userApiKey = userRepository.findById(userId)
                .map(User::getAnthropicApiKey)
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
            String rawJson = callClaudeRaw(dataContext, activeApiKey);
            List<SmartInsight> insights = objectMapper.readValue(rawJson, 
                objectMapper.getTypeFactory().constructCollectionType(List.class, SmartInsight.class));
            
            // Save to Persistence
            AiInsightCache cache = new AiInsightCache(appId, rawJson);
            aiInsightCacheRepository.save(cache);
            
            return insights;
        } catch (HttpStatusCodeException e) {
            if (e.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
                System.err.println("AI Insight: Rate limit (429) hit. Returning cooldown message.");
                return getRateLimitFallbackInsights(app);
            }
            System.err.println("AI Insight Generation Failed (HTTP " + e.getStatusCode() + "): " + e.getResponseBodyAsString());
            return getErrorFallbackInsights(app, "API Error: " + e.getStatusCode());
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

    private String callClaudeRaw(String context, String activeApiKey) throws Exception {
        String modelName = "claude-3-5-sonnet-20241022";
        String url = "https://api.anthropic.com/v1/messages";
        
        System.out.println("AI Insight: Calling Claude API (" + modelName + ")...");

        String systemPrompt = "You are an AI Smart Dashboard engine for a Personal OS platform called Omni Tracker. " +
                "Your goal is to analyze user tracking data and provide 3-4 highly relevant, actionable insights or metrics. " +
                "Format your response STRICTLY as a raw JSON array of SmartInsight objects. " +
                "Each object must have: type (METRIC, ADVICE, ALERT), title, value, subtitle, icon (emoji), color (success, warning, danger, primary, magic), priority (1-5), and reasoning. " +
                "Do not include any preamble, markdown formatting (like ```json), or follow-up text. Just the raw JSON array.";

        String userPrompt = "USER DATA CONTEXT:\n" + context +
                "\n\nBased on this data, provide 3-4 smart insights in the requested JSON array format.";

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", modelName);
        requestBody.put("max_tokens", 4096);
        requestBody.put("system", systemPrompt);
        requestBody.put("messages", List.of(
                Map.of("role", "user", "content", userPrompt)
        ));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-api-key", activeApiKey);
        headers.set("anthropic-version", "2023-06-01");
        
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
        
        if (response.getStatusCode() == HttpStatus.OK) {
            JsonNode root = objectMapper.readTree(response.getBody());
            String aiText = root.path("content").get(0).path("text").asText().trim();
            
            // Defensive cleaning in case Claude included markdown anyway
            if (aiText.contains("```json")) {
                aiText = aiText.substring(aiText.indexOf("```json") + 7, aiText.lastIndexOf("```")).trim();
            } else if (aiText.contains("```")) {
                aiText = aiText.substring(aiText.indexOf("```") + 3, aiText.lastIndexOf("```")).trim();
            }
            return aiText;
        }

        throw new RuntimeException("Claude API call failed with status: " + response.getStatusCode());
    }

    private List<SmartInsight> getFallbackInsights(TrackerApp app) {
        return List.of(
            new SmartInsight("ADVICE", "Action Required", "AI Insights Pending", "Please ensure your Claude API Key is set in Profile Settings.", "💡", "primary", 1, "No active API key found."),
            new SmartInsight("METRIC", app.getName() + " Status", "Active", "Operational", "🚀", "success", 2, "System is ready for data analysis.")
        );
    }

    private List<SmartInsight> getErrorFallbackInsights(TrackerApp app, String error) {
        return List.of(
            new SmartInsight("ALERT", "AI Connectivity", "Communication Error", "The AI service is currently unavailable.", "🔌", "danger", 1, error),
            new SmartInsight("ADVICE", "Action Required", "Verify API Key", "Ensure your Claude API key is correct in your profile settings.", "🔑", "warning", 2, "Error details: " + error)
        );
    }

    private List<SmartInsight> getRateLimitFallbackInsights(TrackerApp app) {
        return List.of(
            new SmartInsight("ALERT", "AI Cooling Down", "Rate Limit Reached", "Too many requests. Please wait a moment before refreshing.", "🧊", "warning", 1, "429 Too Many Requests"),
            new SmartInsight("ADVICE", "Pro Tip", "Usage Limits", "Your Claude API tier may have reached its hourly or daily limit.", "💡", "primary", 2, "Check Anthropic Dashboard.")
        );
    }
}
