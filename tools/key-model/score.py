import csv, sys
from collections import defaultdict
NAMES=["C","C#","D","D#","E","F","F#","G","G#","A","A#","B"]
rows=list(csv.DictReader(open(sys.argv[1])))
def truth(r): return NAMES[int(r['tonic'])]+("m" if r['minor']=='1' else "")
def rel(k):
    m=k.endswith('m'); r=NAMES.index(k.rstrip('m'))
    return NAMES[(r+3)%12] if m else NAMES[(r+9)%12]+"m"
for cp in ('5.0','10.0','15.0'):
    R=[r for r in rows if r['checkpoint']==cp]
    n=len(R); t1=sum(r['k1']==truth(r) for r in R); t3=sum(truth(r) in (r['k1'],r['k2'],r['k3']) for r in R)
    ans=sum(r['k1']!='-' for r in R)
    relerr=sum(r['k1']!='-' and r['k1']!=truth(r) and r['k1']==rel(truth(r)) for r in R)
    fifth=0
    for r in R:
        if r['k1'] in ('-',truth(r)): continue
        a=NAMES.index(r['k1'].rstrip('m')); b=int(r['tonic'])
        if (a-b)%12 in (5,7) and r['k1'].endswith('m')==(r['minor']=='1'): fifth+=1
    alta=[r for r in R if r['status']=='ALTA']; altaok=sum(r['k1']==truth(r) for r in alta)
    print(f"{cp:>5}s  top1 {t1/n:5.1%}  top3 {t3/n:5.1%}  respondeu {ans/n:5.1%}  | erros: relativa {relerr}, quinta {fifth}  | ALTA {len(alta)} ({altaok/max(1,len(alta)):.0%} certas)")
R=[r for r in rows if r['checkpoint']=='15.0']
def show(name,RR):
    if not RR: return
    n=len(RR); ok=sum(r['stop_k1']==truth(r) for r in RR); ans=sum(r['stop_k1']!='-' for r in RR)
    alta=[r for r in RR if r['stop_status']=='ALTA']
    print(f"FLUXO DO APP ({name}, n={n}): acerta {ok/n:.1%} | responde {ans/n:.0%} | quando responde acerta {ok/max(1,ans):.1%}"
          f" | parou sozinho (identificado) {len(alta)/n:.0%} em média aos {sum(float(r['stop_s']) for r in alta)/max(1,len(alta)):.1f}s, acertando {sum(r['stop_k1']==truth(r) for r in alta)/max(1,len(alta)):.0%}")
show("todos",R); show("hinos simples",[r for r in R if r['src']=='essen'])
by=defaultdict(list)
for r in R: by[(r['scene'])].append(r['k1']==truth(r))
print("por cenário (15 s):", {k:f"{sum(v)/len(v):.0%} (n={len(v)})" for k,v in by.items()})
by=defaultdict(list)
for r in R: by[(r['src'],r['minor'])].append(r['k1']==truth(r))
print("por fonte/modo (15 s):", {k:f"{sum(v)/len(v):.0%} (n={len(v)})" for k,v in by.items()})
