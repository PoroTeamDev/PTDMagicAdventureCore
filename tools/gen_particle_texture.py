"""
Генератор текстуры частицы «арканный крестик» (8x8 RGBA PNG).

ВНИМАНИЕ: скрипт ПЕРЕЗАПИСЫВАЕТ текстуру по пути ниже. Актуальная текстура
нарисована вручную — запускать скрипт только если нужно вернуть старый вариант
или сгенерировать основу для новой отрисовки.

Запуск (нужен Pillow):
    python tools/gen_particle_texture.py
Результат: src/main/resources/assets/magicadventurecore/textures/particle/arcane_cross.png
"""

import os

from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(
    ROOT,
    "src", "main", "resources", "assets", "magicadventurecore",
    "textures", "particle", "arcane_cross.png",
)

ARM = (138, 63, 209, 220)     # фиолетовые «руки» креста
CORE = (224, 179, 255, 255)   # светлый центр
GLOW = (176, 107, 255, 90)    # свечение вокруг


def main() -> None:
    img = Image.new("RGBA", (8, 8), (0, 0, 0, 0))
    px = img.load()

    for i in range(8):
        px[3, i] = ARM
        px[4, i] = ARM
        px[i, 3] = ARM
        px[i, 4] = ARM

    px[3, 3] = CORE
    px[4, 3] = CORE
    px[3, 4] = CORE
    px[4, 4] = CORE

    px[2, 3] = GLOW
    px[2, 4] = GLOW
    px[5, 3] = GLOW
    px[5, 4] = GLOW
    px[3, 2] = GLOW
    px[4, 2] = GLOW
    px[3, 5] = GLOW
    px[4, 5] = GLOW

    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    img.save(OUT)
    print("saved", OUT)


if __name__ == "__main__":
    main()
