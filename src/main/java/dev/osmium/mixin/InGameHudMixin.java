package dev.osmium.mixin;
 
import dev.osmium.OsmiumClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
 
/**
 * InGameHudMixin — tracks HUD render calls for the GuiOptimizer.
 *
 * Instead of trying to skip redraws (which caused flickering),
 * we simply notify the GuiOptimizer each time a render happens.
 * This keeps the dirty flag system accurate for future use
 * without breaking anything visually.
 */
@Mixin(InGameHud.class)
public class InGameHudMixin {
 
    @Inject(
        method = "render",
        at = @At("HEAD")
    )
    private void osmium$onHudRenderStart(DrawContext context, float tickDelta, CallbackInfo ci) {
        // Mark everything dirty before each render so we never skip a frame.
        // This is conservative but correct — no more flickering.
        OsmiumClient.GUI_OPTIMIZER.markAllDirty();
    }
 
    @Inject(
        method = "render",
        at = @At("TAIL")
    )
    private void osmium$onHudRenderEnd(DrawContext context, float tickDelta, CallbackInfo ci) {
        // Clear flags after render so dirty tracking stays accurate.
        OsmiumClient.GUI_OPTIMIZER.clearAllDirty();
    }
}
 
