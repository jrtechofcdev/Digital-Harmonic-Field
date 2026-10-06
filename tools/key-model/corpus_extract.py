"""Extrai hinos/melodias do corpus music21 para JSON simbólico com o tom verdadeiro."""
import json, glob, os, sys, warnings
warnings.filterwarnings("ignore")
from music21 import corpus, converter, key as m21key, harmony, note, chord
OUT=[]
def notes_of(part):
    ev=[]
    for n in part.flatten().notesAndRests:
        if isinstance(n, harmony.ChordSymbol): continue
        if isinstance(n, note.Note) and not n.duration.isGrace:
            if n.tie is not None and n.tie.type in ('stop','continue'):
                # junta com a anterior
                if ev and abs(ev[-1][0]+ev[-1][1]-float(n.offset))<1e-6 and ev[-1][2]==n.pitch.midi:
                    ev[-1][1]+=float(n.quarterLength); continue
            ev.append([float(n.offset), float(n.quarterLength), n.pitch.midi])
    return ev

def label_from_ks(s, final_bass_pc=None):
    ks=s.flatten().getElementsByClass('KeySignature')
    if not ks: return None
    k=ks[0]
    sharps=k.sharps
    maj=(sharps*7)%12
    mino=(maj+9)%12
    if isinstance(k, m21key.Key) and k.mode in ('major','minor') and final_bass_pc is None:
        return (k.tonic.pitchClass, k.mode=='minor')
    if final_bass_pc==maj: return (maj, False)
    if final_bass_pc==mino: return (mino, True)
    return None

# --- Corais de Bach (hinos luteranos a 4 vozes) ---
n_ok=n_skip=0
for p in corpus.getComposer('bach'):
    p=str(p)
    if not p.endswith('.mxl') or 'bwv' not in p: continue
    try:
        s=converter.parse(p)
        parts=s.parts
        if len(parts)!=4: n_skip+=1; continue
        P=[notes_of(pt) for pt in parts]
        if any(len(x)<8 for x in P): n_skip+=1; continue
        bass_last=P[3][-1][2]%12
        lab=label_from_ks(s, bass_last)
        if lab is None: n_skip+=1; continue
        # o soprano também deve terminar na tônica ou 3ª/5ª (sanidade)
        OUT.append(dict(src='coral', name=p.split('/')[-1], tonic=lab[0], minor=lab[1],
                        parts=P, chords=None))
        n_ok+=1
    except Exception as e:
        n_skip+=1
print('corais',n_ok,'pulados',n_skip, file=sys.stderr)

# --- Essen (melodias folclóricas alemãs, muitas viraram hinos) ---
n_ok=n_skip=0
import music21
ESSEN=os.path.join(os.path.dirname(music21.__file__),'corpus','essenFolksong')
for f in sorted(glob.glob(os.path.join(ESSEN,'*.abc'))):
    try: op=converter.parse(f)
    except Exception: continue
    scores=op.scores if hasattr(op,'scores') else [op]
    for s in scores:
        try:
            ks=s.flatten().getElementsByClass('KeySignature')
            if not ks or not isinstance(ks[0], m21key.Key) or ks[0].mode not in ('major','minor'): n_skip+=1; continue
            P=notes_of(s.parts[0] if s.parts else s)
            if len(P)<16: n_skip+=1; continue
            k=ks[0]
            # o final da melodia precisa bater com a tônica anotada (descarta modais mal rotulados)
            if P[-1][2]%12 != k.tonic.pitchClass: n_skip+=1; continue
            OUT.append(dict(src='essen', name=f.split('/')[-1]+':'+str(s.metadata.number if s.metadata else ''),
                            tonic=k.tonic.pitchClass, minor=k.mode=='minor', parts=[P], chords=None))
            n_ok+=1
        except Exception: n_skip+=1
print('essen',n_ok,'pulados',n_skip, file=sys.stderr)
json.dump(OUT, open('corpus.json','w'))
from collections import Counter
print(Counter((o['src'],o['minor']) for o in OUT), file=sys.stderr)
