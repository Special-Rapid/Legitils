package com.snkisk.hypixellegitils.evidence;

public final class PolicyDecision {
    public final boolean shouldAlert;
    public final String reason;
    public final long retryAfterMillis;

    private PolicyDecision(boolean shouldAlert, String reason, long retryAfterMillis) {
        this.shouldAlert = shouldAlert;
        this.reason = reason;
        this.retryAfterMillis = retryAfterMillis;
    }

    public static PolicyDecision allow() {
        return new PolicyDecision(true, "allowed", 0L);
    }

    public static PolicyDecision suppress(String reason, long retryAfterMillis) {
        return new PolicyDecision(false, reason, Math.max(0L, retryAfterMillis));
    }
}
