package com.mini_Postman.util;

import java.util.UUID;

public class IdGenerator {

    private IdGenerator() {
        // Utility class
    }

    public static String generateId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }
}
