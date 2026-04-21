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

public class OsmiumClient implements ClientModInitializer {

    public static final Logger LOGGER = LoggerFactory.getLogger("osmium");

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

        FRAMETIME_LOGGER = new FrametimeLogger();
        BUDGET           = new FrameBudgetController(144);
        SCHEDULER        = new ModularTaskScheduler(BUDGET);
        SPIKE_SHIELD     = new SpikeShieldMode(FRAMETIME_LOGGER);
        ANIMATION_THROTTLER = new AdaptiveAnimationThrottler(BUDGET);
        PARTICLE_CULLER  = new SmartParticleCuller(BUDGET, SPIKE_SHIELD);
        GUI_OPTIMIZER    = new GuiOptimizer();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            SCHEDULER.tick();
            SPIKE_SHIELD.tick();
            ANIMATION_THROTTLER.tick();
        });

        HudRenderCallback.EVENT.register((drawContext, tickDelta) -> {
            double frameMs = FRAMETIME_LOGGER.endFrame();  // fixed: double not long
            BUDGET.update(frameMs);
            FRAMETIME_LOGGER.beginFrame();
        });

        OsmiumDebugOverlay.register();

        LOGGER.info("Osmium ready. Target: {}ms per frame.", BUDGET.getTargetFrametimeMs());
    }
}
