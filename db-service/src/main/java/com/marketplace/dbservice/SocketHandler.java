package com.marketplace.dbservice;

import com.fasterxml.jackson.databind.ObjectMapper;

import javax.sql.DataSource;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SocketHandler implements Runnable {

    // Thread-safe session registry: sessionId -> Map of active database connection keys to Connections
    private static final Map<String, Map<String, Connection>> transactionConnections = new ConcurrentHashMap<>();
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final Socket socket;
    private final Map<String, DataSource> dataSources;

    public SocketHandler(Socket socket, Map<String, DataSource> dataSources) {
        this.socket = socket;
        this.dataSources = dataSources;
    }

    @Override
    public void run() {
        try (
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter writer = new PrintWriter(socket.getOutputStream(), true)
        ) {
            String line = reader.readLine();
            if (line == null) return;

            Map<String, Object> request = objectMapper.readValue(line, Map.class);
            String sessionId = (String) request.get("sessionId");
            String type = (String) request.get("type");
            String sql = (String) request.get("sql");
            List<Object> params = (List<Object>) request.get("params");

            Map<String, Object> response = handleRequest(sessionId, type, sql, params);
            String responseJson = objectMapper.writeValueAsString(response);
            writer.println(responseJson);

        } catch (Exception e) {
            System.err.println("Error handling socket request: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                socket.close();
            } catch (Exception ignored) {}
        }
    }

    private Map<String, Object> handleRequest(String sessionId, String type, String sql, List<Object> params) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);

        boolean inTransaction = sessionId != null && transactionConnections.containsKey(sessionId);
        String cleanedSql = "";

        try {
            if ("BEGIN".equalsIgnoreCase(type)) {
                if (!inTransaction && sessionId != null) {
                    transactionConnections.put(sessionId, new ConcurrentHashMap<>());
                }
                response.put("success", true);
                return response;
            }

            if ("COMMIT".equalsIgnoreCase(type)) {
                if (inTransaction) {
                    Map<String, Connection> connMap = transactionConnections.remove(sessionId);
                    if (connMap != null) {
                        List<Exception> exceptions = new ArrayList<>();
                        for (Connection conn : connMap.values()) {
                            try {
                                conn.commit();
                                conn.setAutoCommit(true);
                                conn.close();
                            } catch (Exception e) {
                                exceptions.add(e);
                            }
                        }
                        if (!exceptions.isEmpty()) {
                            throw new SQLException("Failed to commit one or more transaction connections: " + exceptions.get(0).getMessage());
                        }
                    }
                }
                response.put("success", true);
                return response;
            }

            if ("ROLLBACK".equalsIgnoreCase(type)) {
                if (inTransaction) {
                    Map<String, Connection> connMap = transactionConnections.remove(sessionId);
                    if (connMap != null) {
                        for (Connection conn : connMap.values()) {
                            try {
                                conn.rollback();
                                conn.setAutoCommit(true);
                                conn.close();
                            } catch (Exception ignored) {}
                        }
                    }
                }
                response.put("success", true);
                return response;
            }

            if ("CLOSE".equalsIgnoreCase(type)) {
                if (inTransaction) {
                    Map<String, Connection> connMap = transactionConnections.remove(sessionId);
                    if (connMap != null) {
                        for (Connection conn : connMap.values()) {
                            try {
                                conn.close();
                            } catch (Exception ignored) {}
                        }
                    }
                }
                response.put("success", true);
                return response;
            }

            // Route to correct MariaDB container pool
            String dbKey = determineDatabaseKey(sql);
            cleanedSql = cleanSql(sql);

            Connection conn = null;
            if (inTransaction) {
                Map<String, Connection> connMap = transactionConnections.get(sessionId);
                if (connMap != null) {
                    conn = connMap.get(dbKey);
                    if (conn == null) {
                        DataSource ds = dataSources.get(dbKey);
                        if (ds == null) {
                            throw new SQLException("Data source connection pool not found for key: " + dbKey);
                        }
                        conn = ds.getConnection();
                        conn.setAutoCommit(false);
                        connMap.put(dbKey, conn);
                    }
                }
            } else {
                DataSource ds = dataSources.get(dbKey);
                if (ds == null) {
                    throw new SQLException("Data source connection pool not found for key: " + dbKey);
                }
                conn = ds.getConnection();
            }

            if ("QUERY".equalsIgnoreCase(type)) {
                try (PreparedStatement ps = conn.prepareStatement(cleanedSql)) {
                    bindParams(ps, params);
                    try (ResultSet rs = ps.executeQuery()) {
                        List<Map<String, Object>> rows = new ArrayList<>();
                        ResultSetMetaData md = rs.getMetaData();
                        int colCount = md.getColumnCount();

                        while (rs.next()) {
                            Map<String, Object> row = new HashMap<>();
                            for (int i = 1; i <= colCount; i++) {
                                String colName = md.getColumnLabel(i);
                                Object val = rs.getObject(i);
                                row.put(colName, safeValue(val));
                            }
                            rows.add(row);
                        }

                        response.put("success", true);
                        response.put("rows", rows);
                    }
                }
            } else if ("UPDATE".equalsIgnoreCase(type)) {
                try (PreparedStatement ps = conn.prepareStatement(cleanedSql)) {
                    bindParams(ps, params);
                    int affectedRows = ps.executeUpdate();
                    response.put("success", true);
                    response.put("affectedRows", affectedRows);
                }
            } else {
                response.put("error", "Unknown operation type: " + type);
            }

            // If not in a transaction session, close the connection immediately
            if (!inTransaction && conn != null) {
                try {
                    conn.close();
                } catch (SQLException ignored) {}
            }

        } catch (Exception e) {
            System.err.println("SQL execution error in socket handler: " + e.getMessage() + " for query: " + sql + " (routed: " + cleanedSql + ")");
            response.put("success", false);
            response.put("error", e.getMessage());
        }

        return response;
    }

    private String determineDatabaseKey(String sql) {
        if (sql == null) {
            return "coordinator";
        }
        String upper = sql.toUpperCase();
        if (upper.contains("SHARD_0.")) {
            return "shard1";
        } else if (upper.contains("SHARD_1.")) {
            return "shard2";
        } else if (upper.contains("CORE.")) {
            return "coordinator";
        }
        return "coordinator";
    }

    private String cleanSql(String sql) {
        if (sql == null) return null;
        // Strip out Postgres qualifiers core.*, shard_0.*, shard_1.* case insensitively
        // and escape MariaDB reserved keyword `condition` as `condition`
        return sql
            .replaceAll("(?i)\\bshard_0\\.", "")
            .replaceAll("(?i)\\bshard_1\\.", "")
            .replaceAll("(?i)\\bcore\\.", "")
            .replaceAll("(?i)\\bcondition\\b", "`condition`");
    }

    private void bindParams(PreparedStatement ps, List<Object> params) throws SQLException {
        if (params == null) return;
        for (int i = 0; i < params.size(); i++) {
            Object param = params.get(i);
            if (param instanceof String && ((String) param).startsWith("timestamp:")) {
                String timestampStr = ((String) param).substring("timestamp:".length());
                try {
                    ps.setTimestamp(i + 1, java.sql.Timestamp.valueOf(timestampStr.replace("T", " ").replace("Z", "")));
                } catch (Exception e) {
                    ps.setTimestamp(i + 1, java.sql.Timestamp.valueOf(timestampStr));
                }
            } else {
                ps.setObject(i + 1, param);
            }
        }
    }

    private static Object safeValue(Object val) {
        if (val == null) return null;
        if (val instanceof java.sql.Timestamp) {
            return ((java.sql.Timestamp) val).toInstant().toString();
        }
        if (val instanceof java.sql.Date) {
            return ((java.sql.Date) val).toString();
        }
        if (val instanceof java.sql.Time) {
            return ((java.sql.Time) val).toString();
        }
        if (val instanceof java.util.UUID) {
            return val.toString();
        }
        if (val instanceof java.math.BigDecimal) {
            return ((java.math.BigDecimal) val).doubleValue();
        }
        return val;
    }
}
