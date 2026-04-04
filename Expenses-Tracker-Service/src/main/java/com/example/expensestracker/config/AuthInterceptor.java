package com.example.expensestracker.config;

import com.example.expensestracker.controller.AuthController;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        
        // Handle preflight requests explicitly
        if (request.getMethod().equalsIgnoreCase("OPTIONS")) {
            return true;
        }

        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            Long userId = AuthController.TOKEN_STORE.get(token);
            if (userId != null) {
                request.setAttribute("userId", userId);
                return true;
            }
        }

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        return false;
    }
}
