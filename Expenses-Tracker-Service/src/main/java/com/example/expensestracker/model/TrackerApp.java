package com.example.expensestracker.model;

import jakarta.persistence.*;

/**
 * High-level container for multiple trackers (e.g. "Health", "Wealth", "Work")
 */
@Entity
public class TrackerApp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;        // e.g. "Work", "Personal Wealth"
    private String icon;        // emoji or icon name, e.g. "💼", "💹"
    private String description;
    private Long userId;        // Ownership
    private String colorStyle;  // Optional hex or gradient name for styling

    public TrackerApp() {}

    public TrackerApp(String name, String icon, Long userId) {
        this.name = name;
        this.icon = icon;
        this.userId = userId;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getColorStyle() { return colorStyle; }
    public void setColorStyle(String colorStyle) { this.colorStyle = colorStyle; }
}
