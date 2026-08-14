package com.snkisk.hypixellegitils.stats;

/** Safe, keyless result of Companion's one-shot Hypixel API-key validation. */
public enum HypixelKeyValidationResult {
    VALID,
    INVALID,
    UNAVAILABLE,
    ALREADY_REQUESTED
}
