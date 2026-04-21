package dev.osmium.gui;

/**
 * GuiOptimizer — prevents unnecessary UI redraws.
 *
 * Minecraft redraws the entire HUD every frame by default.
 * Many elements (hotbar, health bar, experience bar) only change
 * when something actually happens — not every 6.9ms.
 *
 * This class implements a "dirty flag" system:
 *   - When something changes (health, hunger, xp), mark the UI dirty.
 *   - The HUD mixin only redraws if dirty, then clears the flag.
 *   - On unchanged frames, the cached render is reused.
 *
 * The InGameHudMixin hooks into this class.
 */
public class GuiOptimizer {

    // Individual dirty flags for each HUD element.
    // true = needs redraw. false = can use cache.
    private boolean healthDirty     = true;
    private boolean hungerDirty     = true;
    private boolean xpBarDirty      = true;
    private boolean hotbarDirty     = true;
    private boolean armorDirty      = true;
    private boolean airBubblesDirty = true;

    // Frame counter — force a full redraw every 20 frames
    // as a safety net (in case we miss a dirty event).
    private int framesSinceForceRedraw = 0;
    private static final int FORCE_REDRAW_INTERVAL = 20;

    /**
     * Called every frame by the HUD mixin.
     * Returns true if ANY element needs redrawing.
     */
    public boolean isAnyElementDirty() {
        framesSinceForceRedraw++;
        if (framesSinceForceRedraw >= FORCE_REDRAW_INTERVAL) {
            framesSinceForceRedraw = 0;
            return true; // Force redraw as safety net
        }
        return healthDirty || hungerDirty || xpBarDirty
            || hotbarDirty || armorDirty || airBubblesDirty;
    }

    /**
     * After redrawing, clear all dirty flags.
     * The mixin calls this once the HUD has been redrawn.
     */
    public void clearAllDirty() {
        healthDirty = hungerDirty = xpBarDirty =
        hotbarDirty = armorDirty = airBubblesDirty = false;
    }

    // --- Mark specific elements dirty when game state changes ---
    // These are called from wherever that state changes in Minecraft.
    // For example, when the player takes damage, call markHealthDirty().

    public void markHealthDirty()     { healthDirty = true; }
    public void markHungerDirty()     { hungerDirty = true; }
    public void markXpBarDirty()      { xpBarDirty  = true; }
    public void markHotbarDirty()     { hotbarDirty = true; }
    public void markArmorDirty()      { armorDirty  = true; }
    public void markAirBubblesDirty() { airBubblesDirty = true; }

    /** Mark everything dirty — call when entering/leaving a GUI. */
    public void markAllDirty() {
        healthDirty = hungerDirty = xpBarDirty =
        hotbarDirty = armorDirty = airBubblesDirty = true;
    }

    // Getters for individual flags (used by mixin for element-level caching)
    public boolean isHealthDirty()     { return healthDirty; }
    public boolean isHungerDirty()     { return hungerDirty; }
    public boolean isXpBarDirty()      { return xpBarDirty; }
    public boolean isHotbarDirty()     { return hotbarDirty; }
    public boolean isArmorDirty()      { return armorDirty; }
    public boolean isAirBubblesDirty() { return airBubblesDirty; }
}
