#!/usr/bin/env python3
"""
Desenha o ícone do app e gera todos os formatos de uma vez.

O desenho (logo minimalista):
  - um anel fino = os 12 tons, o círculo das quintas;
  - três pontos no topo do anel = os três vizinhos que formam o coração de
    qualquer campo harmônico: Tônica no meio (maior, é a "casa"), Subdominante
    à esquerda e Dominante à direita, nas mesmas cores que o app usa para as
    funções dos acordes.
Lê-se como "o tom e os seus vizinhos": é exatamente o que o app mostra.

Tudo é geometria (nada de imagem de fora): fica nítido em qualquer tamanho e
qualquer ajuste é feito mudando os números abaixo.

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
import math
import os

from PIL import Image, ImageDraw

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
RES = os.path.join(ROOT, "app", "src", "main", "res")
DEV_RES = os.path.join(ROOT, "app", "src", "dev", "res")

# --- Cores (as mesmas de ui/theme/Color.kt) ---------------------------------
BACKGROUND = "#0C0D10"   # fundo do ícone
RING = "#474E5B"         # o anel (os 12 tons)
TONIC = "#57B892"        # FuncTonic
SUBDOMINANT = "#6B97DF"  # FuncSubdominant
DOMINANT = "#E07468"     # FuncDominant

# --- Geometria, em dp, na camada de 108 dp (zona segura: círculo de raio 33) --
RING_RADIUS = 24.0
RING_WIDTH = 1.9
STEP_DEG = 30.0          # um tom para o outro no círculo das quintas (360 / 12)
TONIC_RADIUS = 5.0       # a Tônica é a maior: é a "casa"
SIDE_RADIUS = 4.0        # Subdominante e Dominante
GAP = 1.8                # folga entre o ponto lateral e a ponta do anel
# O desenho tem mais peso em cima; descer o centro deixa o conjunto equilibrado.
CENTER_X = 54.0
CENTER_Y = 54.0 + (TONIC_RADIUS - RING_WIDTH / 2) / 2

SS = 40                  # pixels por dp no desenho mestre (bem grande, depois reduz)
DENSITIES = (("mdpi", 1), ("hdpi", 1.5), ("xhdpi", 2), ("xxhdpi", 3), ("xxxhdpi", 4))


def rgba(hex_color, alpha=255):
    h = hex_color.lstrip("#")
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4)) + (alpha,)


def px(v):
    return v * SS


def circle(d, x, y, r, fill):
    d.ellipse((px(x - r), px(y - r), px(x + r), px(y + r)), fill=fill)


def draw_glyph(mono=False):
    """Camada de 108 dp com o desenho, em alta resolução e fundo transparente."""
    size = 108 * SS
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    color = (lambda c, a=255: (255, 255, 255, a)) if mono else rgba

    # O anel: um arco que vai da Dominante, por baixo, até a Subdominante (o topo
    # fica livre para os três pontos, sem sobras de linha entre eles).
    r, w = RING_RADIUS, RING_WIDTH
    open_deg = math.degrees((SIDE_RADIUS + GAP) / r)
    d.arc((px(CENTER_X - r - w / 2), px(CENTER_Y - r - w / 2), px(CENTER_X + r + w / 2), px(CENTER_Y + r + w / 2)),
          start=-90 + STEP_DEG + open_deg, end=270 - STEP_DEG - open_deg,
          fill=color(RING, 150 if mono else 255), width=round(px(w)))
    for ang in (-90 + STEP_DEG + open_deg, 270 - STEP_DEG - open_deg):   # pontas arredondadas
        t = math.radians(ang)
        circle(d, CENTER_X + r * math.cos(t), CENTER_Y + r * math.sin(t), w / 2, color(RING, 150 if mono else 255))

    # Os três pontos: Subdominante (um passo para a esquerda), Tônica (topo), Dominante (direita).
    dots = ((-1, SIDE_RADIUS, SUBDOMINANT), (0, TONIC_RADIUS, TONIC), (1, SIDE_RADIUS, DOMINANT))
    def where(step):
        a = math.radians(-90 + STEP_DEG * step)
        return CENTER_X + r * math.cos(a), CENTER_Y + r * math.sin(a)

    for step, radius, c in dots:
        circle(d, *where(step), radius, color(c))
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
    pad = SS
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
        android:pathData="M0,86h108v22h-108z" />""", "Ícone da versão DEV: o mesmo desenho, com faixa embaixo."))
    print("ícone gerado")


if __name__ == "__main__":
    main()
