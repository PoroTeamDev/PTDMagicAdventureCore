package com.ptdteam.magicadventurecore.world.essence;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

public final class EssenceStorage {
    public static final String TYPE_TAG = "EssenceType";
    public static final String AMOUNT_TAG = "EssenceAmount";

    private EssenceStorage() {
    }

    public static EssenceType getEssenceType(CompoundTag tag) {
        if (tag == null || !tag.contains(TYPE_TAG)) {
            return EssenceType.NONE;
        }
        return EssenceType.fromId(tag.getString(TYPE_TAG));
    }

    public static int getEssenceAmount(CompoundTag tag) {
        if (tag == null || !tag.contains(AMOUNT_TAG)) {
            return 0;
        }
        return tag.getInt(AMOUNT_TAG);
    }

    public static void writeToTag(CompoundTag tag, EssenceType type, int amount) {
        if (tag == null) {
            return;
        }
        if (amount <= 0 || type == EssenceType.NONE) {
            tag.remove(TYPE_TAG);
            tag.remove(AMOUNT_TAG);
        } else {
            tag.putString(TYPE_TAG, type.getId());
            tag.putInt(AMOUNT_TAG, amount);
        }
    }

    public static EssenceType getEssenceType(ItemStack stack) {
        if (stack.isEmpty() || !stack.hasTag()) {
            return EssenceType.NONE;
        }
        return getEssenceType(stack.getTag());
    }

    public static int getEssenceAmount(ItemStack stack) {
        if (stack.isEmpty() || !stack.hasTag()) {
            return 0;
        }
        return getEssenceAmount(stack.getTag());
    }

    public static void writeToStack(ItemStack stack, EssenceType type, int amount) {
        if (stack.isEmpty()) {
            return;
        }
        CompoundTag tag = stack.getOrCreateTag();
        writeToTag(tag, type, amount);
    }

    public static boolean canAccept(EssenceType existing, EssenceType incoming) {
        return existing == EssenceType.NONE || existing == incoming;
    }

    public static int addEssence(ItemStack stack, EssenceType type, int amount, int maxAmount) {
        if (stack.isEmpty() || amount <= 0) {
            return 0;
        }
        EssenceType existing = getEssenceType(stack);
        if (!canAccept(existing, type)) {
            return 0;
        }
        int current = getEssenceAmount(stack);
        if (current >= maxAmount) {
            return 0;
        }
        int accepted = Math.min(amount, maxAmount - current);
        EssenceType finalType = existing == EssenceType.NONE ? type : existing;
        writeToStack(stack, finalType, current + accepted);
        return accepted;
    }
}