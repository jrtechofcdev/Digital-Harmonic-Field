"""Renderiza trechos de hinos (corpus.json) como áudio de culto: congregação, teclado,
baixo, reverberação de templo, burburinho, zumbido e microfone de celular."""
import json, sys, os, wave, numpy as np
from scipy.signal import lfilter
SR=44100
def mtof(m): return 440.0*2**((m-69)/12)

MAJ_CH={0:[0,4,7],2:[2,5,9],5:[5,9,0],7:[7,11,2],9:[9,0,4]}
MIN_CH={0:[0,3,7],3:[3,7,10],5:[5,8,0],7:[7,11,2],8:[8,0,3],10:[10,2,5]}
PREF_MAJ={0:1.0,5:0.8,7:0.9,9:0.5,2:0.4}
PREF_MIN={0:1.0,5:0.8,7:0.9,8:0.5,3:0.5,10:0.3}

def harmonize(mel, tonic, minor, win=2.0):
    """Teclado de igreja: escolhe I/IV/V/vi (ou i/iv/V/VI/III) por janela de 2 tempos."""
    chords=MIN_CH if minor else MAJ_CH; pref=PREF_MIN if minor else PREF_MAJ
    end=max(o+d for o,d,m in mel); out=[]; t=0.0; prev=0
    while t<end:
        w=np.zeros(12)
        for o,d,m in mel:
            ov=min(o+d,t+win)-max(o,t)
            if ov>0: w[(m-tonic)%12]+=ov
        best=None
        for r,ch in chords.items():
            s=sum(w[c] for c in ch)-0.5*sum(w[c] for c in range(12) if c not in ch)+0.3*pref[r]
            if r==prev: s+=0.1
            if best is None or s>best[0]: best=(s,r)
        r=best[1]; prev=r
        out.append((t,win,[(tonic+c)%12 for c in chords[r]],(tonic+r)%12)); t+=win
    return out

def vowel_gain(f, kind):
    F={'a':[(700,130,1.0),(1200,120,0.5),(2600,160,0.25)],'o':[(450,100,1.0),(800,100,0.6),(2500,150,0.15)],
       'e':[(400,100,1.0),(2000,150,0.4),(2700,170,0.25)]}[kind]
    g=0.02
    for fc,bw,a in F: g+=a/(1+((f-fc)/bw)**2)
    return g

def sing(out, notes, rng, tempo, detune, male_oct, amp, vowel, t0):
    """Um cantor: vibrato, 'escorregada' no ataque, formantes de vogal, atraso humano."""
    vib_r=rng.uniform(4.8,6.2); vib_d=rng.uniform(0.002,0.007); lag=rng.normal(0,0.03)
    for o,d,m in notes:
        st=o*tempo-t0+lag; du=d*tempo*0.93
        if st+du<0 or st>len(out)/SR: continue
        i0=int(max(0,st)*SR); i1=min(len(out),int((st+du)*SR))
        if i1-i0<200: continue
        tt=(np.arange(i0,i1)/SR)-st
        cents=detune+rng.normal(0,4)-60*np.exp(-tt/0.05)*rng.uniform(0,1)
        f0=mtof(m+male_oct)*2**(cents/1200)*(1+vib_d*np.sin(2*np.pi*vib_r*tt+rng.uniform(0,6)))
        ph=2*np.pi*np.cumsum(f0)/SR
        env=np.minimum(1,tt/0.05)*np.minimum(1,np.maximum(0,du-tt)/0.08)
        base=mtof(m+male_oct); z=np.exp(1j*ph); zk=z.copy(); acc=np.zeros(i1-i0,dtype=complex)
        for k in range(1,14):
            if base*k>7000: break
            acc+=(vowel_gain(base*k,vowel)/k**0.6)*zk; zk*=z
        s=acc.imag
        out[i0:i1]+=amp*env*s*(1+0.1*rng.normal())

def keys_note(out, m, st, du, amp, organ, rng):
    i0=int(max(0,st)*SR); i1=min(len(out),int((st+du+ (0.05 if organ else 0.6))*SR))
    if i1<=i0: return
    tt=np.arange(i0,i1)/SR-st; f=mtof(m)*(1+rng.normal(0,0.0003))
    if organ: env=np.minimum(1,tt/0.03)*np.clip((du-tt)/0.05+1,0,1)
    else: env=np.exp(-tt*rng.uniform(1.2,2.5))*np.minimum(1,tt/0.005)
    s=sum((0.7**k if organ else 1/(k+1)**1.3)*np.sin(2*np.pi*f*(k+1)*tt) for k in range(8 if organ else 7) if f*(k+1)<8000)
    out[i0:i1]+=amp*env*s

