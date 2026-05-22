package com.marketplace.model;

public class StoredUser extends User {
    private String passwordHash;
    private String schema;

    public StoredUser() {}

    public StoredUser(User user, String passwordHash, String schema) {
        super(user.getId(), user.getFullName(), user.getEmail(), user.getBalance(), user.getCreatedAt(), user.isTwoFactorEnabled());
        this.passwordHash = passwordHash;
        this.schema = schema;
    }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getSchema() { return schema; }
    public void setSchema(String schema) { this.schema = schema; }
}
