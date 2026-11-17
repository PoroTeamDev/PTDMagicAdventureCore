# [PTD] MagicAdventure Core

Cross-mod magic core for **Minecraft 1.20.1 / Forge 47.x**. It ties three magic mods into one
essence economy: **Blood Magic** (blood/LP), **Botania** (mana) and **DivineRPG** (arcana),
plus the mod's own "Life" essence.

## Blocks

### Synergetic Crafting Table (`magicadventurecore:sct_crafting_block`)
A circular 14-slot crafting grid with two extra sockets: a **Blood Orb** and a **Mana item**
(Mana Mirror / mana pool binding). A recipe lists its 14 ingredients plus `blood_cost` and
`mana_cost`; taking the result consumes the ingredients, syphons LP from the orb's soul network
and drains mana from the bound pool.

Recipe JSON (`data/<namespace>/recipes/*.json`):

```json
{
  "type": "magicadventurecore:sct_hybrid_crafting",
  "ingredients": [ { "item": "minecraft:stone" }, { "item": "minecraft:stone" } ],
  "blood_cost": 1000,
  "mana_cost": 1000,
  "blood_orb": { "item": "bloodmagic:weakbloodorb" },
  "mana_mirror": { "item": "botania:mana_mirror" },
  "result": { "item": "minecraft:diamond" }
}
```

`ingredients` may contain from 1 to 14 entries; the rest of the grid is treated as empty.
`blood_cost` / `mana_cost` are optional (default 0). Recipe matching is order-independent:
each non-empty ingredient must match exactly one non-empty grid slot.

### Essence Reservoir (`magicadventurecore:essence_reservoir`)
Stores up to 100 000 units of a single essence type: `mana`, `blood`, `life` or `arcana`
(`NONE` when empty). Its fill level (blockstate `level` 0..4), fluid colour and contents
survive being broken — the reservoir item keeps its NBT and restores it when placed.

- Botania mana can be pushed in through the `ManaReceiver` capability (spreader, sparks).
- Blood Magic life essence can be poured in with a fluid container.
- Killing a mob while carrying a reservoir adds **Life** essence (scaled by max health).
- The Arcane Extractor feeds **Arcana** into adjacent reservoirs.

### Arcane Extractor (`magicadventurecore:arcane_extractor`)
Holds one DivineRPG item and slowly converts it into arcana (100/tick, buffer 10 000,
500/tick pushed into an adjacent Essence Reservoir). "How much arcana" is defined by
`ArcanaSources`: datapack item tags `magicadventurecore:arcane_sources/<world>/<ore|mob_drop>`
take priority, otherwise the value is derived from the DivineRPG world the item belongs to.

Extraction progress is stored on the item itself (`ArcanaExtracted` NBT), so pulling the item
out and putting it back continues where it stopped instead of resetting it.

## Dependencies

- Forge 47.4+ (1.20.1), Java 17
- **required**: Blood Magic, Botania
- **optional**: DivineRPG (Arcane Extractor sources), JEI (recipe category)

## Building

```bash
./gradlew build        # jar -> build/libs
./gradlew runClient    # dev client (deps from build.gradle are pulled in)
./gradlew runData      # regenerate resources into src/generated/resources
```

Note: `src/main/resources` currently holds the canonical models/blockstates/lang; `runData`
generates matching files, so both stay in sync.

## Layout

```
src/main/java/com/ptdteam/magicadventurecore
├── MagicAdventureCore.java      mod entry point + DeferredRegister wiring
├── registry/                    blocks, items, block entities, menus, recipe types
├── world/
│   ├── block/                   SCT, ER, AE blocks
│   ├── block/entity/            their block entities
│   ├── essence/                 EssenceType, EssenceStorage, ArcanaSources
│   ├── menu/                    SCT crafting menu + container wrapper
│   ├── recipe/                  SCT hybrid recipe + serializer
│   ├── item/                    ER block item with tooltip
│   └── event/                   Life essence on mob death
├── client/                      GUI, block entity renderer, colours
├── datagen/                     blockstate/item model/lang/tags/recipes/loot providers
└── integration/jei/             JEI category for SCT recipes
```
