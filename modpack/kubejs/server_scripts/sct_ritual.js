// ---------------------------------------------------------------------------
// Рецепты SCT-ритуала: мел, рамка, модули, стабилизатор.
// Все — простые SCT-крафты (без stability): работают на голом столе.
// ---------------------------------------------------------------------------

const RITUAL_SCT_TYPE = 'magicadventurecore:sct_hybrid_crafting'

const ritualSCT = (event, output, ingredients, blood, mana, craftTime, count) => {
  event.custom({
    type: RITUAL_SCT_TYPE,
    ingredients: ingredients.map(item => ({ item: item })),
    blood_cost: blood,
    mana_cost: mana,
    craft_time: craftTime,
    blood_orb: { item: 'bloodmagic:weakbloodorb' },
    mana_mirror: { item: 'botania:mana_mirror' },
    result: { item: output, count: count || 1 }
  })
}

ServerEvents.recipes(event => {
  // Магический мел: мел + аметист + бумага. Один мел = 100 меток (полный круг 88).
  ritualSCT(event, 'magicadventurecore:arcane_chalk', [
    'minecraft:calcite',
    'minecraft:calcite',
    'minecraft:calcite',
    'minecraft:white_dye',
    'minecraft:white_dye',
    'minecraft:amethyst_shard',
    'minecraft:paper'
  ], 0, 500, 60)

  // Рамка платформы (по 2 за крафт, чтобы собрать кольцо быстрее).
  ritualSCT(event, 'magicadventurecore:sct_frame', [
    'minecraft:stone_bricks',
    'minecraft:stone_bricks',
    'minecraft:stone_bricks',
    'minecraft:stone_bricks',
    'minecraft:stone_bricks',
    'minecraft:stone_bricks',
    'botania:manasteel_ingot',
    'botania:manasteel_ingot'
  ], 200, 800, 100, 2)

  // Модуль-резервуар маны.
  ritualSCT(event, 'magicadventurecore:sct_mana_reservoir', [
    'minecraft:glass',
    'minecraft:glass',
    'minecraft:glass',
    'minecraft:glass',
    'botania:mana_diamond',
    'botania:mana_diamond',
    'botania:terrasteel_ingot'
  ], 300, 3000, 160)

  // Модуль-резервуар крови.
  ritualSCT(event, 'magicadventurecore:sct_blood_reservoir', [
    'minecraft:glass',
    'minecraft:glass',
    'minecraft:glass',
    'minecraft:glass',
    'bloodmagic:reinforcedslate',
    'bloodmagic:reinforcedslate',
    'bloodmagic:blankrune'
  ], 1000, 800, 160)

  // Модуль-ускоритель.
  ritualSCT(event, 'magicadventurecore:sct_accelerator', [
    'minecraft:gold_ingot',
    'minecraft:gold_ingot',
    'divinerpg:arcanium',
    'divinerpg:arcanium',
    'botania:terrasteel_ingot'
  ], 200, 4000, 200)

  // Магический стабилизатор (4 штуки на ритуал).
  ritualSCT(event, 'magicadventurecore:mage_stabilizer', [
    'minecraft:amethyst_shard',
    'minecraft:amethyst_shard',
    'minecraft:amethyst_shard',
    'minecraft:amethyst_shard',
    'minecraft:amethyst_shard',
    'divinerpg:arcanium',
    'divinerpg:arcanium',
    'botania:mana_diamond'
  ], 500, 5000, 220)

  console.info('[sct-ritual] registered SCT recipes for chalk, frame, modules and stabilizer')
})
