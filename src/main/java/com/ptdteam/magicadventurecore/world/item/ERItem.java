package com.ptdteam.magicadventurecore.world.item;

import com.ptdteam.magicadventurecore.world.essence.EssenceStorage;
import com.ptdteam.magicadventurecore.world.essence.EssenceType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ERItem extends BlockItem {
    public ERItem(Properties properties) {
        super(com.ptdteam.magicadventurecore.registry.MACBlocks.ESSENCE_RESERVOIR.get(), properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        EssenceType type = EssenceStorage.getEssenceType(stack);
        int amount = EssenceStorage.getEssenceAmount(stack);
        if (type != EssenceType.NONE && amount > 0) {
            tooltip.add(Component.literal(type.getId() + " = " + amount));
        }
        super.appendHoverText(stack, level, tooltip, flag);
    }
}