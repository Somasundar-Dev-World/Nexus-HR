package com.example.expensestracker.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "deep_insight_cache")
public class DeepInsightCache {

    @Id
    private Long appId;

    @Column(columnDefinition = "TEXT")
    private String reportJson;

    private LocalDateTime lastUpdated;

    public DeepInsightCache() {}

    public DeepInsightCache(Long appId, String reportJson) {
        this.appId = appId;
        this.reportJson = reportJson;
        this.lastUpdated = LocalDateTime.now();
    }

    public Long getAppId() { return appId; }
    public void setAppId(Long appId) { this.appId = appId; }
    public String getReportJson() { return reportJson; }
    public void setReportJson(String reportJson) { this.reportJson = reportJson; }
    public LocalDateTime getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(LocalDateTime lastUpdated) { this.lastUpdated = lastUpdated; }
}
