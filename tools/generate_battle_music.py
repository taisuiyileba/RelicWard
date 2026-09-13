"""Original procedural score: Bronze Vigil / The Shattered Bell. No sampled music.
Requires numpy and soundfile. Circular mixing retains reverb tails across loop seams.
"""
from pathlib import Path
import json
import numpy as np
import soundfile as sf

ROOT = Path(__file__).resolve().parents[1] / 'src/main/resources/assets/relicward'
SR = 44100

def compose(phase):
    rng = np.random.default_rng(820 + phase)
    beat = 60 / (96 if phase == 1 else 120)
    length = round(64 * beat * SR)
    mix = np.zeros((length, 2), dtype=np.float64)

    def add(signal, at, gain=1, pan=0):
        indexes = (round(at*SR) + np.arange(len(signal))) % length
        for delay, amount in [(0, 1), (.173, .17), (.347, .10), (.631, .06)]:
            ix = (indexes + round(delay*SR)) % length
            np.add.at(mix[:, 0], ix, signal*gain*amount*np.sqrt((1-pan)/2))
            np.add.at(mix[:, 1], ix, signal*gain*amount*np.sqrt((1+pan)/2))

    def tone(midi, seconds, kind):
        t = np.arange(round(seconds*SR))/SR
        f = 440*2**((midi-69)/12)
        if kind == 'bell':
            s = sum(a*np.sin(2*np.pi*f*r*t)*np.exp(-t*d) for r,a,d in [(1,1,1.5),(2.01,.45,2.4),(2.76,.2,3.7),(4.07,.12,5)])
            return s*np.minimum(t/.006,1)
        env = np.minimum(t/.08, 1)*np.minimum((seconds-t)/.15,1)
        return env*(np.sin(2*np.pi*f*t)+.25*np.sin(2*np.pi*f*2*t)+.1*np.sin(2*np.pi*f*3*t))

    roots = [38,38,34,36,38,41,36,33]*2
    motif = [74,77,81,77,72,69,65,69,74,77,84,81,77,76,69,73]
    for bar, root in enumerate(roots):
        at=bar*4*beat
        for interval in [0,7,12]:
            add(tone(root+interval,4*beat,'pad'),at,.10 if interval else .17, interval/20-.3)
        add(tone(motif[bar],2.8,'bell'),at,.25,(-1)**bar*.4)
        for step in range(8):
            note=root+12+[0,7,12,7,3,7,12,15][step]
            add(tone(note,.38 if phase==1 else .27,'pad'),at+step*beat/2,.085 if phase==1 else .15,(-1)**step*.3)
        for hit in ([0,2,3.5] if phase==1 else [0,1.5,2,2.75,3.5]):
            t=np.arange(round(.65*SR))/SR
            drum=np.sin(2*np.pi*(48*t+45*.04*(1-np.exp(-t/.04))))*np.exp(-t*7)
            drum+=rng.normal(0,1,len(t))*.12*np.exp(-t*55)
            add(drum,at+hit*beat,.55 if hit in [0,2] else .30)
        if phase==2:
            for hit in [1,3]:
                t=np.arange(round(.25*SR))/SR
                add(rng.normal(0,1,len(t))*np.exp(-t*24),at+hit*beat,.13,.15)
    mix=np.tanh(mix*1.2)
    mix*=.88/max(abs(mix).max(),.001)
    out=ROOT/'sounds/music'/f'bell_warden_{phase}.ogg'
    out.parent.mkdir(parents=True,exist_ok=True)
    sf.write(out,mix,SR,format='OGG',subtype='VORBIS')
    print(out.name,round(length/SR,2),'seconds',out.stat().st_size,'bytes')

def main():
    for phase in [1,2]: compose(phase)
    target=ROOT/'sounds.json'
    data=json.loads(target.read_text()) if target.exists() else {}
    for phase in [1,2]:
        data[f'music.bell_warden_{phase}']={'sounds':[{'name':f'relicward:music/bell_warden_{phase}','stream':True}]}
    target.write_text(json.dumps(data,indent=2)+'\n',encoding='utf-8')

if __name__ == "__main__":
    import threading
    threading.stack_size(16 * 1024 * 1024)
    worker = threading.Thread(target=main)
    worker.start()
    worker.join()
