package com.ptdteam.magicadventurecore.client.particle;

import com.ptdteam.magicadventurecore.world.particle.ArcaneCrossParticleData;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import org.jetbrains.annotations.Nullable;

public class ArcaneCrossProvider implements ParticleProvider<ArcaneCrossParticleData> {
    private final SpriteSet sprites;

    public ArcaneCrossProvider(SpriteSet sprites) {
        this.sprites = sprites;
    }

    @Override
    public @Nullable Particle createParticle(
            ArcaneCrossParticleData data,
            ClientLevel level,
            double x,
            double y,
            double z,
            double xSpeed,
            double ySpeed,
            double zSpeed
    ) {
        return new ArcaneCrossParticle(level, x, y, z, data.target(), sprites);
    }
}
