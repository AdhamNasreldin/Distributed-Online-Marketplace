package com.marketplace.model;

public class AuthChallenge {
    private String challengeId;
    private User user;
    private String message;

    public AuthChallenge() {}

    public AuthChallenge(String challengeId, User user, String message) {
        this.challengeId = challengeId;
        this.user = user;
        this.message = message;
    }

    public String getChallengeId() { return challengeId; }
    public void setChallengeId(String challengeId) { this.challengeId = challengeId; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
