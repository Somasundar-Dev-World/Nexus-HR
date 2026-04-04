package com.example.expensestracker.model;

import jakarta.persistence.*;
import com.example.expensestracker.util.JpaListConverter;
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
    
    @Enumerated(EnumType.STRING)
    private TrackerType type;      // FINANCE, HEALTH, STOCK, CUSTOM

    private String metadata;       // Flexible JSON holder for Tickers, Targets, etc.
    
    @Convert(converter = JpaListConverter.class)
    @Column(length = 2000)
    private List<Object> fieldDefinitions; // Schema representing dynamic form fields

    private Long userId;
    private Long appId;            // Parent App ID

    public Tracker() {}

    public Tracker(String name, List<Object> fieldDefinitions, TrackerType type, Long userId) {
        this.name = name;
        this.fieldDefinitions = fieldDefinitions;
        this.type = type;
        this.userId = userId;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public List<Object> getFieldDefinitions() { return fieldDefinitions; }
    public void setFieldDefinitions(List<Object> fieldDefinitions) { this.fieldDefinitions = fieldDefinitions; }
    public TrackerType getType() { return type; }
    public void setType(TrackerType type) { this.type = type; }
    public String getMetadata() { return metadata; }
    public void setMetadata(String metadata) { this.metadata = metadata; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
}
