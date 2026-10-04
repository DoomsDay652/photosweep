"""Original cyclic arrangements, without samples. Requires numpy/scipy/ffmpeg.

Every note tail and stereo delay wraps into the start, so these are real loop
masters rather than faded previews or repetitions of the old 24-second asset.
"""
from pathlib import Path
import json
import subprocess
import tempfile
import numpy as np
from scipy.io.wavfile import write
from scipy.signal import butter, sosfilt

RATE = 44100
OUT = Path(__file__).resolve().parents[1] / 'app/src/main/res/raw'
TRACKS = [
    ('dreamy_sweep', 'Dreamy Sweep', 80, 'dream', [[60,64,67,71],[57,60,64,67],[53,57,60,64],[55,59,62,69]],
     [[76,79,74,0],[72,76,71,0],[69,72,76,0],[74,71,69,67]]),
    ('paper_lantern', 'Paper Lantern', 72, 'lofi', [[57,60,64,67],[62,65,69,72],[55,59,62,65],[60,64,67,71]],
     [[76,0,72,71],[74,77,0,76],[71,0,69,67],[72,76,79,0]]),
    ('nebula_drift', 'Nebula Drift', 64, 'space', [[62,66,69,73],[59,62,66,69],[55,59,62,66],[57,61,64,71]],
     [[81,0,78,0],[78,74,0,73],[78,0,74,71],[76,0,73,69]]),
    ('midnight_polaroid', 'Midnight Polaroid', 76, 'lofi', [[53,57,60,64],[58,62,65,69],[55,58,62,65],[60,64,67,70]],
     [[72,76,0,69],[74,0,77,72],[70,74,0,65],[76,0,79,77]]),
]

def note(midi, seconds, kind):
    t = np.arange(round(seconds*RATE))/RATE
    f = 440*2**((midi-69)/12)
    end = np.clip((seconds-t)/.12, 0, 1)
    if kind in ('pad', 'space'):
        # Smooth unison oscillators with slowly moving stereo-friendly harmonics.
        x = np.sin(2*np.pi*f*t) + .28*np.sin(2*np.pi*f*1.002*t+.4)
        x += .10*np.sin(4*np.pi*f*t+.2*np.sin(2*np.pi*.17*t))
        env = np.minimum(t/.7,1)*np.minimum((seconds-t)/1.0,1)
    elif kind == 'bass':
        x = np.sin(2*np.pi*f*t)+.12*np.sin(4*np.pi*f*t)
        env = np.minimum(t/.025,1)*np.exp(-t*.65)*end
    elif kind == 'bell':
        x = np.sin(2*np.pi*f*t+.35*np.sin(4*np.pi*f*t)*np.exp(-t*2))
        env = (1-np.exp(-t*90))*np.exp(-t*1.8)*end
    else:
        x = np.sin(2*np.pi*f*t)+.20*np.sin(4*np.pi*f*t)*np.exp(-t*3)
        x += .08*np.sin(6*np.pi*f*t)*np.exp(-t*4)
        env = (1-np.exp(-t*130))*np.exp(-t*1.4)*end
    return x*np.clip(env,0,1)

def add(buf, sound, seconds, gain, pan=0):
    a = round(seconds*RATE) % len(buf)
    sound = sound[:,None]*np.sqrt([(1-pan)/2,(1+pan)/2])*gain
    n = min(len(sound),len(buf)-a)
    buf[a:a+n] += sound[:n]
    if n < len(sound): buf[:len(sound)-n] += sound[n:]

def drum(kind, rng):
    d = .23 if kind=='kick' else .12
    t = np.arange(round(d*RATE))/RATE
    if kind=='kick':
        return np.sin(2*np.pi*(48*t+30*(1-np.exp(-t*30))/30))*np.exp(-t*22)*(1-np.exp(-t*350))
    hz = 1900 if kind=='snare' else 5800
    x = sosfilt(butter(2,hz,fs=RATE,btype='highpass',output='sos'),rng.normal(size=len(t)))
    x *= np.exp(-t*(38 if kind=='snare' else 65))*np.minimum(t/.003,1)
    return x

