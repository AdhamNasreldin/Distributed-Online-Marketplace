package com.marketplace.controller;

import com.marketplace.model.ReportSummary;
import com.marketplace.service.MarketplaceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ReportController {

    @Autowired
    private MarketplaceService marketplaceService;

    @GetMapping("/reports/transactions")
    public ReportSummary getReport(@RequestParam(required = false, defaultValue = "") String userId) {
        return marketplaceService.buildReport(userId);
    }
}
