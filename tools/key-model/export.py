"""Exporta o modelo treinado (npz) para key_model.bin (little-endian float32), lido por KeyModel.kt."""
import numpy as np, sys, struct
m=np.load(sys.argv[1]); out=sys.argv[2]
T=float(m['T'])
if 'W1' in m:
    W1,b1,W2,b2=m['W1'],m['b1'],m['W2'],m['b2']
else:
    # linear = relu(z) - relu(-z): vira uma rede com 4 neurônios ocultos, sem perda.
    W,b=m['W'],m['b']; D=W.shape[0]
    W1=np.concatenate([W,-W],axis=1); b1=np.concatenate([b,-b])
    W2=np.array([[1,0],[0,1],[-1,0],[0,-1]],dtype=float); b2=np.zeros(2)
D,H=W1.shape
with open(out,'wb') as f:
    f.write(struct.pack('<iif',D,H,T))
    for a in (W1,b1,W2,b2): f.write(np.asarray(a,dtype='<f4').ravel().tobytes())
print('ok',D,H,T, sum(a.size for a in (W1,b1,W2,b2)),'pesos')
