package com.example.expensestracker.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SmartInsight {
    private String type;     // METRIC, ADVICE, ALERT, CHART
    private String title;
    private String value;
    private String subtitle;
    private String icon;
    private String color;    // success, warning, danger, primary, magic
    private int priority;
    private String reasoning; // Why the AI suggested this (for debugging/context)
}
