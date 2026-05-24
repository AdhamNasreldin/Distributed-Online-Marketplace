package com.marketplace.socket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketplace.config.EnvConfig;
import com.marketplace.exception.AppException;
import com.marketplace.model.Listing;
import com.marketplace.model.Purchase;
import com.marketplace.model.PurchaseChallenge;
import com.marketplace.service.MarketplaceService;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StorePortalSocketHandler implements Runnable {

    private final Socket socket;
    private final MarketplaceService marketplaceService;
    private final ObjectMapper objectMapper;

    public StorePortalSocketHandler(Socket socket, MarketplaceService marketplaceService, ObjectMapper objectMapper) {
        this.socket = socket;
        this.marketplaceService = marketplaceService;
        this.objectMapper = objectMapper;
    }

    @Override
    public void run() {
        try (
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter writer = new PrintWriter(socket.getOutputStream(), true)
        ) {
            String line = reader.readLine();
            if (line == null || line.trim().isEmpty()) {
                return;
            }

            Map<String, Object> request = objectMapper.readValue(line, Map.class);
            String action = (String) request.get("action");
            if (action == null) {
                action = (String) request.get("type"); // fallback to 'type'
            }

            if (action == null) {
                sendErrorResponse(writer, "Missing 'action' or 'type' in request payload.");
                return;
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);

            try {
                switch (action.toUpperCase()) {
                    case "SEARCH":
                        String searchUserId = request.get("userId") != null ? (String) request.get("userId") : "";
                        String searchQuery = request.get("query") != null ? (String) request.get("query") : "";
                        String searchCategory = request.get("category") != null ? (String) request.get("category") : "All";
                        
                        List<Listing> listings = marketplaceService.searchListings(searchUserId, searchQuery, searchCategory);
                        response.put("listings", listings);
                        break;

                    case "BEGIN_PURCHASE":
                        String beginUserId = request.get("userId") != null ? (String) request.get("userId") : "";
                        String beginProductId = request.get("productId") != null ? (String) request.get("productId") : "";
                        
                        PurchaseChallenge challenge = marketplaceService.beginPurchase(beginUserId, beginProductId);
                        response.put("challengeId", challenge.getChallengeId());
                        if (challenge.getProduct() != null) {
                            response.put("productId", challenge.getProduct().getId());
                        } else {
                            response.put("productId", beginProductId);
                        }
                        response.put("amount", challenge.getAmount());
                        response.put("message", challenge.getMessage());
                        response.put("code", EnvConfig.get("DEMO_2FA_CODE", "246810")); // include for testing or user-friendliness
                        break;

                    case "CONFIRM_PURCHASE":
                        String confirmUserId = request.get("userId") != null ? (String) request.get("userId") : "";
                        String confirmChallengeId = request.get("challengeId") != null ? (String) request.get("challengeId") : "";
                        String confirmCode = request.get("code") != null ? (String) request.get("code") : "";
                        
                        Purchase purchase = marketplaceService.confirmPurchase(confirmUserId, confirmChallengeId, confirmCode);
                        response.put("purchase", purchase);
                        break;

                    default:
                        sendErrorResponse(writer, "Unsupported action: " + action);
                        return;
                }

                String jsonResponse = objectMapper.writeValueAsString(response);
                writer.println(jsonResponse);

            } catch (AppException e) {
                sendErrorResponse(writer, e.getMessage());
            } catch (Exception e) {
                System.err.println("Database/Internal error in Store Portal TCP Socket Handler: " + e.getMessage());
                e.printStackTrace();
                sendErrorResponse(writer, "Internal error: " + e.getMessage());
            }

        } catch (Exception e) {
            System.err.println("Network error in Store Portal TCP Socket Handler: " + e.getMessage());
        } finally {
            try {
                socket.close();
            } catch (Exception ignored) {}
        }
    }

    private void sendErrorResponse(PrintWriter writer, String errorMessage) {
        try {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", errorMessage);
            String jsonResponse = objectMapper.writeValueAsString(errorResponse);
            writer.println(jsonResponse);
        } catch (Exception e) {
            System.err.println("Failed to write error response: " + e.getMessage());
        }
    }
}
