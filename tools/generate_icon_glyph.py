from __future__ import annotations

import argparse
from pathlib import Path

from fontTools.misc.transform import Transform
from fontTools.pens.boundsPen import BoundsPen
from fontTools.pens.svgPathPen import SVGPathPen
from fontTools.pens.transformPen import TransformPen
from fontTools.ttLib import TTFont
from fontTools.varLib.instancer import instantiateVariableFont


DEFAULT_FONT = r"C:\Windows\Fonts\NotoSerifTC-VF.ttf"

FOREGROUND_TEMPLATE = """<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <path
        android:fillColor="#B23A2E"
        android:pathData="M54,22 a32,32 0 1,0 0.1,0 z M54,27 a27,27 0 1,1 -0.1,0 z" />
    <path
        android:fillColor="#B23A2E"
        android:pathData="__GLYPH__" />
</vector>
"""

MONOCHROME_TEMPLATE = """<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <path
        android:fillColor="#FFFFFFFF"
        android:pathData="M54,22 a32,32 0 1,0 0.1,0 z M54,27 a27,27 0 1,1 -0.1,0 z" />
    <path
        android:fillColor="#FFFFFFFF"
        android:pathData="__GLYPH__" />
</vector>
"""


def load_font(font_path: str, weight: int) -> TTFont:
    font = TTFont(font_path)
    if "fvar" in font:
        font = instantiateVariableFont(font, {"wght": weight}, inplace=False)
    return font


def glyph_path(
    font: TTFont,
    char: str,
    center_x: float,
    center_y: float,
    size: float,
) -> tuple[str, tuple[float, float, float, float]]:
    cmap = font.getBestCmap()
    glyph_name = cmap[ord(char)]
    glyph_set = font.getGlyphSet()
    glyph = glyph_set[glyph_name]

    bounds_pen = BoundsPen(glyph_set)
    glyph.draw(bounds_pen)
    if bounds_pen.bounds is None:
        raise ValueError(f"Glyph {char!r} has no outline")
    x_min, y_min, x_max, y_max = bounds_pen.bounds
    width = x_max - x_min
    height = y_max - y_min
    scale = size / max(width, height)

    tx = center_x - scale * (x_min + x_max) / 2.0
    ty = center_y + scale * (y_min + y_max) / 2.0
    transform = Transform(scale, 0.0, 0.0, -scale, tx, ty)

    svg_pen = SVGPathPen(glyph_set)
    transform_pen = TransformPen(svg_pen, transform)
    glyph.draw(transform_pen)
    return svg_pen.getCommands(), bounds_pen.bounds


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--font", default=DEFAULT_FONT)
    parser.add_argument("--char", default="帥")
    parser.add_argument("--weight", type=int, default=700)
    parser.add_argument("--foreground-size", type=float, default=48.0)
    parser.add_argument("--monochrome-size", type=float, default=48.0)
    parser.add_argument("--center-x", type=float, default=54.0)
    parser.add_argument("--center-y", type=float, default=54.0)
    parser.add_argument("--text-output", default=str(Path(__file__).with_name("icon_glyph_path.txt")))
    parser.add_argument(
        "--foreground-out",
        default=str(Path(__file__).resolve().parents[1] / "app" / "src" / "main" / "res" / "drawable" / "ic_launcher_foreground.xml"),
    )
    parser.add_argument(
        "--monochrome-out",
        default=str(Path(__file__).resolve().parents[1] / "app" / "src" / "main" / "res" / "drawable" / "ic_launcher_monochrome.xml"),
    )
    args = parser.parse_args()

    font = load_font(args.font, args.weight)
    foreground_path, bounds = glyph_path(
        font, args.char, args.center_x, args.center_y, args.foreground_size
    )
    monochrome_path, _ = glyph_path(
        font, args.char, args.center_x, args.center_y, args.monochrome_size
    )

    Path(args.text_output).write_text(foreground_path, encoding="utf-8")
    Path(args.foreground_out).write_text(
        FOREGROUND_TEMPLATE.replace("__GLYPH__", foreground_path),
        encoding="utf-8",
    )
    Path(args.monochrome_out).write_text(
        MONOCHROME_TEMPLATE.replace("__GLYPH__", monochrome_path),
        encoding="utf-8",
    )
    print(f"glyph={args.char} bounds={bounds} path-length={len(foreground_path)}")
    print(f"wrote {args.foreground_out}")
    print(f"wrote {args.monochrome_out}")


if __name__ == "__main__":
    main()

