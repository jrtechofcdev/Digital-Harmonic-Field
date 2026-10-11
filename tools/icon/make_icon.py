#!/usr/bin/env python3
"""
Gera o ícone do app a partir da arte do autor (tools/icon/source/icone-original.png):
braço de violão com as notas da Tônica (verde), Subdominante (azul) e
Dominante (vermelho), dentro de um anel em latão.

A arte original vem como "mockup" (um quadrado arredondado sobre fundo preto).
O script recorta só o desenho, centraliza na zona segura do ícone adaptativo e
gera tudo de uma vez, para os arquivos nunca ficarem diferentes entre si:

  res/mipmap-*/ic_launcher_foreground.png   desenho (ícone adaptativo, Android 8+)
  res/mipmap-*/ic_launcher_monochrome.png   só as partes claras (ícone temático, Android 13+)
  res/mipmap-*/ic_launcher(_round).webp     ícone antigo (Android 7)
  res/drawable/ic_launcher_background.xml   cor de fundo (a mesma da arte)
  res/drawable-nodpi/brand_mark.png         marca usada dentro do app (fundo transparente)
  app/src/main/ic_launcher-playstore.png    512 px para a loja

Uso (na raiz do repositório):  pip install pillow && python3 tools/icon/make_icon.py
Para trocar o ícone: substitua tools/icon/source/icone-original.png e rode de novo.
"""
import os

from PIL import Image, ImageChops, ImageDraw, ImageFilter

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
RES = os.path.join(ROOT, "app", "src", "main", "res")
SOURCE = os.path.join(os.path.dirname(__file__), "source", "icone-original.png")

# Quanto o desenho pode se afastar do centro, em dp, na camada de 108 dp.
# A zona segura (que nenhuma máscara corta) é um círculo de raio 33 dp.
SAFE_RADIUS_DP = 33.0

DENSITIES = (("mdpi", 1), ("hdpi", 1.5), ("xhdpi", 2), ("xxhdpi", 3), ("xxxhdpi", 4))


def tile_background(img):
    """Cor do fundo do quadrado da arte (mediana de pontos sem desenho)."""
    w, h = img.size
    pts = [(int(w * x), int(h * y)) for x in (0.17, 0.2, 0.8, 0.83) for y in (0.2, 0.5, 0.8)]
    px = sorted(img.getpixel(p) for p in pts)
    return px[len(px) // 2]


def bright_mask(img):
    """Partes claras do desenho (cordas, trastes, notas, anel, rastilho)."""
    return img.convert("L").point(lambda v: 255 if v > 60 else 0).filter(ImageFilter.MedianFilter(3))


def inside_tile(img):
    """Recorte do interior do quadrado (sem a borda e o fundo preto do mockup)."""
    w, h = img.size
    m = int(w * 0.135)          # a borda do quadrado fica em ~11% da arte
    return img.crop((m, m, w - m, h - m))


def main():
    src = Image.open(SOURCE).convert("RGB")
    tile = inside_tile(src)
    bg = tile_background(tile)

    # Onde está o desenho: caixa das partes claras + a borda escura do braço.
    bright = bright_mask(tile)
    box = bright.getbbox()
    cx, cy = (box[0] + box[2]) / 2, (box[1] + box[3]) / 2
    a = bright.load()
    far = 0.0
    for y in range(box[1], box[3], 2):
        for x in range(box[0], box[2], 2):
            if a[x, y]:
                far = max(far, ((x - cx) ** 2 + (y - cy) ** 2) ** 0.5)
    far += 10                                   # a madeira passa um pouco das cordas de fora
    dp_per_px = SAFE_RADIUS_DP / far

    # A arte fica INTACTA dentro de um círculo com borda suave; fora dele vale a
    # cor de fundo (a mesma da arte), então não aparece emenda.
    r = far + 24
    side = int(2 * r)
    box = (int(cx - r), int(cy - r), int(cx - r) + side, int(cy - r) + side)
    disc = Image.new("L", (side * 2, side * 2), 0)
    ImageDraw.Draw(disc).ellipse((24, 24, side * 2 - 24, side * 2 - 24), fill=255)
    disc = disc.resize((side, side), Image.LANCZOS).filter(ImageFilter.GaussianBlur(6))
    canvas = Image.new("RGB", (tile.width + 2 * side, tile.height + 2 * side), bg)
    canvas.paste(tile, (side, side))
    glyph = canvas.crop((box[0] + side, box[1] + side, box[2] + side, box[3] + side)).convert("RGBA")
    glyph.putalpha(disc)
    cx, cy = box[0] + side / 2, box[1] + side / 2

    def layer(px_per_dp, img):
        """Camada de 108 dp com o desenho centralizado."""
        size = int(round(108 * px_per_dp))
        s = dp_per_px * px_per_dp
        g = img.resize((max(1, round(img.width * s)), max(1, round(img.height * s))), Image.LANCZOS)
        out = Image.new("RGBA", (size, size), (0, 0, 0, 0))
        ox = round(size / 2 - (cx - box[0]) * s)
        oy = round(size / 2 - (cy - box[1]) * s)
        out.alpha_composite(g, (ox, oy))
        return out

    # Partes claras (cordas, trastes, notas, anel) para o ícone temático.
    luma = glyph.convert("L")
    mono_alpha = ImageChops.multiply(luma.point(lambda v: 0 if v < 45 else 255 if v > 110 else int((v - 45) * 255 / 65)),
                                     glyph.getchannel("A"))
    mono = Image.new("RGBA", glyph.size, (255, 255, 255, 0))
    mono.putalpha(mono_alpha)

    hexbg = "#%02X%02X%02X" % bg
    for dens, k in DENSITIES:
        d = os.path.join(RES, f"mipmap-{dens}")
        layer(k, glyph).save(os.path.join(d, "ic_launcher_foreground.png"), optimize=True)
        layer(k, mono).save(os.path.join(d, "ic_launcher_monochrome.png"), optimize=True)

    # Ícones antigos (Android 7): os 72 dp visíveis, com máscara.
    big = Image.new("RGBA", (1296, 1296), bg + (255,))
    big.alpha_composite(layer(12, glyph))
    c = 1296 * 18 // 108
    visible = big.crop((c, c, 1296 - c, 1296 - c))
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

    # Marca dentro do app: a arte no círculo (o fundo dela é o mesmo do app).
    os.makedirs(os.path.join(RES, "drawable-nodpi"), exist_ok=True)
    glyph.resize((512, 512), Image.LANCZOS).save(os.path.join(RES, "drawable-nodpi", "brand_mark.png"), optimize=True)

    # Cor de fundo do ícone adaptativo = fundo da arte (sem emenda).
    with open(os.path.join(RES, "drawable", "ic_launcher_background.xml"), "w", encoding="utf-8") as f:
        f.write(f"""<?xml version="1.0" encoding="utf-8"?>
<!-- Fundo do ícone: a mesma cor do fundo da arte. Gerado por tools/icon/make_icon.py. -->
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <path
        android:fillColor="{hexbg}"
        android:pathData="M0,0h108v108h-108z" />
</vector>
""")
    print(f"ícone gerado · fundo {hexbg} · desenho {glyph.width}x{glyph.height}px · {dp_per_px * 1000:.1f} dp por 1000 px")


if __name__ == "__main__":
    main()
