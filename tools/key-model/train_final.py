"""Treino do modelo do app: sintético + aumento ("sujeira" do culto) + hinos reais.

Uso:
    python3 train_final.py train_notes.jsonl train/labels.csv val_notes.jsonl val/labels.csv \
        [--real real_notes.jsonl:treino-real/labels.csv] [--out modelo.npz]
Depois: python3 export.py modelo.npz ../../app/src/main/resources/com/example/music/key_model.bin
"""
import sys, numpy as np, train as T
from augment import load_abs, rot, augment

args=sys.argv[1:]
tr_n,tr_l,va_n,va_l=args[:4]
out=args[args.index('--out')+1] if '--out' in args else 'modelo.npz'
rng=np.random.default_rng(11)
A,Y,_,_=load_abs(tr_n,tr_l); Av,Yv,Tv,_=load_abs(va_n,va_l)
parts=[A,augment(A,rng,1.4),augment(A,rng,1.4)]; ys=[Y,Y,Y]
if '--real' in args:
    rn,rl=args[args.index('--real')+1].split(':')
    Ar,Yr,_,_=load_abs(rn,rl)
    if len(Ar):
        # hinos reais: originais + 2 cópias levemente aumentadas (peso 3)
        parts+=[Ar]+[augment(Ar,rng,0.7) for _ in range(2)]; ys+=[Yr]*3
        print('reais:',len(Ar),'momentos')
P=T.train(rot(np.concatenate(parts)),np.concatenate(ys),hidden=16,epochs=20,seed=5)
Xv=rot(Av); Tc=T.calibrate(P,Xv,Yv)
ok=T.softmax(T.forward(P,Xv)[0]/Tc).argmax(1)==Yv
print("validação sintética: "+" ".join(f"{t:.0f}s {ok[Tv==t].mean():.0%}" for t in (5.0,10.0,15.0)))
np.savez(out,T=Tc,**P); print('salvo',out,'temperatura',round(Tc,3))
