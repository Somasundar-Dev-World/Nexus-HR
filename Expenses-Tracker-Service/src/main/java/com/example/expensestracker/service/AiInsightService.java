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
    private final DeepInsightCacheRepository deepInsightCacheRepository;
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
                            DeepInsightCacheRepository deepInsightCacheRepository,
                            RestTemplate restTemplate,
                            ObjectMapper objectMapper) {
        this.trackerAppRepository = trackerAppRepository;
        this.trackerRepository = trackerRepository;
        this.trackerEntryRepository = trackerEntryRepository;
        this.userRepository = userRepository;
        this.aiInsightCacheRepository = aiInsightCacheRepository;
        this.deepInsightCacheRepository = deepInsightCacheRepository;
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
        String model = user.getAiModel() != null ? user.getAiModel() : "gemini-2.0-flash";
        
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

    // ══════════════════════════════════════════════════════════
    //  AI DEEP RESEARCH
    // ══════════════════════════════════════════════════════════
    public DeepInsightReport getDeepInsightForApp(Long appId, Long userId, boolean forceRefresh) {
        TrackerApp app = trackerAppRepository.findById(appId).orElse(null);
        if (app == null || !app.getUserId().equals(userId)) return null;

        // Serve from cache unless forceRefresh
        if (!forceRefresh) {
            Optional<DeepInsightCache> cached = deepInsightCacheRepository.findById(appId);
            if (cached.isPresent()) {
                System.out.println("Deep Insight: Serving from DB cache for appId " + appId);
                try {
                    return objectMapper.readValue(cached.get().getReportJson(), DeepInsightReport.class);
                } catch (Exception e) {
                    System.err.println("Deep Insight: Cache deserialization failed: " + e.getMessage());
                }
            }
        }

        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return null;

        String provider = user.getAiProvider() != null ? user.getAiProvider() : "GOOGLE";
        String model = user.getAiModel() != null ? user.getAiModel() : "gemini-2.0-flash";

        String activeApiKey = null;
        if ("GOOGLE".equalsIgnoreCase(provider)) {
            activeApiKey = (user.getGeminiApiKey() != null && !user.getGeminiApiKey().isEmpty()) ? user.getGeminiApiKey() : defaultGeminiKey;
        } else if ("ANTHROPIC".equalsIgnoreCase(provider)) {
            activeApiKey = (user.getAnthropicApiKey() != null && !user.getAnthropicApiKey().isEmpty()) ? user.getAnthropicApiKey() : defaultAnthropicKey;
        } else if ("OPENAI".equalsIgnoreCase(provider)) {
            activeApiKey = (user.getOpenaiApiKey() != null && !user.getOpenaiApiKey().isEmpty()) ? user.getOpenaiApiKey() : defaultOpenaiKey;
        }

        if (activeApiKey == null || activeApiKey.isEmpty()) {
            return buildErrorDeepReport("No AI API key configured. Please set your key in Profile Settings.");
        }

        List<Tracker> trackers = trackerRepository.findByUserIdAndAppId(userId, appId);
        int[] counts = {0};
        String dataContext = buildDeepDataContext(app, trackers, counts);
        int totalEntries = counts[0];

        String systemPrompt = "You are an elite AI data analyst embedded inside a personal productivity OS called Omni Tracker. " +
            "You will receive full historical tracking data from a user's personal app. " +
            "Your job is to produce a DEEP RESEARCH REPORT — exhaustive, rigorous, insightful, and actionable. " +
            "You MUST analyze every angle: trends over time, anomalies, risk factors, cross-tracker correlations, " +
            "behavioral patterns, forecasting, and prioritized recommendations. " +
            "Be specific. Reference actual data values, dates, and field names from the context. " +
            "The response MUST be a single raw JSON object (no markdown, no preamble) matching this exact structure:\n" +
            "{\n" +
            "  \"executiveSummary\": \"4-6 sentence high-level synthesis of the entire dataset\",\n" +
            "  \"overallScore\": <integer 0-100 representing overall health/performance>,\n" +
            "  \"scoreLabel\": \"e.g. Excellent / Good / Needs Attention / Critical\",\n" +
            "  \"sections\": [\n" +
            "    {\n" +
            "      \"id\": \"unique_id\",\n" +
            "      \"title\": \"Section title\",\n" +
            "      \"icon\": \"single emoji\",\n" +
            "      \"type\": \"SUMMARY|TREND|ANOMALY|FORECAST|RECOMMENDATION|RISK|CORRELATION\",\n" +
            "      \"color\": \"success|warning|danger|primary|magic|info\",\n" +
            "      \"priority\": <1-5>,\n" +
            "      \"headline\": \"One-line key finding or stat\",\n" +
            "      \"content\": \"Detailed 3-5 sentence analysis of this section\",\n" +
            "      \"dataPoints\": [\"bullet 1\", \"bullet 2\", \"bullet 3\"],\n" +
            "      \"actionItems\": [\"actionable step 1\", \"actionable step 2\"]\n" +
            "    }\n" +
            "  ]\n" +
            "}\n" +
            "Include at least 6-8 sections covering: overall performance, key trends, anomalies/outliers, " +
            "30/60/90 day forecast, top recommendations, risk assessment, and if multiple trackers exist, cross-tracker correlations.";

        String userPrompt = "FULL DATA CONTEXT:\n" + dataContext;

        try {
            String rawJson = "";
            if ("ANTHROPIC".equalsIgnoreCase(provider)) {
                rawJson = callClaudeRawForDoc(userPrompt, systemPrompt, activeApiKey, model);
            } else if ("OPENAI".equalsIgnoreCase(provider)) {
                rawJson = callOpenAiRawForDoc(userPrompt, systemPrompt, activeApiKey, model);
            } else {
                rawJson = callGeminiRawForDoc(userPrompt, systemPrompt, activeApiKey, model);
            }

            DeepInsightReport report = objectMapper.readValue(cleanJson(rawJson), DeepInsightReport.class);
            report.setAnalysisDepth(totalEntries);
            report.setTrackerCount(trackers.size());
            report.setGeneratedAt(LocalDateTime.now().toString());
            report.setProvider(provider);

            // Persist to cache
            try {
                String reportJson = objectMapper.writeValueAsString(report);
                deepInsightCacheRepository.save(new DeepInsightCache(appId, reportJson));
            } catch (Exception cacheEx) {
                System.err.println("Deep Insight: Failed to save cache: " + cacheEx.getMessage());
            }

            return report;
        } catch (Exception e) {
            System.err.println("Deep Insight Generation Failed: " + e.getMessage());
            return buildErrorDeepReport("AI analysis failed: " + e.getMessage());
        }
    }

    public Map<String, String> chatWithApp(Long appId, Long userId, com.example.expensestracker.dto.ChatRequest request) {
        TrackerApp app = trackerAppRepository.findById(appId).orElse(null);
        if (app == null || !app.getUserId().equals(userId)) return null;

        List<Tracker> trackers = trackerRepository.findByUserIdAndAppId(userId, appId);
        int[] totalCount = {0};
        String dataContext = buildDeepDataContext(app, trackers, totalCount);

        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return null;

        String provider = user.getAiProvider() != null ? user.getAiProvider() : "GOOGLE";
        String model = user.getAiModel() != null ? user.getAiModel() : "gemini-2.0-flash";

        String activeApiKey = null;
        if ("GOOGLE".equalsIgnoreCase(provider)) {
            activeApiKey = (user.getGeminiApiKey() != null && !user.getGeminiApiKey().isEmpty()) ? user.getGeminiApiKey() : defaultGeminiKey;
        } else if ("ANTHROPIC".equalsIgnoreCase(provider)) {
            activeApiKey = (user.getAnthropicApiKey() != null && !user.getAnthropicApiKey().isEmpty()) ? user.getAnthropicApiKey() : defaultAnthropicKey;
        } else if ("OPENAI".equalsIgnoreCase(provider)) {
            activeApiKey = (user.getOpenaiApiKey() != null && !user.getOpenaiApiKey().isEmpty()) ? user.getOpenaiApiKey() : defaultOpenaiKey;
        }

        if (activeApiKey == null || activeApiKey.isEmpty()) {
            throw new RuntimeException("No active API Key found for " + provider);
        }

        String systemPrompt = "You are an ultra-modern, helpful AI Chatbot integrated into the OmniTracker Personal OS. " +
            "Your purpose is to answer the user's questions based on their application data context. " +
            "Be concise, insightful, and user-friendly. Format your responses in clean Markdown.";

        StringBuilder userPromptBuilder = new StringBuilder();
        userPromptBuilder.append("APP DATA CONTEXT:\n").append(dataContext).append("\n\n");
        userPromptBuilder.append("CHAT HISTORY:\n");
        if (request.getHistory() != null) {
            for (com.example.expensestracker.dto.ChatMessage msg : request.getHistory()) {
                userPromptBuilder.append(msg.getRole().toUpperCase()).append(": ").append(msg.getContent()).append("\n");
            }
        }
        userPromptBuilder.append("USER: ").append(request.getMessage()).append("\n");
        userPromptBuilder.append("ASSISTANT: ");

        String userPrompt = userPromptBuilder.toString();
        String rawResponse = "";

        try {
            if ("ANTHROPIC".equalsIgnoreCase(provider)) {
                rawResponse = callClaudeRawForDoc(userPrompt, systemPrompt, activeApiKey, model);
            } else if ("OPENAI".equalsIgnoreCase(provider)) {
                rawResponse = callOpenAiRawForDoc(userPrompt, systemPrompt, activeApiKey, model);
            } else {
                rawResponse = callGeminiRawForDoc(userPrompt, systemPrompt, activeApiKey, model);
            }
        } catch (Exception e) {
            System.err.println("Chat Generation Failed: " + e.getMessage());
            rawResponse = "AI Chat failed: " + e.getMessage();
        }

        Map<String, String> result = new java.util.HashMap<>();
        result.put("reply", rawResponse);
        return result;
    }

    private String buildDeepDataContext(TrackerApp app, List<Tracker> trackers, int[] totalCount) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== APP: ").append(app.getName()).append(" ===\n");
        sb.append("Description: ").append(app.getDescription() != null ? app.getDescription() : "N/A").append("\n\n");

        int total = 0;
        for (Tracker t : trackers) {
            sb.append("== TRACKER: ").append(t.getName()).append(" (Type: ").append(t.getType()).append(") ==\n");
            sb.append("Fields: ");
            if (t.getFieldDefinitions() != null) {
                for (Object def : t.getFieldDefinitions()) {
                    if (def instanceof Map) {
                        Map<?, ?> m = (Map<?, ?>) def;
                        sb.append(m.get("name")).append("(").append(m.get("type")).append(") ");
                    }
                }
            }
            sb.append("\n");

            List<TrackerEntry> entries = trackerEntryRepository.findByTrackerIdOrderByDateAsc(t.getId());
            total += entries.size();
            sb.append("Total entries: ").append(entries.size()).append("\n");

            if (!entries.isEmpty()) {
                sb.append("Date range: ").append(entries.get(0).getDate())
                  .append(" → ").append(entries.get(entries.size() - 1).getDate()).append("\n");

                // Compute simple numeric stats per field
                Map<String, List<Double>> numericFields = new LinkedHashMap<>();
                for (TrackerEntry e : entries) {
                    if (e.getFieldValues() != null) {
                        for (Map.Entry<String, Object> fv : e.getFieldValues().entrySet()) {
                            try {
                                double val = Double.parseDouble(fv.getValue().toString());
                                numericFields.computeIfAbsent(fv.getKey(), k -> new ArrayList<>()).add(val);
                            } catch (Exception ignored) {}
                        }
                    }
                }
                for (Map.Entry<String, List<Double>> nf : numericFields.entrySet()) {
                    List<Double> vals = nf.getValue();
                    double min = vals.stream().mapToDouble(Double::doubleValue).min().orElse(0);
                    double max = vals.stream().mapToDouble(Double::doubleValue).max().orElse(0);
                    double avg = vals.stream().mapToDouble(Double::doubleValue).average().orElse(0);
                    double first = vals.get(0);
                    double last = vals.get(vals.size() - 1);
                    String trend = last > first ? "↑ increasing" : last < first ? "↓ decreasing" : "→ stable";
                    sb.append(String.format("  [STATS] %s: min=%.2f, max=%.2f, avg=%.2f, trend=%s\n",
                        nf.getKey(), min, max, avg, trend));
                }

                // All entries (up to 200 for deep context)
                int start = Math.max(0, entries.size() - 200);
                sb.append("  All Data:\n");
                for (TrackerEntry e : entries.subList(start, entries.size())) {
                    sb.append("    ").append(e.getDate()).append(": ").append(e.getFieldValues()).append("\n");
                }
            }
            sb.append("\n");
        }
        totalCount[0] = total;
        return sb.toString();
    }

    private DeepInsightReport buildErrorDeepReport(String message) {
        DeepInsightReport report = new DeepInsightReport();
        report.setExecutiveSummary(message);
        report.setOverallScore(0);
        report.setScoreLabel("Error");
        report.setGeneratedAt(LocalDateTime.now().toString());
        report.setSections(Collections.emptyList());
        return report;
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
        // Switching to header-based auth to resolve 404 errors on Render
        String url = "https://generativelanguage.googleapis.com/v1beta/models/" + modelName + ":generateContent";
        
        System.out.println("AI Insight: Calling Gemini API (" + modelName + ") via Header Auth...");
        System.out.println("AI Insight: Target URL (masked): " + url.replace(activeApiKey, "REDACTED"));

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
        headers.set("x-goog-api-key", activeApiKey.trim());
        
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
    
    // --- Document to Tracker Generation ---
    public JsonNode generateTrackerFromDocument(String documentText, String trackerName, User user) throws Exception {
        String provider = user.getAiProvider() != null ? user.getAiProvider() : "GOOGLE";
        String model = user.getAiModel() != null ? user.getAiModel() : "gemini-2.0-flash";
        
        String activeApiKey = null;
        if ("GOOGLE".equalsIgnoreCase(provider)) {
            activeApiKey = (user.getGeminiApiKey() != null && !user.getGeminiApiKey().isEmpty()) ? user.getGeminiApiKey() : defaultGeminiKey;
        } else if ("ANTHROPIC".equalsIgnoreCase(provider)) {
            activeApiKey = (user.getAnthropicApiKey() != null && !user.getAnthropicApiKey().isEmpty()) ? user.getAnthropicApiKey() : defaultAnthropicKey;
        } else if ("OPENAI".equalsIgnoreCase(provider)) {
            activeApiKey = (user.getOpenaiApiKey() != null && !user.getOpenaiApiKey().isEmpty()) ? user.getOpenaiApiKey() : defaultOpenaiKey;
        }

        if (activeApiKey == null || activeApiKey.isEmpty()) {
            throw new RuntimeException("No active API Key found for " + provider);
        }

        String systemPrompt = "You are an intelligent data architect inside a Personal OS dashboard. " +
                "You are provided with raw text extracted from a user's document (such as a CSV or PDF). " +
                "Your task is to:\n" +
                "1. Infer a logical Tracker Schema to hold this data. If the user provided a name, use it, otherwise invent an appropriate one.\n" +
                "2. Determine the 'type' of tracker: FINANCE, HEALTH, STOCK, or CUSTOM.\n" +
                "3. Define the 'fieldDefinitions' which must be an array of objects. Available types for fields: NUMBER, CURRENCY, TEXT, LONG_TEXT, DATE, TIME, BOOLEAN, SELECT, RATING.\n" +
                "4. Extract ALL logical rows of data from the raw text as 'entries'. Each entry should map string field names exactly to string/number values.\n" +
                "Return ONLY a raw JSON object with this exact structure:\n" +
                "{\n" +
                "  \"name\": \"Tracker Name\",\n" +
                "  \"type\": \"Tracker Type\",\n" +
                "  \"icon\": \"Emoji for the tracker\",\n" +
                "  \"fieldDefinitions\": [\n" +
                "    { \"name\": \"FieldName\", \"type\": \"FIELD_TYPE\" }\n" +
                "  ],\n" +
                "  \"entries\": [\n" +
                "    { \"FieldName\": \"Value1\", ... }\n" +
                "  ]\n" +
                "}";

        String userPrompt = "User Suggested Tracker Name (if any): " + (trackerName != null ? trackerName : "None") + "\n\n" +
                            "DOCUMENT TEXT:\n" + documentText;

        String rawJson = "";
        int retries = 2;
        while (true) {
            try {
                if ("GOOGLE".equalsIgnoreCase(provider)) {
                    rawJson = callGeminiRawForDoc(userPrompt, systemPrompt, activeApiKey, model);
                } else if ("ANTHROPIC".equalsIgnoreCase(provider)) {
                    rawJson = callClaudeRawForDoc(userPrompt, systemPrompt, activeApiKey, model);
                } else if ("OPENAI".equalsIgnoreCase(provider)) {
                    rawJson = callOpenAiRawForDoc(userPrompt, systemPrompt, activeApiKey, model);
                }
                break;
            } catch (Exception e) {
                if (retries > 0 && e.getMessage().contains("503")) {
                    retries--;
                    Thread.sleep(3000);
                } else {
                    throw e;
                }
            }
        }

        return objectMapper.readTree(cleanJson(rawJson));
    }

    public JsonNode extractEntriesForTracker(String documentText, Tracker tracker, User user) throws Exception {
        String provider = user.getAiProvider() != null ? user.getAiProvider() : "GOOGLE";
        String model = user.getAiModel() != null ? user.getAiModel() : "gemini-2.0-flash";
        
        String activeApiKey = null;
        if ("GOOGLE".equalsIgnoreCase(provider)) {
            activeApiKey = (user.getGeminiApiKey() != null && !user.getGeminiApiKey().isEmpty()) ? user.getGeminiApiKey() : defaultGeminiKey;
        } else if ("ANTHROPIC".equalsIgnoreCase(provider)) {
            activeApiKey = (user.getAnthropicApiKey() != null && !user.getAnthropicApiKey().isEmpty()) ? user.getAnthropicApiKey() : defaultAnthropicKey;
        } else if ("OPENAI".equalsIgnoreCase(provider)) {
            activeApiKey = (user.getOpenaiApiKey() != null && !user.getOpenaiApiKey().isEmpty()) ? user.getOpenaiApiKey() : defaultOpenaiKey;
        }

        if (activeApiKey == null || activeApiKey.isEmpty()) {
            throw new RuntimeException("No active API Key found for " + provider);
        }

        StringBuilder fieldsSchema = new StringBuilder();
        if (tracker.getFieldDefinitions() != null) {
            for (Object def : tracker.getFieldDefinitions()) {
                if (def instanceof Map) {
                    Map<?, ?> fieldMap = (Map<?, ?>) def;
                    fieldsSchema.append("- Name: '").append(fieldMap.get("name"))
                                .append("', Type: '").append(fieldMap.get("type")).append("'\n");
                }
            }
        }

        String systemPrompt = "You are a precise data extractor tool. " +
                "Your ONLY task is to parse raw text from a document and map it to an exact JSON array of objects based on a PRE-EXISTING schema.\n" +
                "You must use EXACTLY the field names provided. Do not invent new fields. Ignore data that does not fit the schema.\n" +
                "TARGET SCHEMA FIELDS:\n" + fieldsSchema.toString() + "\n" +
                "Return ONLY a raw JSON array of objects, e.g.:\n" +
                "[\n  { \"Field1\": \"Value1\", \"Field2\": \"Value2\" }\n]";

        String userPrompt = "DOCUMENT TEXT:\n" + documentText;

        String rawJson = "";
        int retries = 2;
        while (true) {
            try {
                if ("GOOGLE".equalsIgnoreCase(provider)) {
                    rawJson = callGeminiRawForDoc(userPrompt, systemPrompt, activeApiKey, model);
                } else if ("ANTHROPIC".equalsIgnoreCase(provider)) {
                    rawJson = callClaudeRawForDoc(userPrompt, systemPrompt, activeApiKey, model);
                } else if ("OPENAI".equalsIgnoreCase(provider)) {
                    rawJson = callOpenAiRawForDoc(userPrompt, systemPrompt, activeApiKey, model);
                }
                break;
            } catch (Exception e) {
                if (retries > 0 && e.getMessage().contains("503")) {
                    retries--;
                    Thread.sleep(3000);
                } else {
                    throw e;
                }
            }
        }

        return objectMapper.readTree(cleanJson(rawJson));
    }

    public Map<String, String> learnCsvMapping(String csvHeaders, Tracker tracker, User user) throws Exception {
        String provider = user.getAiProvider() != null ? user.getAiProvider() : "GOOGLE";
        String model = user.getAiModel() != null ? user.getAiModel() : "gemini-2.0-flash";
        
        String activeApiKey = null;
        if ("GOOGLE".equalsIgnoreCase(provider)) {
            activeApiKey = (user.getGeminiApiKey() != null && !user.getGeminiApiKey().isEmpty()) ? user.getGeminiApiKey() : defaultGeminiKey;
        } else if ("ANTHROPIC".equalsIgnoreCase(provider)) {
            activeApiKey = (user.getAnthropicApiKey() != null && !user.getAnthropicApiKey().isEmpty()) ? user.getAnthropicApiKey() : defaultAnthropicKey;
        } else if ("OPENAI".equalsIgnoreCase(provider)) {
            activeApiKey = (user.getOpenaiApiKey() != null && !user.getOpenaiApiKey().isEmpty()) ? user.getOpenaiApiKey() : defaultOpenaiKey;
        }

        if (activeApiKey == null || activeApiKey.isEmpty()) {
            throw new RuntimeException("No active API Key found for " + provider);
        }

        StringBuilder fieldsSchema = new StringBuilder();
        if (tracker.getFieldDefinitions() != null) {
            for (Object def : tracker.getFieldDefinitions()) {
                if (def instanceof Map) {
                    Map<?, ?> fieldMap = (Map<?, ?>) def;
                    fieldsSchema.append("- Name: '").append(fieldMap.get("name"))
                                .append("', Type: '").append(fieldMap.get("type")).append("'\n");
                }
            }
        }

        String systemPrompt = "You are a CSV mapping engine. " +
                "I will provide you with a list of CSV HEADERS. " +
                "Your job is to map these headers to the closest matching TARGET SCHEMA FIELDS.\n" +
                "TARGET SCHEMA FIELDS:\n" + fieldsSchema.toString() + "\n" +
                "Return ONLY a raw JSON object where Keys are the CSV HEADERS, and Values are the EXACT TARGET SCHEMA FIELDS they map to. " +
                "If a header does not match any field, skip it. Do not include it in the object.\n" +
                "Example Answer: { \"Transaction Date\": \"Date\", \"Cost\": \"Amount\" }";

        String userPrompt = "CSV HEADERS:\n" + csvHeaders;

        String rawJson = "";
        int retries = 2;
        while (true) {
            try {
                if ("GOOGLE".equalsIgnoreCase(provider)) {
                    rawJson = callGeminiRawForDoc(userPrompt, systemPrompt, activeApiKey, model);
                } else if ("ANTHROPIC".equalsIgnoreCase(provider)) {
                    rawJson = callClaudeRawForDoc(userPrompt, systemPrompt, activeApiKey, model);
                } else if ("OPENAI".equalsIgnoreCase(provider)) {
                    rawJson = callOpenAiRawForDoc(userPrompt, systemPrompt, activeApiKey, model);
                }
                break; // Break if successful
            } catch (Exception e) {
                if (retries > 0 && e.getMessage().contains("503")) {
                    retries--;
                    Thread.sleep(3000); // Wait 3 seconds
                } else {
                    throw e;
                }
            }
        }

        JsonNode root = objectMapper.readTree(cleanJson(rawJson));
        Map<String, String> mapping = new java.util.HashMap<>();
        if (root.isObject()) {
            java.util.Iterator<Map.Entry<String, JsonNode>> fields = root.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                mapping.put(field.getKey(), field.getValue().asText());
            }
        }
        return mapping;
    }

    public Map<String, String> suggestPlaidMapping(Tracker tracker, User user) throws Exception {
        String provider = user.getAiProvider() != null ? user.getAiProvider() : "GOOGLE";
        String model = user.getAiModel() != null ? user.getAiModel() : "gemini-2.0-flash";

        String activeApiKey = null;
        if ("GOOGLE".equalsIgnoreCase(provider)) {
            activeApiKey = (user.getGeminiApiKey() != null && !user.getGeminiApiKey().isEmpty()) ? user.getGeminiApiKey() : defaultGeminiKey;
        } else if ("ANTHROPIC".equalsIgnoreCase(provider)) {
            activeApiKey = (user.getAnthropicApiKey() != null && !user.getAnthropicApiKey().isEmpty()) ? user.getAnthropicApiKey() : defaultAnthropicKey;
        } else if ("OPENAI".equalsIgnoreCase(provider)) {
            activeApiKey = (user.getOpenaiApiKey() != null && !user.getOpenaiApiKey().isEmpty()) ? user.getOpenaiApiKey() : defaultOpenaiKey;
        }

        if (activeApiKey == null || activeApiKey.isEmpty()) {
            throw new RuntimeException("No active API Key found for " + provider);
        }

        StringBuilder fieldsSchema = new StringBuilder();
        if (tracker.getFieldDefinitions() != null) {
            for (Object def : tracker.getFieldDefinitions()) {
                if (def instanceof Map) {
                    Map<?, ?> fieldMap = (Map<?, ?>) def;
                    fieldsSchema.append("- Name: '").append(fieldMap.get("name"))
                                .append("', Type: '").append(fieldMap.get("type")).append("'\n");
                }
            }
        }

        String systemPrompt = "You are a mapping engine for a banking aggregation API. " +
                "I will provide you with standard Plaid bank fields: amount, name, date, merchant_name, category, payment_channel. " +
                "Your job is to smartly map these Plaid fields to the closest matching TARGET SCHEMA FIELDS in a private finance tracker.\n" +
                "TARGET SCHEMA FIELDS:\n" + fieldsSchema.toString() + "\n" +
                "Return ONLY a raw JSON object where Keys are the Plaid fields, and Values are the EXACT TARGET SCHEMA FIELDS they map to. " +
                "Only map fields you are extremely confident about. A target schema field can only be used once.\n" +
                "Example Answer: { \"amount\": \"Cost\", \"name\": \"Vendor\", \"date\": \"Date\" }";

        String userPrompt = "STANDARD PLAID FIELDS:\namount, name, date, merchant_name, category, payment_channel";

        String rawJson = "";
        int retries = 2;
        while (true) {
            try {
                if ("GOOGLE".equalsIgnoreCase(provider)) {
                    rawJson = callGeminiRawForDoc(userPrompt, systemPrompt, activeApiKey, model);
                } else if ("ANTHROPIC".equalsIgnoreCase(provider)) {
                    rawJson = callClaudeRawForDoc(userPrompt, systemPrompt, activeApiKey, model);
                } else if ("OPENAI".equalsIgnoreCase(provider)) {
                    rawJson = callOpenAiRawForDoc(userPrompt, systemPrompt, activeApiKey, model);
                }
                break;
            } catch (Exception e) {
                if (retries > 0 && e.getMessage() != null && e.getMessage().contains("503")) {
                    retries--;
                    Thread.sleep(3000);
                } else {
                    throw e;
                }
            }
        }

        Map<String, String> mapping = new java.util.HashMap<>();
        JsonNode root = objectMapper.readTree(cleanJson(rawJson));
        if (root.isObject()) {
            java.util.Iterator<Map.Entry<String, JsonNode>> fields = root.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                mapping.put(field.getKey(), field.getValue().asText());
            }
        }
        return mapping;
    }

    private String callGeminiRawForDoc(String userPrompt, String systemPrompt, String apiKey, String modelName) throws Exception {
        String url = "https://generativelanguage.googleapis.com/v1beta/models/" + modelName + ":generateContent";
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("systemInstruction", Map.of("parts", List.of(Map.of("text", systemPrompt))));
        requestBody.put("contents", List.of(Map.of("parts", List.of(Map.of("text", userPrompt)))));
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-goog-api-key", apiKey);
        
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
        
        if (response.getStatusCode() == HttpStatus.OK) {
            JsonNode root = objectMapper.readTree(response.getBody());
            return cleanJson(root.path("candidates").get(0).path("content").path("parts").get(0).path("text").asText().trim());
        }
        throw new RuntimeException("Gemini API failed: " + response.getStatusCode());
    }

    private String callClaudeRawForDoc(String userPrompt, String systemPrompt, String apiKey, String modelName) throws Exception {
        String url = "https://api.anthropic.com/v1/messages";
        Map<String, Object> requestBody = Map.of(
                "model", modelName,
                "max_tokens", 8192,
                "system", systemPrompt,
                "messages", List.of(Map.of("role", "user", "content", userPrompt))
        );
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-api-key", apiKey);
        headers.set("anthropic-version", "2023-06-01");
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
        if (response.getStatusCode() == HttpStatus.OK) {
            JsonNode root = objectMapper.readTree(response.getBody());
            return cleanJson(root.path("content").get(0).path("text").asText().trim());
        }
        throw new RuntimeException("Claude API failed: " + response.getStatusCode());
    }

    private String callOpenAiRawForDoc(String userPrompt, String systemPrompt, String apiKey, String modelName) throws Exception {
        String url = "https://api.openai.com/v1/chat/completions";
        Map<String, Object> requestBody = Map.of(
                "model", modelName,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)
                )
        );
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKey);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
        if (response.getStatusCode() == HttpStatus.OK) {
            JsonNode root = objectMapper.readTree(response.getBody());
            return cleanJson(root.path("choices").get(0).path("message").path("content").asText().trim());
        }
        throw new RuntimeException("OpenAI API failed: " + response.getStatusCode());
    }
}
