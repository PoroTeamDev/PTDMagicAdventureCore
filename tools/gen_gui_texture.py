"""
Подготовка фона GUI стола SCT.

Скрипт нужен только если исходник снова окажется «сырым» большим изображением.
Актуальная текстура уже нарисована вручную в размере 176x230 и лежит прямо
в assets .../textures/gui/sct_gui.png — тогда скрипт просто ничего не делает.

Правило: рамка слота должна быть 18x18 px (ванильный размер слота).

Запуск:
    python tools/gen_gui_texture.py [путь_к_исходнику]
"""

import os
import sys

from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DEFAULT_SOURCE = os.path.join(os.path.dirname(ROOT), "SCT.png")
OUT = os.path.join(
    ROOT, "src", "main", "resources", "assets", "magicadventurecore",
    "textures", "gui", "sct_gui.png",
)

TARGET_SIZE = (176, 230)
SLOT_PX_SOURCE = 105
SLOT_PX_TARGET = 18


def main() -> None:
    source = sys.argv[1] if len(sys.argv) > 1 else DEFAULT_SOURCE
    image = Image.open(source).convert("RGBA")
    print("source", image.size)

    if image.size == TARGET_SIZE:
        resized = image
        print("уже нужный размер — сохраняю как есть")
    else:
        scale = SLOT_PX_TARGET / SLOT_PX_SOURCE
        width = round(image.width * scale)
        height = round(image.height * scale)
        print("scale", scale, "->", width, "x", height)
        resized = image.resize((width, height), Image.LANCZOS)

    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    resized.save(OUT)
    print("saved", OUT)


if __name__ == "__main__":
    main()
