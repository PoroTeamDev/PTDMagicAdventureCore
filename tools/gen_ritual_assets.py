"""Генерация процедурных ассетов ритуала SCT.

1) textures/block/chalk_mark.png — глиф мела 16x16 (прозрачный фон).
2) textures/block/ritual_circle.png — большой рисунок круга 512x512 (13x13 блоков),
   прозрачный центр 3x3 под платформу SCT.

Запуск: python tools/gen_ritual_assets.py
"""

from __future__ import annotations

import math
import os

from PIL import Image, ImageDraw, ImageFilter

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
TEX = os.path.join(
    ROOT, "src", "main", "resources", "assets", "magicadventurecore", "textures", "block"
)


def _rgba(color: tuple[int, int, int], alpha: int) -> tuple[int, int, int, int]:
    return (color[0], color[1], color[2], alpha)


def chalk_mark() -> None:
    """16x16 глиф: ромб с перекрестием, фиолетовое свечение."""
    size = 16
    image = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    draw = ImageDraw.Draw(image)
    core = (214, 200, 255)
    mid = (185, 140, 255)
    dark = (122, 79, 191)

    # Внешний ромб.
    draw.line([(8, 1), (15, 8), (8, 15), (1, 8), (8, 1)], fill=_rgba(mid, 255))
    # Внутренний ромб.
    draw.line([(8, 4), (12, 8), (8, 12), (4, 8), (8, 4)], fill=_rgba(dark, 230))
    # Перекрестие и центр.
    draw.line([(8, 2), (8, 14)], fill=_rgba(core, 255))
    draw.line([(2, 8), (14, 8)], fill=_rgba(core, 255))
    draw.point([(8, 8)], fill=_rgba(core, 255))
    # Уголки-искры.
    for (x, y) in [(1, 1), (14, 1), (1, 14), (14, 14)]:
        draw.point([(x, y)], fill=_rgba(mid, 180))

    image.save(os.path.join(TEX, "chalk_mark.png"))


def ritual_circle() -> None:
    """512x512: дисковый рисунок r=5.5 блоков, центр 3x3 прозрачный."""
    size = 512
    px_per_block = size / 13.0
    center = size / 2.0

    image = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    draw = ImageDraw.Draw(image)

    violet = (185, 140, 255)
    light = (214, 200, 255)
    dark = (122, 79, 191)
    mana = (127, 212, 255)

    def circle(radius_blocks: float, color: tuple[int, int, int, int], width: int = 2) -> None:
        r = radius_blocks * px_per_block
        draw.ellipse(
            [center - r, center - r, center + r, center + r],
            outline=color,
            width=width,
        )

    # Внешнее кольцо диска и тонкие внутренние кольца.
    circle(5.5, _rgba(light, 235), 3)
    circle(5.45, _rgba(violet, 110), 1)
    circle(4.6, _rgba(violet, 190), 2)
    circle(3.7, _rgba(dark, 200), 2)
    circle(2.9, _rgba(violet, 170), 1)
    circle(2.3, _rgba(dark, 150), 1)

    # Радиальные лучи.
    for index in range(16):
        angle = index * (2.0 * math.pi / 16.0)
        r0 = 2.4 * px_per_block
        r1 = (5.35 if index % 4 == 0 else 5.15) * px_per_block
        x0 = center + math.cos(angle) * r0
        y0 = center + math.sin(angle) * r0
        x1 = center + math.cos(angle) * r1
        y1 = center + math.sin(angle) * r1
        draw.line([(x0, y0), (x1, y1)], fill=_rgba(violet, 150), width=1)

    # Руны-штрихи по внешнему кольцу.
    for index in range(24):
        angle = index * (2.0 * math.pi / 24.0)
        r0 = 5.02 * px_per_block
        r1 = 5.32 * px_per_block
        x0 = center + math.cos(angle) * r0
        y0 = center + math.sin(angle) * r0
        x1 = center + math.cos(angle) * r1
        y1 = center + math.sin(angle) * r1
        draw.line([(x0, y0), (x1, y1)], fill=_rgba(light if index % 3 == 0 else violet, 220), width=2)

    # Малые рунные кружки между кольцами.
    rune_radius = 4.15 * px_per_block
    for index in range(8):
        angle = index * (2.0 * math.pi / 8.0)
        x = center + math.cos(angle) * rune_radius
        y = center + math.sin(angle) * rune_radius
        rr = 0.22 * px_per_block
        draw.ellipse([x - rr, y - rr, x + rr, y + rr], outline=_rgba(mana, 220), width=2)

    # Четыре эмблемы стабилизаторов (кардинальные точки на границе 13x13, r=6).
    for angle in (0.0, math.pi / 2.0, math.pi, 3.0 * math.pi / 2.0):
        x = center + math.cos(angle) * (6.0 * px_per_block)
        y = center + math.sin(angle) * (6.0 * px_per_block)
        rr = 0.3 * px_per_block
        draw.ellipse([x - rr, y - rr, x + rr, y + rr], fill=_rgba(dark, 200),
                     outline=_rgba(mana, 240), width=2)

    # Мягкое свечение: размытая копия под основным рисунком.
    glow = image.filter(ImageFilter.GaussianBlur(radius=6))
    glow.putalpha(glow.getchannel("A").point(lambda value: int(value * 0.45)))
    image = Image.alpha_composite(glow, image)

    # Прозрачный центр 3x3 блока под платформу.
    hole = int(round(3.0 * px_per_block))
    hole_start = int(round(center - hole / 2.0))
    pixels = image.load()
    for y in range(hole_start, hole_start + hole):
        for x in range(hole_start, hole_start + hole):
            pixels[x, y] = (0, 0, 0, 0)

    image.save(os.path.join(TEX, "ritual_circle.png"))


def main() -> None:
    os.makedirs(TEX, exist_ok=True)
    chalk_mark()
    ritual_circle()
    print("generated chalk_mark.png and ritual_circle.png in", TEX)


if __name__ == "__main__":
    main()
