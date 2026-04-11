package com.example.expensestracker.model;

import java.util.List;

public class DeepInsightReport {

    private String executiveSummary;
    private int analysisDepth;
    private int trackerCount;
    private List<DeepSection> sections;
    private int overallScore;
    private String scoreLabel;
    private String generatedAt;
    private String provider;

    // --- Nested Section ---
    public static class DeepSection {
        private String id;
        private String title;
        private String icon;
        private String type; // SUMMARY, TREND, ANOMALY, FORECAST, RECOMMENDATION, RISK, CORRELATION
        private String color; // success, warning, danger, primary, magic, info
        private int priority;
        private String headline;
        private String content;
        private List<String> dataPoints;
        private List<String> actionItems;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getIcon() { return icon; }
        public void setIcon(String icon) { this.icon = icon; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getColor() { return color; }
        public void setColor(String color) { this.color = color; }
        public int getPriority() { return priority; }
        public void setPriority(int priority) { this.priority = priority; }
        public String getHeadline() { return headline; }
        public void setHeadline(String headline) { this.headline = headline; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        public List<String> getDataPoints() { return dataPoints; }
        public void setDataPoints(List<String> dataPoints) { this.dataPoints = dataPoints; }
        public List<String> getActionItems() { return actionItems; }
        public void setActionItems(List<String> actionItems) { this.actionItems = actionItems; }
    }

    // --- Getters & Setters ---
    public String getExecutiveSummary() { return executiveSummary; }
    public void setExecutiveSummary(String executiveSummary) { this.executiveSummary = executiveSummary; }
    public int getAnalysisDepth() { return analysisDepth; }
    public void setAnalysisDepth(int analysisDepth) { this.analysisDepth = analysisDepth; }
    public int getTrackerCount() { return trackerCount; }
    public void setTrackerCount(int trackerCount) { this.trackerCount = trackerCount; }
    public List<DeepSection> getSections() { return sections; }
    public void setSections(List<DeepSection> sections) { this.sections = sections; }
    public int getOverallScore() { return overallScore; }
    public void setOverallScore(int overallScore) { this.overallScore = overallScore; }
    public String getScoreLabel() { return scoreLabel; }
    public void setScoreLabel(String scoreLabel) { this.scoreLabel = scoreLabel; }
    public String getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(String generatedAt) { this.generatedAt = generatedAt; }
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
}
