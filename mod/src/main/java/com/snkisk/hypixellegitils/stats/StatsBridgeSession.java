package com.snkisk.hypixellegitils.stats;

import java.util.concurrent.atomic.AtomicLong;

/** Prevents an asynchronous result from an old client world leaking into a new one. */
public final class StatsBridgeSession {
    private final AtomicLong generation = new AtomicLong(0L);

    public long currentGeneration() {
        return generation.get();
    }

    public long reset() {
        return generation.incrementAndGet();
    }

    public boolean isCurrent(long candidate) {
        return generation.get() == candidate;
    }
}
