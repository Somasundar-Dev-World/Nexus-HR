package com.example.expensestracker.model;

import jakarta.persistence.*;
import com.example.expensestracker.util.JpaMapConverter;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Universal Tracker Entry (The specific data point logged)
 */
@Entity
public class TrackerEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Convert(converter = JpaMapConverter.class)
    @Column(length = 2000)
    private Map<String, Object> fieldValues; // Dynamic values correlating to the Tracker's field definitions
    
    private LocalDateTime date;    // When the tracking occurred
    private String note;           // Optional context or observation

    @Column(name = "tracker_id")
    @JsonProperty("trackerId")
    private Long trackerId;        // Foreign key back to the Tracker definition

    @Column(name = "user_id")
    @JsonProperty("userId")
    private Long userId;

    public TrackerEntry() {
        this.date = LocalDateTime.now();
    }

    public TrackerEntry(Map<String, Object> fieldValues, Long trackerId, Long userId) {
        this.fieldValues = fieldValues;
        this.trackerId = trackerId;
        this.userId = userId;
        this.date = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Map<String, Object> getFieldValues() { return fieldValues; }
    public void setFieldValues(Map<String, Object> fieldValues) { this.fieldValues = fieldValues; }
    public LocalDateTime getDate() { return date; }
    public void setDate(LocalDateTime date) { this.date = date; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public Long getTrackerId() { return trackerId; }
    public void setTrackerId(Long trackerId) { this.trackerId = trackerId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
}
