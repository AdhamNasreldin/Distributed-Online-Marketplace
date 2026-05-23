package com.marketplace.controller;

import com.marketplace.model.AuthChallenge;
import com.marketplace.model.User;
import com.marketplace.service.MarketplaceService;
import com.marketplace.utils.TokenUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private MarketplaceService marketplaceService;

    @PostMapping("/login")
    public User login(@RequestBody LoginRequest request, HttpServletResponse response) {
        User user = marketplaceService.login(
            request.getEmail() != null ? request.getEmail() : "",
            request.getPassword() != null ? request.getPassword() : ""
        );
        setSessionCookie(response, user.getId());
        return user;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthChallenge register(@RequestBody RegisterRequest request) {
        return marketplaceService.register(
            request.getFullName() != null ? request.getFullName() : "",
            request.getEmail() != null ? request.getEmail() : "",
            request.getPassword() != null ? request.getPassword() : ""
        );
    }

    @PostMapping("/verify-2fa")
    public User verify2fa(@RequestBody Verify2faRequest request, HttpServletResponse response) {
        User user = marketplaceService.verifyAuthChallenge(
            request.getChallengeId() != null ? request.getChallengeId() : "",
            request.getCode() != null ? request.getCode() : ""
        );
        setSessionCookie(response, user.getId());
        return user;
    }

    @GetMapping("/session")
    public User getSession(HttpServletRequest request) {
        String token = null;
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("session_token".equals(cookie.getName())) {
                    token = cookie.getValue();
                    break;
                }
            }
        }

        if (token == null) {
            throw new com.marketplace.exception.AppException(401, "No active session.");
        }

        String userId = TokenUtils.verifyToken(token);
        if (userId == null) {
            throw new com.marketplace.exception.AppException(401, "Invalid or expired session.");
        }

        User user = marketplaceService.findUserById(userId);
        if (user == null) {
            throw new com.marketplace.exception.AppException(401, "User not found.");
        }

        return user;
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletResponse response) {
        clearSessionCookie(response);
        return ResponseEntity.noContent().build();
    }

    private void setSessionCookie(HttpServletResponse response, String userId) {
        String token = TokenUtils.createToken(userId);
        ResponseCookie cookie = ResponseCookie.from("session_token", token)
                .httpOnly(true)
                .secure(false) // Set to true if running in production with HTTPS
                .path("/")
                .maxAge(7L * 24 * 60 * 60) // 7 days
                .sameSite("Lax")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private void clearSessionCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from("session_token", "")
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(0) // immediately expires the cookie
                .sameSite("Lax")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }


    // DTOs
    public static class LoginRequest {
        private String email;
        private String password;

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }

    public static class RegisterRequest {
        private String fullName;
        private String email;
        private String password;

        public String getFullName() { return fullName; }
        public void setFullName(String fullName) { this.fullName = fullName; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }

    public static class Verify2faRequest {
        private String challengeId;
        private String code;

        public String getChallengeId() { return challengeId; }
        public void setChallengeId(String challengeId) { this.challengeId = challengeId; }
        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
    }
}
