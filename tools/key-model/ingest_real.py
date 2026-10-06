"""Lê a pasta treino-real/ enviada pelo app DEV e prepara para a bancada e o treino.

Uso:
    python3 ingest_real.py <pasta treino-real> [--so-certeza]

Gera <pasta>/labels.csv (formato da bancada: arquivo,tônica,menor,origem,cenário,nome)
e imprime um resumo: quanto o app acertou NO CULTO, por rodada, e os erros mais comuns.
Depois:
    ./runbench.sh <pasta> real.csv real_notes.jsonl   # notas pelo detector atual
    python3 score.py real.csv                           # acerto do detector atual
    python3 train.py 16 30 --extra real_notes.jsonl:<pasta>/labels.csv
"""
import json, sys, os
from collections import Counter

NAMES = ["C","C#","D","D#","E","F","F#","G","G#","A","A#","B"]
SCENES = {"a_capela": "acapela", "com_teclado": "teclado", "banda_completa": "banda", "solo_ministro": "solo"}

def main():
    root = sys.argv[1]
    only_sure = "--so-certeza" in sys.argv
    data = json.load(open(os.path.join(root, "sessoes.json")))
    rows, skipped = [], Counter()
    per_round, finals, stars, errors = {}, [], [], Counter()
    for s in data["sessoes"]:
        lab = s.get("rotulo")
        if not lab: skipped["pendente"] += 1; continue
        tom = lab.get("tom") or ""
        if not tom: skipped["sem tom (não sei / não era hino)"] += 1; continue
        if only_sure and lab.get("certeza") != "certeza": skipped["só 'acho que é'"] += 1; continue
        wav = s["arquivo_audio"]
        if not os.path.exists(os.path.join(root, wav)): skipped["sem áudio"] += 1; continue
        minor = tom.endswith("m"); tonic = NAMES.index(tom.rstrip("m"))
        tags = lab.get("tags", [])
        scene = next((SCENES[t] for t in tags if t in SCENES), "real")
        name = f"harpa{lab.get('harpa_numero')}" if lab.get("harpa_numero") else s["id"]
        rows.append(f"{wav},{tonic},{int(minor)},real,{scene},{name}")
        av = s.get("avaliacao", {})
        for i, ok in enumerate(av.get("acertou_rodada", [])): per_round.setdefault(i + 1, []).append(ok)
        if "acertou_final" in av: finals.append(av["acertou_final"])
        if av.get("tipo_erro"): errors[av["tipo_erro"]] += 1
        if lab.get("estrelas"): stars.append(lab["estrelas"])
    open(os.path.join(root, "labels.csv"), "w").write("\n".join(rows) + ("\n" if rows else ""))
    print(f"{len(rows)} hinos rotulados prontos em {root}/labels.csv; pulados: {dict(skipped)}")
    for r, v in sorted(per_round.items()):
        print(f"  {r}ª rodada ({r*5} s): o app acertou {sum(v)/len(v):.0%} (n={len(v)})")
    if finals: print(f"  resultado final: {sum(finals)/len(finals):.0%}")
    if stars: print(f"  nota média: {sum(stars)/len(stars):.1f} estrelas")
    if errors: print("  erros:", dict(errors.most_common()))

if __name__ == "__main__":
    main()
