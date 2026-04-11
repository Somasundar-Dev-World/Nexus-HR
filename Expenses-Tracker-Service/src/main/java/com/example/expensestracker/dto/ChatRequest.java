package com.example.expensestracker.dto;

import java.util.List;

public class ChatRequest {
    private List<ChatMessage> history;
    private String message;

    public ChatRequest() {}

    public List<ChatMessage> getHistory() {
        return history;
    }

    public void setHistory(List<ChatMessage> history) {
        this.history = history;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
