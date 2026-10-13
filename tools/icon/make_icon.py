#!/usr/bin/env python3
"""
Gera o ícone do app a partir do logo do autor (tools/icon/source/logo-dhf.png):
a palheta iridescente com "DHF" (Digital Harmonic Field), sobre fundo preto.

A arte NÃO é redesenhada. O script só:
  1. acha a palheta na imagem e o centro que melhor a encaixa num círculo;
  2. ajusta o tamanho para ela caber inteira na zona segura do ícone adaptativo
     (círculo de 33 dp: nenhum formato de ícone do Android corta ali dentro);
  3. transforma o preto do fundo em transparência ("o brilho vira a cor"), para
     o logo ficar idêntico sobre qualquer fundo escuro, sem quadrado em volta;
  4. gera todos os arquivos de uma vez, para nunca ficarem diferentes entre si.

Arquivos gerados:
  res/mipmap-*/ic_launcher_foreground.png   logo (ícone adaptativo, Android 8+)
  res/mipmap-*/ic_launcher_monochrome.png   só as partes claras (ícone temático, Android 13+)
  res/mipmap-*/ic_launcher(_round).webp     ícone antigo (Android 7)
  res/drawable/ic_launcher_background.xml   cor de fundo (a mesma da arte)
  res/drawable-nodpi/brand_mark.png         marca usada dentro do app (fundo transparente)
  app/src/main/ic_launcher-playstore.png    512 px para a loja
  app/src/dev/res/drawable/ic_launcher_background.xml   fundo da versão DEV (com faixa)

Ícone CLARO (o quadro final da abertura: palheta de vidro sobre azul e bege),
de tools/icon/source/logo-dhf-claro.png. O usuário escolhe nos Ajustes:
  res/mipmap-*/ic_launcher_light_foreground.png, ic_launcher_light(_round).webp
  res/mipmap-anydpi-v26/ic_launcher_light(_round).xml
  res/drawable-nodpi/brand_mark_light.png   marca dentro do app no tema claro

Uso (na raiz do repositório):  pip install pillow && python3 tools/icon/make_icon.py
Para trocar o logo: substitua as imagens em tools/icon/source/ e rode de novo.
"""
import os

from PIL import Image, ImageChops, ImageDraw, ImageFilter

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
RES = os.path.join(ROOT, "app", "src", "main", "res")
DEV_RES = os.path.join(ROOT, "app", "src", "dev", "res")
SOURCE = os.path.join(os.path.dirname(__file__), "source", "logo-dhf.png")
SOURCE_LIGHT = os.path.join(os.path.dirname(__file__), "source", "logo-dhf-claro.png")

FIT_RADIUS_DP = 32.0     # o logo fica 1 dp para dentro da zona segura (33 dp)
LAYER_PX = 1296          # camada de 108 dp desenhada a 12 px por dp, depois reduzida
DEV_STRIPE = "#E07468"   # faixa da versão DEV (FuncDominant)
DENSITIES = (("mdpi", 1), ("hdpi", 1.5), ("xhdpi", 2), ("xxhdpi", 3), ("xxxhdpi", 4))


