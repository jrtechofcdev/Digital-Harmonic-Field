"""Features do modelo de tom (espelhadas em Kotlin, KeyModel.kt).
Tudo em 'classe de altura absoluta' (0=Dó); o modelo gira para cada tônica candidata."""
import numpy as np, json
GAP_PHRASE=0.30   # respiro: fim de frase
GAP_LINK=0.45     # notas ligadas (para pares melódicos)

def offset_of(segs):
    if not segs: return 0.0
    m=np.array([s[2] for s in segs]); w=np.array([s[3] for s in segs])
    a=2*np.pi*(m-np.round(m))
    return float(np.arctan2((w*np.sin(a)).sum(),(w*np.cos(a)).sum())/(2*np.pi))

def pcs_of(segs, off):
    return [int(np.round(s[2]-off))%12 for s in segs]

def norm(v):
    s=v.sum(); return v/s if s>0 else v

def features(voice, bass, harmony, bchroma=None, bpresent=False):
    """Devolve (vec absoluto de blocos, extras). Blocos de 12 e de 144 (pares)."""
    off=offset_of(voice)
    pv=pcs_of(voice,off)
    dur=np.array([s[1]-s[0] for s in voice]) if voice else np.zeros(0)
    V_hist=np.zeros(12); V_cnt=np.zeros(12); V_last=np.zeros(12); V_end=np.zeros(12); V_long=np.zeros(12)
    V_big=np.zeros((12,12))
    for i,s in enumerate(voice):
        d=s[1]-s[0]; V_hist[pv[i]]+=d; V_cnt[pv[i]]+=1
        gap = voice[i+1][0]-s[1] if i+1<len(voice) else None
        if gap is not None and gap>=GAP_PHRASE: V_end[pv[i]]+=d
        if gap is not None and gap<GAP_LINK and pv[i]!=pv[i+1]: V_big[pv[i],pv[i+1]]+=1
    if voice:
        V_last[pv[-1]]=1
        md=np.median(dur)
        for i,s in enumerate(voice):
            if s[1]-s[0]>=1.6*md: V_long[pv[i]]+=s[1]-s[0]
    boff=offset_of(bass) if bass else 0.0
    pb=pcs_of(bass,boff)
    B_hist=np.zeros(12); B_big=np.zeros((12,12)); B_last=np.zeros(12)
    bsec=0.0
    for i,s in enumerate(bass):
        d=s[1]-s[0]; B_hist[pb[i]]+=d; bsec+=d
        if i+1<len(bass) and bass[i+1][0]-s[1]<0.6 and pb[i]!=pb[i+1]: B_big[pb[i],pb[i+1]]+=1
    if bass: B_last[pb[-1]]=1
    has_bass=1.0 if bsec>=0.5 else 0.0
    H=norm(np.array(harmony,dtype=float))
    blocks12=[norm(V_hist),norm(V_cnt),V_last,norm(V_end),norm(V_long),
              has_bass*norm(B_hist),has_bass*B_last,H,
              (1.0 if bpresent else 0.0)*norm(np.array(bchroma if bchroma is not None else np.zeros(12),dtype=float))]
    blocks144=[norm(V_big.ravel()).reshape(12,12), has_bass*norm(B_big.ravel()).reshape(12,12)]
    vsec=float(dur.sum()) if voice else 0.0
    return blocks12, blocks144, dict(voiced=vsec, bass=bsec, notes=len(voice), offset=off,
                                      distinct=int((norm(V_hist)>0.05).sum()))

def rotated(blocks12, blocks144):
    """Matriz [12 tônicas × D]: features vistas a partir de cada tônica candidata."""
    out=[]
    for k in range(12):
        parts=[np.roll(b,-k) for b in blocks12]
        parts+=[np.roll(np.roll(b,-k,axis=0),-k,axis=1).ravel() for b in blocks144]
        out.append(np.concatenate(parts))
    return np.array(out)
D=12*9+144*2

def load(jsonl, labels_csv):
    lab={}
    for line in open(labels_csv):
        c=line.strip().split(',')
        if len(c)>=3: lab[c[0]]=(int(c[1]),int(c[2]),c[3],c[4])
    X=[];Y=[];T=[];F=[];E=[]
    for line in open(jsonl):
        d=json.loads(line)
        if d['file'] not in lab: continue
        b12,b144,ex=features(d['voice'],d['bass'],d['harmony'],d.get('bchroma'),d.get('bpresent',False))
        X.append(rotated(b12,b144)); t,mi,src,sc=lab[d['file']]
        Y.append(t+12*mi); T.append(d['t']); F.append(d['file']); E.append(ex)
    return np.array(X),np.array(Y),np.array(T),F,E
