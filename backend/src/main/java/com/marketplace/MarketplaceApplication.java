package com.marketplace;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import com.marketplace.config.EnvConfig;

@SpringBootApplication
public class MarketplaceApplication {
    public static void main(String[] args) {
        // Load .env variables before application starts
        EnvConfig.loadDotEnv();
        
        // Dynamically set server port from env variable or .env file
        String port = EnvConfig.get("PORT", "4000");
        System.setProperty("server.port", port);
        
        SpringApplication.run(MarketplaceApplication.class, args);
    }
}
