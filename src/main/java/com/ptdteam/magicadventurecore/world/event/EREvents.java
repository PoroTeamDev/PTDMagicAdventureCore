package com.ptdteam.magicadventurecore.world.event;

import com.ptdteam.magicadventurecore.MagicAdventureCore;
import com.ptdteam.magicadventurecore.registry.MACItems;
import com.ptdteam.magicadventurecore.world.essence.EssenceStorage;
import com.ptdteam.magicadventurecore.world.essence.EssenceType;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = MagicAdventureCore.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class EREvents {
    private EREvents() {
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof Player player)) {
            return;
        }

        LivingEntity target = event.getEntity();

        if (player.level().isClientSide) {
            return;
        }

        int amount = calculateLifeEssence(target);
        if (amount <= 0) {
            return;
        }

        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.getItem() != MACItems.ESSENCE_RESERVOIR.get()) {
                continue;
            }
            int added = EssenceStorage.addEssence(stack, EssenceType.LIFE, amount, 100000);
            if (added > 0) {
                return;
            }
        }
    }

    private static int calculateLifeEssence(LivingEntity target) {
        float maxHealth = target.getMaxHealth();
        float clamped = Mth.clamp(maxHealth, 20F, 200F);
        return Mth.floor(1000F + (clamped - 20F) / 180F * 4000F);
    }
}