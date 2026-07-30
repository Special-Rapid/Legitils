package com.snkisk.hypixellegitils.evidence;

/** Conditions that make a local observation unreliable. */
public final class EvidencePolicyContext {
    public final long nowMillis;
    public final boolean globalLag;
    public final boolean worldTransition;
    public final boolean sufficientHistory;

    public EvidencePolicyContext(long nowMillis, boolean globalLag, boolean worldTransition, boolean sufficientHistory) {
        if (nowMillis < 0L) throw new IllegalArgumentException("Policy time must be non-negative");
        this.nowMillis = nowMillis;
        this.globalLag = globalLag;
        this.worldTransition = worldTransition;
        this.sufficientHistory = sufficientHistory;
    }
}
