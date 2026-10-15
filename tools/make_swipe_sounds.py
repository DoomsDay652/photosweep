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

# Warm ascending glass/pluck: C5 then E5, little high-frequency energy, no coin rattle.
keep = np.zeros(n)
for start, frequency, gain in [(0, 523.251, 1), (.075, 659.255, .78)]:
    u = t - start
    tone = np.sin(2*np.pi*frequency*u) + .15*np.sin(2*np.pi*frequency*2*u) + .035*np.sin(2*np.pi*frequency*3*u)
    keep += gain * tone * envelope(u, .005, .095)
for delay, gain in [(.038, .14), (.067, .07)]:
    offset = int(delay * RATE)
    keep[offset:] += keep[:-offset].copy() * gain

# Soft paper sweep: falling air noise with three quiet crinkle textures, no buzzer/explosion.
noise = rng.normal(0, 1, n)
soft = signal.sosfilt(signal.butter(3, [180, 2500], btype='bandpass', fs=RATE, output='sos'), noise)
low = signal.sosfilt(signal.butter(3, [100, 750], btype='bandpass', fs=RATE, output='sos'), noise)
progress = t / DURATION
sweep = soft * (1-progress) + low * progress
shape = np.sin(np.pi * np.minimum(1, t/.36)) ** 1.6
crinkles = sum(.28*np.exp(-((t-center)/width)**2) for center,width in [(.08,.013),(.145,.018),(.225,.024)])
trash = sweep * (shape*.75 + crinkles)
phase = 2*np.pi*(160*t-75*t*t)
trash += .045*np.sin(phase)*envelope(t, .015, .085)

report = [finish('bubble_keep', keep), finish('bubble_trash', trash)]
print(json.dumps(report))
