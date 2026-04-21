package dev.osmium.mixin;

import dev.osmium.OsmiumClient;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * InGameHudMixin — reduces unnecessary HUD redraws.
 *
 * Minecraft calls InGameHud.render() every frame.
 * If nothing has changed (no damage, no XP, no hotbar switch),
 * we can skip the redraw entirely and reuse the last frame's output.
 *
 * Note: true element-level caching requires rendering to a framebuffer.
 * This Mixin implements the dirty-flag tracking and skips redraws
 * when the GuiOptimizer says nothing has changed.
 *
 * The render method signature changed slightly between MC versions —
 * if this doesn't compile, check the Yarn-mapped method name for 1.20.4.
 */
@Mixin(InGameHud.class)
public class InGameHudMixin {

    /**
     * Injected at the very start of InGameHud.render().
     *
     * If nothing is dirty, we cancel the render entirely.
     * This saves the CPU time of iterating all HUD elements.
     *
     * IMPORTANT: This is intentionally conservative — we force a redraw
     * every 20 frames as a safety net (handled inside GuiOptimizer).
     * So even if we miss a dirty event, the HUD will still update within ~1 second.
     */
    @Inject(
        method = "render",
        at = @At("HEAD"),
        cancellable = true
    )
    private void osmium$onHudRenderStart(MatrixStack matrices, float tickDelta, CallbackInfo ci) {
        if (!OsmiumClient.GUI_OPTIMIZER.isAnyElementDirty()) {
            // Nothing changed — skip this HUD render.
            ci.cancel();
        }
    }

    /**
     * Injected at the end of InGameHud.render().
     *
     * Once we've redrawn, clear all dirty flags so the next frame
     * can be skipped (if nothing changes again).
     */
    @Inject(
        method = "render",
        at = @At("TAIL")
    )
    private void osmium$onHudRenderEnd(MatrixStack matrices, float tickDelta, CallbackInfo ci) {
        OsmiumClient.GUI_OPTIMIZER.clearAllDirty();
    }
}
