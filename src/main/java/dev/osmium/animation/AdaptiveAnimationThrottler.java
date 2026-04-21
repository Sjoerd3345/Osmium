package dev.osmium.animation;

import dev.osmium.core.FrameBudgetController;

/**
 * AdaptiveAnimationThrottler — reduces animation update frequency
 * for entities that are far away or not important.
 *
 * The insight: a mob 80 blocks away doesn't need 60fps animation updates.
 * Updating it every 4th frame instead saves CPU time the player won't notice.
 *
 * How to use from a Mixin:
 *
 *   // Before updating an entity's animation:
 *   if (OsmiumClient.ANIMATION_THROTTLER.shouldUpdate(entity, distance)) {
 *       // do the animation update
 *   }
 */
public class AdaptiveAnimationThrottler {

    private final FrameBudgetController budget;

    // Frame counter — increments every tick, used to stagger updates.
    private int frameTick = 0;

    // Distance thresholds (in blocks) for animation quality tiers.
    // Entities closer than NEAR get full updates every frame.
    // Between NEAR and FAR: every other frame.
    // Beyond FAR: every 4th frame.
    private static final double NEAR_THRESHOLD = 20.0;
    private static final double FAR_THRESHOLD  = 50.0;

    public AdaptiveAnimationThrottler(FrameBudgetController budget) {
        this.budget = budget;
    }

    /**
     * Called every game tick to advance the frame counter.
     */
    public void tick() {
        frameTick++;
    }

    /**
     * The main method — call this before doing an entity animation update.
     *
     * @param entityId  the entity's unique integer ID (used to stagger different entities)
     * @param distance  distance from the player in blocks
     * @return true if the animation should update this frame, false to skip
     */
    public boolean shouldUpdate(int entityId, double distance) {
        // Always do full-rate updates when there's plenty of budget.
        if (budget.getLoadLevel() == FrameBudgetController.LoadLevel.LIGHT) {
            return true;
        }

        // The update interval depends on distance.
        int interval = getUpdateInterval(distance);

        // Stagger entities using their ID so they don't all update on the same frame.
        // (entityId % interval) == (frameTick % interval) means "this entity's turn".
        return (frameTick + entityId) % interval == 0;
    }

    /**
     * How many frames to wait between animation updates for this distance.
     *
     * 1 = update every frame (full rate, 60fps)
     * 2 = update every other frame (30fps)
     * 4 = update every 4th frame (15fps)
     */
    private int getUpdateInterval(double distance) {
        if (distance < NEAR_THRESHOLD) {
            return 1; // Always full rate for nearby entities
        }

        // Scale the interval based on frame load and distance.
        int baseInterval = (distance < FAR_THRESHOLD) ? 2 : 4;

        // Under heavy load, increase the interval further.
        if (budget.getLoadLevel() == FrameBudgetController.LoadLevel.HEAVY) {
            baseInterval *= 2;
        }

        return baseInterval;
    }

    /**
     * Returns a 0.0–1.0 interpolation precision factor for the given distance.
     *
     * Nearby entities get full interpolation (1.0).
     * Far entities get reduced precision (e.g. 0.25 = quarter precision).
     * This makes position/rotation updates snappier rather than smooth — barely
     * noticeable at distance.
     */
    public double getInterpolationPrecision(double distance) {
        if (distance < NEAR_THRESHOLD)  return 1.0;
        if (distance < FAR_THRESHOLD)   return 0.5;
        return 0.25;
    }

    /**
     * Special rule for armor stands and item frames — these are often
     * decorative and static, so we update them very rarely.
     */
    public boolean shouldUpdateArmorStand(int entityId, boolean isMoving) {
        if (isMoving) return shouldUpdate(entityId, 10.0); // Treat as nearby
        // Static armor stands: update every 8th frame only.
        return (frameTick + entityId) % 8 == 0;
    }
}