def bass_note(out, m, st, du, amp):
    i0=int(max(0,st)*SR); i1=min(len(out),int((st+du)*SR))
    if i1<=i0: return
    tt=np.arange(i0,i1)/SR-st; f=mtof(m)
    env=np.exp(-tt*0.9)*np.minimum(1,tt/0.01)*np.minimum(1,np.maximum(0,du-tt)/0.03)
    out[i0:i1]+=amp*env*sum((0.55**k)*np.sin(2*np.pi*f*(k+1)*tt) for k in range(5))

def reverb(x, rng, rt60, wet):
    n=int(rt60*SR); t=np.arange(n)/SR
    ir=rng.normal(0,1,n)*np.exp(-6.9*t/rt60); ir[:int(0.02*SR)]=0
    ir/=np.sqrt((ir**2).sum())
    L=1<<int(np.ceil(np.log2(len(x)+n)))
    y=np.fft.irfft(np.fft.rfft(x,L)*np.fft.rfft(ir,L),L)[:len(x)]
    return (1-wet)*x+wet*y*np.sqrt((x**2).sum()/max(1e-12,(y**2).sum()))

def babble(n, rng, amp):
    out=np.zeros(n)
    for _ in range(int(n/SR*6)):
        st=rng.integers(0,n-2000); L=int(rng.uniform(0.15,0.6)*SR); L=min(L,n-st)
        tt=np.arange(L)/SR; base=rng.uniform(100,240)
        f=base*(1+0.25*np.sin(2*np.pi*rng.uniform(2,6)*tt+rng.uniform(0,6)))*np.linspace(1.15,0.85,L)
        ph=2*np.pi*np.cumsum(f)/SR
        z=np.exp(1j*ph); zk=z.copy(); acc=np.zeros(L,dtype=complex)
        for k in range(1,10):
            acc+=(vowel_gain(base*k,'a')/k**0.5)*zk; zk*=z
        s=acc.imag
        out[st:st+L]+=amp*rng.uniform(0.3,1)*s*np.hanning(L)
    return out

