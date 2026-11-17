// ---------------------------------------------------------------------------
// Рецепты стола SCT (Synergetic Crafting Table) для халитовой экипировки DivineRPG.
//
// Цепочка:
//   боссы DivineRPG -> halite_shard
//   halite_shard x2 + катализаторы -> halite_ingot            (SCT)
//   ingot + катализаторы -> базовая броня / оружие            (SCT)
//   базовая вещь + corrupted_* + ingot -> awakened-броня      (SCT, долгий крафт)
//
// craft_time — длительность фазы поглощения в тиках (20 = 1 секунда).
// Фаза перекачки крови/маны считается модом отдельно: 1000 единиц в секунду,
// то есть blood_cost/mana_cost = 4000/8000 дают 8 секунд перекачки.
//
// Ванильные awakened-рецепты DivineRPG удаляются, чтобы путь был только через SCT.
// ---------------------------------------------------------------------------

const SCT_TYPE = 'magicadventurecore:sct_hybrid_crafting'

const SCT = (event, output, ingredients, blood, mana, craftTime) => {
  event.custom({
    type: SCT_TYPE,
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
  // --- 1. Только SCT: убираем ванильные рецепты awakened-брони DivineRPG -------
  const awakenedPieces = [
    'awakened_halite_helmet',
    'awakened_halite_chestplate',
    'awakened_halite_leggings',
    'awakened_halite_boots'
  ]
  awakenedPieces.forEach(piece => event.remove({ output: 'divinerpg:' + piece }))

  // --- 2. Общие ингредиенты --------------------------------------------------
  const shard = 'magicadventurecore:halite_shard'
  const ingot = 'magicadventurecore:halite_ingot'
  const arcanium = 'divinerpg:arcanium'
  const divineStone = 'divinerpg:divine_stone'
  const shimmer = 'divinerpg:shimmer'
  const manoDiamond = 'botania:mana_diamond'
  const manasteel = 'botania:manasteel_ingot'
  const terrasteel = 'botania:terrasteel_ingot'
  const gaiaIngot = 'botania:gaia_ingot'
  const infusedSlate = 'bloodmagic:infusedslate'
  const reinforcedSlate = 'bloodmagic:reinforcedslate'
  const demonSlate = 'bloodmagic:demonslate'
  const voidStone = 'cataclysm:void_stone'

  // --- 3. Халитовый слиток ---------------------------------------------------
  SCT(event, ingot, [
    shard, shard,
    arcanium, divineStone, divineStone,
    demonSlate, terrasteel, voidStone
  ], 1000, 2000, 140)

  // --- 4. Базовая броня ------------------------------------------------------
  SCT(event, 'divinerpg:halite_helmet', [
    ingot, ingot, ingot,
    divineStone, divineStone,
    arcanium, shimmer, infusedSlate, manoDiamond, voidStone
  ], 1500, 3000, 120)

  SCT(event, 'divinerpg:halite_chestplate', [
    ingot, ingot, ingot, ingot, ingot,
    divineStone, divineStone,
    arcanium, arcanium,
    demonSlate, voidStone
  ], 2000, 4000, 160)

  SCT(event, 'divinerpg:halite_leggings', [
    ingot, ingot, ingot, ingot,
    divineStone, divineStone,
    arcanium, arcanium,
    manasteel, reinforcedSlate
  ], 1800, 3600, 140)

  SCT(event, 'divinerpg:halite_boots', [
    ingot, ingot,
    divineStone, divineStone,
    arcanium, shimmer, manasteel, infusedSlate, voidStone, manoDiamond
  ], 1200, 2400, 100)

  // --- 5. Базовое оружие -----------------------------------------------------
  SCT(event, 'divinerpg:halite_blade', [
    ingot, ingot, ingot,
    divineStone, divineStone,
    arcanium, arcanium,
    gaiaIngot, demonSlate, voidStone
  ], 2000, 4500, 160)

  SCT(event, 'divinerpg:halite_bow', [
    ingot, ingot,
    divineStone, divineStone,
    arcanium, shimmer,
    'minecraft:string', 'minecraft:string',
    manoDiamond
  ], 1500, 3500, 140)

  SCT(event, 'divinerpg:halite_phaser', [
    ingot, ingot, ingot,
    divineStone, divineStone,
    arcanium, arcanium,
    'divinerpg:corrupted_shards', 'divinerpg:ender_shards', 'divinerpg:ice_shards'
  ], 2000, 4500, 180)

  SCT(event, 'divinerpg:halite_blitz', [
    ingot, ingot, ingot,
    divineStone, divineStone,
    arcanium, arcanium,
    gaiaIngot, 'divinerpg:molten_shards', 'divinerpg:terran_shards'
  ], 2000, 4500, 180)

  // --- 6. Awakened-броня (долгий "кинематографичный" крафт) ------------------
  // awakened шлем: halite_helmet + corrupted_helmet + инготы + катализаторы
  SCT(event, 'divinerpg:awakened_halite_helmet', [
    'divinerpg:halite_helmet', 'divinerpg:corrupted_helmet',
    ingot, ingot,
    divineStone, divineStone,
    arcanium, arcanium,
    gaiaIngot, demonSlate, voidStone, terrasteel
  ], 4000, 8000, 200)

  SCT(event, 'divinerpg:awakened_halite_chestplate', [
    'divinerpg:halite_chestplate', 'divinerpg:corrupted_chestplate',
    ingot, ingot, ingot,
    divineStone, divineStone,
    arcanium, arcanium,
    gaiaIngot, demonSlate, voidStone
  ], 4000, 8000, 200)

  SCT(event, 'divinerpg:awakened_halite_leggings', [
    'divinerpg:halite_leggings', 'divinerpg:corrupted_leggings',
    ingot, ingot, ingot,
    divineStone, divineStone,
    arcanium, arcanium,
    gaiaIngot, demonSlate, terrasteel
  ], 4000, 8000, 200)

  SCT(event, 'divinerpg:awakened_halite_boots', [
    'divinerpg:halite_boots', 'divinerpg:corrupted_boots',
    ingot, ingot,
    divineStone, divineStone,
    arcanium, arcanium,
    gaiaIngot, demonSlate, voidStone, terrasteel
  ], 4000, 8000, 200)

  console.info('[halite-recipes] registered SCT halite chain')
})
