package dev.osmium.core;

import dev.osmium.OsmiumClient;

/**
 * SpikeShieldMode — Osmium's signature feature.
 *
 * When a lag spike is detected, instead of freezing, the game
 * gradually degrades visual quality to absorb the load, then
 * slowly restores quality once things calm down.
 *
 * The experience: a smooth dip instead of a hard freeze.
 *
 * State machine:
 *
 *   NORMAL ──(spike detected)──► DEGRADING
 *   DEGRADING ──(fully degraded)──► DEGRADED
 *   DEGRADED ──(spike gone for 60 frames)──► RECOVERING
 *   RECOVERING ──(fully recovered)──► NORMAL
 */
public class SpikeShieldMode {

    // How many consecutive spike frames before we trigger Shield Mode.
    private static final int SPIKE_TRIGGER_FRAMES = 3;

    // How many stable frames before we start recovering.
    private static final int STABLE_BEFORE_RECOVERY = 60;

    // How quickly quality degrades (0.0–1.0 per tick, lower = slower)
    private static final double DEGRADE_SPEED   = 0.15;

    // How quickly quality recovers
    private static final double RECOVERY_SPEED  = 0.03;

    private final FrametimeLogger logger;

    // Current state in the state machine.
    private State state = State.NORMAL;

    // Quality level: 1.0 = full quality, 0.0 = minimum quality.
    // All visual systems read this to know how much to reduce their work.
    private double qualityLevel = 1.0;

    // Counts frames of stability during DEGRADED state.
    private int stableFrameCount = 0;

    public SpikeShieldMode(FrametimeLogger logger) {
        this.logger = logger;
    }

    /**
     * Called every game tick. Drives the state machine.
     */
    public void tick() {
        // Check if we're currently spiking.
        // A spike = last frame took >2× the target frametime.
        double targetMs = OsmiumClient.BUDGET.getTargetFrametimeMs();
        double lastMs   = logger.getLastFrameMs();
        boolean isSpiking = lastMs > (targetMs * 2.0);

        switch (state) {
            case NORMAL -> {
                if (isSpiking) {
                    logger.incrementSpikeCount();
                    if (logger.getSpikeFrameCount() >= SPIKE_TRIGGER_FRAMES) {
                        // Spike confirmed — start degrading.
                        transitionTo(State.DEGRADING);
                    }
                } else {
                    logger.resetSpikeCount();
                }
            }

            case DEGRADING -> {
                // Smoothly lower quality each tick.
                qualityLevel = Math.max(0.0, qualityLevel - DEGRADE_SPEED);
                if (qualityLevel <= 0.0) {
                    transitionTo(State.DEGRADED);
                }
            }

            case DEGRADED -> {
                // Wait here until things calm down.
                if (!isSpiking) {
                    stableFrameCount++;
                    if (stableFrameCount >= STABLE_BEFORE_RECOVERY) {
                        transitionTo(State.RECOVERING);
                    }
                } else {
                    stableFrameCount = 0; // Reset if still spiking
                }
            }

            case RECOVERING -> {
                // Slowly bring quality back up.
                qualityLevel = Math.min(1.0, qualityLevel + RECOVERY_SPEED);
                if (qualityLevel >= 1.0) {
                    transitionTo(State.NORMAL);
                }
                // If we spike again mid-recovery, go back to degrading.
                if (isSpiking) {
                    transitionTo(State.DEGRADING);
                }
            }
        }
    }

    private void transitionTo(State newState) {
        OsmiumClient.LOGGER.debug("SpikeShield: {} → {}", state, newState);
        if (newState == State.DEGRADED) stableFrameCount = 0;
        state = newState;
    }

    /**
     * The current quality level, 0.0 to 1.0.
     *
     * All visual systems should scale their quality by this value.
     * Example: particle density = baseCount * qualityLevel
     */
    public double getQualityLevel() { return qualityLevel; }

    /**
     * True when Spike Shield is actively managing quality.
     * Useful for showing an indicator in the debug overlay.
     */
    public boolean isActive() { return state != State.NORMAL; }

    public State getState() { return state; }

    /**
     * The four states Spike Shield moves through.
     */
    public enum State {
        NORMAL,     // Everything fine, full quality
        DEGRADING,  // Spike detected, reducing quality
        DEGRADED,   // At minimum quality, waiting for stability
        RECOVERING  // Gradually restoring quality
    }
}
