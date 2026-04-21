package dev.osmium.mixin;

import dev.osmium.OsmiumClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * InGameHudMixin — reduces unnecessary HUD redraws.
 *
 * Hooks into InGameHud.render() to skip redraws when nothing has changed.
 * Uses GuiOptimizer's dirty-flag system to track what needs updating.
 */
@Mixin(InGameHud.class)
public class InGameHudMixin {

    @Inject(
        method = "render",
        at = @At("HEAD"),
        cancellable = true
    )
    private void osmium$onHudRenderStart(DrawContext context, float tickDelta, CallbackInfo ci) {
        if (!OsmiumClient.GUI_OPTIMIZER.isAnyElementDirty()) {
            ci.cancel();
        }
    }

    @Inject(
        method = "render",
        at = @At("TAIL")
    )
    private void osmium$onHudRenderEnd(DrawContext context, float tickDelta, CallbackInfo ci) {
        OsmiumClient.GUI_OPTIMIZER.clearAllDirty();
    }
}
