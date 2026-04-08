package com.example.expensestracker.controller;

import com.example.expensestracker.model.*;
import com.example.expensestracker.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/dev")
@CrossOrigin(origins = "*")
public class DevDataSeedingController {

    @Autowired
    private TrackerAppRepository appRepository;
    @Autowired
    private TrackerRepository trackerRepository;
    @Autowired
    private TrackerEntryRepository entryRepository;
    @Autowired
    private UserRepository userRepository;

    @PostMapping("/seed")
    public String seedData(@RequestParam String username) {
        User user = userRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("User not found"));
        Long userId = user.getId();

        // 1. Create App
        TrackerApp app = new TrackerApp();
        app.setName("Vitality & Peak Performance");
        app.setIcon("🧬");
        app.setDescription("High-fidelity biological and productivity tracking.");
        app.setUserId(userId);
        app.setColorStyle("linear-gradient(135deg, #0f172a 0%, #334155 100%)");
        app = appRepository.save(app);

        // 2. Create Trackers
        
        // A. Health: Biometric Pulse
        Tracker healthTracker = new Tracker();
        healthTracker.setName("Biometric Pulse");
        healthTracker.setType(TrackerType.HEALTH);
        healthTracker.setIcon("❤️");
        healthTracker.setUserId(userId);
        healthTracker.setAppId(app.getId());
        
        List<Object> hFields = new ArrayList<>();
        hFields.add(Map.of("name", "Sleep Score", "type", "RATING"));
        hFields.add(Map.of("name", "HRV", "type", "NUMBER", "unit", "ms"));
        hFields.add(Map.of("name", "Resting HR", "type", "NUMBER", "unit", "bpm"));
        healthTracker.setFieldDefinitions(hFields);
        healthTracker = trackerRepository.save(healthTracker);

        // B. Finance: Wealth Observatory
        Tracker wealthTracker = new Tracker();
        wealthTracker.setName("Wealth Observatory");
        wealthTracker.setType(TrackerType.FINANCE);
        wealthTracker.setIcon("💹");
        wealthTracker.setUserId(userId);
        wealthTracker.setAppId(app.getId());
        
        List<Object> wFields = new ArrayList<>();
        wFields.add(Map.of("name", "Net Worth", "type", "CURRENCY"));
        wFields.add(Map.of("name", "Daily P&L", "type", "CURRENCY"));
        wFields.add(Map.of("name", "Risk Level", "type", "RATING"));
        wealthTracker.setFieldDefinitions(wFields);
        wealthTracker = trackerRepository.save(wealthTracker);

        // 3. Inject Entries (14 days of data)
        LocalDateTime now = LocalDateTime.now();
        for (int i = 14; i >= 0; i--) {
            LocalDateTime date = now.minusDays(i);
            
            // Health Data
            TrackerEntry hEntry = new TrackerEntry();
            hEntry.setTrackerId(healthTracker.getId());
            hEntry.setUserId(userId);
            hEntry.setDate(date);
            
            Map<String, Object> hValues = new HashMap<>();
            if (i <= 4) { // Stress signal
                hValues.put("Sleep Score", 3);
                hValues.put("HRV", 40 + (i * 2));
                hValues.put("Resting HR", 72 + i);
                hEntry.setNote("Fatigued. AI should flag this.");
            } else { // Normal
                hValues.put("Sleep Score", 5);
                hValues.put("HRV", 80 - (i % 5));
                hValues.put("Resting HR", 52 + (i % 3));
                hEntry.setNote("Stable state.");
            }
            hEntry.setFieldValues(hValues);
            entryRepository.save(hEntry);

            // Wealth Data
            TrackerEntry wEntry = new TrackerEntry();
            wEntry.setTrackerId(wealthTracker.getId());
            wEntry.setUserId(userId);
            wEntry.setDate(date);
            
            Map<String, Object> wValues = new HashMap<>();
            double baseWealth = 1250000;
            wValues.put("Net Worth", baseWealth + (15 - i) * 1000);
            wValues.put("Daily P&L", 1000.0);
            wValues.put("Risk Level", 3);
            
            wEntry.setFieldValues(wValues);
            entryRepository.save(wEntry);
        }

        return "Successfully seeded data for " + username;
    }
}
