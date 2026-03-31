package com.example.expensestracker.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Universal Tracker Entry (The specific data point logged)
 */
@Entity
public class TrackerEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private BigDecimal value;      // Numerical value (e.g., 75, 100.5, 20)
    private LocalDateTime date;    // When the tracking occurred
    private String note;           // Optional context or observation

    private Long trackerId;        // Foreign key back to the Tracker definition
    private Long userId;

    public TrackerEntry() {
        this.date = LocalDateTime.now();
    }

    public TrackerEntry(BigDecimal value, Long trackerId, Long userId) {
        this.value = value;
        this.trackerId = trackerId;
        this.userId = userId;
        this.date = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public BigDecimal getValue() { return value; }
    public void setValue(BigDecimal value) { this.value = value; }
    public LocalDateTime getDate() { return date; }
    public void setDate(LocalDateTime date) { this.date = date; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public Long getTrackerId() { return trackerId; }
    public void setTrackerId(Long trackerId) { this.trackerId = trackerId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
}
