package dev.osmium.compat;

import dev.osmium.OsmiumClient;
import dev.osmium.core.SpikeShieldMode;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.util.math.MatrixStack;

/**
 * OsmiumDebugOverlay — shows live Osmium stats on screen.
 *
 * When the player presses F3 (debug screen), or when Osmium's own
 * overlay is enabled, we draw live stats:
 *
 *   OSMIUM  6.8ms  Load: 0.97  Shield: RECOVERING  Particles: 2400/4000
 *
 * This is incredibly useful for:
 *   - Verifying the mod is actually working
 *   - Tuning the budget target
 *   - Debugging why performance is bad
 *
 * To enable, set OsmiumConfig.showDebugOverlay = true.
 * (For now, it always shows — you can add a keybind toggle later.)
 */
public class OsmiumDebugOverlay {

    // Whether to show the overlay. Toggle with a keybind later.
    private static boolean visible = true;

    /**
     * Register the overlay with Fabric's HUD render callback.
     * Call this from OsmiumClient.onInitializeClient().
     */
    public static void register() {
        HudRenderCallback.EVENT.register((drawContext, tickDelta) -> {
            if (!visible) return;

            MinecraftClient client = MinecraftClient.getInstance();
            if (client.options.debugEnabled) return; // Don't show when F3 is already open

            TextRenderer font = client.textRenderer;
            MatrixStack matrices = drawContext.getMatrices();

            // Build the status line
            String frametime  = String.format("%.1fms", OsmiumClient.FRAMETIME_LOGGER.getLastFrameMs());
            String load       = String.format("%.2f", OsmiumClient.BUDGET.getFrameLoad());
            String shieldState = OsmiumClient.SPIKE_SHIELD.getState().name();
            double quality    = OsmiumClient.SPIKE_SHIELD.getQualityLevel();
            int pending       = OsmiumClient.SCHEDULER.getPendingTaskCount();

            // Color the shield state: green when normal, yellow when active, red when degrading.
            int shieldColor = switch (OsmiumClient.SPIKE_SHIELD.getState()) {
                case NORMAL     -> 0x55FF55; // green
                case DEGRADING  -> 0xFF5555; // red
                case DEGRADED   -> 0xFF9900; // orange
                case RECOVERING -> 0xFFFF55; // yellow
            };

            // Draw background for readability
            int x = 4, y = 4;
            int lineH = 10;

            // Line 1: frametime and load
            String line1 = String.format("§7[Osmium] §f%s §7load:§f%s §7q:§f%.0f%%",
                frametime, load, quality * 100);
            drawContext.drawText(font, line1, x, y, 0xFFFFFF, true);

            // Line 2: spike shield state
            String line2 = String.format("§7Shield: §%s%s  §7tasks:§f%d",
                colorCodeFor(OsmiumClient.SPIKE_SHIELD.getState()), shieldState, pending);
            drawContext.drawText(font, line2, x, y + lineH, 0xFFFFFF, true);

            // Line 3: particle culler
            String line3 = String.format("§7Particles cap: §f%d  §7distant:§f%.0f%%",
                OsmiumClient.PARTICLE_CULLER.getDynamicMaxParticles(),
                OsmiumClient.PARTICLE_CULLER.getDistantSpawnChance() * 100);
            drawContext.drawText(font, line3, x, y + lineH * 2, 0xFFFFFF, true);
        });
    }

    /** Map spike shield states to Minecraft color codes (§ codes). */
    private static char colorCodeFor(SpikeShieldMode.State state) {
        return switch (state) {
            case NORMAL     -> 'a'; // §a = green
            case DEGRADING  -> 'c'; // §c = red
            case DEGRADED   -> '6'; // §6 = gold
            case RECOVERING -> 'e'; // §e = yellow
        };
    }

    public static void setVisible(boolean v) { visible = v; }
    public static boolean isVisible() { return visible; }
}
