package com.example.expensestracker.controller;

import com.example.expensestracker.model.Tracker;
import com.example.expensestracker.model.TrackerApp;
import com.example.expensestracker.model.TrackerEntry;
import com.example.expensestracker.model.TrackerType;
import com.example.expensestracker.repository.TrackerAppRepository;
import com.example.expensestracker.repository.TrackerEntryRepository;
import com.example.expensestracker.repository.TrackerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import com.opencsv.CSVReader;
import com.example.expensestracker.service.AiInsightService;
import com.example.expensestracker.repository.TrackerMappingRepository;
import com.example.expensestracker.model.TrackerMapping;
import com.example.expensestracker.model.TrackerIntegration;
import com.example.expensestracker.service.PlaidIntegrationService;
import com.example.expensestracker.model.User;
import com.example.expensestracker.repository.UserRepository;

import java.io.InputStreamReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

@RestController
@RequestMapping("/api/omni")
@CrossOrigin(origins = "*")
public class OmniTrackerController {

    @Autowired
    private TrackerRepository trackerRepository;

    @Autowired
    private TrackerEntryRepository entryRepository;

    @Autowired
    private TrackerAppRepository appRepository;

    @Autowired
    private AiInsightService aiInsightService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TrackerMappingRepository trackerMappingRepository;

    @Autowired
    private PlaidIntegrationService plaidService;

    @Autowired
    private ObjectMapper objectMapper;

    // --- Apps ---

    @GetMapping("/apps")
    public List<TrackerApp> getApps(@RequestAttribute("userId") Long userId) {
        return appRepository.findByUserId(userId);
    }

    @PostMapping("/apps")
    public TrackerApp createApp(@RequestAttribute("userId") Long userId, @RequestBody TrackerApp app) {
        app.setUserId(userId);
        return appRepository.save(app);
    }