def background_color(img):
    """Cor do fundo da arte: mediana de pontos perto das bordas."""
    w, h = img.size
    pts = [(int(w * x), int(h * y)) for x in (0.02, 0.5, 0.98) for y in (0.02, 0.98)]
    px = sorted(img.getpixel(p) for p in pts)
    return px[len(px) // 2]


def logo_points(img, threshold=30):
    """Contorno das partes visíveis (para achar o círculo que envolve o logo)."""
    mask = img.convert("L").point(lambda v: 255 if v > threshold else 0)
    w, h = mask.size
    a = mask.load()
    pts = []
    for y in range(0, h, 2):
        row = [x for x in range(0, w, 2) if a[x, y]]
        if row:
            pts += [(row[0], y), (row[-1], y)]
    for x in range(0, w, 2):
        col = [y for y in range(0, h, 2) if a[x, y]]
        if col:
            pts += [(x, col[0]), (x, col[-1])]
    return pts


def enclosing_circle(pts):
    """Centro e raio do menor círculo (aproximado) que contém todos os pontos."""
    xs, ys = [p[0] for p in pts], [p[1] for p in pts]
    cx0, cy0 = (min(xs) + max(xs)) / 2, (min(ys) + max(ys)) / 2
    best = None
    for step in (16, 4, 1):
        for dx in range(-6, 7):
            for dy in range(-6, 7):
                cx, cy = cx0 + dx * step, cy0 + dy * step
                r = max((x - cx) ** 2 + (y - cy) ** 2 for x, y in pts) ** 0.5
                if best is None or r < best[2]:
                    best = (cx, cy, r)
        cx0, cy0 = best[0], best[1]
    return best


def glow_to_alpha(img, bg):
    """
    Fundo preto vira transparente: alfa = o canal mais claro (o "brilho") e a cor
    é a original dividida pelo alfa. Sobre fundo preto o resultado é idêntico à
    arte; sobre outros fundos escuros não aparece quadrado nem borda.
    """
    chans = [ImageChops.subtract(ch, Image.new("L", img.size, v)) for ch, v in zip(img.split(), bg)]
    alpha = ImageChops.lighter(ImageChops.lighter(chans[0], chans[1]), chans[2])
    alpha = alpha.point(lambda v: 0 if v < 4 else min(255, int((v - 4) * 255 / 251)))
    rp, gp, bp = (c.load() for c in chans)
    ap = alpha.load()
    out = Image.new("RGBA", img.size)
    op = out.load()
    w, h = img.size
    for y in range(h):
        for x in range(w):
            al = ap[x, y]
            if al:
                k = 255.0 / al
                op[x, y] = (min(255, int(rp[x, y] * k)), min(255, int(gp[x, y] * k)), min(255, int(bp[x, y] * k)), al)
            else:
                op[x, y] = (0, 0, 0, 0)
    return out


def legacy_icons(visible, prefix):
    """Ícones antigos (Android 7): quadrado arredondado e redondo, de 48 dp."""
    for dens, k in DENSITIES:
        size = int(48 * k)
        img = visible.resize((size, size), Image.LANCZOS)
        for suffix, round_ in (("", False), ("_round", True)):
            mask = Image.new("L", (size * 4, size * 4), 0)
            dr = ImageDraw.Draw(mask)
            if round_:
                dr.ellipse((0, 0, size * 4 - 1, size * 4 - 1), fill=255)
            else:
                dr.rounded_rectangle((0, 0, size * 4 - 1, size * 4 - 1), radius=size * 4 * 22 // 100, fill=255)
            out = Image.new("RGBA", (size, size), (0, 0, 0, 0))
            out.paste(img, (0, 0), mask.resize((size, size), Image.LANCZOS))
            out.save(os.path.join(RES, f"mipmap-{dens}", f"{prefix}{suffix}.webp"), lossless=True)


def rounded_tile(visible, size):
    """O ícone como aparece no celular (quadrado arredondado), para mostrar dentro do app."""
    tile = visible.resize((size, size), Image.LANCZOS)
    m = Image.new("L", (size * 4, size * 4), 0)
    ImageDraw.Draw(m).rounded_rectangle((0, 0, size * 4 - 1, size * 4 - 1), radius=size * 4 * 23 // 100, fill=255)
    out = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    out.paste(tile, (0, 0), m.resize((size, size), Image.LANCZOS))
    return out


def make_light():
    """
    Ícone claro: o quadro final da abertura. A palheta fica inteira na zona
    segura; o quadro é estreito para isso, então as laterais são completadas
    com o próprio fundo, ampliado e desfocado (emenda suave, sem corte).
    """
    src = Image.open(SOURCE_LIGHT).convert("RGB")
    bright = src.convert("L").point(lambda v: 255 if v > 205 else 0)
    cx, cy, radius = enclosing_circle(logo_points(src.convert("L").point(lambda v: v if v > 205 else 0).convert("RGB")))
    half = int((radius + 10) * 108 / (2 * FIT_RADIUS_DP))
    side = 2 * half
    # Fundo estendido: o quadro ampliado para cobrir o quadrado e bem desfocado.
    k = side / min(src.width, src.height) * 1.15
    big = src.resize((int(src.width * k), int(src.height * k)), Image.LANCZOS).filter(ImageFilter.GaussianBlur(40))
    bx, by = int(cx * k), int(cy * k)
    layer = big.crop((bx - half, by - half, bx + half, by + half))
    # O quadro original por cima, com as bordas esfumadas POR DENTRO (sem puxar preto de fora).
    feather = 48
    mask = Image.new("L", src.size, 0)
    ImageDraw.Draw(mask).rectangle((feather, feather, src.width - feather, src.height - feather), fill=255)
    mask = mask.filter(ImageFilter.GaussianBlur(feather / 2))
    layer.paste(src, (half - int(cx), half - int(cy)), mask)
    layer = layer.resize((LAYER_PX, LAYER_PX), Image.LANCZOS).convert("RGBA")

    for dens, kk in DENSITIES:
        layer.resize((round(108 * kk), round(108 * kk)), Image.LANCZOS).save(
            os.path.join(RES, f"mipmap-{dens}", "ic_launcher_light_foreground.png"), optimize=True)
    c = LAYER_PX * 18 // 108
    visible = layer.crop((c, c, LAYER_PX - c, LAYER_PX - c))
    legacy_icons(visible, "ic_launcher_light")

    # Marca no tema claro e miniatura para os Ajustes: o ícone em quadrado arredondado.
    rounded_tile(visible, 512).save(os.path.join(RES, "drawable-nodpi", "brand_mark_light.png"), optimize=True)
    rounded_tile(visible, 192).save(os.path.join(RES, "drawable-nodpi", "icon_preview_light.png"), optimize=True)

    # Cor de fundo do ícone adaptativo claro (a média do quadro, só aparece no "parallax").
    avg = src.resize((1, 1), Image.LANCZOS).getpixel((0, 0))
    hexbg = "#%02X%02X%02X" % avg
    with open(os.path.join(RES, "values", "ic_launcher_light.xml"), "w", encoding="utf-8") as f:
        f.write(f"""<?xml version="1.0" encoding="utf-8"?>
<!-- Gerado por tools/icon/make_icon.py: fundo do ícone claro. -->
<resources>
    <color name="ic_launcher_light_background">{hexbg}</color>
</resources>
""")
    os.makedirs(os.path.join(RES, "mipmap-anydpi-v26"), exist_ok=True)
    for name in ("ic_launcher_light", "ic_launcher_light_round"):
        with open(os.path.join(RES, "mipmap-anydpi-v26", f"{name}.xml"), "w", encoding="utf-8") as f:
            f.write("""<?xml version="1.0" encoding="utf-8"?>
<!-- Ícone claro (quadro final da abertura). Gerado por tools/icon/make_icon.py. -->
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@color/ic_launcher_light_background" />
    <foreground android:drawable="@mipmap/ic_launcher_light_foreground" />
    <monochrome android:drawable="@mipmap/ic_launcher_monochrome" />
</adaptive-icon>
""")
    print(f"ícone claro gerado: fundo {hexbg}")


def main():
    src = Image.open(SOURCE).convert("RGB")
    bg = background_color(src)
    cx, cy, radius = enclosing_circle(logo_points(src))
    radius += 6                                    # inclui o brilho fraquinho da borda

    # Recorte quadrado (camada de 108 dp) em volta do logo, já na escala certa.
    half = int(radius * 108 / (2 * FIT_RADIUS_DP)) + 2      # meia camada, em px da arte
    canvas = Image.new("RGB", (src.width + 4 * half, src.height + 4 * half), bg)
    canvas.paste(src, (2 * half, 2 * half))
    ox, oy = int(cx) + 2 * half, int(cy) + 2 * half
    layer_src = canvas.crop((ox - half, oy - half, ox + half, oy + half)).resize((LAYER_PX, LAYER_PX), Image.LANCZOS)
    glyph = glow_to_alpha(layer_src, bg)

    # Ícone temático (uma cor): só as partes claras (borda da palheta e as letras).
    luma = layer_src.convert("L")
    mono_alpha = luma.point(lambda v: 0 if v < 70 else 255 if v > 170 else int((v - 70) * 255 / 100))
    mono = Image.new("RGBA", glyph.size, (255, 255, 255, 0))
    mono.putalpha(mono_alpha)

    def scaled(img, side):
        return img.resize((side, side), Image.LANCZOS)

    for dens, k in DENSITIES:
        out = os.path.join(RES, f"mipmap-{dens}")
        os.makedirs(out, exist_ok=True)
        side = round(108 * k)
        scaled(glyph, side).save(os.path.join(out, "ic_launcher_foreground.png"), optimize=True)
        scaled(mono, side).save(os.path.join(out, "ic_launcher_monochrome.png"), optimize=True)

    # Ícone com fundo, mostrando só os 72 dp visíveis (o que o launcher exibe).
    full = Image.new("RGBA", glyph.size, bg + (255,))
    full.alpha_composite(glyph)
    c = LAYER_PX * 18 // 108
    visible = full.crop((c, c, LAYER_PX - c, LAYER_PX - c))

    legacy_icons(visible, "ic_launcher")
    rounded_tile(visible, 192).save(os.path.join(RES, "drawable-nodpi", "icon_preview_dark.png"), optimize=True)

    # Loja: quadrado cheio de 512 px (a loja aplica os cantos).
    visible.convert("RGB").resize((512, 512), Image.LANCZOS).save(
        os.path.join(ROOT, "app", "src", "main", "ic_launcher-playstore.png"))

    # Marca dentro do app: só o logo, recortado, com fundo transparente.
    box = glyph.getchannel("A").point(lambda v: 255 if v > 24 else 0).getbbox()
    side = max(box[2] - box[0], box[3] - box[1]) + 24
    mx, my = (box[0] + box[2]) // 2, (box[1] + box[3]) // 2
    mark = glyph.crop((mx - side // 2, my - side // 2, mx + side // 2, my + side // 2))
    os.makedirs(os.path.join(RES, "drawable-nodpi"), exist_ok=True)
    scaled(mark, 512).save(os.path.join(RES, "drawable-nodpi", "brand_mark.png"), optimize=True)

    hexbg = "#%02X%02X%02X" % bg

    def background_xml(extra="", note="Fundo do ícone: a mesma cor do fundo da arte."):
        return f"""<?xml version="1.0" encoding="utf-8"?>
<!-- {note} Gerado por tools/icon/make_icon.py. -->
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <path
        android:fillColor="{hexbg}"
        android:pathData="M0,0h108v108h-108z" />{extra}
</vector>
"""

    with open(os.path.join(RES, "drawable", "ic_launcher_background.xml"), "w", encoding="utf-8") as f:
        f.write(background_xml())
    # Versão DEV: o mesmo logo com uma faixa embaixo, para não confundir com o app normal.
    with open(os.path.join(DEV_RES, "drawable", "ic_launcher_background.xml"), "w", encoding="utf-8") as f:
        f.write(background_xml(f"""
    <path
        android:fillColor="{DEV_STRIPE}"
        android:pathData="M0,88h108v20h-108z" />""", "Ícone da versão DEV: o mesmo logo, com faixa embaixo."))
    print(f"ícone gerado: fundo {hexbg}, logo com raio de {radius:.0f} px na arte")
    make_light()


if __name__ == "__main__":
    main()
