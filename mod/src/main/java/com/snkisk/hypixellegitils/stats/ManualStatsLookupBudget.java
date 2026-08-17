package com.snkisk.hypixellegitils.stats;

import java.util.concurrent.atomic.AtomicInteger;

/** Bounded reservations for explicit local Stats lookups, retained through Chat delivery. */
public final class ManualStatsLookupBudget {
    private final int maximumPending;
    private final AtomicInteger pending = new AtomicInteger(0);

    public ManualStatsLookupBudget(int maximumPending) {
        if (maximumPending < 1) throw new IllegalArgumentException("Maximum must be positive");
        this.maximumPending = maximumPending;
    }

    /** Reserves one request and its eventual local Chat result without queuing unbounded work. */
    public boolean tryReserve() {
        while (true) {
            int current = pending.get();
            if (current >= maximumPending) return false;
            if (pending.compareAndSet(current, current + 1)) return true;
        }
    }

    /** Releases one result after it is delivered or discarded by a world transition. */
    public void release() {
        while (true) {
            int current = pending.get();
            if (current == 0) return;
            if (pending.compareAndSet(current, current - 1)) return;
        }
    }

    int pendingCount() {
        return pending.get();
    }
}
