package com.example.expensestracker.controller;

import com.example.expensestracker.model.AiReport;
import com.example.expensestracker.service.ReportIntelligenceService;
import com.example.expensestracker.repository.AiReportRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/reports")
@CrossOrigin(origins = "*")
public class AiReportController {

    private final ReportIntelligenceService reportService;
    private final AiReportRepository reportRepository;

    public AiReportController(ReportIntelligenceService reportService, AiReportRepository reportRepository) {
        this.reportService = reportService;
        this.reportRepository = reportRepository;
    }

    @PostMapping("/suggest")
    public ResponseEntity<?> suggestReports(@RequestAttribute("userId") Long userId, 
                                            @RequestBody Map<String, Object> payload) {
        try {
            Long appId = Long.valueOf(payload.get("appId").toString());
            List<Long> trackerIds = (List<Long>) payload.get("trackerIds");
            return ResponseEntity.ok(reportService.suggestReports(appId, userId, trackerIds));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error suggesting reports: " + e.getMessage());
        }
    }

    @GetMapping("/app/{appId}")
    public List<AiReport> getReportsByApp(@RequestAttribute("userId") Long userId, @PathVariable Long appId) {
        return reportRepository.findByAppIdAndUserId(appId, userId);
    }

    @PostMapping
    public AiReport saveReport(@RequestAttribute("userId") Long userId, @RequestBody AiReport report) {
        report.setUserId(userId);
        return reportRepository.save(report);
    }

    @GetMapping("/{id}/execute")
    public ResponseEntity<?> executeReport(@RequestAttribute("userId") Long userId, @PathVariable Long id) {
        return reportRepository.findById(id).map(report -> {
            if (!report.getUserId().equals(userId)) return ResponseEntity.status(403).build();
            return ResponseEntity.ok(reportService.executeReport(report));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteReport(@RequestAttribute("userId") Long userId, @PathVariable Long id) {
        return reportRepository.findById(id).map(report -> {
            if (!report.getUserId().equals(userId)) return ResponseEntity.status(403).build();
            reportRepository.delete(report);
            return ResponseEntity.ok().build();
        }).orElse(ResponseEntity.notFound().build());
    }
}
