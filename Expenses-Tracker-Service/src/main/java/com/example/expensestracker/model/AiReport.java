package com.example.expensestracker.model;

import jakarta.persistence.*;
import com.example.expensestracker.util.JpaMapConverter;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * AI-Architected Report Definition
 */
@Entity
public class AiReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String description;
    
    @Column(name = "app_id")
    @JsonProperty("appId")
    private Long appId;

    @Column(name = "user_id")
    @JsonProperty("userId")
    private Long userId;

    private String visualType; // BAR, PIE, LINE, RADAR, METRIC_GRID

    @Convert(converter = JpaMapConverter.class)
    @Column(length = 4000)
    private Map<String, Object> querySpec; // The JSON logic for aggregation and filtering

    @Convert(converter = JpaMapConverter.class)
    @Column(length = 2000)
    private Map<String, Object> config; // Visual config like chart colors, labels, axis titles

    private LocalDateTime createdAt;

    public AiReport() {
        this.createdAt = LocalDateTime.now();
    }

    public AiReport(String name, Long appId, Long userId, String visualType, Map<String, Object> querySpec) {
        this.name = name;
        this.appId = appId;
        this.userId = userId;
        this.visualType = visualType;
        this.querySpec = querySpec;
        this.createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Long getAppId() { return appId; }
    public void setAppId(Long appId) { this.appId = appId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getVisualType() { return visualType; }
    public void setVisualType(String visualType) { this.visualType = visualType; }
    public Map<String, Object> getQuerySpec() { return querySpec; }
    public void setQuerySpec(Map<String, Object> querySpec) { this.querySpec = querySpec; }
    public Map<String, Object> getConfig() { return config; }
    public void setConfig(Map<String, Object> config) { this.config = config; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
