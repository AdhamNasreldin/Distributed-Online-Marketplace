package com.marketplace.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
public class HealthController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @GetMapping("/health")
    public Map<String, String> getHealth() {
        jdbcTemplate.execute("SELECT 1");
        Map<String, String> res = new HashMap<>();
        res.put("status", "ok");
        res.put("database", "connected");
        return res;
    }
}
