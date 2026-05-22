package com.marketplace.db;

import com.marketplace.model.*;
import java.sql.ResultSet;
import java.sql.SQLException;

public class Mappers {

    public static String toIso(Object value) {
        if (value == null) return null;
        if (value instanceof java.sql.Timestamp) {
            return ((java.sql.Timestamp) value).toInstant().toString();
        }
        if (value instanceof java.time.OffsetDateTime) {
            return ((java.time.OffsetDateTime) value).toInstant().toString();
        }
        if (value instanceof java.util.Date) {
            return ((java.util.Date) value).toInstant().toString();
        }
        return value.toString();
    }

    public static double toDouble(Object value) {
        if (value == null) return 0.0;
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    public static User mapUser(ResultSet rs) throws SQLException {
        return new User(
            rs.getString("id"),
            rs.getString("full_name"),
            rs.getString("email"),
            toDouble(rs.getObject("balance")),
            toIso(rs.getObject("created_at")),
            rs.getBoolean("two_factor_enabled")
        );
    }

    public static StoredUser mapStoredUser(ResultSet rs, String schema) throws SQLException {
        User user = mapUser(rs);
        return new StoredUser(user, rs.getString("password_hash"), schema);
    }

    public static Product mapProduct(ResultSet rs) throws SQLException {
        return new Product(
            rs.getString("id"),
            rs.getString("owner_id"),
            rs.getString("name"),
            rs.getString("brand"),
            rs.getString("category"),
            rs.getString("description"),
            toDouble(rs.getObject("price")),
            rs.getInt("quantity"),
            rs.getString("condition"),
            rs.getString("status"),
            toIso(rs.getObject("listed_at")),
            rs.getInt("sold_count"),
            rs.getString("color")
        );
    }

    public static StoredProduct mapStoredProduct(ResultSet rs, String schema) throws SQLException {
        Product product = mapProduct(rs);
        return new StoredProduct(product, schema);
    }

    public static Purchase mapPurchase(ResultSet rs) throws SQLException {
        return new Purchase(
            rs.getString("id"),
            rs.getString("product_id"),
            rs.getString("product_name"),
            rs.getString("buyer_id"),
            rs.getString("seller_id"),
            toDouble(rs.getObject("amount")),
            toIso(rs.getObject("purchased_at")),
            rs.getString("status")
        );
    }

    public static Transaction mapTransaction(ResultSet rs) throws SQLException {
        return new Transaction(
            rs.getString("id"),
            rs.getString("type"),
            toDouble(rs.getObject("amount")),
            rs.getString("from_user_id"),
            rs.getString("to_user_id"),
            rs.getString("product_id"),
            rs.getString("description"),
            toIso(rs.getObject("created_at")),
            rs.getString("status")
        );
    }
}
