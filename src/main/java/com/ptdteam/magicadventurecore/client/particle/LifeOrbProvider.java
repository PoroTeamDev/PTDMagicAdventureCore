package com.ptdteam.magicadventurecore.client.particle;

import com.ptdteam.magicadventurecore.world.particle.LifeOrbParticleData;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import org.jetbrains.annotations.Nullable;

public class LifeOrbProvider implements ParticleProvider<LifeOrbParticleData> {
    private final SpriteSet sprites;

    public LifeOrbProvider(SpriteSet sprites) {
        this.sprites = sprites;
    }

    @Override
    public @Nullable Particle createParticle(
            LifeOrbParticleData data,
            ClientLevel level,
            double x,
            double y,
            double z,
            double xSpeed,
            double ySpeed,
            double zSpeed
    ) {
        return new LifeOrbParticle(level, x, y, z, data.target(), sprites);
    }
}
