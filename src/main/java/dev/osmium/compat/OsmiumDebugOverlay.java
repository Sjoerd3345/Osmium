package dev.osmium.compat;

import dev.osmium.OsmiumClient;
import dev.osmium.core.SpikeShieldMode;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;

/**
 * OsmiumDebugOverlay — shows live Osmium stats on screen.
 *
 * Draws a small overlay in the top-left corner showing frametime,
 * load, spike shield state, and particle info.
 */
public class OsmiumDebugOverlay {

    private static boolean visible = true;

    public static void register() {
        HudRenderCallback.EVENT.register((drawContext, tickDelta) -> {
            if (!visible) return;

            MinecraftClient client = MinecraftClient.getInstance();
            if (client == null) return;
            // Hide our overlay when F3 debug screen is open
            if (client.getDebugHud().shouldShowDebugHud()) return;

            TextRenderer font = client.textRenderer;
            int x = 4, y = 4, lineH = 10;

            String line1 = String.format("§7[Osmium] §f%.1fms §7load:§f%.2f §7q:§f%.0f%%",
                OsmiumClient.FRAMETIME_LOGGER.getLastFrameMs(),
                OsmiumClient.BUDGET.getFrameLoad(),
                OsmiumClient.SPIKE_SHIELD.getQualityLevel() * 100);

            String line2 = String.format("§7Shield: §%s%s  §7tasks:§f%d",
                colorCodeFor(OsmiumClient.SPIKE_SHIELD.getState()),
                OsmiumClient.SPIKE_SHIELD.getState().name(),
                OsmiumClient.SCHEDULER.getPendingTaskCount());

            String line3 = String.format("§7Particle cap: §f%d  §7distant:§f%.0f%%",
                OsmiumClient.PARTICLE_CULLER.getDynamicMaxParticles(),
                OsmiumClient.PARTICLE_CULLER.getDistantSpawnChance() * 100);

            drawContext.drawText(font, line1, x, y, 0xFFFFFF, true);
            drawContext.drawText(font, line2, x, y + lineH, 0xFFFFFF, true);
            drawContext.drawText(font, line3, x, y + lineH * 2, 0xFFFFFF, true);
        });
    }

    private static char colorCodeFor(SpikeShieldMode.State state) {
        return switch (state) {
            case NORMAL     -> 'a';
            case DEGRADING  -> 'c';
            case DEGRADED   -> '6';
            case RECOVERING -> 'e';
        };
    }

    public static void setVisible(boolean v) { visible = v; }
    public static boolean isVisible() { return visible; }
}
