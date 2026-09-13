package com.taskflow.util;

/**
 * Central place for configurable defaults. Keeping this separate (rather
 * than hardcoding magic numbers across the app) is what satisfies the
 * "Scalability" and "Maintainability" non-functional requirements —
 * pool size, retry limits, etc. can be tuned in one place.
 */
public class ConfigLoader {

    private static final int DEFAULT_POOL_SIZE = 4;

    public static int getDefaultPoolSize() {
        return DEFAULT_POOL_SIZE;
    }
}
