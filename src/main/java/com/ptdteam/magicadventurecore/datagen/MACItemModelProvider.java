package com.ptdteam.magicadventurecore.datagen;

import com.ptdteam.magicadventurecore.MagicAdventureCore;
import com.ptdteam.magicadventurecore.registry.MACItems;
import net.minecraft.data.PackOutput;
import net.minecraftforge.client.model.generators.ItemModelProvider;
import net.minecraftforge.common.data.ExistingFileHelper;

public class MACItemModelProvider extends ItemModelProvider {
    public MACItemModelProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, MagicAdventureCore.MOD_ID, existingFileHelper);
    }

    @Override
    protected void registerModels() {
        // Блочные предметы используют модели блоков; сами модели блоков генерируются
        // в MACBlockStateProvider, иначе они затираются пустыми cube_all-моделями.
        withExistingParent("sct", modLoc("block/sct_crafting_block"));
        withExistingParent("essence_reservoir", modLoc("block/essence_reservoir"));
        withExistingParent("arcane_extractor", modLoc("block/arcane_extractor"));
        withExistingParent("sct_frame", modLoc("block/sct_frame"));
        withExistingParent("sct_mana_reservoir", modLoc("block/sct_mana_reservoir"));
        withExistingParent("sct_blood_reservoir", modLoc("block/sct_blood_reservoir"));
        withExistingParent("sct_accelerator", modLoc("block/sct_accelerator"));
        withExistingParent("mage_stabilizer", modLoc("block/stabilizer"));

        // Мел использует текстуру item/chulk (так назвал файл автор ассета).
        getBuilder("arcane_chalk")
                .parent(new net.minecraftforge.client.model.generators.ModelFile.UncheckedModelFile("item/generated"))
                .texture("layer0", modLoc("item/chulk"));

        // Обычные предметы: item/generated + текстура item/<id>.
        basicItem(MACItems.HALITE_SHARD.get());
        basicItem(MACItems.HALITE_INGOT.get());
    }
}
