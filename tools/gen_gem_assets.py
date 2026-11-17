"""
Генератор ассетов Гема Жизни:
  - 8 моделей гема по стадиям заполнения + базовая модель с overrides.

Текстуры гема (`gem_of_life_0..7`) нарисованы вручную и лежат в
assets/magicadventurecore/textures/item/gem_of_life/ — скрипт их не трогает.

Частица орбов использует ванильный спрайт `minecraft:glow` (тинтуется цветом),
поэтому собственных текстур частиц здесь не генерируется.

Запуск:
    python tools/gen_gem_assets.py
"""

import json
import os

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "magicadventurecore")
MODELS_DIR = os.path.join(ASSETS, "models", "item")

STAGES = 8


def write_json(path: str, data: dict) -> None:
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as handle:
        json.dump(data, handle, indent=2, ensure_ascii=False)
        handle.write("\n")


def gen_models() -> None:
    # Отдельные модели под каждую стадию.
    for stage in range(STAGES):
        write_json(os.path.join(MODELS_DIR, f"gem_of_life_{stage}.json"), {
            "parent": "item/generated",
            "textures": {
                "layer0": f"magicadventurecore:item/gem_of_life/gem_of_life_{stage}"
            }
        })

    # Базовая модель + overrides по fill: 0.125 -> стадия 1 ... 0.875 -> стадия 7.
    # Ключ predicate — полный ResourceLocation, иначе парсится как minecraft:fill
    # и не совпадает с зарегистрированным magicadventurecore:fill.
    overrides = []
    for stage in range(1, STAGES):
        overrides.append({
            "predicate": {"magicadventurecore:fill": stage / STAGES},
            "model": f"magicadventurecore:item/gem_of_life_{stage}"
        })
    write_json(os.path.join(MODELS_DIR, "gem_of_life.json"), {
        "parent": "item/generated",
        "textures": {
            "layer0": "magicadventurecore:item/gem_of_life/gem_of_life_0"
        },
        "overrides": overrides
    })
    print("saved item models")


def main() -> None:
    gen_models()


if __name__ == "__main__":
    main()
