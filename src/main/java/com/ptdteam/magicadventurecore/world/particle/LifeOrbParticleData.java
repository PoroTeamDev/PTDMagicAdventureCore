package com.ptdteam.magicadventurecore.world.particle;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.ptdteam.magicadventurecore.registry.MACParticles;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;

/**
 * Красный орб (в духе сферы опыта), который летит от игрока к резервуару эссенции.
 */
public record LifeOrbParticleData(Vec3 target) implements ParticleOptions {
    public static final ParticleOptions.Deserializer<LifeOrbParticleData> DESERIALIZER =
            new ParticleOptions.Deserializer<>() {
                @Override
                public LifeOrbParticleData fromNetwork(ParticleType<LifeOrbParticleData> type, FriendlyByteBuf buffer) {
                    return new LifeOrbParticleData(new Vec3(
                            buffer.readDouble(), buffer.readDouble(), buffer.readDouble()));
                }

                @Override
                public LifeOrbParticleData fromCommand(ParticleType<LifeOrbParticleData> type, StringReader reader)
                        throws CommandSyntaxException {
                    return new LifeOrbParticleData(Vec3.ZERO);
                }
            };

    @Override
    public ParticleType<LifeOrbParticleData> getType() {
        return MACParticles.LIFE_ORB.get();
    }

    @Override
    public void writeToNetwork(FriendlyByteBuf buffer) {
        buffer.writeDouble(target.x);
        buffer.writeDouble(target.y);
        buffer.writeDouble(target.z);
    }

    @Override
    public String writeToString() {
        return "magicadventurecore:life_orb";
    }
}
