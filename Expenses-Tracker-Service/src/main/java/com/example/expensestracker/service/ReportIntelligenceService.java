package com.example.expensestracker.service;

import com.example.expensestracker.model.*;
import com.example.expensestracker.repository.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReportIntelligenceService {

    private final AiReportRepository aiReportRepository;
    private final TrackerRepository trackerRepository;
    private final TrackerEntryRepository trackerEntryRepository;
    private final TrackerAppRepository trackerAppRepository;
    private final AiInsightService aiInsightService; // Reuse AI calling logic
    private final ObjectMapper objectMapper;

    public ReportIntelligenceService(AiReportRepository aiReportRepository,
                                     TrackerRepository trackerRepository,
                                     TrackerEntryRepository trackerEntryRepository,
                                     TrackerAppRepository trackerAppRepository,
                                     AiInsightService aiInsightService,
                                     ObjectMapper objectMapper) {
        this.aiReportRepository = aiReportRepository;
        this.trackerRepository = trackerRepository;
        this.trackerEntryRepository = trackerEntryRepository;
        this.trackerAppRepository = trackerAppRepository;
        this.aiInsightService = aiInsightService;
        this.objectMapper = objectMapper;
    }

    /**
     * AI-Driven Report Design: Suggests report architectures based on selected trackers.
     */
    public List<Map<String, Object>> suggestReports(Long appId, Long userId, List<Long> trackerIds) throws Exception {
        TrackerApp app = trackerAppRepository.findById(appId).orElseThrow();
        List<Tracker> selectedTrackers = trackerRepository.findAllById(trackerIds);
        
        StringBuilder schemaContext = new StringBuilder();
        schemaContext.append("APP: ").append(app.getName()).append("\n");
        for (Tracker t : selectedTrackers) {
            schemaContext.append("TRACKER: ").append(t.getName()).append(" (Type: ").append(t.getType()).append(")\n");
            schemaContext.append("FIELDS: ").append(t.getFieldDefinitions()).append("\n\n");
        }

        String systemPrompt = "You are a Senior Business Intelligence Architect for the OmniTracker platform. " +
            "Your job is to ARCHITECT 3-5 highly impactful, modern analytical reports based on the user's tracker schemas. " +
            "Output MUST be a RAW JSON ARRAY ONLY. NO conversational text, NO headers, NO summary. " +
            "Follow this exact structure:\n" +
            "[\n" +
            "  {\n" +
            "    \"name\": \"Clear Report Title\",\n" +
            "    \"description\": \"One sentence on the value of this report\",\n" +
            "    \"visualType\": \"BAR|PIE|LINE|RADAR|METRIC_GRID\",\n" +
            "    \"querySpec\": {\n" +
            "      \"trackers\": [\"tracker_name1\", \"tracker_name2\"],\n" +
            "      \"aggregate\": {\"type\": \"SUM|AVG|COUNT|DISTINCT_COUNT\", \"field\": \"field_name\"},\n" +
            "      \"groupBy\": \"field_name_or_date_part_like_MONTH\",\n" +
            "      \"filter\": []\n" +
            "    },\n" +
            "    \"config\": {\n" +
            "      \"xAxis\": \"Label\", \"yAxis\": \"Label\", \"colorPalette\": \"cool|warm|neo\",\n" +
            "      \"suggestedInsights\": [\"Why this metric matters\"]\n" +
            "    }\n" +
            "  }\n" +
            "]";

        String userPrompt = "SCHEMA CONTEXT:\n" + schemaContext.toString();
        
        // Wrap for chatWithApp
        com.example.expensestracker.dto.ChatRequest request = new com.example.expensestracker.dto.ChatRequest();
        request.setMessage(systemPrompt + "\n\n" + userPrompt);
        
        Map<String, String> aiRes = aiInsightService.chatWithApp(appId, userId, request);
        String rawJson = aiRes.get("reply");
        
        if (rawJson != null && rawJson.startsWith("ERROR:")) {
            throw new RuntimeException(rawJson.substring(6).trim());
        }
        
        return objectMapper.readValue(cleanJson(rawJson), new TypeReference<List<Map<String, Object>>>() {});
    }

    private String cleanJson(String aiText) {
        if (aiText.contains("```json")) {
            aiText = aiText.substring(aiText.indexOf("```json") + 7, aiText.lastIndexOf("```")).trim();
        } else if (aiText.contains("```")) {
            aiText = aiText.substring(aiText.indexOf("```") + 3, aiText.lastIndexOf("```")).trim();
        } else {
            // Fallback: Find the first '[' and last ']' to extract the JSON array from surrounding text
            int start = aiText.indexOf("[");
            int end = aiText.lastIndexOf("]");
            if (start != -1 && end != -1 && end > start) {
                aiText = aiText.substring(start, end + 1).trim();
            }
        }
        return aiText;
    }

    /**
     * The Intelligence Engine: Executes a stored QuerySpec against live data.
     */
    public Map<String, Object> executeReport(AiReport report) {
        List<Long> trackerIds = new ArrayList<>();
        Map<String, Object> spec = report.getQuerySpec();
        List<String> trackerNames = (List<String>) spec.get("trackers");
        
        List<Tracker> trackers = trackerRepository.findByAppIdAndUserId(report.getAppId(), report.getUserId());
        List<Tracker> targetTrackers = trackers.stream()
            .filter(t -> trackerNames.contains(t.getName()))
            .collect(Collectors.toList());

        List<TrackerEntry> allEntries = new ArrayList<>();
        for (Tracker t : targetTrackers) {
            allEntries.addAll(trackerEntryRepository.findByTrackerIdOrderByDateAsc(t.getId()));
        }

        // --- THE ENGINE LOGIC ---
        // 1. Grouping
        String groupBy = (String) spec.get("groupBy");
        Map<String, Object> agg = (Map<String, Object>) spec.get("aggregate");
        String aggType = (String) agg.get("type");
        String aggField = (String) agg.get("field");

        Map<String, List<TrackerEntry>> grouped;
        if ("MONTH".equals(groupBy)) {
            grouped = allEntries.stream().collect(Collectors.groupingBy(e -> e.getDate().getMonth().name()));
        } else {
            grouped = allEntries.stream().collect(Collectors.groupingBy(e -> {
                Object val = e.getFieldValues().get(groupBy);
                return val != null ? val.toString() : "Unknown";
            }));
        }

        // 2. Aggregation
        List<String> labels = new ArrayList<>(grouped.keySet());
        List<Double> values = new ArrayList<>();

        for (String key : labels) {
            List<TrackerEntry> groupEntries = grouped.get(key);
            double result = 0;
            
            if ("SUM".equals(aggType)) {
                result = groupEntries.stream()
                    .mapToDouble(e -> Double.parseDouble(e.getFieldValues().getOrDefault(aggField, 0).toString()))
                    .sum();
            } else if ("AVG".equals(aggType)) {
                result = groupEntries.stream()
                    .mapToDouble(e -> Double.parseDouble(e.getFieldValues().getOrDefault(aggField, 0).toString()))
                    .average().orElse(0);
            } else if ("COUNT".equals(aggType)) {
                result = groupEntries.size();
            }
            values.add(result);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("labels", labels);
        result.put("series", List.of(Map.of("name", report.getName(), "data", values)));
        result.put("config", report.getConfig());
        result.put("visualType", report.getVisualType());
        
        return result;
    }
}
