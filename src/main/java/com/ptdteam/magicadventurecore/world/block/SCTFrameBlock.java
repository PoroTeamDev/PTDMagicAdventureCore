package com.ptdteam.magicadventurecore.world.block;

import net.minecraft.world.level.block.Block;

/**
 * Блок рамки платформы SCT (3×3). Сама по себе ничего не делает: кольцо из 8 рамок —
 * базовая платформа, дающая столу буферы 50 000 маны/крови. Кольцо гомогенное:
 * либо все 8 блоков — рамки, либо все 8 — модули.
 */
public class SCTFrameBlock extends Block {
    public SCTFrameBlock(Properties properties) {
        super(properties);
    }
}
