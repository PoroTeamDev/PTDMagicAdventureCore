"""Append a full mod table to the progression plan."""
import csv
import os

BASE = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
TSV = os.path.join(BASE, "build", "pack_lang", "_mods.tsv")
DOC = os.path.join(BASE, "..", "PTDMagicAdventureCore-progression-plan.md")

CATEGORY = {
    # core
    "magicadventurecore": "Ядро",
    # magic
    "ars_nouveau": "Магия",
    "ars_extended_glyphs": "Магия",
    "ars_botania": "Магия",
    "arsisbotania": "Магия",
    "kubejsarsnouveau": "Магия",
    "arseng": "Магия",
    "bloodmagic": "Магия",
    "bloodmagicteams": "Магия",
    "kubejsbloodmagic": "Магия",
    "botania": "Магия",
    "mythicbotany": "Магия",
    "botanicadds": "Магия",
    "botanicalmachinery": "Магия",
    "aiotbotania": "Магия",
    "botaniajei": "Магия",
    "occultism": "Магия",
    "occultism_kubejs": "Магия",
    "evilcraft": "Магия",
    "etheria": "Магия",
    "kubejs_botania": "Магия",
    # adventure
    "twilightforest": "Приключения",
    "tfbr": "Приключения",
    "tf_dnv": "Приключения",
    "twilight_forest_final_boss": "Приключения",
    "twilight_treehouses": "Приключения",
    "twilight_construct": "Приключения",
    "divinerpg": "Приключения",
    "cataclysm": "Приключения",
    "cataclysm_ut": "Приключения",
    "lootintegrations": "Приключения",
    "lootintegrations_cataclysm": "Приключения",
    "craziness_awakened": "Приключения",
    "awakened_craziness": "Приключения",
    "born_in_chaos_v1": "Приключения",
    # gear
    "apotheosis": "Гир",
    "attributeslib": "Гир",
    "attributefix": "Гир",
    "attributizer": "Гир",
    "tconstruct": "Гир",
    "tconstruct_emergence": "Гир",
    "additionalweaponry": "Гир",
    "mysticalagriculture": "Гир",
    "cucumber": "Гир",
    "relics": "Гир",
    "artifacts": "Гир",
    "rarcompat": "Гир",
    "arcane_abilities": "Гир",
    "quark": "Гир",
    "zeta": "Гир",
    "pehkui": "Гир",
    "ftbmaterials": "Гир",
    # quests / qol
    "ftbquests": "Квесты/QoL",
    "ftbteams": "Квесты/QoL",
    "ftbchunks": "Квесты/QoL",
    "ftbessentials": "Квесты/QoL",
    "ftbranks": "Квесты/QoL",
    "ftbfiltersystem": "Квесты/QoL",
    "ftblibrary": "Квесты/QoL",
    "ftbpc": "Квесты/QoL",
    "ftbpromoter": "Квесты/QoL",
    "ftbxmodcompat": "Квесты/QoL",
    "ftbbackups2": "Квесты/QoL",
    "jei": "Квесты/QoL",
    "jeresources": "Квесты/QoL",
    "jade": "Квесты/QoL",
    "guideme": "Квесты/QoL",
    "modonomicon": "Квесты/QoL",
    "patchouli": "Квесты/QoL",
    "sophisticatedbackpacks": "Квесты/QoL",
    "sophisticatedcore": "Квесты/QoL",
    "storagedrawers": "Квесты/QoL",
    "xaerominimap": "Квесты/QoL",
    "mousetweaks": "Квесты/QoL",
    "clumps": "Квесты/QoL",
    "embeddium": "Квесты/QoL",
    "entityculling": "Квесты/QoL",
    "ferritecore": "Квесты/QoL",
    "sound_physics_remastered": "Квесты/QoL",
    "fancymenu": "Квесты/QoL",
    "konkrete": "Квесты/QoL",
    "melody": "Квесты/QoL",
    "libx": "Библиотеки",
    "octolib": "Библиотеки",
    "polylib": "Библиотеки",
    "placebo": "Библиотеки",
    "mantle": "Библиотеки",
    "cyclopscore": "Библиотеки",
    "hammerlib": "Библиотеки",
    "geckolib": "Библиотеки",
    "smartbrainlib": "Библиотеки",
    "lionfishapi": "Библиотеки",
    "architectury": "Библиотеки",
    "cloth_config": "Библиотеки",
    "jsonthings": "Библиотеки",
    "cupboard": "Библиотеки",
    "appleskin": "Квесты/QoL",
}

rows = []
with open(TSV, encoding="utf-8") as fh:
    for line in fh:
        parts = line.rstrip("\n").split("\t")
        if len(parts) < 5:
            continue
        jar, modid, name, version, desc = parts[:5]
        rows.append((CATEGORY.get(modid, "Библиотеки/прочее"), modid, name, version, desc))

rows.sort(key=lambda r: (r[0], r[2].lower()))

out = ["\n## D. Полный список модов пака (100)\n",
       "| Категория | Мод | modid | Версия | Роль |",
       "|---|---|---|---|---|"]
for cat, modid, name, version, desc in rows:
    desc = desc.replace("|", "/").strip()
    if len(desc) > 80:
        desc = desc[:77] + "..."
    out.append(f"| {cat} | {name} | `{modid}` | {version} | {desc} |")

with open(DOC, "a", encoding="utf-8") as fh:
    fh.write("\n".join(out) + "\n")

print(f"appended {len(rows)} rows")
