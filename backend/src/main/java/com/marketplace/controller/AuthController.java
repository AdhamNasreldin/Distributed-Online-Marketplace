package com.marketplace.controller;

import com.marketplace.model.AuthChallenge;
import com.marketplace.model.User;
import com.marketplace.service.MarketplaceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private MarketplaceService marketplaceService;

    @PostMapping("/login")
    public User login(@RequestBody LoginRequest request) {
        return marketplaceService.login(
            request.getEmail() != null ? request.getEmail() : "",
            request.getPassword() != null ? request.getPassword() : ""
        );
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
    public User verify2fa(@RequestBody Verify2faRequest request) {
        return marketplaceService.verifyAuthChallenge(
            request.getChallengeId() != null ? request.getChallengeId() : "",
            request.getCode() != null ? request.getCode() : ""
        );
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
