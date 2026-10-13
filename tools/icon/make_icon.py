#!/usr/bin/env python3
"""
Desenha o ícone do app e gera todos os formatos de uma vez.

O desenho: o diagrama de acorde "das revistinhas" (o símbolo que todo
violonista reconhece), com o rastilho em latão, 6 cordas cor de marfim, 4
casas e três dedos nas cores das funções do campo harmônico: Tônica (verde),
Subdominante (azul) e Dominante (vermelho). Os dedos formam o desenho do Dó
maior aberto, uma escada que desce da 2ª para a 5ª corda.

Tudo é geometria (nada de imagem de fora): o desenho fica nítido em qualquer
tamanho e qualquer ajuste é feito mudando os números abaixo.

Arquivos gerados:
  res/mipmap-*/ic_launcher_foreground.png   desenho (ícone adaptativo, Android 8+)
  res/mipmap-*/ic_launcher_monochrome.png   versão de uma cor (ícone temático, Android 13+)
  res/mipmap-*/ic_launcher(_round).webp     ícone antigo (Android 7)
  res/drawable/ic_launcher_background.xml   cor de fundo
  res/drawable-nodpi/brand_mark.png         marca usada dentro do app (fundo transparente)
  app/src/main/ic_launcher-playstore.png    512 px para a loja
  app/src/dev/res/drawable/ic_launcher_background.xml   fundo da versão DEV (com faixa)

Uso (na raiz do repositório):  pip install pillow && python3 tools/icon/make_icon.py
"""
import os

from PIL import Image, ImageDraw

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
RES = os.path.join(ROOT, "app", "src", "main", "res")
DEV_RES = os.path.join(ROOT, "app", "src", "dev", "res")

# --- Cores (as mesmas de ui/theme/Color.kt) ---------------------------------
BACKGROUND = "#0C0D10"   # fundo do ícone
BRASS = "#E0A94A"        # rastilho (o acento do app)
IVORY = "#ECE6D8"        # cordas
FRET = "#555C69"         # trastes
TONIC = "#57B892"        # FuncTonic
SUBDOMINANT = "#6B97DF"  # FuncSubdominant
DOMINANT = "#E07468"     # FuncDominant

# --- Geometria, em dp, na camada de 108 dp (zona segura: círculo de raio 33) --
CENTER_X, CENTER_Y = 54.0, 54.0
WIDTH = 35.0             # da 6ª à 1ª corda
FRET_SPACE = 10.0        # altura de cada casa
FRETS = 4
NUT = 3.6                # espessura do rastilho
TAIL = 3.0               # quanto as cordas passam da última casa
STRING_WIDTHS = (1.7, 1.5, 1.35, 1.2, 1.05, 0.95)  # 6ª (grave) → 1ª (aguda)
FRET_WIDTH = 1.1
DOT_RADIUS = 4.3
DOT_RIM = 1.0            # contorno escuro que separa o dedo da corda
MARK_GAP = 2.4           # distância das marcas (× e o) até o rastilho
MARK_SIZE = 2.0          # raio das marcas
# Marcas acima do rastilho, como em toda cifra: × = não tocar, o = corda solta.
MUTED = (0,)
OPEN = (3, 5)
# (corda: 0 = 6ª ... 5 = 1ª, casa 1..4, cor): o Dó maior aberto.
DOTS = ((4, 1, TONIC), (2, 2, SUBDOMINANT), (1, 3, DOMINANT))

SS = 40                  # pixels por dp no desenho mestre (bem grande, depois reduz)
DENSITIES = (("mdpi", 1), ("hdpi", 1.5), ("xhdpi", 2), ("xxhdpi", 3), ("xxxhdpi", 4))


def rgba(hex_color, alpha=255):
    h = hex_color.lstrip("#")
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4)) + (alpha,)