def render(piece, rng, seconds=15.0):
    tonic0=piece['tonic']; minor=piece['minor']; parts=[[tuple(n) for n in p] for p in piece['parts']]
    mel=parts[0]
    # transposição: tônica aleatória, melodia numa região cantável
    shift=int(rng.integers(-6,6)); med=np.median([m for _,_,m in mel])+shift
    while med>70: shift-=12; med-=12
    while med<60: shift+=12; med+=12
    parts=[[(o,d,m+shift) for o,d,m in p] for p in parts]; tonic=(tonic0+shift)%12; mel=parts[0]
    durs=np.array([d for _,d,_ in mel]); tempo=rng.uniform(0.38,0.62)/np.median(durs)  # s por unidade
    total=max(o+d for o,d,_ in mel)*tempo
    # repete (estrofes) até caber o trecho
    reps=int(np.ceil((seconds+2)/total))+1
    span=max(o+d for o,d,_ in mel)
    parts=[[(o+r*span,d,m) for r in range(reps) for o,d,m in p] for p in parts]; mel=parts[0]
    t0=rng.uniform(0, max(0.0, total*reps-seconds-1)) if reps>1 else 0.0
    t0=rng.uniform(0, total)  # entra em qualquer ponto do hino
    n=int(seconds*SR); out=np.zeros(n)
    sc=rng.choice(['acapela','teclado','banda','solo'],p=[0.3,0.2,0.4,0.1])
    choir = piece['src']=='coral' and len(parts)==4 and rng.random()<0.35
    # congregação
    nsing=1 if sc=='solo' else int(rng.integers(6,18))
    gdet=rng.uniform(-45,45); vowel=rng.choice(['a','o','e'])
    for v in range(nsing):
        part=mel
        if choir: part=parts[[0,0,1,2,3][v%5]] if v%5 else mel
        male = (not choir) and rng.random()<0.4 and np.median([m for _,_,m in mel])>60
        sing(out, part, rng, tempo, gdet+rng.normal(0,15 if nsing>1 else 0), -12 if male else 0,
             (0.35 if sc=='solo' else 0.6)/np.sqrt(nsing), vowel, t0)
    # instrumentos
    voices_lvl=np.sqrt((out**2).mean())+1e-9; inst=np.zeros(n)
    if sc in ('teclado','banda','solo'):
        organ=rng.random()<0.4
        if piece['src']=='coral' and len(parts)==4 and rng.random()<0.7:
            for p in parts:
                for o,d,m in p:
                    st=o*tempo-t0
                    if -1<st<seconds: keys_note(inst,m,st,d*tempo,0.25,organ,rng)
            bassline=[(o,d,m) for o,d,m in parts[3]]
        else:
            ch=harmonize([(o,d,m) for o,d,m in mel], tonic, minor, win=rng.choice([1.0,2.0,2.0,4.0]) if True else 2.0)
            bassline=[]
            for o,d,pcs,root in ch:
                st=o*tempo-t0
                if -1<st<seconds:
                    for pc in pcs: keys_note(inst,52+((pc-52)%12),st,d*tempo,0.25,organ,rng)
                bassline.append((o,d,36+((root-36)%12)))
        if sc=='banda':
            for o,d,m in bassline:
                mm=m
                while mm>47: mm-=12
                while mm<28: mm+=12
                st=o*tempo-t0
                if -1<st<seconds: bass_note(inst,mm,st,d*tempo,0.5)
        il=np.sqrt((inst**2).mean())+1e-9
        out+=inst*(voices_lvl/il)*rng.uniform(0.4,1.6)
    out=reverb(out,rng,rng.uniform(0.7,2.2),rng.uniform(0.2,0.6))
    lvl=np.sqrt((out**2).mean())
    out+=babble(n,rng,lvl*rng.uniform(0.0,0.5))
    out+=rng.normal(0,lvl*rng.uniform(0.02,0.15),n)
    out+=lvl*rng.uniform(0,0.3)*np.sin(2*np.pi*60*np.arange(n)/SR+rng.uniform(0,6))
    # microfone de celular: corta graves e agudos
    out=lfilter_hp(out, rng.uniform(80,200)); out=lfilter_lp(out, rng.uniform(6000,9000))
    out*=rng.uniform(0.15,0.6)/np.max(np.abs(out)+1e-9)
    return out, tonic, minor, sc

def biquad(x,b,a):
    return lfilter(b,a,x)
def lfilter_hp(x,fc,q=0.7071):
    w=2*np.pi*fc/SR; c=np.cos(w); al=np.sin(w)/(2*q); a0=1+al
    return lfilter([(1+c)/2/a0,-(1+c)/a0,(1+c)/2/a0],[1,-2*c/a0,(1-al)/a0],x)
def lfilter_lp(x,fc,q=0.7071):
    w=2*np.pi*fc/SR; c=np.cos(w); al=np.sin(w)/(2*q); a0=1+al
    return lfilter([(1-c)/2/a0,(1-c)/a0,(1-c)/2/a0],[1,-2*c/a0,(1-al)/a0],x)

def write_wav(path, x):
    with wave.open(path,'wb') as w:
        w.setnchannels(1); w.setsampwidth(2); w.setframerate(SR)
        w.writeframes((np.clip(x,-1,1)*32767).astype('<i2').tobytes())

if __name__=='__main__':
    outdir, count, seed, split = sys.argv[1], int(sys.argv[2]), int(sys.argv[3]), sys.argv[4]
    import zlib
    pieces=[p for p in json.load(open('corpus.json')) if (zlib.crc32(p['name'].encode())%5==0)==(split=='val')]
    rng=np.random.default_rng(seed); os.makedirs(outdir,exist_ok=True)
    corais=[p for p in pieces if p['src']=='coral']; essen=[p for p in pieces if p['src']=='essen']
    with open(os.path.join(outdir,'labels.csv'),'a') as lab:
        for i in range(count):
            # mistura parecida com a Harpa: maioria maior; corais trazem os menores
            src = corais if rng.random()<0.35 else essen
            p=src[rng.integers(len(src))]
            x,t,mi,sc=render(p,rng)
            name=f'{seed}_{i:05d}.wav'; write_wav(os.path.join(outdir,name),x)
            lab.write(f"{name},{t},{int(mi)},{p['src']},{sc},{p['name']}\n"); lab.flush()
