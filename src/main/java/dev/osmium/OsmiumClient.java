package dev.osmium;

import dev.osmium.core.FrameBudgetController;
import dev.osmium.core.FrametimeLogger;
import dev.osmium.core.ModularTaskScheduler;
import dev.osmium.core.SpikeShieldMode;
import dev.osmium.animation.AdaptiveAnimationThrottler;
import dev.osmium.particle.SmartParticleCuller;
import dev.osmium.gui.GuiOptimizer;
import dev.osmium.compat.OsmiumDebugOverlay;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * OsmiumClient — the main entry point for the Osmium performance mod.
 *
 * This class starts up all of Osmium's systems and wires them together
 * using Fabric's event hooks. Think of it as the "main()" of the mod.
 */
public class OsmiumClient implements ClientModInitializer {

    // A logger so we can print messages to the Minecraft log file.
    // You'll see these in your console when running the game.
    public static final Logger LOGGER = LoggerFactory.getLogger("osmium");

    // The single shared instance of our frame budget controller.
    // "static" means every class in the mod can access it via OsmiumClient.BUDGET.
    public static FrameBudgetController BUDGET;
    public static FrametimeLogger FRAMETIME_LOGGER;
    public static ModularTaskScheduler SCHEDULER;
    public static SpikeShieldMode SPIKE_SHIELD;
    public static AdaptiveAnimationThrottler ANIMATION_THROTTLER;
    public static SmartParticleCuller PARTICLE_CULLER;
    public static GuiOptimizer GUI_OPTIMIZER;

    @Override
    public void onInitializeClient() {
        LOGGER.info("Osmium is starting up...");

        // --- Boot all systems in dependency order ---
        // Logger first — everything else reads from it.
        FRAMETIME_LOGGER = new FrametimeLogger();

        // Budget controller reads from the logger.
        BUDGET = new FrameBudgetController(144); // target: 144 FPS

        // Scheduler uses the budget to decide what runs each frame.
        SCHEDULER = new ModularTaskScheduler(BUDGET);

        // Spike Shield watches the logger and triggers degradation.
        SPIKE_SHIELD = new SpikeShieldMode(FRAMETIME_LOGGER);

        // Visual systems — all aware of the budget.
        ANIMATION_THROTTLER = new AdaptiveAnimationThrottler(BUDGET);
        PARTICLE_CULLER     = new SmartParticleCuller(BUDGET, SPIKE_SHIELD);
        GUI_OPTIMIZER       = new GuiOptimizer();

        // --- Register Fabric event hooks ---

        // ClientTickEvents.END_CLIENT_TICK fires once per game tick (20x/second).
        // We use it to update all systems that don't need per-frame precision.
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            SCHEDULER.tick();
            SPIKE_SHIELD.tick();
            ANIMATION_THROTTLER.tick();
        });

        // HudRenderCallback fires every rendered frame, after the world is drawn.
        // We use it to measure real frametime and update the budget.
        HudRenderCallback.EVENT.register((drawContext, tickDelta) -> {
            long frameMs = FRAMETIME_LOGGER.endFrame();   // measure last frame
            BUDGET.update(frameMs);                        // update budget
            FRAMETIME_LOGGER.beginFrame();                 // start next measurement
        });

        LOGGER.info("Osmium ready. Target: {}ms per frame.", BUDGET.getTargetFrametimeMs());
    }
}
