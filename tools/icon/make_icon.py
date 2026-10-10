#!/usr/bin/env python3
"""
Gera o ícone do app (violão com as 3 notas de acorde no braço) a partir de UMA
descrição só, para não haver diferença entre os arquivos:

  app/src/main/res/drawable/ic_launcher_foreground.xml   (ícone adaptativo, Android 8+)
  app/src/main/res/drawable/ic_launcher_monochrome.xml   (ícone temático, Android 13+)
  app/src/main/res/drawable/brand_mark.xml               (marca dentro do app)
  app/src/main/res/mipmap-*/ic_launcher(_round).webp     (Android 7)
  app/src/main/ic_launcher-playstore.png                 (512 px, vitrine)

Uso (na raiz do repositório):  pip install cairosvg pillow && python3 tools/icon/make_icon.py
Para mudar cores, edite o bloco CORES; para mudar o desenho, edite glyph().
"""
import io
import os
import sys

import cairosvg
from PIL import Image, ImageDraw

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
RES = os.path.join(ROOT, "app", "src", "main", "res")

# ---------------------------------------------------------------- CORES
INK = "#0B0C0F"          # fundo (o mesmo do app)
BRASS = "#E0A94A"        # tampo do violão (acento do app)
BRASS_DARK = "#B9852F"   # filete, roseta e cabeça
FRETBOARD = "#F1E6D0"    # escala do braço
NUT = "#ECEEF1"          # rastilho e tarraxas
FRET = "#8E9197"         # trastes
STRING = "#C9CCD1"       # cordas
TONIC = "#57B892"        # as 3 funções do campo harmônico
SUBDOM = "#5B8AD6"
DOMINANT = "#D9695E"

# Posição do violão no ícone: centro, giro e escala (cabe na zona segura de 66 dp).
ROTATION = 45
SCALE = 0.68
SHIFT_Y = 10             # centraliza o desenho (cabeça em -55, fundo do corpo em +34)


# ---------------------------------------------------------------- DESENHO
# Coordenadas do violão "em pé" (cabeça para cima, y cresce para baixo).
BODY = ("M0,-7 C7,-7 11,-4 11,2 C11,6 7.5,8 7.5,10 C7.5,12 15,15 15,22 C15,30 7.5,34 0,34 "
        "C-7.5,34 -15,30 -15,22 C-15,15 -7.5,12 -7.5,10 C-7.5,8 -11,6 -11,2 C-11,-4 -7,-7 0,-7 Z")
HEAD = "M-3.6,-41 L-5,-53.5 Q0,-55.5 5,-53.5 L3.6,-41 Z"


def circle(cx, cy, r):
    return f"M{cx - r:g},{cy:g} a{r:g},{r:g} 0 1,0 {2 * r:g},0 a{r:g},{r:g} 0 1,0 {-2 * r:g},0 Z"


def rect(x, y, w, h):
    return f"M{x:g},{y:g} h{w:g} v{h:g} h{-w:g} Z"


def glyph():
    """Lista de (pathData, cor de preenchimento, extras) na ordem de pintura."""
    g = [(HEAD, BRASS_DARK, {})]
    for y in (-44, -47.8, -51.6):                      # tarraxas
        for x in (-6.1, 6.1):
            g.append((circle(x, y, 1.25), NUT, {}))
    g.append((BODY, BRASS, {"stroke": BRASS_DARK, "strokeWidth": 1}))
    g.append((circle(0, 5, 4.9), BRASS_DARK, {}))      # roseta
    g.append((circle(0, 5, 3.9), INK, {}))             # boca
    g.append((rect(-3.2, -41.5, 6.4, 37), FRETBOARD, {}))
    g.append((rect(-3.7, -41.9, 7.4, 1.4), NUT, {}))
    for y in (-34.5, -28, -21.5, -15, -9):             # trastes
        g.append((rect(-3.2, y - 0.3, 6.4, 0.6), FRET, {}))
    g.append((rect(-5.6, 22, 11.2, 2.9), INK, {}))     # cavalete
    for x in (-2.5, -1.5, -0.5, 0.5, 1.5, 2.5):        # 6 cordas
        g.append((rect(x - 0.13, -41, 0.26, 64.5), STRING, {"alpha": 0.85}))
    for y, c in ((-37.8, TONIC), (-31.3, SUBDOM), (-24.8, DOMINANT)):   # notas do acorde
        g.append((circle(0, y, 2.35), c, {}))
    return g


def silhouette():
    """Uma cor só, com a boca vazada — para o ícone temático do Android 13+."""
    return [
        (HEAD, "#FFFFFF", {}),
        *[(circle(x, y, 1.25), "#FFFFFF", {}) for y in (-44, -47.8, -51.6) for x in (-6.1, 6.1)],
        (BODY + " " + circle(0, 5, 3.9), "#FFFFFF", {"evenOdd": True}),
        (rect(-3.2, -41.5, 6.4, 37), "#FFFFFF", {}),
    ]


