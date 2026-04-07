package com.example.expensestracker.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "ai_insight_cache")
public class AiInsightCache {

    @Id
    private Long appId;

    @Column(columnDefinition = "TEXT")
    private String insightsJson;

    private LocalDateTime lastUpdated;

    public AiInsightCache() {}

    public AiInsightCache(Long appId, String insightsJson) {
        this.appId = appId;
        this.insightsJson = insightsJson;
        this.lastUpdated = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getAppId() { return appId; }
    public void setAppId(Long appId) { this.appId = appId; }
    public String getInsightsJson() { return insightsJson; }
    public void setInsightsJson(String insightsJson) { this.insightsJson = insightsJson; }
    public LocalDateTime getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(LocalDateTime lastUpdated) { this.lastUpdated = lastUpdated; }
}
