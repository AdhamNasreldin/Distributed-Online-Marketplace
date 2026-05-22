package com.marketplace.db.socket;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class SocketClient {

    private final String host;
    private final int port;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public SocketClient() {
        String envHost = System.getenv("DB_SERVICE_HOST");
        this.host = envHost != null && !envHost.isEmpty() ? envHost : "localhost";

        String envPort = System.getenv("DB_SERVICE_PORT");
        int parsedPort = 5000;
        if (envPort != null && !envPort.isEmpty()) {
            try {
                parsedPort = Integer.parseInt(envPort);
            } catch (NumberFormatException e) {
                // Keep default
            }
        }
        this.port = parsedPort;
    }

    public SocketResponse sendRequest(SocketRequest request) throws Exception {
        try (
            Socket socket = new Socket(host, port);
            PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))
        ) {
            // Serialize request to JSON and send
            String requestJson = objectMapper.writeValueAsString(request);
            writer.println(requestJson);

            // Read response JSON
            String responseJson = reader.readLine();
            if (responseJson == null) {
                SocketResponse errResponse = new SocketResponse();
                errResponse.setSuccess(false);
                errResponse.setError("No response received from Database Socket Server");
                return errResponse;
            }

            return objectMapper.readValue(responseJson, SocketResponse.class);
        } catch (Exception e) {
            SocketResponse errResponse = new SocketResponse();
            errResponse.setSuccess(false);
            errResponse.setError("Database Socket Server Connection Error: " + e.getMessage());
            return errResponse;
        }
    }
}
