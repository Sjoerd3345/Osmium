package dev.osmium.particle;

import dev.osmium.core.FrameBudgetController;
import dev.osmium.core.SpikeShieldMode;

/**
 * SmartParticleCuller — reduces particle rendering cost intelligently.
 *
 * Particles are one of the most expensive visual systems in Minecraft.
 * This class decides:
 *   1. Should this particle spawn at all? (density check)
 *   2. What's the current maximum particle count? (dynamic cap)
 *
 * The Mixin (ParticleManagerMixin) calls into this class before
 * adding each particle to the scene.
 *
 * Design: we never cull 100% of particles — that would look broken.
 * Instead we probabilistically thin them out: roll a random number
 * and only spawn the particle if it beats the cull threshold.
 */
public class SmartParticleCuller {

    private final FrameBudgetController budget;
    private final SpikeShieldMode spikeShield;

    // Base maximum particle count.
    private static final int BASE_MAX_PARTICLES = 4000;

    // Distance beyond which particles are aggressively culled.
    private static final double CULL_DISTANCE = 32.0;

    // Current dynamic cap — updated every tick based on load.
    private int dynamicMaxParticles = BASE_MAX_PARTICLES;

    // Current spawn probability for distant particles (0.0–1.0).
    private double distantSpawnChance = 1.0;

    public SmartParticleCuller(FrameBudgetController budget, SpikeShieldMode spikeShield) {
        this.budget = budget;
        this.spikeShield = spikeShield;
    }

    /**
     * The main method — called by the Mixin before adding a particle.
     *
     * @param distanceFromPlayer  how far this particle is from the camera
     * @param currentParticleCount how many particles currently exist
     * @return true = spawn the particle, false = skip it
     */
    public boolean shouldSpawnParticle(double distanceFromPlayer, int currentParticleCount) {
        // Hard cap — never exceed the dynamic maximum.
        if (currentParticleCount >= dynamicMaxParticles) {
            return false;
        }

        // Close particles always spawn (within 16 blocks).
        if (distanceFromPlayer < 16.0) {
            return true;
        }

        // Distant particles: apply probabilistic culling.
        if (distanceFromPlayer > CULL_DISTANCE) {
            // Roll a random number — if it's above our spawn chance, skip it.
            return Math.random() < distantSpawnChance;
        }

        // Mid-range: always spawn unless under heavy load.
        return budget.getLoadLevel() != FrameBudgetController.LoadLevel.HEAVY;
    }

    /**
     * Update the dynamic cap and spawn chance based on current load.
     * Call this from a tick event (once per tick is enough).
     *
     * @param currentParticleCount how many particles are currently alive
     */
    public void updateDynamicCap(int currentParticleCount) {
        double quality = spikeShield.getQualityLevel(); // 0.0–1.0
        double load    = budget.getFrameLoad();         // 0.0–1.5+

        // Dynamic cap scales down with spike shield quality.
        // At full quality: 4000 particles. At zero quality: 500.
        dynamicMaxParticles = (int) (500 + (BASE_MAX_PARTICLES - 500) * quality);

        // Distant spawn chance: full quality = 100%, heavy load = 25%.
        if (load < 0.6) {
            distantSpawnChance = 1.0;
        } else if (load < 0.9) {
            distantSpawnChance = 0.6;
        } else {
            distantSpawnChance = 0.25 * quality;
        }
    }

    /**
     * Get the current dynamic particle cap.
     * The Mixin uses this to enforce the hard limit.
     */
    public int getDynamicMaxParticles() { return dynamicMaxParticles; }

    /**
     * Current spawn chance for distant particles (0.0–1.0).
     * Exposed for the debug overlay.
     */
    public double getDistantSpawnChance() { return distantSpawnChance; }
}
