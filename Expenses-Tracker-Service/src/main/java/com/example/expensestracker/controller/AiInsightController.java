package com.example.expensestracker.controller;

import com.example.expensestracker.model.SmartInsight;
import com.example.expensestracker.service.AiInsightService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/omni")
@CrossOrigin(origins = "*")
public class AiInsightController {

    @Autowired
    private AiInsightService aiInsightService;

    @GetMapping("/apps/{appId}/insights")
    public List<SmartInsight> getInsights(@RequestAttribute("userId") Long userId, @PathVariable Long appId) {
        return aiInsightService.getInsightsForApp(appId, userId);
    }
}
