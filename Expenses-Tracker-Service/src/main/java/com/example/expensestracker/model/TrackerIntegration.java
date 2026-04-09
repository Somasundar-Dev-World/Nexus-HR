package com.example.expensestracker.model;

import jakarta.persistence.*;
import com.example.expensestracker.util.JpaMapConverter;
import java.util.Map;

@Entity
@Table(name = "tracker_integrations")
public class TrackerIntegration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long trackerId;

    @Column(nullable = false)
    private String provider; // e.g., "PLAID"

    @Column()
    private String institutionName; // e.g., "Chase", "Bank of America"

    @Column(length = 1000)
    private String accessToken;

    @Column(length = 255)
    private String itemId;

    @Column(length = 1000)
    private String syncCursor;

    @Convert(converter = JpaMapConverter.class)
    @Column(columnDefinition = "TEXT")
    private Map<String, String> fieldMapping;

    public TrackerIntegration() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTrackerId() {
        return trackerId;
    }

    public void setTrackerId(Long trackerId) {
        this.trackerId = trackerId;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getInstitutionName() {
        return institutionName;
    }

    public void setInstitutionName(String institutionName) {
        this.institutionName = institutionName;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getItemId() {
        return itemId;
    }

    public void setItemId(String itemId) {
        this.itemId = itemId;
    }

    public String getSyncCursor() {
        return syncCursor;
    }

    public void setSyncCursor(String syncCursor) {
        this.syncCursor = syncCursor;
    }

    public Map<String, String> getFieldMapping() {
        return fieldMapping;
    }

    public void setFieldMapping(Map<String, String> fieldMapping) {
        this.fieldMapping = fieldMapping;
    }
}
