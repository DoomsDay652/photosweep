"""Original synthesized Photo Sweep feedback. No samples or external sound licenses."""
from pathlib import Path
import json
import argparse
import subprocess
import numpy as np
from scipy import signal
from scipy.io import wavfile

RATE = 48000
parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--output', type=Path, default=Path(__file__).resolve().parents[1] / 'app/src/main/res/raw')
OUT = parser.parse_args().output
OUT.mkdir(parents=True, exist_ok=True)
DURATION = .46
n = int(RATE * DURATION)
t = np.arange(n) / RATE
rng = np.random.default_rng(526)

def envelope(t, attack, decay):
    return (1 - np.exp(-np.maximum(t, 0) / attack)) * np.exp(-np.maximum(t, 0) / decay) * (t >= 0)

def finish(name, sound):
    # Remove DC, keep the last 45 ms silent-ended, and match perceived energy gently.
    sound = signal.sosfilt(signal.butter(2, 75, btype='highpass', fs=RATE, output='sos'), sound)
    sound *= np.minimum(1, t / .006) * np.minimum(1, (DURATION - t) / .045)
    rms = np.sqrt(np.mean(sound ** 2))
    sound *= min(.17 / max(rms, 1e-9), .79 / max(np.max(np.abs(sound)), 1e-9))
    sound[0] = sound[-1] = 0
    wav = OUT / (name + '.wav')
    wavfile.write(wav, RATE, np.int16(np.clip(sound, -1, 1) * 32767))
    ogg = OUT / (name + '.ogg')
    subprocess.run(['ffmpeg', '-nostdin', '-loglevel', 'error', '-y', '-i', str(wav), '-c:a', 'libvorbis', '-q:a', '5', '-map_metadata', '-1', str(ogg)], check=True)
    # Decode the actual packaged file and check headroom and fades after encoding.
    decoded = subprocess.run(['ffmpeg', '-nostdin', '-loglevel', 'error', '-i', str(ogg), '-f', 'f32le', '-ac', '1', '-ar', str(RATE), '-'], check=True, stdout=subprocess.PIPE).stdout
    samples = np.frombuffer(decoded, dtype='<f4')
    stats = {'file': ogg.name, 'duration_seconds': len(samples) / RATE,
             'peak_dbfs': float(20 * np.log10(max(np.max(np.abs(samples)), 1e-9))),
             'rms_dbfs': float(20 * np.log10(max(np.sqrt(np.mean(samples**2)), 1e-9))),
             'bytes': ogg.stat().st_size}
    assert np.all(np.isfinite(samples)) and np.max(np.abs(samples)) < .95
    assert np.max(np.abs(samples[:40])) < .04 and np.max(np.abs(samples[-40:])) < .04
    wav.unlink()
    return stats

# Rounded retro triangle/pulse arpeggios, with softened edges instead of harsh square waves.
def retro_note(frequency, start, decay, gain):
    u=t-start
    phase=2*np.pi*frequency*u
    triangle=signal.sawtooth(phase, width=.5)
    pulse=np.tanh(2.0*np.sin(phase))
    tone=.78*triangle+.22*pulse
    tone=signal.sosfilt(signal.butter(3, 2600, fs=RATE, output='sos'), tone)
    return gain*tone*envelope(u,.004,decay)

# Soft ascending 8-bit save sparkle: C5, E5, G5.
keep=sum(retro_note(freq,start,.073,gain) for freq,start,gain in
         [(523.251,0,1.0),(659.255,.058,.8),(783.991,.116,.58)])
# Playful descending chip tumble: three rounded boops, with a tiny airy texture.
trash=sum(retro_note(freq,start,.055,gain) for freq,start,gain in
          [(329.628,0,1.0),(246.942,.055,.75),(195.998,.11,.52)])
air=signal.sosfilt(signal.butter(2,[450,2100],btype='bandpass',fs=RATE,output='sos'),rng.normal(0,1,n))
trash+=.08*air*envelope(t,.012,.075)
report=[finish('bubble_keep',keep),finish('bubble_trash',trash)]
print(json.dumps(report))
