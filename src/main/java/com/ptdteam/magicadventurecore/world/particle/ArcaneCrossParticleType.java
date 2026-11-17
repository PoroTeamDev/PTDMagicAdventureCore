package com.ptdteam.magicadventurecore.world.particle;

import com.mojang.serialization.Codec;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.world.phys.Vec3;

/**
 * Тип частицы-крестика. Цель полёта передаётся отдельно в {@link ArcaneCrossParticleData},
 * поэтому codec нужен только для команд/датапаков и отдаёт константу.
 */
public class ArcaneCrossParticleType extends ParticleType<ArcaneCrossParticleData> {
    private static final Codec<ArcaneCrossParticleData> CODEC =
            Codec.unit(new ArcaneCrossParticleData(Vec3.ZERO));

    public ArcaneCrossParticleType() {
        super(false, ArcaneCrossParticleData.DESERIALIZER);
    }

    @Override
    public Codec<ArcaneCrossParticleData> codec() {
        return CODEC;
    }
}
