package com.example.expensestracker.controller;

import com.example.expensestracker.model.*;
import com.example.expensestracker.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
    public String seedData(@RequestParam String email) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));
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
        healthTracker.setFieldDefinitions(List.of(
            new FieldDefinition("Sleep Score", FieldType.RATING, null, null),
            new FieldDefinition("HRV", FieldType.NUMBER, "ms", null),
            new FieldDefinition("Resting HR", FieldType.NUMBER, "bpm", null)
        ));
        healthTracker = trackerRepository.save(healthTracker);

        // B. Finance: Wealth Observatory
        Tracker wealthTracker = new Tracker();
        wealthTracker.setName("Wealth Observatory");
        wealthTracker.setType(TrackerType.FINANCE);
        wealthTracker.setIcon("💹");
        wealthTracker.setUserId(userId);
        wealthTracker.setAppId(app.getId());
        wealthTracker.setFieldDefinitions(List.of(
            new FieldDefinition("Net Worth", FieldType.CURRENCY, null, null),
            new FieldDefinition("Daily P&L", FieldType.CURRENCY, null, null),
            new FieldDefinition("Risk Level", FieldType.RATING, null, null)
        ));
        wealthTracker = trackerRepository.save(wealthTracker);

        // 3. Inject Entries (14 days of data)
        LocalDateTime now = LocalDateTime.now();
        for (int i = 14; i >= 0; i--) {
            LocalDateTime date = now.minusDays(i);
            
            // Health Data (The Story: Getting sick/stressed at day 4)
            TrackerEntry hEntry = new TrackerEntry();
            hEntry.setTrackerId(healthTracker.getId());
            hEntry.setUserId(userId);
            hEntry.setDate(date.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            
            Map<String, String> hValues = new HashMap<>();
            if (i <= 4) { // Getting worse
                hValues.put("Sleep Score", String.valueOf(3 + (i % 2)));
                hValues.put("HRV", String.valueOf(35 + (i * 2)));
                hValues.put("Resting HR", String.valueOf(72 + i));
                hEntry.setNote("Feeling fatigued. Overtraining suspected.");
            } else { // Optimal state
                hValues.put("Sleep Score", String.valueOf(5));
                hValues.put("HRV", String.valueOf(82 - (i % 5)));
                hValues.put("Resting HR", String.valueOf(52 + (i % 3)));
                hEntry.setNote("Feeling refreshed.");
            }
            hEntry.setFieldValues(hValues);
            entryRepository.save(hEntry);

            // Wealth Data (High Volatility)
            TrackerEntry wEntry = new TrackerEntry();
            wEntry.setTrackerId(wealthTracker.getId());
            wEntry.setUserId(userId);
            wEntry.setDate(date.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            
            Map<String, String> wValues = new HashMap<>();
            double baseWealth = 1250000;
            double dailyFluc = (Math.random() - 0.5) * 50000;
            wValues.put("Net Worth", String.valueOf(baseWealth + (15-i) * 5000 + dailyFluc));
            wValues.put("Daily P&L", String.valueOf(dailyFluc));
            wValues.put("Risk Level", String.valueOf(3 + (i % 3)));
            
            wEntry.setFieldValues(wValues);
            entryRepository.save(wEntry);
        }

        return "Successfully seeded 1 App, 2 Trackers, and 30 Entries for " + email;
    }
}
