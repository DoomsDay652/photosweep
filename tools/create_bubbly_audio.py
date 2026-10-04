"""Original Photo Sweep effects; no recordings or third-party samples.

Requires numpy, scipy and ffmpeg. The approved Dreamy Sweep composition is
supplied separately; this script regenerates the nine gentle interaction cues.
"""
from pathlib import Path
import subprocess
import tempfile
import numpy as np
from scipy.io.wavfile import write
from scipy.signal import butter, sosfilt

RATE = 44100
OUT = Path(__file__).resolve().parents[1] / 'app/src/main/res/raw'
RNG = np.random.default_rng(831)

def bubble(duration=.24, start=520, end=240):
    t = np.arange(round(duration * RATE)) / RATE
    phase = 2 * np.pi * (end * t + (start-end) * (1-np.exp(-t*18))/18)
    envelope = (1-np.exp(-t*160)) * np.exp(-t*17)
    envelope *= np.minimum((duration-t)/.035, 1)
    return np.sin(phase + .12*np.sin(phase*2)) * envelope

def cue(duration, events, air=0):
    x = np.zeros((round(duration*RATE), 2))
    for time, pitch, end, gain, pan in events:
        sound = bubble(min(.4, duration-time), pitch, end) * gain
        a = round(time*RATE)
        x[a:a+len(sound)] += sound[:, None] * np.sqrt([(1-pan)/2, (1+pan)/2])
    if air:
        t = np.arange(len(x))/RATE
        noise = sosfilt(butter(2, 1100, fs=RATE, output='sos'), RNG.normal(size=len(x)))
        x += (noise * np.sin(np.pi*t/duration)**2 * air)[:,None]
    fade = min(round(.018*RATE), len(x)//2)
    x[:fade] *= np.linspace(0,1,fade)[:,None]
    x[-fade:] *= np.linspace(1,0,fade)[:,None]
    peak = abs(x).max()
    return x * (.60/max(peak, .001))

EFFECTS = {
 'bubble_trash': (.48, [(0,440,190,.45,-.45),(.11,340,155,.25,-.6)], .015),
 'bubble_keep': (.64, [(0,480,260,.4,.35),(.10,620,330,.28,.5),(.22,760,410,.18,.3)], .005),
 'bubble_undo': (.58, [(0,620,290,.28,.2),(.12,460,240,.32,-.2),(.24,340,200,.2,-.3)], 0),
 'bubble_tap': (.24, [(0,510,280,.3,0)], 0),
 'bubble_complete': (1.1, [(0,520,320,.35,-.2),(.13,660,420,.3,0),(.26,790,510,.25,.2),(.42,1040,640,.18,0)], 0),
 'bubble_fire': (.75, [(0,280,125,.4,-.15),(.10,350,160,.25,.2),(.24,430,190,.15,0)], .035),
 'bubble_water': (.65, [(0,690,280,.4,.1),(.17,440,210,.23,-.15)], .005),
 'bubble_toxic': (1.25, [(0,220,85,.4,-.2),(.30,260,105,.35,.15),(.65,190,75,.25,0)], .003),
 'bubble_candy': (.65, [(0,750,390,.35,-.2),(.10,900,490,.25,.2),(.23,660,330,.2,0)], 0),
}

if __name__ == '__main__':
    OUT.mkdir(parents=True, exist_ok=True)
    with tempfile.TemporaryDirectory() as tmp:
        for name, (duration, events, air) in EFFECTS.items():
            wav = Path(tmp)/f'{name}.wav'
            write(wav, RATE, (cue(duration, events, air)*32767).astype(np.int16))
            subprocess.run(['ffmpeg','-v','error','-y','-i',str(wav),'-c:a','libvorbis','-q:a','5',str(OUT/f'{name}.ogg')], check=True)
    print('Created nine original soft, bubbly effects.')
