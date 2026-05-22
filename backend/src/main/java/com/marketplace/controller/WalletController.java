package com.marketplace.controller;

import com.marketplace.model.User;
import com.marketplace.service.MarketplaceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class WalletController {

    @Autowired
    private MarketplaceService marketplaceService;

    @PostMapping("/wallet/deposit")
    public User deposit(@RequestBody DepositRequest request) {
        return marketplaceService.deposit(
            request.getUserId() != null ? request.getUserId() : "",
            request.getAmount()
        );
    }

    // DTO
    public static class DepositRequest {
        private String userId;
        private double amount;

        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        public double getAmount() { return amount; }
        public void setAmount(double amount) { this.amount = amount; }
    }
}
