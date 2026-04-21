package dev.osmium.core;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * FrametimeLogger — measures how long each frame actually takes.
 *
 * Every frame:
 *   1. beginFrame() records the start time in nanoseconds.
 *   2. endFrame()   computes elapsed time, stores it, returns milliseconds.
 *
 * It keeps a rolling history of the last 60 frames so we can detect
 * whether performance is stable or spiking.
 */
public class FrametimeLogger {

    // How many frames of history to keep.
    private static final int HISTORY_SIZE = 60;

    // System.nanoTime() value at the start of the current frame.
    // "volatile" = safe to read from multiple threads.
    private volatile long frameStartNanos = 0;

    // A sliding window of the last 60 frame times (in milliseconds).
    // Deque = double-ended queue — we add to the end and remove from the front.
    private final Deque<Double> history = new ArrayDeque<>(HISTORY_SIZE);

    // The most recent frame time, in milliseconds.
    private volatile double lastFrameMs = 0.0;

    // How many consecutive frames have been above 2× the target.
    // Used by SpikeShieldMode to decide when to trigger.
    private int spikeFrameCount = 0;

    /**
     * Call this at the very start of each frame.
     * Records the current time so we can measure duration later.
     */
    public void beginFrame() {
        frameStartNanos = System.nanoTime();
    }

    /**
     * Call this at the end of each frame.
     * Computes how long the frame took and stores it.
     *
     * @return elapsed time in milliseconds (e.g. 6.8, 14.2)
     */
    public double endFrame() {
        if (frameStartNanos == 0) {
            // beginFrame() was never called — return 0 safely.
            return 0.0;
        }

        long nowNanos = System.nanoTime();
        // Convert nanoseconds → milliseconds: divide by 1,000,000
        double elapsedMs = (nowNanos - frameStartNanos) / 1_000_000.0;

        lastFrameMs = elapsedMs;

        // Add to history, trimming if we've exceeded HISTORY_SIZE
        history.addLast(elapsedMs);
        if (history.size() > HISTORY_SIZE) {
            history.removeFirst();
        }

        return elapsedMs;
    }

    /**
     * The most recent frame time in milliseconds.
     * Safe to call from anywhere.
     */
    public double getLastFrameMs() {
        return lastFrameMs;
    }

    /**
     * Average frametime over the last 60 frames.
     * A smooth number even if individual frames vary.
     */
    public double getAverageFrameMs() {
        if (history.isEmpty()) return 0.0;
        double sum = 0;
        for (double v : history) sum += v;
        return sum / history.size();
    }

    /**
     * The worst (highest) frametime in the last 60 frames.
     * If this is much higher than the average, you have spikes.
     */
    public double getWorstFrameMs() {
        double worst = 0;
        for (double v : history) {
            if (v > worst) worst = v;
        }
        return worst;
    }

    /**
     * Returns true if performance looks stable right now.
     * "Stable" = last frame was within 50% of the average.
     */
    public boolean isStable() {
        double avg = getAverageFrameMs();
        if (avg == 0) return true;
        return lastFrameMs <= avg * 1.5;
    }

    /**
     * Increments the spike counter. Called by SpikeShieldMode.
     */
    public void incrementSpikeCount() { spikeFrameCount++; }

    /**
     * Resets the spike counter back to zero.
     */
    public void resetSpikeCount() { spikeFrameCount = 0; }

    /**
     * How many consecutive spike frames have we seen?
     */
    public int getSpikeFrameCount() { return spikeFrameCount; }
}
