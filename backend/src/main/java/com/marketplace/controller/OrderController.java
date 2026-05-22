package com.marketplace.controller;

import com.marketplace.model.Purchase;
import com.marketplace.model.PurchaseChallenge;
import com.marketplace.service.MarketplaceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/orders")
public class OrderController {

    @Autowired
    private MarketplaceService marketplaceService;

    @PostMapping("/begin-purchase")
    @ResponseStatus(HttpStatus.CREATED)
    public PurchaseChallenge beginPurchase(@RequestBody BeginPurchaseRequest request) {
        return marketplaceService.beginPurchase(
            request.getUserId() != null ? request.getUserId() : "",
            request.getProductId() != null ? request.getProductId() : ""
        );
    }

    @PostMapping("/confirm-purchase")
    @ResponseStatus(HttpStatus.CREATED)
    public Purchase confirmPurchase(@RequestBody ConfirmPurchaseRequest request) {
        return marketplaceService.confirmPurchase(
            request.getUserId() != null ? request.getUserId() : "",
            request.getChallengeId() != null ? request.getChallengeId() : "",
            request.getCode() != null ? request.getCode() : ""
        );
    }

    // DTOs
    public static class BeginPurchaseRequest {
        private String userId;
        private String productId;

        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        public String getProductId() { return productId; }
        public void setProductId(String productId) { this.productId = productId; }
    }

    public static class ConfirmPurchaseRequest {
        private String userId;
        private String challengeId;
        private String code;

        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        public String getChallengeId() { return challengeId; }
        public void setChallengeId(String challengeId) { this.challengeId = challengeId; }
        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
    }
}
