#!/usr/bin/env python3
"""
Prepara os logos da JR TECH OFC (arte do autor, em tools/brand/source/) para o app:
recorta o conteúdo (sem mexer nos pixels) e reduz para um tamanho leve.

  res/drawable-nodpi/jrtech_vertical.png    símbolo + Jr_Tech_OFC + "Nossa Arte, Seu Sucesso" (fundo #292929)
  res/drawable-nodpi/jrtech_horizontal.png  versão em linha (fundo #292929), usada no Sobre
  res/drawable-nodpi/jrtech_simbolo.png     só o símbolo (fundo transparente)

Uso (na raiz do repositório):  python3 tools/brand/make_brand.py
"""
import os

from PIL import Image

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
SRC = os.path.join(os.path.dirname(__file__), "source")
OUT = os.path.join(ROOT, "app", "src", "main", "res", "drawable-nodpi")


def crop_on_dark(name, out, width, pad=0.08):
    """Recorta o que não é o fundo escuro, com uma margem, mantendo o fundo da arte."""
    im = Image.open(os.path.join(SRC, name)).convert("RGB")
    box = im.convert("L").point(lambda v: 255 if v > 60 else 0).getbbox()
    w, h = box[2] - box[0], box[3] - box[1]
    m = int(max(w, h) * pad)
    im = im.crop((box[0] - m, box[1] - m, box[2] + m, box[3] + m))
    im = im.resize((width, round(im.height * width / im.width)), Image.LANCZOS)
    im.save(os.path.join(OUT, out), optimize=True)
    return im.size


def crop_alpha(name, out, width):
    im = Image.open(os.path.join(SRC, name)).convert("RGBA")
    im = im.crop(im.getchannel("A").point(lambda v: 255 if v > 8 else 0).getbbox())
    im = im.resize((width, round(im.height * width / im.width)), Image.LANCZOS)
    im.save(os.path.join(OUT, out), optimize=True)
    return im.size


if __name__ == "__main__":
    os.makedirs(OUT, exist_ok=True)
    print("vertical", crop_on_dark("jrtech-vertical.png", "jrtech_vertical.png", 760))
    print("horizontal", crop_on_dark("jrtech-horizontal.png", "jrtech_horizontal.png", 900, pad=0.06))
    print("simbolo", crop_alpha("jrtech-simbolo.png", "jrtech_simbolo.png", 360))
