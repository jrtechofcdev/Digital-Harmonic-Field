"""Treina o modelo de tom: rede pequena aplicada às features vistas de cada tônica
(mesmos pesos para as 12 tônicas = invariante à transposição) → 24 pontuações → softmax."""
import numpy as np, sys, json
from feats import load, D

def forward(P, X):
    if 'W1' in P:
        Hpre=X@P['W1']+P['b1']; H=np.maximum(Hpre,0)
        L=H@P['W2']+P['b2']             # [N,12,2]
    else:
        Hpre=H=None; L=X@P['W']+P['b']
    logits=np.concatenate([L[:,:,0],L[:,:,1]],axis=1)  # índice = tônica + 12*menor
    return logits,(Hpre,H)

def softmax(z):
    z=z-z.max(1,keepdims=True); e=np.exp(z); return e/e.sum(1,keepdims=True)

def train(X,Y,hidden=0,epochs=60,lr=0.01,l2=1e-4,batch=256,seed=0,Xv=None,Yv=None):
    rng=np.random.default_rng(seed)
    if hidden:
        P={'W1':rng.normal(0,np.sqrt(2/D),(D,hidden)),'b1':np.zeros(hidden),
           'W2':rng.normal(0,np.sqrt(1/hidden),(hidden,2)),'b2':np.zeros(2)}
    else:
        P={'W':np.zeros((D,2)),'b':np.zeros(2)}
    M={k:np.zeros_like(v) for k,v in P.items()}; V={k:np.zeros_like(v) for k,v in P.items()}; t=0
    N=len(X)
    for ep in range(epochs):
        idx=rng.permutation(N)
        for s in range(0,N,batch):
            b=idx[s:s+batch]; xb=X[b]; yb=Y[b]
            logits,(Hpre,H)=forward(P,xb); p=softmax(logits)
            p[np.arange(len(b)),yb]-=1; g=p/len(b)           # dL/dlogits
            gL=np.stack([g[:,:12],g[:,12:]],axis=2)          # [B,12,2]
            G={}
            if hidden:
                G['W2']=np.einsum('bkh,bkm->hm',H,gL); G['b2']=gL.sum((0,1))
                gH=gL@P['W2'].T; gH[Hpre<=0]=0
                G['W1']=np.einsum('bkd,bkh->dh',xb,gH); G['b1']=gH.sum((0,1))
            else:
                G['W']=np.einsum('bkd,bkm->dm',xb,gL); G['b']=gL.sum((0,1))
            t+=1
            for k in P:
                if k.startswith('W'): G[k]+=l2*P[k]
                M[k]=0.9*M[k]+0.1*G[k]; V[k]=0.999*V[k]+0.001*G[k]**2
                P[k]-=lr*(M[k]/(1-0.9**t))/(np.sqrt(V[k]/(1-0.999**t))+1e-8)
        if Xv is not None and (ep%10==9 or ep==epochs-1):
            acc=(forward(P,Xv)[0].argmax(1)==Yv).mean()
            print(f"  época {ep+1}: validação top-1 {acc:.1%}",flush=True)
    return P

def calibrate(P,X,Y):
    """Temperatura que melhor calibra as chances (as % mostradas ficam honestas)."""
    logits,_=forward(P,X); best=(1e9,1.0)
    for T in np.linspace(0.3,4,75):
        p=softmax(logits/T); nll=-np.log(p[np.arange(len(Y)),Y]+1e-12).mean()
        best=min(best,(nll,T))
    return best[1]

if __name__=='__main__':
    X,Y,T,F,E=load('train_notes.jsonl','train/labels.csv')
    # Dados reais do app DEV: --extra notas.jsonl:labels.csv (pode repetir; peso 3×,
    # porque um hino real vale mais que um sintético).
    for i,arg in enumerate(sys.argv):
        if arg=='--extra':
            nj,lc=sys.argv[i+1].split(':')
            Xr,Yr,_,_,_=load(nj,lc)
            if len(Xr):
                X=np.concatenate([X]+[Xr]*3); Y=np.concatenate([Y]+[Yr]*3)
                print('reais',len(Xr),'(peso 3×)')
    Xv,Yv,Tv,Fv,Ev=load('val_notes.jsonl','val/labels.csv')
    print('treino',X.shape,'validação',Xv.shape)
    pos=[a for a in sys.argv[1:] if not a.startswith('--') and ':' not in a]
    hidden=int(pos[0]) if pos else 16
    P=train(X,Y,hidden=hidden,Xv=Xv,Yv=Yv,epochs=int(pos[1]) if len(pos)>1 else 30)
    Tcal=calibrate(P,Xv,Yv); print('temperatura',round(Tcal,3))
    logits,_=forward(P,Xv); p=softmax(logits/Tcal)
    for t in sorted(set(Tv)):
        m=Tv==t; top=p[m].argmax(1); conf=p[m].max(1)
        print(f"{t:4.1f}s top1 {(top==Yv[m]).mean():.1%} top3 {np.mean([Yv[m][i] in np.argsort(-p[m][i])[:3] for i in range(m.sum())]):.1%}"
              + "".join(f" | conf≥{c:.2f}: {(conf>=c).mean():.0%} resp, {((top==Yv[m])[conf>=c]).mean() if (conf>=c).any() else 0:.0%} certo" for c in (0.6,0.8,0.9)))
    np.savez(f'model_h{hidden}.npz',T=Tcal,**P)
