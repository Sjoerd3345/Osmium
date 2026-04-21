package dev.osmium.core;

/**
 * FrameBudgetController — the brain of Osmium.
 *
 * This class answers one question for every other system in the mod:
 *   "How much time do I have left this frame?"
 *
 * Every frame:
 *   - We know the target frametime (e.g. 6.9ms for 144 FPS)
 *   - We measure how much of that time has already been used
 *   - The remaining budget tells systems whether they can do work
 *
 * If a system asks for budget and there isn't enough, it skips or defers
 * its work to a future frame — preventing spikes.
 */
public class FrameBudgetController {

    // The target FPS we're aiming for (e.g. 144).
    private final int targetFps;

    // Target milliseconds per frame. For 144 FPS: 1000ms / 144 = ~6.94ms
    private final double targetFrametimeMs;

    // How much of the frame budget has been consumed so far this frame.
    // Reset to 0 at the start of each frame.
    private double consumedMs = 0.0;

    // The most recent actual frame time, updated by update().
    private double lastActualMs = 0.0;

    // A 0.0–1.0 measure of how loaded the frame is.
    // 0.0 = idle, 1.0 = exactly at budget, >1.0 = over budget.
    private double frameLoad = 0.0;

    /**
     * Create a FrameBudgetController targeting the given FPS.
     *
     * @param targetFps  e.g. 60, 144, 240
     */
    public FrameBudgetController(int targetFps) {
        this.targetFps = targetFps;
        // Calculate milliseconds per frame from the FPS target
        this.targetFrametimeMs = 1000.0 / targetFps;
    }

    /**
     * Called every frame with the real elapsed time.
     * Updates our understanding of how loaded the frame is.
     *
     * @param actualFrameMs  how long the last frame actually took
     */
    public void update(double actualFrameMs) {
        this.lastActualMs = actualFrameMs;
        // frameLoad: 1.0 = we used exactly our budget.
        // 0.5 = we had plenty of headroom. 1.5 = we were 50% over.
        this.frameLoad = actualFrameMs / targetFrametimeMs;
        // Reset consumed budget for the new frame
        this.consumedMs = 0.0;
    }

    /**
     * The most important method. Call this before doing expensive work.
     *
     * Returns true  → you have budget, go ahead.
     * Returns false → you're out of budget, skip or defer this work.
     *
     * @param estimatedCostMs  how many milliseconds your task will take
     */
    public boolean hasBudget(double estimatedCostMs) {
        return (consumedMs + estimatedCostMs) <= targetFrametimeMs;
    }

    /**
     * After doing work, tell the controller how much time you spent.
     * This reduces the remaining budget for other systems this frame.
     *
     * @param costMs  how many milliseconds you actually used
     */
    public void consume(double costMs) {
        consumedMs += costMs;
    }

    /**
     * How many milliseconds remain in this frame's budget.
     * Can be negative if we've already gone over.
     */
    public double getRemainingBudgetMs() {
        return targetFrametimeMs - consumedMs;
    }

    /**
     * Frame load as a 0.0–1.0+ value.
     * Useful for systems that want to scale their work proportionally.
     *
     * Examples:
     *   0.3 → very light load, do full-quality work
     *   0.8 → moderate load, consider reducing quality
     *   1.2 → over budget, reduce quality significantly
     */
    public double getFrameLoad() {
        return frameLoad;
    }

    /**
     * A convenience method that returns one of three load levels.
     * Easier to use than raw frameLoad numbers.
     */
    public LoadLevel getLoadLevel() {
        if (frameLoad < 0.6) return LoadLevel.LIGHT;
        if (frameLoad < 0.9) return LoadLevel.MODERATE;
        return LoadLevel.HEAVY;
    }

    public double getTargetFrametimeMs() { return targetFrametimeMs; }
    public double getLastActualMs()      { return lastActualMs; }
    public int    getTargetFps()         { return targetFps; }

    /**
     * Simple enum representing how loaded the current frame is.
     * Used by subsystems to pick their quality level.
     */
    public enum LoadLevel {
        LIGHT,    // Plenty of headroom — use full quality
        MODERATE, // Getting busy — consider light reductions
        HEAVY     // Over or near budget — reduce quality
    }
}