# ---------------------------------------------------------------- SAÍDAS
def to_svg(items, scale=SCALE, background=True):
    out = ['<svg xmlns="http://www.w3.org/2000/svg" width="108" height="108" viewBox="0 0 108 108">']
    if background:
        out.append(f'<rect width="108" height="108" fill="{INK}"/>')
    out.append(f'<g transform="translate(54,54) rotate({ROTATION}) scale({scale}) translate(0,{SHIFT_Y})">')
    for d, color, ex in items:
        attrs = f'd="{d}" fill="{color}"'
        if "alpha" in ex:
            attrs += f' fill-opacity="{ex["alpha"]}"'
        if "stroke" in ex:
            attrs += f' stroke="{ex["stroke"]}" stroke-width="{ex["strokeWidth"]}"'
        if ex.get("evenOdd"):
            attrs += ' fill-rule="evenodd"'
        out.append(f"<path {attrs}/>")
    out.append("</g></svg>")
    return "".join(out)


def to_vector(items, comment, scale=SCALE):
    lines = [
        '<?xml version="1.0" encoding="utf-8"?>',
        f"<!-- {comment} Gerado por tools/icon/make_icon.py — edite lá, não aqui. -->",
        '<vector xmlns:android="http://schemas.android.com/apk/res/android"',
        '    android:width="108dp"',
        '    android:height="108dp"',
        '    android:viewportWidth="108"',
        '    android:viewportHeight="108">',
        f'    <group android:translateX="54" android:translateY="54" android:rotation="{ROTATION}"',
        f'        android:scaleX="{scale}" android:scaleY="{scale}">',
        f'        <group android:translateY="{SHIFT_Y}">',
    ]
    for d, color, ex in items:
        attrs = [f'android:pathData="{d}"', f'android:fillColor="{color}"']
        if "alpha" in ex:
            attrs.append(f'android:fillAlpha="{ex["alpha"]}"')
        if "stroke" in ex:
            attrs += [f'android:strokeColor="{ex["stroke"]}"', f'android:strokeWidth="{ex["strokeWidth"]}"']
        if ex.get("evenOdd"):
            attrs.append('android:fillType="evenOdd"')
        lines.append("            <path\n                " + "\n                ".join(attrs) + " />")
    lines += ["        </group>", "    </group>", "</vector>", ""]
    return "\n".join(lines)


def render(svg, px):
    png = cairosvg.svg2png(bytestring=svg.encode(), output_width=px, output_height=px)
    return Image.open(io.BytesIO(png)).convert("RGBA")


def legacy(full, size, round_):
    """Ícone antigo (Android 7): a área visível (72 de 108) com máscara."""
    w = full.size[0]
    crop = int(round(w * 18 / 108))
    img = full.crop((crop, crop, w - crop, w - crop)).resize((size, size), Image.LANCZOS)
    mask = Image.new("L", (size * 4, size * 4), 0)
    d = ImageDraw.Draw(mask)
    if round_:
        d.ellipse((0, 0, size * 4 - 1, size * 4 - 1), fill=255)
    else:
        d.rounded_rectangle((0, 0, size * 4 - 1, size * 4 - 1), radius=int(size * 4 * 0.22), fill=255)
    out = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    out.paste(img, (0, 0), mask.resize((size, size), Image.LANCZOS))
    return out


def main():
    write = lambda p, s: open(os.path.join(RES, p), "w", encoding="utf-8").write(s)
    write("drawable/ic_launcher_foreground.xml",
          to_vector(glyph(), "Ícone: violão em latão com as 3 notas do campo (T · SD · D) no braço."))
    write("drawable/ic_launcher_monochrome.xml",
          to_vector(silhouette(), "Ícone temático (uma cor): silhueta do violão."))
    write("drawable/brand_mark.xml",
          to_vector(glyph(), "Marca dentro do app (o mesmo violão, maior).", scale=1.12))

    full = render(to_svg(glyph()), 1024)
    for dens, size in (("mdpi", 48), ("hdpi", 72), ("xhdpi", 96), ("xxhdpi", 144), ("xxxhdpi", 192)):
        legacy(full, size, False).save(os.path.join(RES, f"mipmap-{dens}", "ic_launcher.webp"), lossless=True)
        legacy(full, size, True).save(os.path.join(RES, f"mipmap-{dens}", "ic_launcher_round.webp"), lossless=True)
    # Vitrine (512 px, quadrado cheio — a loja aplica a máscara).
    store = render(to_svg(glyph()), 768).crop((0, 0, 768, 768))
    c = int(768 * 18 / 108)
    store.crop((c, c, 768 - c, 768 - c)).resize((512, 512), Image.LANCZOS).save(
        os.path.join(ROOT, "app", "src", "main", "ic_launcher-playstore.png"))
    if "--preview" in sys.argv:
        out = sys.argv[sys.argv.index("--preview") + 1]
        render(to_svg(glyph()), 1024).save(out)
    print("ícone gerado")


if __name__ == "__main__":
    main()
