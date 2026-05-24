package com.marketplace.socket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketplace.config.EnvConfig;
import com.marketplace.service.MarketplaceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
public class StorePortalSocketServer implements CommandLineRunner {

    @Autowired
    private MarketplaceService marketplaceService;

    @Autowired
    private ObjectMapper objectMapper;

    private ServerSocket serverSocket;
    private ExecutorService threadPool;
    private volatile boolean running = true;

    @Override
    public void run(String... args) throws Exception {
        int port = 4001;
        String portStr = EnvConfig.get("STORE_PORTAL_SOCKET_PORT");
        if (portStr != null && !portStr.isEmpty()) {
            try {
                port = Integer.parseInt(portStr);
            } catch (NumberFormatException e) {
                System.err.println("Invalid STORE_PORTAL_SOCKET_PORT, using default 4001: " + e.getMessage());
            }
        }

        final int serverPort = port;
        threadPool = Executors.newCachedThreadPool();

        System.out.println("Initializing Store Portal TCP Socket Server on port " + serverPort + "...");

        // Run the socket server in a separate background thread so it does not block the Spring Boot startup thread
        Thread serverThread = new Thread(() -> {
            try {
                serverSocket = new ServerSocket(serverPort);
                System.out.println("Store Portal TCP Socket Server is listening on port " + serverPort);

                while (running) {
                    try {
                        Socket clientSocket = serverSocket.accept();
                        threadPool.submit(new StorePortalSocketHandler(clientSocket, marketplaceService, objectMapper));
                    } catch (IOException e) {
                        if (!running) {
                            break;
                        }
                        System.err.println("Error accepting Store Portal client connection: " + e.getMessage());
                    }
                }
            } catch (IOException e) {
                System.err.println("Store Portal TCP Socket Server failed to start: " + e.getMessage());
            }
        }, "store-portal-socket-server");

        serverThread.setDaemon(true);
        serverThread.start();
    }

    @PreDestroy
    public void stop() {
        System.out.println("Shutting down Store Portal TCP Socket Server...");
        running = false;
        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException ignored) {}
        }
        if (threadPool != null) {
            threadPool.shutdownNow();
        }
    }
}
