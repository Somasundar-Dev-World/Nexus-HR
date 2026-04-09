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
import com.example.expensestracker.model.User;
import com.example.expensestracker.repository.UserRepository;

import java.io.InputStreamReader;
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
            if (entriesNode.isArray()) {
                for (JsonNode entryRow : entriesNode) {
                    TrackerEntry entry = new TrackerEntry();
                    entry.setTrackerId(tracker.getId());
                    entry.setUserId(userId);
                    
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
                    entry.setFieldValues(fieldValues);
                    entryRepository.save(entry);
                    entryCount++;
                }
            }

            Map<String, Object> response = new HashMap<>();
            response.put("tracker", tracker);
            response.put("entryCount", entryCount);
            
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Error importing document: " + e.getMessage());
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
}
