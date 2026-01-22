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
    private MACItems() {
    }

    public static void register(net.minecraftforge.eventbus.api.IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}