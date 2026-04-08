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

    @Value("${gemini.api.key:}")
    private String defaultGeminiKey;

    @Value("${anthropic.api.key:}")
    private String defaultAnthropicKey;

    @Value("${openai.api.key:}")
    private String defaultOpenaiKey;

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
        }

        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return Collections.emptyList();

        String provider = user.getAiProvider() != null ? user.getAiProvider() : "GOOGLE";
        String model = user.getAiModel() != null ? user.getAiModel() : "gemini-1.5-flash";
        
        String activeApiKey = null;
        if ("GOOGLE".equalsIgnoreCase(provider)) {
            activeApiKey = (user.getGeminiApiKey() != null && !user.getGeminiApiKey().isEmpty()) ? user.getGeminiApiKey() : defaultGeminiKey;
        } else if ("ANTHROPIC".equalsIgnoreCase(provider)) {
            activeApiKey = (user.getAnthropicApiKey() != null && !user.getAnthropicApiKey().isEmpty()) ? user.getAnthropicApiKey() : defaultAnthropicKey;
        } else if ("OPENAI".equalsIgnoreCase(provider)) {
            activeApiKey = (user.getOpenaiApiKey() != null && !user.getOpenaiApiKey().isEmpty()) ? user.getOpenaiApiKey() : defaultOpenaiKey;
        }

        if (activeApiKey == null || activeApiKey.isEmpty()) {
            return getFallbackInsights(app, provider);
        }

        // Validate key format for common errors
        if ("ANTHROPIC".equalsIgnoreCase(provider) && !activeApiKey.startsWith("sk-ant-")) {
            return getErrorFallbackInsights(app, "Invalid Claude API Key format (must start with sk-ant-)");
        }
        if ("OPENAI".equalsIgnoreCase(provider) && !activeApiKey.startsWith("sk-")) {
            return getErrorFallbackInsights(app, "Invalid OpenAI API Key format (must start with sk-)");
        }

        List<Tracker> trackers = trackerRepository.findByUserIdAndAppId(userId, appId);
        String dataContext = buildDataContext(app, trackers);
        
        System.out.println("AI Insight: Provider=" + provider + ", Model=" + model + ", ContextLen=" + dataContext.length());

        try {
            String rawJson;
            if ("ANTHROPIC".equalsIgnoreCase(provider)) {
                rawJson = callClaudeRaw(dataContext, activeApiKey, model);
            } else if ("OPENAI".equalsIgnoreCase(provider)) {
                rawJson = callOpenAiRaw(dataContext, activeApiKey, model);
            } else {
                rawJson = callGeminiRaw(dataContext, activeApiKey, model);
            }

            List<SmartInsight> insights = objectMapper.readValue(rawJson, 
                objectMapper.getTypeFactory().constructCollectionType(List.class, SmartInsight.class));
            
            // Save to Persistence
            AiInsightCache cache = new AiInsightCache(appId, rawJson);
            aiInsightCacheRepository.save(cache);
            
            return insights;
        } catch (HttpStatusCodeException e) {
            String responseBody = e.getResponseBodyAsString();
            System.err.println("AI Insight Generation Failed (HTTP " + e.getStatusCode() + "): " + responseBody);
            
            if (e.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
                return getRateLimitFallbackInsights(app, provider);
            }
            return getErrorFallbackInsights(app, "API Error (" + provider + "): " + e.getStatusCode());
        } catch (Exception e) {
            System.err.println("AI Insight Generation Failed: " + e.getMessage());
            return getErrorFallbackInsights(app, e.getMessage());
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

    private String callGeminiRaw(String context, String activeApiKey, String modelName) throws Exception {
        String url = "https://generativelanguage.googleapis.com/v1beta/models/" + modelName + ":generateContent?key=" + activeApiKey;
        
        System.out.println("AI Insight: Calling Gemini API (" + modelName + ")...");

        String prompt = "You are an AI Smart Dashboard engine for a Personal OS platform called Omni Tracker. " +
                "Your goal is to analyze user tracking data and provide 3-4 highly relevant, actionable insights or metrics. " +
                "Format your response strictly as a JSON array of SmartInsight objects. " +
                "Do not include markdown formatting or follow-up text. Just the raw JSON array. " +
                "Each object must have: type (METRIC, ADVICE, ALERT), title, value, subtitle, icon (emoji), color (success, warning, danger, primary, magic), priority (1-5), and reasoning. " +
                "\n\nUSER DATA CONTEXT:\n" + context;

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
            return cleanJson(aiText);
        }
        throw new RuntimeException("Gemini API failed: " + response.getStatusCode());
    }

    private String callClaudeRaw(String context, String activeApiKey, String modelName) throws Exception {
        String url = "https://api.anthropic.com/v1/messages";
        System.out.println("AI Insight: Calling Claude API (" + modelName + ")...");

        String systemPrompt = "You are an AI Smart Dashboard engine for a Personal OS platform called Omni Tracker. " +
                "Your goal is to analyze user tracking data and provide 3-4 highly relevant, actionable insights or metrics. " +
                "Format your response STRICTLY as a raw JSON array of SmartInsight objects. " +
                "Do not include preamble, markdown, or follow-up text. Just raw JSON.";

        String userPrompt = "USER DATA CONTEXT:\n" + context;

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", modelName);
        requestBody.put("max_tokens", 2048);
        requestBody.put("system", systemPrompt);
        requestBody.put("messages", List.of(Map.of("role", "user", "content", userPrompt)));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-api-key", activeApiKey);
        headers.set("anthropic-version", "2023-06-01");
        
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
        
        if (response.getStatusCode() == HttpStatus.OK) {
            JsonNode root = objectMapper.readTree(response.getBody());
            String aiText = root.path("content").get(0).path("text").asText().trim();
            return cleanJson(aiText);
        }
        throw new RuntimeException("Claude API failed: " + response.getStatusCode());
    }

    private String callOpenAiRaw(String context, String activeApiKey, String modelName) throws Exception {
        String url = "https://api.openai.com/v1/chat/completions";
        System.out.println("AI Insight: Calling OpenAI API (" + modelName + ")...");

        String systemPrompt = "You are an AI Smart Dashboard engine for a Personal OS platform called Omni Tracker. " +
                "Analyze user data and provide 3-4 smart insights. Format strictly as a raw JSON array of SmartInsight objects.";

        String userPrompt = "USER DATA CONTEXT:\n" + context;

        Map<String, Object> requestBody = Map.of(
                "model", modelName,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)
                )
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + activeApiKey);
        
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
        
        if (response.getStatusCode() == HttpStatus.OK) {
            JsonNode root = objectMapper.readTree(response.getBody());
            String aiText = root.path("choices").get(0).path("message").path("content").asText().trim();
            return cleanJson(aiText);
        }
        throw new RuntimeException("OpenAI API failed: " + response.getStatusCode());
    }

    private String cleanJson(String aiText) {
        if (aiText.contains("```json")) {
            aiText = aiText.substring(aiText.indexOf("```json") + 7, aiText.lastIndexOf("```")).trim();
        } else if (aiText.contains("```")) {
            aiText = aiText.substring(aiText.indexOf("```") + 3, aiText.lastIndexOf("```")).trim();
        }
        return aiText;
    }

    private List<SmartInsight> getFallbackInsights(TrackerApp app, String provider) {
        return List.of(
            new SmartInsight("ADVICE", "Action Required", "AI Settings Pending", "Please ensure your " + provider + " API Key is set in Profile Settings.", "💡", "primary", 1, "No active API key found."),
            new SmartInsight("METRIC", app.getName() + " Status", "Active", "Operational", "🚀", "success", 2, "System is ready for data analysis.")
        );
    }

    private List<SmartInsight> getErrorFallbackInsights(TrackerApp app, String error) {
        return List.of(
            new SmartInsight("ALERT", "AI Connectivity", "Communication Error", "The AI service is currently unavailable.", "🔌", "danger", 1, error),
            new SmartInsight("ADVICE", "Action Required", "Verify Settings", "Check your model and API key in profile settings.", "🔑", "warning", 2, "Error details: " + error)
        );
    }

    private List<SmartInsight> getRateLimitFallbackInsights(TrackerApp app, String provider) {
        return List.of(
            new SmartInsight("ALERT", "AI Cooling Down", "Rate Limit Reached", "Too many requests to " + provider + ".", "🧊", "warning", 1, "429 Too Many Requests"),
            new SmartInsight("ADVICE", "Pro Tip", "Usage Limits", "Your API tier may have reached its hourly or daily limit.", "💡", "primary", 2, "Check " + provider + " Dashboard.")
        );
    }
}
