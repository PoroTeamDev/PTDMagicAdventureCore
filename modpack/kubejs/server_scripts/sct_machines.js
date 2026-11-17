// ---------------------------------------------------------------------------
// Рецепты машин мода через стол SCT (и восстановленный крафт самого стола).
//
// - стол SCT: обычный крафт из матов Botania + Blood Magic;
// - Arcane Extractor: только мана (кровь не требуется);
// - Essence Reservoir: мана + немного крови.
// ---------------------------------------------------------------------------

const MACHINE_SCT_TYPE = 'magicadventurecore:sct_hybrid_crafting'

const machineSCT = (event, output, ingredients, blood, mana, craftTime) => {
  event.custom({
    type: MACHINE_SCT_TYPE,
    ingredients: ingredients.map(item => ({ item: item })),
    blood_cost: blood,
    mana_cost: mana,
    craft_time: craftTime,
    blood_orb: { item: 'bloodmagic:weakbloodorb' },
    mana_mirror: { item: 'botania:mana_mirror' },
    result: { item: output }
  })
}

ServerEvents.recipes(event => {
  // Сам стол — обычный верстак
  event.shaped('magicadventurecore:sct', [
    'ABA',
    'CDC',
    'EFE'
  ], {
    A: 'botania:mana_diamond',
    B: 'bloodmagic:infusedslate',
    C: 'botania:mana_pearl',
    D: 'minecraft:crafting_table',
    E: 'bloodmagic:reinforcedslate',
    F: 'botania:manasteel_ingot'
  })

  // Аркан-экстрактор: только мана, кровь не нужна
  machineSCT(event, 'magicadventurecore:arcane_extractor', [
    'botania:manasteel_ingot',
    'botania:manasteel_ingot',
    'divinerpg:divine_stone',
    'divinerpg:divine_stone',
    'divinerpg:arcanium',
    'divinerpg:arcanium',
    'divinerpg:shimmer',
    'bloodmagic:infusedslate',
    'botania:mana_diamond',
    'botania:mana_pearl',
    'cataclysm:void_stone',
    'minecraft:amethyst_shard'
  ], 0, 12000, 240)

  // Резервуар эссенции: мана + немного крови
  machineSCT(event, 'magicadventurecore:essence_reservoir', [
    'minecraft:glass',
    'minecraft:glass',
    'minecraft:glass',
    'minecraft:glass',
    'botania:manasteel_ingot',
    'botania:manasteel_ingot',
    'divinerpg:divine_stone',
    'divinerpg:divine_stone',
    'divinerpg:arcanium',
    'bloodmagic:reinforcedslate',
    'botania:mana_diamond',
    'botania:terrasteel_ingot'
  ], 1000, 6000, 180)

  console.info('[sct-machines] registered SCT recipes for the mod machines')
})
