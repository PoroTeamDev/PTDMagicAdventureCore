package com.ptdteam.magicadventurecore.world.essence;

import net.minecraft.resources.ResourceLocation;

import java.util.Locale;

public enum EssenceType {
    NONE("empty", 0x000000),
    MANA("botania:mana", 0x2aa6d9),
    BLOOD("bloodmagic:life_essence", 0x7a0b0b),
    LIFE("magicadventurecore:life", 0xff2a2a),
    ARCANA("divinerpg:arcana", 0x8d5dff);

    private final String id;
    private final int color;

    EssenceType(String id, int color) {
        this.id = id;
        this.color = color;
    }

    public String getId() {
        return id;
    }

    public ResourceLocation getResourceLocation() {
        return ResourceLocation.tryParse(id);
    }

    public int getColor() {
        return color;
    }

    public static EssenceType fromId(String id) {
        if (id == null || id.isBlank()) {
            return NONE;
        }
        String normalized = id.toLowerCase(Locale.ROOT);
        for (EssenceType type : values()) {
            if (type.id.equals(normalized)) {
                return type;
            }
        }
        return NONE;
    }
}