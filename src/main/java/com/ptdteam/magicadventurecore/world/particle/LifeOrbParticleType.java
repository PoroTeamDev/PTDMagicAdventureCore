package com.ptdteam.magicadventurecore.world.particle;

import com.mojang.serialization.Codec;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.world.phys.Vec3;

public class LifeOrbParticleType extends ParticleType<LifeOrbParticleData> {
    private static final Codec<LifeOrbParticleData> CODEC =
            Codec.unit(new LifeOrbParticleData(Vec3.ZERO));

    public LifeOrbParticleType() {
        super(false, LifeOrbParticleData.DESERIALIZER);
    }

    @Override
    public Codec<LifeOrbParticleData> codec() {
        return CODEC;
    }
}
