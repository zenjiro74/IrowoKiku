#!/usr/bin/env python3
"""ランチャーアイコンのベクタドロウアブルを生成する。

    python3 scripts/generate_launcher_icon.py

色相環の扇形と正弦波はパスを手書きすると調整が効かないので、ここで計算して吐く。
出力先の XML は生成物なので直接編集せず、形を変えるときはこのスクリプトを直すこと。
"""
import colorsys, math, pathlib

RES = pathlib.Path(__file__).resolve().parent.parent / "app/src/main/res/drawable"

CX = CY = 54.0          # adaptive icon viewport center
RO, RI = 31.0, 19.0     # hue ring outer / inner radius (safe zone is r<=36)
SEGMENTS = 12
GAP_DEG = 1.6           # small gap so segments read as separate swatches

def pt(r, deg):
    a = math.radians(deg)
    return CX + r * math.cos(a), CY + r * math.sin(a)

def donut_sector(a1, a2, ri, ro):
    """Path for one ring segment, sweeping clockwise from a1 to a2."""
    x1, y1 = pt(ro, a1); x2, y2 = pt(ro, a2)
    x3, y3 = pt(ri, a2); x4, y4 = pt(ri, a1)
    large = 1 if (a2 - a1) % 360 > 180 else 0
    return (f"M{x1:.2f},{y1:.2f}"
            f"A{ro:.2f},{ro:.2f} 0 {large} 1 {x2:.2f},{y2:.2f}"
            f"L{x3:.2f},{y3:.2f}"
            f"A{ri:.2f},{ri:.2f} 0 {large} 0 {x4:.2f},{y4:.2f}Z")

def hue_color(i):
    r, g, b = colorsys.hsv_to_rgb(i / SEGMENTS, 0.86, 1.0)
    return "#FF%02X%02X%02X" % (round(r * 255), round(g * 255), round(b * 255))

def sine_path(x0, x1, amp, cycles, samples=96):
    pts = []
    for i in range(samples + 1):
        t = i / samples
        x = x0 + (x1 - x0) * t
        y = CY - amp * math.sin(2 * math.pi * cycles * t)
        pts.append((x, y))
    head = f"M{pts[0][0]:.2f},{pts[0][1]:.2f}"
    return head + "".join(f"L{x:.2f},{y:.2f}" for x, y in pts[1:])

# The wave starts at the ring's left edge and runs out past the right edge:
# colour goes in, sound comes out.
WAVE = sine_path(x0=CX - RO - 2, x1=CX + RO + 2, amp=8.5, cycles=1.75)

ring = "\n".join(
    f'''    <path
        android:fillColor="{hue_color(i)}"
        android:pathData="{donut_sector(i * 360.0 / SEGMENTS + GAP_DEG / 2,
                                        (i + 1) * 360.0 / SEGMENTS - GAP_DEG / 2, RI, RO)}" />'''
    for i in range(SEGMENTS)
)

FOREGROUND = f'''<?xml version="1.0" encoding="utf-8"?>
<!--
  色相環からサイン波が伸びる形。「色を測って音にする」という変換そのものを表す。
  アダプティブアイコンの安全域 (中心から半径 36) に収まるよう半径を決めている。
-->
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">

{ring}

    <!-- 波の下敷き。色相環の上を通るので、暗い縁取りで波を浮かせる。 -->
    <path
        android:pathData="{WAVE}"
        android:strokeColor="#E6101418"
        android:strokeWidth="7"
        android:strokeLineCap="round"
        android:strokeLineJoin="round" />
    <path
        android:pathData="{WAVE}"
        android:strokeColor="#FFFFFFFF"
        android:strokeWidth="3.6"
        android:strokeLineCap="round"
        android:strokeLineJoin="round" />
</vector>
'''

# Themed icons render as a single tint colour, so the ring has to be an outline
# rather than a filled donut — a filled one would merge with the wave.
MONO_WAVE = sine_path(x0=CX - RO - 2, x1=CX + RO + 2, amp=8.5, cycles=1.75)
MONOCHROME = f'''<?xml version="1.0" encoding="utf-8"?>
<!--
  テーマアイコン用。単色で塗られるため、色相環は塗りではなく輪郭線で表す。
  塗りにすると波と溶けて形が読めなくなる。
-->
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108"
    android:tint="#FF000000">

    <path
        android:pathData="M{CX + RO:.2f},{CY:.2f}A{RO:.2f},{RO:.2f} 0 1 1 {CX - RO:.2f},{CY:.2f}A{RO:.2f},{RO:.2f} 0 1 1 {CX + RO:.2f},{CY:.2f}Z"
        android:strokeColor="#FFFFFFFF"
        android:strokeWidth="4" />
    <path
        android:pathData="{MONO_WAVE}"
        android:strokeColor="#FFFFFFFF"
        android:strokeWidth="4.5"
        android:strokeLineCap="round"
        android:strokeLineJoin="round" />
</vector>
'''

BACKGROUND = '''<?xml version="1.0" encoding="utf-8"?>
<!-- ビューファインダの暗さに合わせた無地の背景。前景の彩度を邪魔しない。 -->
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <path
        android:fillColor="#FF11151C"
        android:pathData="M0,0h108v108h-108z" />
</vector>
'''

for name, content in [
    ("ic_launcher_foreground.xml", FOREGROUND),
    ("ic_launcher_monochrome.xml", MONOCHROME),
    ("ic_launcher_background.xml", BACKGROUND),
]:
    (RES / name).write_text(content)
    print("wrote", RES / name)
