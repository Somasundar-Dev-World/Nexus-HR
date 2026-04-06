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

import java.util.List;

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
}