def draw_glyph(mono=False):
    """Camada de 108 dp com o diagrama, em alta resolução e fundo transparente."""
    size = 108 * SS
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    px = lambda v: v * SS                                   # dp → px
    color = (lambda c, a=255: (255, 255, 255, a)) if mono else rgba

    height = NUT + FRET_SPACE * FRETS
    marks = MARK_GAP + 2 * MARK_SIZE
    left = CENTER_X - WIDTH / 2
    top = CENTER_Y - (height + TAIL - marks) / 2
    gap = WIDTH / 5
    xs = [left + i * gap for i in range(6)]
    nut_bottom = top + NUT
    fret_ys = [nut_bottom + FRET_SPACE * k for k in range(1, FRETS + 1)]

    # Trastes (atrás das cordas).
    for y in fret_ys:
        d.line((px(left), px(y), px(left + WIDTH), px(y)), fill=color(FRET, 190), width=round(px(FRET_WIDTH)))

    # Cordas, com ponta arredondada embaixo.
    bottom = fret_ys[-1] + TAIL
    for x, w in zip(xs, STRING_WIDTHS):
        d.rounded_rectangle((px(x - w / 2), px(nut_bottom - 0.5), px(x + w / 2), px(bottom)),
                            radius=px(w / 2), fill=color(IVORY))

    # Rastilho (um pouco mais largo que as cordas).
    over = 1.6
    d.rounded_rectangle((px(left - over), px(top), px(left + WIDTH + over), px(nut_bottom)),
                        radius=px(1.2), fill=color(BRASS))

    # Marcas acima do rastilho.
    my = top - MARK_GAP - MARK_SIZE
    line = round(px(0.9))
    for i in OPEN:
        x, r = xs[i], MARK_SIZE
        d.ellipse((px(x - r), px(my - r), px(x + r), px(my + r)), outline=color(IVORY, 220), width=line)
    for i in MUTED:
        x, r = xs[i], MARK_SIZE * 0.85
        d.line((px(x - r), px(my - r), px(x + r), px(my + r)), fill=color(IVORY, 220), width=line)
        d.line((px(x - r), px(my + r), px(x + r), px(my - r)), fill=color(IVORY, 220), width=line)

    # Dedos: contorno da cor do fundo + bolinha na cor da função.
    for string, fret, c in DOTS:
        cx = xs[string]
        cy = nut_bottom + FRET_SPACE * (fret - 0.5)
        r = DOT_RADIUS + DOT_RIM
        if mono:
            d.ellipse((px(cx - r), px(cy - r), px(cx + r), px(cy + r)), fill=(0, 0, 0, 0))
        else:
            d.ellipse((px(cx - r), px(cy - r), px(cx + r), px(cy + r)), fill=rgba(BACKGROUND))
        r = DOT_RADIUS
        d.ellipse((px(cx - r), px(cy - r), px(cx + r), px(cy + r)), fill=color(c))
    return img


def scaled(img, px_size):
    return img.resize((px_size, px_size), Image.LANCZOS)


def main():
    glyph = draw_glyph()
    mono = draw_glyph(mono=True)

    for dens, k in DENSITIES:
        out = os.path.join(RES, f"mipmap-{dens}")
        os.makedirs(out, exist_ok=True)
        side = round(108 * k)
        scaled(glyph, side).save(os.path.join(out, "ic_launcher_foreground.png"), optimize=True)
        scaled(mono, side).save(os.path.join(out, "ic_launcher_monochrome.png"), optimize=True)

    # Ícone com fundo, mostrando só os 72 dp visíveis (o que o launcher exibe).
    full = Image.new("RGBA", glyph.size, rgba(BACKGROUND))
    full.alpha_composite(glyph)
    c = 18 * SS
    visible = full.crop((c, c, 108 * SS - c, 108 * SS - c))

    # Ícones antigos (Android 7): quadrado arredondado e redondo.
    for dens, k in DENSITIES:
        size = int(48 * k)
        img = visible.resize((size, size), Image.LANCZOS)
        for name, round_ in (("ic_launcher", False), ("ic_launcher_round", True)):
            mask = Image.new("L", (size * 4, size * 4), 0)
            dr = ImageDraw.Draw(mask)
            if round_:
                dr.ellipse((0, 0, size * 4 - 1, size * 4 - 1), fill=255)
            else:
                dr.rounded_rectangle((0, 0, size * 4 - 1, size * 4 - 1), radius=size * 4 * 22 // 100, fill=255)
            out = Image.new("RGBA", (size, size), (0, 0, 0, 0))
            out.paste(img, (0, 0), mask.resize((size, size), Image.LANCZOS))
            out.save(os.path.join(RES, f"mipmap-{dens}", f"{name}.webp"), lossless=True)

    # Loja: quadrado cheio de 512 px (a loja aplica os cantos).
    visible.convert("RGB").resize((512, 512), Image.LANCZOS).save(
        os.path.join(ROOT, "app", "src", "main", "ic_launcher-playstore.png"))

    # Marca dentro do app: só o desenho, recortado (fundo transparente).
    box = glyph.getbbox()
    pad = 2 * SS
    side = max(box[2] - box[0], box[3] - box[1]) + 2 * pad
    cx, cy = (box[0] + box[2]) // 2, (box[1] + box[3]) // 2
    mark = glyph.crop((cx - side // 2, cy - side // 2, cx + side // 2, cy + side // 2))
    os.makedirs(os.path.join(RES, "drawable-nodpi"), exist_ok=True)
    scaled(mark, 512).save(os.path.join(RES, "drawable-nodpi", "brand_mark.png"), optimize=True)

    def background_xml(extra="", note="Fundo do ícone."):
        return f"""<?xml version="1.0" encoding="utf-8"?>
<!-- {note} Gerado por tools/icon/make_icon.py. -->
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <path
        android:fillColor="{BACKGROUND}"
        android:pathData="M0,0h108v108h-108z" />{extra}
</vector>
"""

    with open(os.path.join(RES, "drawable", "ic_launcher_background.xml"), "w", encoding="utf-8") as f:
        f.write(background_xml())
    # Versão DEV: o mesmo ícone com uma faixa embaixo, para não confundir com o app normal.
    with open(os.path.join(DEV_RES, "drawable", "ic_launcher_background.xml"), "w", encoding="utf-8") as f:
        f.write(background_xml(f"""
    <path
        android:fillColor="{DOMINANT}"
        android:pathData="M0,84h108v24h-108z" />""", "Ícone da versão DEV: o mesmo desenho, com faixa embaixo."))
    print("ícone gerado")


if __name__ == "__main__":
    main()