    @PutMapping("/apps/{id}")
    public ResponseEntity<TrackerApp> updateApp(@RequestAttribute("userId") Long userId, @PathVariable Long id, @RequestBody TrackerApp updatedApp) {
        return appRepository.findById(id).map(app -> {
            if (app.getUserId().equals(userId)) {
                app.setName(updatedApp.getName());
                app.setIcon(updatedApp.getIcon());
                app.setDescription(updatedApp.getDescription());
                app.setColorStyle(updatedApp.getColorStyle());
                return ResponseEntity.ok(appRepository.save(app));
            }
            return ResponseEntity.status(403).<TrackerApp>build();
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/apps/{id}")
    public ResponseEntity<?> deleteApp(@RequestAttribute("userId") Long userId, @PathVariable Long id) {
        return appRepository.findById(id).map(app -> {
            if (app.getUserId().equals(userId)) {
                // Cascading delete trackers in this app
                List<Tracker> trackers = trackerRepository.findByUserIdAndAppId(userId, id);
                trackerRepository.deleteAll(trackers);
                appRepository.delete(app);
                return ResponseEntity.ok().build();
            }
            return ResponseEntity.status(403).build();
        }).orElse(ResponseEntity.notFound().build());
    }

    // --- Trackers ---

    @GetMapping("/trackers")
    public List<Tracker> getAllTrackers(@RequestAttribute("userId") Long userId, @RequestParam(required = false) Long appId) {
        if (appId != null) {
            return trackerRepository.findByUserIdAndAppId(userId, appId);
        }
        return trackerRepository.findByUserId(userId);
    }

    @GetMapping("/trackers/type/{type}")
    public List<Tracker> getTrackersByType(@RequestAttribute("userId") Long userId, @PathVariable TrackerType type) {
        return trackerRepository.findByUserIdAndType(userId, type);
    }

    @PostMapping("/trackers")
    public Tracker createTracker(@RequestAttribute("userId") Long userId, @RequestBody Tracker tracker) {
        tracker.setUserId(userId);
        return trackerRepository.save(tracker);
    }

    @PutMapping("/trackers/{id}")
    public ResponseEntity<Tracker> updateTracker(@RequestAttribute("userId") Long userId, @PathVariable Long id, @RequestBody Tracker updatedTracker) {
        return trackerRepository.findById(id).map(tracker -> {
            if (tracker.getUserId().equals(userId)) {
                tracker.setName(updatedTracker.getName());
                tracker.setType(updatedTracker.getType());
                tracker.setIcon(updatedTracker.getIcon());
                tracker.setMetadata(updatedTracker.getMetadata());
                tracker.setFieldDefinitions(updatedTracker.getFieldDefinitions());
                return ResponseEntity.ok(trackerRepository.save(tracker));
            }
            return ResponseEntity.status(403).<Tracker>build();
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/trackers/{id}")
    public ResponseEntity<?> deleteTracker(@RequestAttribute("userId") Long userId, @PathVariable Long id) {
        return trackerRepository.findById(id).map(tracker -> {
            if (tracker.getUserId().equals(userId)) {
                trackerRepository.delete(tracker);
                return ResponseEntity.ok().build();
            }
            return ResponseEntity.status(403).build();
        }).orElse(ResponseEntity.notFound().build());
    }

    // --- Entries ---

    @GetMapping("/entries/tracker/{trackerId}")
    public List<TrackerEntry> getEntriesByTracker(@RequestAttribute("userId") Long userId, @PathVariable Long trackerId) {
        return entryRepository.findByTrackerIdOrderByDateAsc(trackerId);
    }

    @PostMapping("/entries")
    public TrackerEntry addEntry(@RequestAttribute("userId") Long userId, @RequestBody TrackerEntry entry) {
        entry.setUserId(userId);
        return entryRepository.save(entry);
    }

    @PutMapping("/entries/{id}")
    public ResponseEntity<?> updateEntry(@RequestAttribute("userId") Long userId, @PathVariable Long id, @RequestBody TrackerEntry updatedEntry) {
        return entryRepository.findById(id).map(entry -> {
            if (entry.getUserId().equals(userId)) {
                entry.setFieldValues(updatedEntry.getFieldValues());
                entry.setNote(updatedEntry.getNote());
                entryRepository.save(entry);
                return ResponseEntity.ok(entry);
            }
            return ResponseEntity.status(403).build();
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/entries/{id}")
    public ResponseEntity<?> deleteEntry(@RequestAttribute("userId") Long userId, @PathVariable Long id) {
        return entryRepository.findById(id).map(entry -> {
            if (entry.getUserId().equals(userId)) {
                entryRepository.delete(entry);
                return ResponseEntity.ok().build();
            }
            return ResponseEntity.status(403).build();
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/entries/tracker/{trackerId}")
    public ResponseEntity<?> deleteAllEntries(@RequestAttribute("userId") Long userId, @PathVariable Long trackerId) {
        List<TrackerEntry> entries = entryRepository.findByTrackerIdAndUserId(trackerId, userId);
        if (!entries.isEmpty()) {
            entryRepository.deleteAll(entries);
        }
        return ResponseEntity.ok().build();
    }

    // --- Document Import AI ---
    @PostMapping("/trackers/import")
    public ResponseEntity<?> importTrackerFromDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam("appId") Long appId,
            @RequestParam(value = "trackerName", required = false) String trackerName,
            @RequestAttribute("userId") Long userId) {

        try {
            User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
            TrackerApp app = appRepository.findById(appId).orElseThrow(() -> new RuntimeException("App not found"));

            if (!app.getUserId().equals(userId)) {
                return ResponseEntity.status(403).body("Access denied to this app.");
            }

            String documentText = extractTextFromFile(file);
            if (documentText == null || documentText.isEmpty()) {
                return ResponseEntity.badRequest().body("Could not extract text from the provided file.");
            }

            JsonNode aiResult = aiInsightService.generateTrackerFromDocument(documentText, trackerName, user);

            // Create Tracker
            Tracker tracker = new Tracker();
            tracker.setUserId(userId);
            tracker.setAppId(appId);
            tracker.setName(aiResult.path("name").asText("AI Imported Tracker"));
            tracker.setIcon(aiResult.path("icon").asText("📄"));
            
            try {
                tracker.setType(TrackerType.valueOf(aiResult.path("type").asText("CUSTOM").toUpperCase()));
            } catch (Exception e) {
                tracker.setType(TrackerType.CUSTOM);
            }

            JsonNode fieldsNode = aiResult.path("fieldDefinitions");
            List<Object> fieldDefs = new ArrayList<>();
            if (fieldsNode.isArray()) {
                for (JsonNode field : fieldsNode) {
                    Map<String, Object> fd = new HashMap<>();
                    fd.put("name", field.path("name").asText());
                    fd.put("type", field.path("type").asText("TEXT"));
                    if (field.has("options")) fd.put("options", field.path("options").asText());
                    fieldDefs.add(fd);
                }
            }
            tracker.setFieldDefinitions(fieldDefs);
            tracker = trackerRepository.save(tracker);

            // Create Entries
            JsonNode entriesNode = aiResult.path("entries");
            
            int entryCount = 0;
            int skippedCount = 0;
            List<TrackerEntry> existingEntries = new ArrayList<>(); // To track internal duplicates
            
            if (entriesNode.isArray()) {
                for (JsonNode entryRow : entriesNode) {
                    Map<String, Object> fieldValues = new HashMap<>();
                    Iterator<String> fieldNames = entryRow.fieldNames();
                    while (fieldNames.hasNext()) {
                        String fieldName = fieldNames.next();
                        JsonNode valNode = entryRow.get(fieldName);
                        
                        // Parse values based on field schema types roughly
                        if (valNode.isNumber()) {
                            fieldValues.put(fieldName, valNode.asDouble());
                        } else if (valNode.isBoolean()) {
                            fieldValues.put(fieldName, valNode.asBoolean());
                        } else {
                            fieldValues.put(fieldName, valNode.asText());
                        }
                    }
                    
                    boolean isDuplicate = false;
                    for (TrackerEntry existing : existingEntries) {
                        if (existing.getFieldValues() != null && existing.getFieldValues().equals(fieldValues)) {
                            isDuplicate = true;
                            break;
                        }
                    }
                    
                    if (isDuplicate) {
                        skippedCount++;
                    } else {
                        TrackerEntry entry = new TrackerEntry();
                        entry.setTrackerId(tracker.getId());
                        entry.setUserId(userId);
                        entry.setFieldValues(fieldValues);
                        entryRepository.save(entry);
                        entryCount++;
                    }
                }
            }

            Map<String, Object> response = new HashMap<>();
            response.put("tracker", tracker);
            response.put("entryCount", entryCount);
            response.put("skippedCount", skippedCount);
            
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Error importing document: " + e.getMessage());
        }
    }

    @PostMapping("/entries/import")
    public ResponseEntity<?> importEntriesForTracker(
            @RequestParam("file") MultipartFile file,
            @RequestParam("trackerId") Long trackerId,
            @RequestAttribute("userId") Long userId) {

        try {
            User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
            Tracker tracker = trackerRepository.findById(trackerId).orElseThrow(() -> new RuntimeException("Tracker not found"));

            if (!tracker.getUserId().equals(userId)) {
                return ResponseEntity.status(403).body("Access denied to this tracker.");
            }

            String filename = file.getOriginalFilename() != null ? file.getOriginalFilename().toLowerCase() : "";
            boolean isCsv = filename.endsWith(".csv") || (file.getContentType() != null && file.getContentType().contains("csv"));

            List<TrackerEntry> existingEntries = entryRepository.findByTrackerIdAndUserId(tracker.getId(), userId);
            
            int entryCount = 0;
            int skippedCount = 0;

            if (isCsv) {
                try (CSVReader csvReader = new CSVReader(new java.io.InputStreamReader(file.getInputStream()))) {
                    List<String[]> allData = csvReader.readAll();
                    if (!allData.isEmpty()) {
                        String[] headers = allData.get(0);
                        String headersString = String.join(",", headers);
                        
                        Map<String, Object> colMap;
                        java.util.Optional<TrackerMapping> optMapping = trackerMappingRepository.findByTrackerIdAndCsvHeaders(tracker.getId(), headersString);
                        if (optMapping.isPresent()) {
                            colMap = optMapping.get().getColumnMapping();
                        } else {
                            Map<String, String> learned = aiInsightService.learnCsvMapping(headersString, tracker, user);
                            colMap = new HashMap<>(learned);
                            TrackerMapping mapping = new TrackerMapping();
                            mapping.setTrackerId(tracker.getId());
                            mapping.setCsvHeaders(headersString);
                            mapping.setColumnMapping(colMap);
                            trackerMappingRepository.save(mapping);
                        }

                        for (int i = 1; i < allData.size(); i++) {
                            String[] row = allData.get(i);
                            Map<String, Object> fieldValues = new HashMap<>();
                            for (int j = 0; j < headers.length && j < row.length; j++) {
                                String header = headers[j];
                                if (colMap.containsKey(header)) {
                                    String targetField = String.valueOf(colMap.get(header));
                                    String val = row[j].trim();
                                    if (val.isEmpty()) continue;
                                    
                                    // Basic type inference
                                    try {
                                        if (val.equalsIgnoreCase("true") || val.equalsIgnoreCase("false")) {
                                            fieldValues.put(targetField, Boolean.parseBoolean(val));
                                        } else if (val.matches("-?\\d+(\\.\\d+)?")) {
                                            fieldValues.put(targetField, Double.parseDouble(val));
                                        } else if (val.startsWith("$")) {
                                            fieldValues.put(targetField, Double.parseDouble(val.replace("$", "").replace(",", "")));
                                        } else {
                                            fieldValues.put(targetField, val);
                                        }
                                    } catch (Exception e) {
                                        fieldValues.put(targetField, val);
                                    }
                                }
                            }
                            
                            if (fieldValues.isEmpty()) continue;

                            boolean isDuplicate = false;
                            for (TrackerEntry existing : existingEntries) {
                                if (existing.getFieldValues() != null && existing.getFieldValues().equals(fieldValues)) {
                                    isDuplicate = true;
                                    break;
                                }
                            }
                            
                            if (isDuplicate) {
                                skippedCount++;
                            } else {
                                TrackerEntry entry = new TrackerEntry();
                                entry.setTrackerId(tracker.getId());
                                entry.setUserId(userId);
                                entry.setFieldValues(fieldValues);
                                entryRepository.save(entry);
                                entryCount++;
                            }
                        }
                    }
                }
            } else {
                String documentText = extractTextFromFile(file);
                if (documentText == null || documentText.isEmpty()) {
                    return ResponseEntity.badRequest().body("Could not extract text from the provided file.");
                }

                com.fasterxml.jackson.databind.JsonNode entriesNode = aiInsightService.extractEntriesForTracker(documentText, tracker, user);
                
                if (entriesNode.isArray()) {
                    for (com.fasterxml.jackson.databind.JsonNode entryRow : entriesNode) {
                        Map<String, Object> fieldValues = new HashMap<>();
                        java.util.Iterator<String> fieldNames = entryRow.fieldNames();
                        while (fieldNames.hasNext()) {
                            String fieldName = fieldNames.next();
                            com.fasterxml.jackson.databind.JsonNode valNode = entryRow.get(fieldName);
                            
                            if (valNode.isNumber()) {
                                fieldValues.put(fieldName, valNode.asDouble());
                            } else if (valNode.isBoolean()) {
                                fieldValues.put(fieldName, valNode.asBoolean());
                            } else {
                                fieldValues.put(fieldName, valNode.asText());
                            }
                        }
                        
                        boolean isDuplicate = false;
                        for (TrackerEntry existing : existingEntries) {
                            if (existing.getFieldValues() != null && existing.getFieldValues().equals(fieldValues)) {
                                isDuplicate = true;
                                break;
                            }
                        }
                        
                        if (isDuplicate) {
                            skippedCount++;
                        } else {
                            TrackerEntry entry = new TrackerEntry();
                            entry.setTrackerId(tracker.getId());
                            entry.setUserId(userId);
                            entry.setFieldValues(fieldValues);
                            entryRepository.save(entry);
                            entryCount++;
                        }
                    }
                }
            }

            Map<String, Object> response = new HashMap<>();
            response.put("entryCount", entryCount);
            response.put("skippedCount", skippedCount);
            
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Error importing entries: " + e.getMessage());
        }
    }

    private String extractTextFromFile(MultipartFile file) throws Exception {
        String filename = file.getOriginalFilename() != null ? file.getOriginalFilename().toLowerCase() : "";
        if (filename.endsWith(".pdf") || file.getContentType() != null && file.getContentType().contains("pdf")) {
            try (PDDocument document = PDDocument.load(file.getInputStream())) {
                PDFTextStripper stripper = new PDFTextStripper();
                return stripper.getText(document);
            }
        } else if (filename.endsWith(".csv") || file.getContentType() != null && file.getContentType().contains("csv")) {
            try (CSVReader csvReader = new CSVReader(new InputStreamReader(file.getInputStream()))) {
                List<String[]> allData = csvReader.readAll();
                StringBuilder sb = new StringBuilder();
                for (String[] row : allData) {
                    sb.append(String.join(",", row)).append("\n");
                }
                return sb.toString();
            }
        } else {
            // Fallback to reading as standard text
            return new String(file.getBytes());
        }
    }

    // --- Plaid Integrations (Plugin Model) ---

    @PostMapping("/integrations/plaid/link-token")
    public ResponseEntity<?> createPlaidLinkToken(@RequestAttribute("userId") Long userId) {
        try {
            User user = userRepository.findById(userId).orElseThrow();
            String linkToken = plaidService.createLinkToken(user);
            return ResponseEntity.ok(Collections.singletonMap("link_token", linkToken));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", e.getMessage()));
        }
    }

    @PostMapping("/integrations/plaid/exchange/{trackerId}")
    public ResponseEntity<?> exchangePlaidToken(@PathVariable Long trackerId, 
                                                @RequestBody Map<String, String> payload, 
                                                @RequestAttribute("userId") Long userId) {
        try {
            User user = userRepository.findById(userId).orElseThrow();
            Tracker tracker = trackerRepository.findById(trackerId).orElseThrow();
            if (!tracker.getUserId().equals(userId)) return ResponseEntity.status(403).build();

            String publicToken = payload.get("public_token");
            String institutionName = payload.get("institution_name");
            TrackerIntegration integration = plaidService.exchangePublicToken(publicToken, institutionName, tracker, user);
            return ResponseEntity.ok(integration);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", e.getMessage()));
        }
    }

    @PostMapping("/integrations/plaid/mapping/{trackerId}")
    public ResponseEntity<?> setPlaidMapping(@PathVariable Long trackerId, 
                                             @RequestBody Map<String, String> mapping, 
                                             @RequestAttribute("userId") Long userId) {
        try {
            Tracker tracker = trackerRepository.findById(trackerId).orElseThrow();
            if (!tracker.getUserId().equals(userId)) return ResponseEntity.status(403).build();

            return ResponseEntity.ok(plaidService.setMapping(trackerId, mapping));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", e.getMessage()));
        }
    }

    @PostMapping("/integrations/plaid/sync/{trackerId}")
    public ResponseEntity<?> syncPlaidTransactions(@PathVariable Long trackerId, 
                                                   @RequestAttribute("userId") Long userId) {
        try {
            User user = userRepository.findById(userId).orElseThrow();
            Tracker tracker = trackerRepository.findById(trackerId).orElseThrow();
            if (!tracker.getUserId().equals(userId)) return ResponseEntity.status(403).build();

            int addedCount = plaidService.syncTransactions(tracker, user);
            return ResponseEntity.ok(Collections.singletonMap("addedCount", addedCount));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", e.getMessage()));
        }
    }

    @GetMapping("/integrations/plaid/suggest-mapping/{trackerId}")
    public ResponseEntity<?> suggestPlaidMapping(@PathVariable Long trackerId,
                                                  @RequestAttribute("userId") Long userId) {
        try {
            User user = userRepository.findById(userId).orElseThrow();
            Tracker tracker = trackerRepository.findById(trackerId).orElseThrow();
            if (!tracker.getUserId().equals(userId)) return ResponseEntity.status(403).build();

            Map<String, String> suggestedMapping = aiInsightService.suggestPlaidMapping(tracker, user);
            return ResponseEntity.ok(suggestedMapping);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", e.getMessage()));
        }
    }
}
