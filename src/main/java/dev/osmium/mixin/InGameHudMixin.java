package dev.osmium.mixin;

import dev.osmium.OsmiumClient;
import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * GameRendererMixin — hooks into Minecraft's render loop.
 * Measures real frametime every frame and feeds it into the budget controller.
 */
@Mixin(GameRenderer.class)
public class GameRendererMixin {

    @Inject(
        method = "render",
        at = @At("HEAD")
    )
    private void osmium$onRenderStart(float tickDelta, long startTime, boolean tick, CallbackInfo ci) {
        OsmiumClient.FRAMETIME_LOGGER.beginFrame();
        OsmiumClient.PARTICLE_CULLER.updateDynamicCap(0);
    }

    @Inject(
        method = "render",
        at = @At("TAIL")
    )
    private void osmium$onRenderEnd(float tickDelta, long startTime, boolean tick, CallbackInfo ci) {
        double frameMs = OsmiumClient.FRAMETIME_LOGGER.endFrame();
        OsmiumClient.BUDGET.update(frameMs);
    }
}

