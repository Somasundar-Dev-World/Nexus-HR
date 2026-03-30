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
                .map(user -> ResponseEntity.ok(new ProfileDTO(user.getUsername(), user.getName())))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(@RequestAttribute("userId") Long userId, @RequestBody ProfileDTO dto) {
        return userRepository.findById(userId).map(user -> {
            user.setName(dto.getName());
            if (dto.getPassword() != null && !dto.getPassword().trim().isEmpty()) {
                user.setPassword(dto.getPassword());
            }
            userRepository.save(user);
            return ResponseEntity.ok(new ProfileDTO(user.getUsername(), user.getName()));
        }).orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    static class ProfileDTO {
        private String username;
        private String name;
        private String password;

        public ProfileDTO() {}
        public ProfileDTO(String username, String name) {
            this.username = username;
            this.name = name;
        }

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }
}
