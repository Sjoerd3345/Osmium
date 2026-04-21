package dev.osmium.mixin;

import dev.osmium.OsmiumClient;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.particle.ParticleEffect;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

/**
 * ParticleManagerMixin — hooks into particle spawning.
 *
 * Every time Minecraft tries to add a particle to the world, we intercept
 * and ask our SmartParticleCuller whether to allow it.
 *
 * @Shadow lets us access private fields from the target class.
 * In this case we access "particles" to count how many currently exist.
 */
@Mixin(ParticleManager.class)
public class ParticleManagerMixin {

    /**
     * @Shadow gives us access to ParticleManager's private "particles" map.
     *
     * The field holds particles grouped by render layer. We use its size
     * to know the total current particle count.
     *
     * Note: the actual field name in Minecraft's code is obfuscated, but
     * Yarn mappings translate it to "particles". If this doesn't compile,
     * check the current Yarn mapping for 1.20.4.
     */
    @Shadow
    private Map<?, ?> particles;

    /**
     * Injected at the start of addParticle().
     *
     * If our culler says "don't spawn this", we cancel the method entirely
     * by calling ci.cancel() — the particle never gets added.
     *
     * @param parameters  the particle type and settings
     * @param x, y, z     world position
     * @param velocityX/Y/Z  initial velocity
     * @param ci          CallbackInfo — calling ci.cancel() stops the method
     */
    @Inject(
        method = "addParticle(Lnet/minecraft/particle/ParticleEffect;DDDDDD)V",
        at = @At("HEAD"),
        cancellable = true // Required to use ci.cancel()
    )
    private void osmium$onAddParticle(
        ParticleEffect parameters,
        double x, double y, double z,
        double velocityX, double velocityY, double velocityZ,
        CallbackInfo ci
    ) {
        // Get the current particle count from the particles map.
        int currentCount = particles == null ? 0 : particles.size();

        // We'd need the player position to compute real distance.
        // For now, use a simplified distance of 0 (always in range).
        // TODO: inject the client to get the camera position for real distance.
        double distance = 0.0;

        // Ask our culler — if it says no, cancel the particle spawn.
        if (!OsmiumClient.PARTICLE_CULLER.shouldSpawnParticle(distance, currentCount)) {
            ci.cancel();
        }
    }
}