def generate(spec, index, tmp):
    resource,title,bpm,style,chords,melody=spec
    rng=np.random.default_rng(831+index)
    beat=60/bpm
    duration=32*4*beat
    buf=np.zeros((round(duration*RATE),2))
    for bar in range(32):
        phrase=bar//8
        chord=chords[bar%4]
        start=bar*4*beat
        # Four 8-bar sections develop the arrangement, then lead back to bar one.
        for j,m in enumerate(chord):
            add(buf,note(m,4*beat+1.4,'space' if style=='space' else 'pad'),start,.065 if style=='lofi' else .10,-.6+j*.4)
        for b in (0,2):
            add(buf,note(chord[0]-24,beat*1.7,'bass'),start+b*beat,.17 if style=='lofi' else .095)
        if style!='space' or phrase in (1,2):
            for step in range(8):
                if style=='dream' and phrase==0 and step%2: continue
                when=start+step*.5*beat+(beat*.065 if style=='lofi' and step%2 else 0)
                pitch=chord[(step+phrase)%4]+(12 if style=='space' else 0)
                add(buf,note(pitch,beat*1.5,'bell' if style=='space' else 'keys'),when,.06,-.32 if step%2 else .32)
        for j,m in enumerate(melody[bar%4]):
            if m and (phrase!=3 or j<2):
                pitch=m+(12 if phrase==2 and bar%2 else 0)
                when=start+j*beat+(beat*.03 if style=='lofi' else 0)
                add(buf,note(pitch,beat*2,'bell' if style=='space' else 'keys'),when,.14 if style=='dream' else .10,.12)
        if style=='lofi':
            for b in (0,2,3.5) if phrase==2 else (0,2): add(buf,drum('kick',rng),start+b*beat,.13)
            for b in (1,3): add(buf,drum('snare',rng),start+b*beat,.038,-.12)
            for b in np.arange(.5,4,.5): add(buf,drum('hat',rng),start+(b+.055)*beat,.015,.38)
        elif style=='space' and phrase in (1,2):
            for b in (0,2): add(buf,drum('kick',rng),start+b*beat,.045)
    # Circular delay/reverb preserves both ends of each loop, including percussion.
    for beats,gain in ((.5,.16),(.75,.10),(1.5,.065),(2.75,.04)):
        buf += np.roll(buf.copy()[:,::-1],round(beats*beat*RATE),axis=0)*gain
    buf -= buf.mean(axis=0)
    buf=np.tanh(buf*1.1)
    buf *= min(.73/max(abs(buf).max(),.001), .115/max(np.sqrt(np.mean(buf**2)),.001))
    assert np.isfinite(buf).all() and abs(buf).max()<.8
    seam=float(abs(buf[-1]-buf[0]).max())
    assert seam<.025, (title,seam)
    wav=Path(tmp)/f'{resource}.wav'
    write(wav,RATE,(buf*32767).astype(np.int16))
    destination=OUT/f'{resource}.ogg'
    subprocess.run(['ffmpeg','-v','error','-y','-i',str(wav),'-c:a','libvorbis','-q:a','4',
                    '-metadata',f'title={title}','-metadata','artist=Dreamy Game Studios',str(destination)],check=True)
    # Decode the delivered codec, check duration and the actual loop boundary.
    raw=subprocess.check_output(['ffmpeg','-v','error','-i',str(destination),'-f','f32le','-acodec','pcm_f32le','-'])
    decoded=np.frombuffer(raw,dtype='<f4').reshape(-1,2)
    assert abs(len(decoded)/RATE-duration)<.03
    assert np.isfinite(decoded).all() and abs(decoded).max()<.85
    assert abs(decoded[-1]-decoded[0]).max()<.035
    return {'resource':resource,'title':title,'bpm':bpm,'seconds':round(duration,3),
            'style':style,'bytes':destination.stat().st_size,'seam_delta':round(seam,6)}

if __name__=='__main__':
    OUT.mkdir(parents=True,exist_ok=True)
    with tempfile.TemporaryDirectory() as tmp:
        report=[generate(spec,i,tmp) for i,spec in enumerate(TRACKS)]
    (Path(__file__).parent/'radio_music_manifest.json').write_text(json.dumps(report,indent=2)+'\n')
    print(json.dumps(report,indent=2))
