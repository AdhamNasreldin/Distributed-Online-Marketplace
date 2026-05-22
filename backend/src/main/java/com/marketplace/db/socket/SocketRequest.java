package com.marketplace.db.socket;

import java.util.List;

public class SocketRequest {
    private String sessionId;
    private String type;
    private String sql;
    private List<Object> params;

    public SocketRequest() {}

    public SocketRequest(String sessionId, String type, String sql, List<Object> params) {
        this.sessionId = sessionId;
        this.type = type;
        this.sql = sql;
        this.params = params;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getSql() {
        return sql;
    }

    public void setSql(String sql) {
        this.sql = sql;
    }

    public List<Object> getParams() {
        return params;
    }

    public void setParams(List<Object> params) {
        this.params = params;
    }
}
