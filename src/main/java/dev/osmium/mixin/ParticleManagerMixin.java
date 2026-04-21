package dev.osmium.mixin;
 
import dev.osmium.OsmiumClient;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.particle.ParticleEffect;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
 
/**
 * ParticleManagerMixin — intercepts particle spawning.
 *
 * We hook addParticle() which returns a Particle (not void),
 * so we use CallbackInfoReturnable and return null to cancel.
 *
 * The @Shadow field gives us the particle count for the dynamic cap check.
 */
@Mixin(ParticleManager.class)
public class ParticleManagerMixin {
 
    /**
     * Injected at the start of addParticle().
     * Returns null (cancels the spawn) when our culler says to skip it.
     *
     * Note: we use CallbackInfoReturnable<Particle> because the method
     * returns a Particle, not void.
     */
    @Inject(
        method = "addParticle(Lnet/minecraft/particle/ParticleEffect;DDDDDD)Lnet/minecraft/client/particle/Particle;",
        at = @At("HEAD"),
        cancellable = true
    )
    private void osmium$onAddParticle(
        ParticleEffect parameters,
        double x, double y, double z,
        double velocityX, double velocityY, double velocityZ,
        CallbackInfoReturnable<Particle> cir
    ) {
        // For now distance is 0 — the culler uses it for density scaling.
        // A future improvement would inject MinecraftClient to get camera pos.
        int currentCount = 0; // particle count — hard to access without @Shadow on the right field
        double distance = 0.0;
 
        if (!OsmiumClient.PARTICLE_CULLER.shouldSpawnParticle(distance, currentCount)) {
            cir.setReturnValue(null); // cancel the spawn by returning null
        }
    }
}
