package com.example.expensestracker.controller;

import com.example.expensestracker.model.Tracker;
import com.example.expensestracker.model.TrackerEntry;
import com.example.expensestracker.model.TrackerType;
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

    // --- Trackers ---

    @GetMapping("/trackers")
    public List<Tracker> getAllTrackers(@RequestAttribute("userId") Long userId) {
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
