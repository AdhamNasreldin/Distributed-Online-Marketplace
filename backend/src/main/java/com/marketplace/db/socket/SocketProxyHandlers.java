package com.marketplace.db.socket;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class SocketProxyHandlers {

    public static class SocketConnectionHandler implements InvocationHandler {
        private final SocketClient socketClient;
        private final String sessionId;
        private boolean autoCommit = true;

        public SocketConnectionHandler(SocketClient socketClient) {
            this.socketClient = socketClient;
            this.sessionId = UUID.randomUUID().toString();
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            String methodName = method.getName();
            if (methodName.equals("setAutoCommit")) {
                this.autoCommit = (boolean) args[0];
                if (!autoCommit) {
                    SocketResponse response = socketClient.sendRequest(new SocketRequest(sessionId, "BEGIN", null, null));
                    if (!response.isSuccess()) {
                        throw new SQLException(response.getError());
                    }
                }
                return null;
            } else if (methodName.equals("getAutoCommit")) {
                return this.autoCommit;
            } else if (methodName.equals("commit")) {
                SocketResponse response = socketClient.sendRequest(new SocketRequest(sessionId, "COMMIT", null, null));
                if (!response.isSuccess()) {
                    throw new SQLException(response.getError());
                }
                return null;
            } else if (methodName.equals("rollback")) {
                SocketResponse response = socketClient.sendRequest(new SocketRequest(sessionId, "ROLLBACK", null, null));
                if (!response.isSuccess()) {
                    throw new SQLException(response.getError());
                }
                return null;
            } else if (methodName.equals("close")) {
                socketClient.sendRequest(new SocketRequest(sessionId, "CLOSE", null, null));
                return null;
            } else if (methodName.equals("prepareStatement")) {
                String sql = (String) args[0];
                return Proxy.newProxyInstance(
                    Connection.class.getClassLoader(),
                    new Class[]{PreparedStatement.class},
                    new SocketPreparedStatementHandler(socketClient, sessionId, sql)
                );
            } else if (methodName.equals("isClosed")) {
                return false;
            } else if (methodName.equals("isValid")) {
                return true;
            } else if (methodName.equals("getMetaData")) {
                return null;
            } else if (methodName.equals("unwrap") || methodName.equals("isWrapperFor")) {
                return false;
            }
            return null;
        }
    }

    public static class SocketPreparedStatementHandler implements InvocationHandler {
        private final SocketClient socketClient;
        private final String sessionId;
        private final String sql;
        private final List<Object> params = new ArrayList<>();

        public SocketPreparedStatementHandler(SocketClient socketClient, String sessionId, String sql) {
            this.socketClient = socketClient;
            this.sessionId = sessionId;
            this.sql = sql;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            String methodName = method.getName();
            if (methodName.startsWith("set") && args.length >= 2 && args[0] instanceof Integer) {
                int index = (int) args[0] - 1;
                Object value = args[1];
                while (params.size() <= index) {
                    params.add(null);
                }
                // Handle Timestamp special formatting for socket
                if (value instanceof java.sql.Timestamp) {
                    params.set(index, "timestamp:" + value.toString());
                } else {
                    params.set(index, value);
                }
                return null;
            } else if (methodName.equals("setNull")) {
                int index = (int) args[0] - 1;
                while (params.size() <= index) {
                    params.add(null);
                }
                params.set(index, null);
                return null;
            } else if (methodName.equals("executeQuery")) {
                SocketResponse response = socketClient.sendRequest(new SocketRequest(sessionId, "QUERY", sql, params));
                if (!response.isSuccess()) {
                    throw new SQLException(response.getError());
                }
                return Proxy.newProxyInstance(
                    PreparedStatement.class.getClassLoader(),
                    new Class[]{ResultSet.class},
                    new SocketResultSetHandler(response.getRows())
                );
            } else if (methodName.equals("executeUpdate") || methodName.equals("execute")) {
                SocketResponse response = socketClient.sendRequest(new SocketRequest(sessionId, "UPDATE", sql, params));
                if (!response.isSuccess()) {
                    throw new SQLException(response.getError());
                }
                if (methodName.equals("executeUpdate")) {
                    return response.getAffectedRows();
                }
                return true;
            } else if (methodName.equals("getUpdateCount")) {
                return 0;
            } else if (methodName.equals("close")) {
                return null;
            }
            return null;
        }
    }

    public static class SocketResultSetHandler implements InvocationHandler {
        private final List<Map<String, Object>> rows;
        private int cursor = -1;
        private Map<String, Object> currentRow = null;
        private boolean wasLastNull = false;

        public SocketResultSetHandler(List<Map<String, Object>> rows) {
            this.rows = rows != null ? rows : new ArrayList<>();
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            String methodName = method.getName();
            if (methodName.equals("next")) {
                cursor++;
                boolean hasNext = cursor < rows.size();
                currentRow = hasNext ? rows.get(cursor) : null;
                return hasNext;
            } else if (methodName.startsWith("get") && args.length >= 1) {
                if (currentRow == null) {
                    throw new SQLException("No current row in ResultSet");
                }
                Object key = args[0];
                Object value = null;

                if (key instanceof String) {
                    String column = (String) key;
                    value = currentRow.get(column);
                    if (value == null) {
                        for (Map.Entry<String, Object> entry : currentRow.entrySet()) {
                            if (entry.getKey().equalsIgnoreCase(column)) {
                                value = entry.getValue();
                                break;
                            }
                        }
                    }
                } else if (key instanceof Integer) {
                    int index = (int) key - 1;
                    List<Object> values = new ArrayList<>(currentRow.values());
                    if (index >= 0 && index < values.size()) {
                        value = values.get(index);
                    }
                }

                wasLastNull = (value == null);

                if (methodName.equals("getString")) {
                    return value != null ? value.toString() : null;
                } else if (methodName.equals("getBoolean")) {
                    if (value instanceof Boolean) return value;
                    return value != null && Boolean.parseBoolean(value.toString());
                } else if (methodName.equals("getInt")) {
                    if (value instanceof Number) return ((Number) value).intValue();
                    return value != null ? Integer.parseInt(value.toString()) : 0;
                } else if (methodName.equals("getDouble")) {
                    if (value instanceof Number) return ((Number) value).doubleValue();
                    return value != null ? Double.parseDouble(value.toString()) : 0.0;
                } else if (methodName.equals("getObject")) {
                    if (value instanceof String) {
                        String s = (String) value;
                        if (s.contains("T") || s.contains("-")) {
                            try {
                                Instant instant = Instant.parse(s);
                                return java.sql.Timestamp.from(instant);
                            } catch (Exception e1) {
                                try {
                                    s = s.replace("T", " ").replace("Z", "");
                                    if (s.contains(".")) {
                                        s = s.substring(0, s.indexOf("."));
                                    }
                                    return java.sql.Timestamp.valueOf(s);
                                } catch (Exception ignored) {}
                            }
                        }
                    }
                    return value;
                }
                return value;
            } else if (methodName.equals("wasNull")) {
                return wasLastNull;
            } else if (methodName.equals("close")) {
                return null;
            } else if (methodName.equals("getMetaData")) {
                return Proxy.newProxyInstance(
                    ResultSet.class.getClassLoader(),
                    new Class[]{java.sql.ResultSetMetaData.class},
                    (p, m, a) -> {
                        if (m.getName().equals("getColumnCount")) {
                            return currentRow != null ? currentRow.size() : 0;
                        }
                        return null;
                    }
                );
            }
            return null;
        }
    }
}
