package com.example.expensestracker.model;

import jakarta.persistence.*;
import java.util.List;

/**
 * Universal Tracker Definition (The "What" being tracked)
 */
@Entity
public class Tracker {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;           // e.g. "Weight", "AAPL", "Daily Coffee"
    private String unit;           // e.g. "kg", "$", "shares", "count"
    
    @Enumerated(EnumType.STRING)
    private TrackerType type;      // FINANCE, HEALTH, STOCK, CUSTOM

    private String metadata;       // Flexible JSON holder for Tickers, Targets, etc.
    
    private Long userId;

    public Tracker() {}

    public Tracker(String name, String unit, TrackerType type, Long userId) {
        this.name = name;
        this.unit = unit;
        this.type = type;
        this.userId = userId;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    public TrackerType getType() { return type; }
    public void setType(TrackerType type) { this.type = type; }
    public String getMetadata() { return metadata; }
    public void setMetadata(String metadata) { this.metadata = metadata; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
}
