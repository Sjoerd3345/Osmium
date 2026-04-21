package dev.osmium.mixin;

import dev.osmium.OsmiumClient;
import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * GameRendererMixin — hooks into Minecraft's render loop.
 *
 * This is a Mixin: a way to inject our code into Minecraft's code
 * without editing Minecraft's source directly. Fabric loads this at
 * startup and weaves our injected methods into the game's bytecode.
 *
 * What we're doing here:
 *   - @Inject at the START of render() → begin our frametime measurement
 *   - @Inject at the END   of render() → end measurement + update budget
 *
 * The @Mixin annotation tells Mixin which class to target.
 * The @Inject annotation specifies WHERE to inject (which method, which point).
 */
@Mixin(GameRenderer.class)
public class GameRendererMixin {

    /**
     * Injected at the very beginning of GameRenderer.render().
     * Starts our frametime clock for this frame.
     *
     * @param tickDelta  how far between ticks we are (Minecraft passes this)
     * @param startTime  system time when rendering started (Minecraft passes this)
     * @param tick       whether this is a full tick
     * @param ci         CallbackInfo — allows cancelling the method (we don't here)
     */
    @Inject(
        method = "render",
        at = @At("HEAD") // "HEAD" = inject at the very start of the method
    )
    private void osmium$onRenderStart(float tickDelta, long startTime, boolean tick, CallbackInfo ci) {
        // Tell our frametime logger that a new frame is starting.
        OsmiumClient.FRAMETIME_LOGGER.beginFrame();

        // Also update our particle culler's dynamic cap once per render frame.
        // We use a tick counter check to not do this every single frame.
        // (The actual particle count requires a Mixin to access — see ParticleManagerMixin)
        OsmiumClient.PARTICLE_CULLER.updateDynamicCap(0);
    }

    /**
     * Injected at the very end of GameRenderer.render().
     * Stops our clock, computes frametime, updates the budget controller.
     */
    @Inject(
        method = "render",
        at = @At("TAIL") // "TAIL" = inject at the very end of the method
    )
    private void osmium$onRenderEnd(float tickDelta, long startTime, boolean tick, CallbackInfo ci) {
        // Measure how long this frame took.
        double frameMs = OsmiumClient.FRAMETIME_LOGGER.endFrame();

        // Feed that measurement into the budget controller.
        OsmiumClient.BUDGET.update(frameMs);
    }
}
