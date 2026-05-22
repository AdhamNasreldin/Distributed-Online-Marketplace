package com.marketplace.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import com.marketplace.db.socket.SocketDataSource;

import javax.sql.DataSource;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class EnvConfig {
    private static final Map<String, String> dotEnvMap = new HashMap<>();

    public static void loadDotEnv() {
        // Paths to search for .env
        String[] paths = {
            "../backend/.env",
            "backend/.env",
            "./.env",
            "../.env",
            "../../.env"
        };

        for (String path : paths) {
            File file = new File(path);
            if (file.exists() && file.isFile()) {
                System.out.println("Found .env file at: " + file.getAbsolutePath());
                try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        line = line.trim();
                        if (line.isEmpty() || line.startsWith("#")) {
                            continue;
                        }
                        int eqIdx = line.indexOf('=');
                        if (eqIdx > 0) {
                            String key = line.substring(0, eqIdx).trim();
                            String value = line.substring(eqIdx + 1).trim();
                            // Strip quotes if any
                            if (value.startsWith("\"") && value.endsWith("\"") && value.length() >= 2) {
                                value = value.substring(1, value.length() - 1);
                            } else if (value.startsWith("'") && value.endsWith("'") && value.length() >= 2) {
                                value = value.substring(1, value.length() - 1);
                            }
                            dotEnvMap.put(key, value);
                        }
                    }
                } catch (IOException e) {
                    System.err.println("Failed to read .env file: " + e.getMessage());
                }
                break;
            }
        }
    }

    public static String get(String key) {
        String envVal = System.getenv(key);
        if (envVal != null && !envVal.isEmpty()) {
            return envVal;
        }
        return dotEnvMap.get(key);
    }

    public static String get(String key, String defaultValue) {
        String val = get(key);
        return val != null ? val : defaultValue;
    }

    @Bean
    public DataSource dataSource() {
        System.out.println("Configuring custom SocketDataSource proxy connecting to Database Socket Server...");
        return new SocketDataSource();
    }
}
