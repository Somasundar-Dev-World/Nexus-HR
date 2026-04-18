package com.example.expensestracker.controller;

import com.example.expensestracker.service.OmniQueryService;
import com.example.expensestracker.model.AiReport;
import com.example.expensestracker.repository.AiReportRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/omni/query")
@CrossOrigin(origins = "*")
public class OmniQueryController {

    private final OmniQueryService omniQueryService;
    private final AiReportRepository aiReportRepository;

    public OmniQueryController(OmniQueryService omniQueryService, AiReportRepository aiReportRepository) {
        this.omniQueryService = omniQueryService;
        this.aiReportRepository = aiReportRepository;
    }

    @PostMapping("/execute")
    public ResponseEntity<?> executeQuery(@RequestBody Map<String, String> request, @RequestAttribute("userId") Long userId) {
        String query = request.get("query");
        if (query == null || query.isEmpty()) {
            return ResponseEntity.badRequest().body("Query is required");
        }
        try {
            List<Map<String, Object>> results = omniQueryService.executeQuery(query, userId);
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }

    @PostMapping("/save-report")
    public ResponseEntity<AiReport> saveQueryReport(@RequestBody AiReport report, @RequestAttribute("userId") Long userId) {
        report.setUserId(userId);
        AiReport saved = aiReportRepository.save(report);
        return ResponseEntity.ok(saved);
    }

    @GetMapping("/run-report/{reportId}")
    public ResponseEntity<?> runSavedReport(@PathVariable Long reportId, @RequestAttribute("userId") Long userId) {
        AiReport report = aiReportRepository.findById(reportId).orElse(null);
        if (report == null || !report.getUserId().equals(userId)) {
            return ResponseEntity.notFound().build();
        }
        if (report.getOmniQuery() == null || report.getOmniQuery().isEmpty()) {
            return ResponseEntity.badRequest().body("Report has no OmniQuery associated");
        }
        try {
            List<Map<String, Object>> results = omniQueryService.executeQuery(report.getOmniQuery(), userId);
            return ResponseEntity.ok(Map.of(
                "reportName", report.getName(),
                "query", report.getOmniQuery(),
                "results", results
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }
}
