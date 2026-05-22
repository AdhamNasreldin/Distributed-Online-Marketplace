package com.marketplace.db;

public class ShardRouting {
    public static final String[] SHARD_SCHEMAS = {"shard_0", "shard_1"};

    public static String shardForKey(String value) {
        if (value == null) {
            return "shard_0";
        }
        int score = 0;
        for (int i = 0; i < value.length(); i++) {
            score += value.charAt(i);
        }
        return SHARD_SCHEMAS[score % SHARD_SCHEMAS.length];
    }

    public static String assertShardSchema(String value) {
        if ("shard_0".equals(value) || "shard_1".equals(value)) {
            return value;
        }
        throw new IllegalArgumentException("Invalid shard schema: " + value);
    }
}
