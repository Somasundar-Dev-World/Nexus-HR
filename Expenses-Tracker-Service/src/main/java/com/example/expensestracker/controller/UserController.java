package com.example.expensestracker.controller;

import com.example.expensestracker.model.User;
import com.example.expensestracker.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
@CrossOrigin(origins = "*")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(@RequestAttribute("userId") Long userId) {
        return userRepository.findById(userId)
                .map(user -> ResponseEntity.ok(new ProfileDTO(user.getUsername(), user.getName(), 
                    user.getGeminiApiKey(), user.getAnthropicApiKey(), user.getOpenaiApiKey(),
                    user.getAiProvider(), user.getAiModel())))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(@RequestAttribute("userId") Long userId, @RequestBody ProfileDTO dto) {
        return userRepository.findById(userId).map(user -> {
            user.setName(dto.getName());
            user.setGeminiApiKey(dto.getGeminiApiKey());
            user.setAnthropicApiKey(dto.getAnthropicApiKey());
            user.setOpenaiApiKey(dto.getOpenaiApiKey());
            user.setAiProvider(dto.getAiProvider());
            user.setAiModel(dto.getAiModel());
            if (dto.getPassword() != null && !dto.getPassword().trim().isEmpty()) {
                user.setPassword(dto.getPassword());
            }
            userRepository.save(user);
            return ResponseEntity.ok(new ProfileDTO(user.getUsername(), user.getName(), 
                user.getGeminiApiKey(), user.getAnthropicApiKey(), user.getOpenaiApiKey(),
                user.getAiProvider(), user.getAiModel()));
        }).orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    static class ProfileDTO {
        private String username;
        private String name;
        private String password;
        private String geminiApiKey;
        private String anthropicApiKey;
        private String openaiApiKey;
        private String aiProvider;
        private String aiModel;

        public ProfileDTO() {}
        public ProfileDTO(String username, String name, String geminiApiKey, String anthropicApiKey, String openaiApiKey, String aiProvider, String aiModel) {
            this.username = username;
            this.name = name;
            this.geminiApiKey = geminiApiKey;
            this.anthropicApiKey = anthropicApiKey;
            this.openaiApiKey = openaiApiKey;
            this.aiProvider = aiProvider;
            this.aiModel = aiModel;
        }

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public String getGeminiApiKey() { return geminiApiKey; }
        public void setGeminiApiKey(String geminiApiKey) { this.geminiApiKey = geminiApiKey; }
        public String getAnthropicApiKey() { return anthropicApiKey; }
        public void setAnthropicApiKey(String anthropicApiKey) { this.anthropicApiKey = anthropicApiKey; }
        public String getOpenaiApiKey() { return openaiApiKey; }
        public void setOpenaiApiKey(String openaiApiKey) { this.openaiApiKey = openaiApiKey; }
        public String getAiProvider() { return aiProvider; }
        public void setAiProvider(String aiProvider) { this.aiProvider = aiProvider; }
        public String getAiModel() { return aiModel; }
        public void setAiModel(String aiModel) { this.aiModel = aiModel; }
    }
}
