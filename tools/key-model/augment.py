"""Aumento de dados no nível das features: imita o canto "sujo" do culto real.

Medido com o material real do app DEV (out/2026): no culto, ~27% das notas
captadas caem fora da escala (afinação do grupo andando, eco, vozes
desencontradas), o perfil harmônico sai quase plano e o baixo quase não chega
ao celular. O aumento abaixo ensina o modelo a decidir mesmo assim."""
import numpy as np, json, sys, train as T
from feats import features, D
# permutação de rotação: rot[k][d] = x[perm[k,d]]
perm=np.zeros((12,D),dtype=int)
for k in range(12):
    p=[]
    for b in range(9): p+= [b*12+(i+k)%12 for i in range(12)]
    for t in range(2):
        base=108+t*144; p+=[base+((i+k)%12)*12+(j+k)%12 for i in range(12) for j in range(12)]
    perm[k]=p
def load_abs(jsonl, labels):
    lab={}
    for l in open(labels):
        c=l.strip().split(',')
        if len(c)>=3: lab[c[0]]=(int(c[1]),int(c[2]),c[3])
    A=[];Y=[];T_=[];F=[]
    for l in open(jsonl):
        d=json.loads(l)
        if d['file'] not in lab: continue
        b12,b144,_=features(d['voice'],d['bass'],d['harmony'],d.get('bchroma'),d.get('bpresent',False))
        A.append(np.concatenate(b12+[b.ravel() for b in b144])); t,mi,src=lab[d['file']]
        Y.append(t+12*mi); T_.append(d['t']); F.append(d['file'])
    return np.array(A),np.array(Y),np.array(T_),F
def rot(A): return A[:,perm]
MEL=[0,1,3,4]; LAST=2; HARM=7; BASSB=[5,6,8]; MBIG=slice(108,252); BBIG=slice(252,396)
def augment(A, rng, s=1.0):
    A=A.copy(); N=len(A)
    for b in MEL:
        lam=rng.uniform(0,0.45*s,N)[:,None]
        noise=rng.dirichlet(np.ones(12)*0.7,N)
        blk=A[:,b*12:(b+1)*12]; has=blk.sum(1,keepdims=True)>0
        A[:,b*12:(b+1)*12]=np.where(has,(1-lam)*blk+lam*noise,blk)
    m=rng.random(N)<0.3*s
    A[m,LAST*12:(LAST+1)*12]=np.eye(12)[rng.integers(0,12,m.sum())]
    mu=rng.uniform(0,0.9*s,N)[:,None]
    A[:,HARM*12:(HARM+1)*12]=(1-mu)*A[:,HARM*12:(HARM+1)*12]+mu/12
    m=rng.random(N)<0.6
    for b in BASSB: A[m,b*12:(b+1)*12]=0
    A[m,BBIG]=0
    lam=rng.uniform(0,0.45*s,N)[:,None]; nb=rng.dirichlet(np.ones(144)*0.3,N)
    has=A[:,MBIG].sum(1,keepdims=True)>0
    A[:,MBIG]=np.where(has,(1-lam)*A[:,MBIG]+lam*nb,A[:,MBIG])
    return A
