package com.snkisk.hypixellegitils.alert;

import com.snkisk.hypixellegitils.evidence.Evidence;

public interface AlertSink {
    void accept(Evidence evidence, long nowMillis);
    AlertPresentation presentation(long nowMillis);
    void reset();
}
