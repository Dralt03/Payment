package com.payment.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Binds all 'payment.*' keys from application.yml into a strongly-typed class.
 *
 * WHY DO THIS instead of @Value?
 *   - One place to find all custom config (no searching across the codebase).
 *   - Type-safe: typos in yml cause a startup failure, not a runtime NPE.
 *   - Easily injectable anywhere with a single @Autowired field.
 *   - Supports nested objects (Psp, Scheduler inner classes below).
 *
 * application.yml bindings:
 *   payment.psp.mock-failure-rate  → psp.mockFailureRate
 *   payment.psp.mock-delay-ms      → psp.mockDelayMs
 *   payment.scheduler.poll-interval-ms → scheduler.pollIntervalMs
 */
@Component
@ConfigurationProperties(prefix = "payment")
public class PaymentProperties {

    private Psp psp = new Psp();
    private Scheduler scheduler = new Scheduler();

    // ---- Getters / Setters (needed by Spring's binding mechanism) ----

    public Psp getPsp() { return psp; }
    public void setPsp(Psp psp) { this.psp = psp; }

    public Scheduler getScheduler() { return scheduler; }
    public void setScheduler(Scheduler scheduler) { this.scheduler = scheduler; }

    // ---- Nested config groups ----

    public static class Psp {
        /** Fraction of PSP calls that will randomly fail (0.0–1.0). Default 10%. */
        private double mockFailureRate = 0.10;
        /** Simulated PSP processing delay in milliseconds. */
        private long mockDelayMs = 500;

        public double getMockFailureRate() { return mockFailureRate; }
        public void setMockFailureRate(double mockFailureRate) { this.mockFailureRate = mockFailureRate; }

        public long getMockDelayMs() { return mockDelayMs; }
        public void setMockDelayMs(long mockDelayMs) { this.mockDelayMs = mockDelayMs; }
    }

    public static class Scheduler {
        /** How often (ms) the scheduler polls for due payments. Default 60s. */
        private long pollIntervalMs = 60_000;

        public long getPollIntervalMs() { return pollIntervalMs; }
        public void setPollIntervalMs(long pollIntervalMs) { this.pollIntervalMs = pollIntervalMs; }
    }
}
