package com.marketplace.utils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class TokenUtils {
    private static final String SECRET = "cse352s_super_secret_key_distributed_online_marketplace_2026";
    private static final String HMAC_SHA256 = "HmacSHA256";

    /**
     * Creates a signed token of format Base64UrlEncoded(userId + "." + timestamp + "." + signature).
     */
    public static String createToken(String userId) {
        long timestamp = System.currentTimeMillis();
        String payload = userId + "." + timestamp;
        String signature = sign(payload);
        String combined = payload + "." + signature;
        return Base64.getUrlEncoder().withoutPadding().encodeToString(combined.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Verifies the token and returns the userId if valid, or null if invalid/expired.
     */
    public static String verifyToken(String tokenStr) {
        if (tokenStr == null || tokenStr.trim().isEmpty()) {
            return null;
        }
        try {
            byte[] decodedBytes = Base64.getUrlDecoder().decode(tokenStr);
            String decoded = new String(decodedBytes, StandardCharsets.UTF_8);
            String[] parts = decoded.split("\\.");
            if (parts.length != 3) {
                return null;
            }
            String userId = parts[0];
            String timestampStr = parts[1];
            String signature = parts[2];

            String payload = userId + "." + timestampStr;
            String expectedSignature = sign(payload);
            if (!expectedSignature.equals(signature)) {
                return null;
            }

            // Enforce a 7-day expiration time
            long timestamp = Long.parseLong(timestampStr);
            long now = System.currentTimeMillis();
            if (now - timestamp > 7L * 24 * 60 * 60 * 1000) {
                return null;
            }

            return userId;
        } catch (Exception e) {
            return null;
        }
    }

    private static String sign(String data) {
        try {
            Mac sha256Mac = Mac.getInstance(HMAC_SHA256);
            SecretKeySpec secretKey = new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), HMAC_SHA256);
            sha256Mac.init(secretKey);
            byte[] hash = sha256Mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("Failed to sign token", e);
        }
    }
}
