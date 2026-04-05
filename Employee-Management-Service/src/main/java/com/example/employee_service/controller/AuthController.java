package com.example.employee_service.controller;

import com.example.employee_service.model.User;
import com.example.employee_service.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    // Simple universally accessible token store
    public static final Map<String, Long> TOKEN_STORE = new ConcurrentHashMap<>();

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Username already exists");
        }
        User user = new User(request.getUsername(), request.getPassword(), request.getName());
        userRepository.save(user);
        
        String token = UUID.randomUUID().toString();
        TOKEN_STORE.put(token, user.getId());
        
        return ResponseEntity.ok(new AuthResponse(token, user.getName(), user.getUsername()));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        Optional<User> userOpt = userRepository.findByUsername(request.getUsername());
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (user.getPassword().equals(request.getPassword())) {
                String token = UUID.randomUUID().toString();
                TOKEN_STORE.put(token, user.getId());
                return ResponseEntity.ok(new AuthResponse(token, user.getName(), user.getUsername()));
            }
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid credentials");
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader("Authorization") String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            TOKEN_STORE.remove(token);
        }
        return ResponseEntity.ok().build();
    }

    // --- DTOs ---
    static class RegisterRequest {
        private String username;
        private String password;
        private String name;
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }

    static class LoginRequest {
        private String username;
        private String password;
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }

    static class AuthResponse {
        private String token;
        private String name;
        private String username;
        public AuthResponse(String token, String name, String username) {
            this.token = token; this.name = name; this.username = username;
        }
        public String getToken() { return token; }
        public String getName() { return name; }
        public String getUsername() { return username; }
    }
}
