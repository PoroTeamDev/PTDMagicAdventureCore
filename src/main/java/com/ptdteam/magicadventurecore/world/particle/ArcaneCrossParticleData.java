package com.ptdteam.magicadventurecore.world.particle;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;

/**
 * Параметры частицы: куда она должна прилететь (центр ближайшего резервуара эссенции).
 */
public record ArcaneCrossParticleData(Vec3 target) implements ParticleOptions {
    public static final ParticleOptions.Deserializer<ArcaneCrossParticleData> DESERIALIZER =
            new ParticleOptions.Deserializer<>() {
                @Override
                public ArcaneCrossParticleData fromNetwork(ParticleType<ArcaneCrossParticleData> type, FriendlyByteBuf buffer) {
                    return new ArcaneCrossParticleData(new Vec3(
                            buffer.readDouble(), buffer.readDouble(), buffer.readDouble()));
                }

                @Override
                public ArcaneCrossParticleData fromCommand(ParticleType<ArcaneCrossParticleData> type, StringReader reader)
                        throws CommandSyntaxException {
                    return new ArcaneCrossParticleData(Vec3.ZERO);
                }
            };

    @Override
    public ParticleType<ArcaneCrossParticleData> getType() {
        return com.ptdteam.magicadventurecore.registry.MACParticles.ARCANE_CROSS.get();
    }

    @Override
    public void writeToNetwork(FriendlyByteBuf buffer) {
        buffer.writeDouble(target.x);
        buffer.writeDouble(target.y);
        buffer.writeDouble(target.z);
    }

    @Override
    public String writeToString() {
        return "magicadventurecore:arcane_cross";
    }
}
