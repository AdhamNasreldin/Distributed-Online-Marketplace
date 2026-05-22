package com.marketplace.controller;

import com.marketplace.model.MarketplaceSnapshot;
import com.marketplace.service.MarketplaceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserController {

    @Autowired
    private MarketplaceService marketplaceService;

    @GetMapping("/users/{userId}/snapshot")
    public MarketplaceSnapshot getSnapshot(@PathVariable String userId) {
        return marketplaceService.getSnapshot(userId);
    }
}
