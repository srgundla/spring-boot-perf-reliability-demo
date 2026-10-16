package io.demo.catalog.chaos;

import org.springframework.stereotype.Component;

/** Runtime-adjustable fault injection so the resilience demo does not need a restart. */
@Component
public class ChaosState {

    private volatile long latencyMs = 0;
    private volatile double errorRate = 0.0;

    public long latencyMs() {
        return latencyMs;
    }

    public double errorRate() {
        return errorRate;
    }

    public void set(long latencyMs, double errorRate) {
        this.latencyMs = Math.max(0, latencyMs);
        this.errorRate = Math.clamp(errorRate, 0.0, 1.0);
    }

    public void reset() {
        set(0, 0.0);
    }
}
