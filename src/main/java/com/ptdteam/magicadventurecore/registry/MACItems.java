package com.ptdteam.magicadventurecore.registry;

import com.ptdteam.magicadventurecore.MagicAdventureCore;
import com.ptdteam.magicadventurecore.world.item.ERItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class MACItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, MagicAdventureCore.MOD_ID);

    public static final RegistryObject<Item> SCT = ITEMS.register(
            "sct",
            () -> new BlockItem(MACBlocks.SCT.get(), new Item.Properties())
    );

    public static final RegistryObject<Item> ESSENCE_RESERVOIR = ITEMS.register(
            "essence_reservoir",
            () -> new ERItem(new Item.Properties())
    );

    public static final RegistryObject<Item> ARCANE_EXTRACTOR = ITEMS.register(
            "arcane_extractor",
            () -> new BlockItem(MACBlocks.ARCANE_EXTRACTOR.get(), new Item.Properties())
    );

    /** Осколок халита: падает с боссов DivineRPG вместо халитовой экипировки. */
    public static final RegistryObject<Item> HALITE_SHARD = ITEMS.register(
            "halite_shard",
            () -> new Item(new Item.Properties())
    );

    /** Халитовый слиток: выплавляется из осколков на столе SCT. */
    public static final RegistryObject<Item> HALITE_INGOT = ITEMS.register(
            "halite_ingot",
            () -> new Item(new Item.Properties())
    );

    /**
     * Гем Жизни: копит эссенцию жизни от убийств (до 16 000) и переливает её
     * в резервуар при удержании ПКМ рядом с урной.
     */
    public static final RegistryObject<Item> GEM_OF_LIFE = ITEMS.register(
            "gem_of_life",
            () -> new com.ptdteam.magicadventurecore.world.item.GemOfLifeItem(new Item.Properties())
    );

    private MACItems() {
    }

    public static void register(net.minecraftforge.eventbus.api.IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
