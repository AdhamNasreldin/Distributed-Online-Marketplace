package com.marketplace.dbservice;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;

import javax.sql.DataSource;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SpringBootApplication(exclude = {
    DataSourceAutoConfiguration.class,
    DataSourceTransactionManagerAutoConfiguration.class
})
public class DbServiceApplication implements CommandLineRunner {

    public static void main(String[] args) {
        SpringApplication.run(DbServiceApplication.class, args);
    }

    private HikariDataSource createHikariDataSource(String envPrefix, String defaultUrl) {
        String dbUrl = System.getenv(envPrefix + "_DB_URL");
        String username = System.getenv(envPrefix + "_DB_USER");
        String password = System.getenv(envPrefix + "_DB_PASS");

        if (dbUrl == null || dbUrl.isEmpty()) {
            dbUrl = defaultUrl;
        }
        if (username == null || username.isEmpty()) {
            username = "appuser";
        }
        if (password == null || password.isEmpty()) {
            password = "apppass";
        }

        HikariDataSource ds = new HikariDataSource();
        ds.setDriverClassName("org.mariadb.jdbc.Driver");
        ds.setJdbcUrl(dbUrl);
        ds.setUsername(username);
        ds.setPassword(password);
        ds.setMaximumPoolSize(10);
        ds.setIdleTimeout(30000);
        ds.setMinimumIdle(2);

        System.out.println("Initialized Hikari Connection Pool for " + envPrefix + " at " + dbUrl);
        return ds;
    }

    @Override
    public void run(String... args) throws Exception {
        int port = 5000;
        String portStr = System.getenv("PORT");
        if (portStr != null && !portStr.isEmpty()) {
            try {
                port = Integer.parseInt(portStr);
            } catch (NumberFormatException e) {
                // Keep default
            }
        }

        Map<String, DataSource> dataSources = new HashMap<>();
        dataSources.put("coordinator", createHikariDataSource("COORDINATOR", "jdbc:mariadb://localhost:3306/marketplace_coordinator"));
        dataSources.put("shard1", createHikariDataSource("SHARD1", "jdbc:mariadb://localhost:3307/marketplace_shard1"));
        dataSources.put("shard2", createHikariDataSource("SHARD2", "jdbc:mariadb://localhost:3308/marketplace_shard2"));

        System.out.println("Starting Database Socket Server on port " + port + "...");
        ExecutorService threadPool = Executors.newCachedThreadPool();

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Database Socket Server is listening on port " + port);
            while (true) {
                Socket clientSocket = serverSocket.accept();
                threadPool.submit(new SocketHandler(clientSocket, dataSources));
            }
        } catch (Exception e) {
            System.err.println("Database Socket Server crashed: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Close all Hikari pools on server shutdown
            for (DataSource ds : dataSources.values()) {
                if (ds instanceof HikariDataSource) {
                    ((HikariDataSource) ds).close();
                }
            }
        }
    }
}
