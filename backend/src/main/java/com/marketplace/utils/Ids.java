package com.marketplace.utils;

import java.util.UUID;

public class Ids {
    public static String makeId(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8);
    }
}
