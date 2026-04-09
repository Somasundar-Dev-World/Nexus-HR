package com.example.expensestracker.model;

import jakarta.persistence.*;
import java.util.Map;
import com.example.expensestracker.util.JpaMapConverter;

@Entity
@Table(name = "tracker_mappings")
public class TrackerMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tracker_id", nullable = false)
    private Long trackerId;

    @Column(name = "csv_headers", nullable = false, length = 1000)
    private String csvHeaders;

    @Convert(converter = JpaMapConverter.class)
    @Column(name = "column_mapping", columnDefinition = "TEXT")
    private Map<String, Object> columnMapping;

    public TrackerMapping() {}

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

    public String getCsvHeaders() {
        return csvHeaders;
    }

    public void setCsvHeaders(String csvHeaders) {
        this.csvHeaders = csvHeaders;
    }

    public Map<String, Object> getColumnMapping() {
        return columnMapping;
    }

    public void setColumnMapping(Map<String, Object> columnMapping) {
        this.columnMapping = columnMapping;
    }
}
