// ---------------------------------------------------------------------------
// Халитовые шарды вместо халитовой экипировки с боссов DivineRPG.
//
// Было: боссы гарантированно дропали halite_* броню/оружие.
// Стало: падают halite_shard (2-6 в зависимости от босса) + старый предмет
// с шансом 0.5%. Остальные дропы (сердца, статуэтки, фрагменты) сохранены.
//
// Почему таблица пересобирается целиком, а не правится: вырезать отдельные
// записи из чужой loot-таблицы через KJS ненадёжно (Rhino превращает
// java.lang.String в объект, из-за чего сравнение имён молча не срабатывает).
// Поэтому делаем clearPools() и добавляем нужные пулы заново — поведение
// предсказуемое. При обновлении DivineRPG список extra-дропов стоит сверить.
// ---------------------------------------------------------------------------

const SHARD = 'magicadventurecore:halite_shard'
const LEGACY_EMPTY_WEIGHT = 199

const ResourceLocation = Java.loadClass('net.minecraft.resources.ResourceLocation')

// extra — остальные дропы DivineRPG, которые сохраняем:
//   item — id, min..max — количество роллов (0..N), playerOnly — условие killed_by_player
const BOSSES = [
  {
    id: 'divinerpg:densos', min: 2, max: 3,
    legacy: ['divinerpg:halite_chestplate'],
    extra: [
      { item: 'divinerpg:mortum_heart', min: 1, max: 4 },
      { item: 'divinerpg:densos_statue', min: 1, max: 1, playerOnly: true }
    ]
  },
  {
    id: 'divinerpg:reyvor', min: 2, max: 3,
    legacy: ['divinerpg:halite_leggings'],
    extra: [
      { item: 'divinerpg:mortum_heart', min: 1, max: 4 },
      { item: 'divinerpg:reyvor_statue', min: 1, max: 1, playerOnly: true }
    ]
  },
  {
    id: 'divinerpg:twilight_demon', min: 2, max: 2,
    legacy: ['divinerpg:halite_boots', 'divinerpg:halite_helmet'],
    extra: [
      { item: 'divinerpg:twilight_demon_statue', min: 1, max: 1, playerOnly: true }
    ]
  },
  {
    id: 'divinerpg:vamacheron', min: 3, max: 3,
    legacy: ['divinerpg:halite_blade'],
    extra: [
      { item: 'divinerpg:mortum_heart', min: 1, max: 4 },
      { item: 'divinerpg:vamacheron_statue', min: 1, max: 1, playerOnly: true }
    ]
  },
  {
    id: 'divinerpg:eternal_archer', min: 3, max: 3,
    legacy: ['divinerpg:halite_bow'],
    extra: [
      { item: 'divinerpg:apalachia_heart', min: 1, max: 4 },
      { item: 'divinerpg:eternal_archer_statue', min: 1, max: 1, playerOnly: true }
    ]
  },
  {
    id: 'divinerpg:karot', min: 2, max: 3,
    legacy: ['divinerpg:halite_shickaxe', 'divinerpg:halite_phaser'],
    extra: [
      { item: 'divinerpg:mortum_heart', min: 1, max: 4 },
      { item: 'divinerpg:karot_statue', min: 1, max: 1, playerOnly: true }
    ]
  },
  {
    id: 'divinerpg:experienced_cori', min: 1, max: 2,
    legacy: ['divinerpg:halite_shield'],
    extra: [
      { item: 'divinerpg:skythern_fragments', min: 0, max: 4 },
      { item: 'divinerpg:skythern_blitz', min: 1, max: 1 },
      { item: 'divinerpg:skythern_heart', min: 1, max: 4 },
      { item: 'divinerpg:experienced_cori_statue', min: 1, max: 1, playerOnly: true }
    ]
  },
  {
    id: 'divinerpg:soul_fiend', min: 4, max: 6,
    legacy: ['divinerpg:halite_slicer', 'divinerpg:halite_blitz'],
    extra: [
      { item: 'divinerpg:mortum_heart', min: 1, max: 4 },
      { item: 'divinerpg:soul_fiend_statue', min: 1, max: 1, playerOnly: true }
    ]
  }
]

ServerEvents.entityLootTables(event => {
  BOSSES.forEach(def => {
    const id = ResourceLocation.tryParse(def.id)
    if (!id) {
      console.error('[halite-loot] invalid entity id: ' + def.id)
      return
    }
    event.modify(id, loot => {
      // Таблица пересобирается с нуля — старые халитовые записи просто исчезают.
      loot.clearPools()

      // 1. Основной дроп: халитовые шарды.
      loot.addPool(shardPool => {
        shardPool.setUniformRolls(def.min, def.max)
        shardPool.addItem(Item.of(SHARD))
        shardPool.killedByPlayer()
      })

      // 2. Старый дроп с шансом 0.5% на предмет.
      loot.addPool(legacyPool => {
        legacyPool.addEmpty(LEGACY_EMPTY_WEIGHT * def.legacy.length)
        def.legacy.forEach(item => legacyPool.addItem(Item.of(item)))
        legacyPool.killedByPlayer()
      })

      // 3. Сохранённые дропы DivineRPG (сердца, статуэтки, фрагменты).
      def.extra.forEach(extra => {
        loot.addPool(extraPool => {
          extraPool.setUniformRolls(extra.min, extra.max)
          extraPool.addItem(Item.of(extra.item))
          if (extra.playerOnly) extraPool.killedByPlayer()
        })
      })
    })
  })
  console.info('[halite-loot] rebuilt ' + BOSSES.length + ' DivineRPG boss loot tables')
})
