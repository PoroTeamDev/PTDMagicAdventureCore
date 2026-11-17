package com.ptdteam.magicadventurecore.registry;

import com.ptdteam.magicadventurecore.MagicAdventureCore;
import com.ptdteam.magicadventurecore.world.particle.ArcaneCrossParticleData;
import com.ptdteam.magicadventurecore.world.particle.ArcaneCrossParticleType;
import com.ptdteam.magicadventurecore.world.particle.LifeOrbParticleData;
import com.ptdteam.magicadventurecore.world.particle.LifeOrbParticleType;
import net.minecraft.core.particles.ParticleType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class MACParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLES =
            DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, MagicAdventureCore.MOD_ID);

    /**
     * Фиолетовые частицы-крестики, которые летят от аркан-экстрактора к ближайшему
     * резервуару эссенции.
     */
    public static final RegistryObject<ParticleType<ArcaneCrossParticleData>> ARCANE_CROSS =
            PARTICLES.register("arcane_cross", ArcaneCrossParticleType::new);

    /** Красные орбы, летящие от игрока с гемом жизни к резервуару эссенции. */
    public static final RegistryObject<ParticleType<LifeOrbParticleData>> LIFE_ORB =
            PARTICLES.register("life_orb", LifeOrbParticleType::new);

    private MACParticles() {
    }

    public static void register(IEventBus eventBus) {
        PARTICLES.register(eventBus);
    }
}
