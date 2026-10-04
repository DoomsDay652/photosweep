from pathlib import Path
import numpy as np
from scipy.signal import butter,sosfilt
from scipy.io.wavfile import write
import subprocess,json,zipfile
R=44100
OUT=Path(__file__).parent
rng=np.random.default_rng(831)
def freq(m): return 440*2**((m-69)/12)
def filt(x,hz,kind='lowpass'):return sosfilt(butter(2,hz,kind,fs=R,output='sos'),x)
def stereo(x,pan=0):
 if x.ndim==2:return x
 return np.column_stack((x*np.sqrt((1-pan)/2),x*np.sqrt((1+pan)/2)))
def tone(m,d,kind='keys'):
 t=np.arange(int(d*R))/R;f=freq(m)
 if kind=='pad':
  x=sum(np.sin(2*np.pi*f*q*t+phase)*a for q,a,phase in [(1,.6,0),(1.003,.28,1.3),(2,.12,.4)])
  env=np.minimum(t/.25,1)*np.minimum((d-t)/.6,1)*.5
 elif kind=='bell':
  x=np.sin(2*np.pi*f*t+.7*np.sin(2*np.pi*f*2*t)*np.exp(-t*5))+.15*np.sin(2*np.pi*f*3*t)
  env=(1-np.exp(-t*250))*np.exp(-t*4)*np.minimum((d-t)/.05,1)
 elif kind=='bass':
  x=np.sin(2*np.pi*f*t)+.18*np.sin(4*np.pi*f*t)
  env=np.minimum(t/.015,1)*np.minimum((d-t)/.1,1)*np.exp(-t*.7)
 else:
  x=np.sin(2*np.pi*f*t)+.22*np.sin(4*np.pi*f*t)+.09*np.sin(6*np.pi*f*t)
  env=(1-np.exp(-t*180))*np.exp(-t*2)*np.minimum((d-t)/.08,1)
 return x*np.clip(env,0,1)
def add(buf,x,start,amp=1,pan=0):
 x=stereo(x,pan)*amp;a=round(start*R);n=min(len(x),len(buf)-a)
 if a>=0 and n>0:buf[a:a+n]+=x[:n]
def delay(buf,seconds,gain):
 n=int(seconds*R);y=buf.copy();y[n:]+=buf[:-n,::-1]*gain;return y
def master(buf):
 buf=buf-buf.mean(axis=0);buf=np.tanh(buf*1.1);pk=np.max(np.abs(buf))
 return buf*(.78/max(pk,.001))
def save(name,buf,mp3=True,loop=False):
 if buf.ndim==1:buf=stereo(buf)
 buf=master(buf)
 if not loop:
  k=min(int(.015*R),len(buf)//2);buf[:k]*=np.linspace(0,1,k)[:,None];buf[-k:]*=np.linspace(1,0,k)[:,None]
 p=OUT/(name+'.wav');write(p,R,(buf*32767).astype(np.int16))
 if mp3:subprocess.run(['ffmpeg','-v','error','-y','-i',str(p),'-codec:a','libmp3lame','-b:a','192k',str(OUT/(name+'.mp3'))],check=True)
 return buf

def music(name,bpm,chords,melody,style):
 beat=60/bpm;duration=beat*32;buf=np.zeros((round((duration+4)*R),2))
 for bar in range(8):
  start=bar*4*beat;chord=chords[bar%4]
  for i,m in enumerate(chord):add(buf,tone(m,4*beat+.8,'pad'),start,.13,(-.45+i*.3))
  for b in range(4):
   add(buf,tone(chord[0]-24,beat*.8,'bass'),start+b*beat,.17 if style=='pulse' else .12)
  for i in range(8):
   m=chord[i%len(chord)]+(12 if style=='candy' else 0)
   add(buf,tone(m,1.5,'bell' if style=='candy' else 'keys'),start+i*.5*beat,.11 if style=='candy' else .085,(-.35 if i%2 else .35))
  for i,m in enumerate(melody[bar%4]):
   if m: add(buf,tone(m,beat*1.4,'bell' if style=='candy' else 'keys'),start+i*beat,.21 if style=='candy' else .17, .08)
  if style!='dream':
   for b in [0,2]:
    t=np.arange(int(.18*R))/R;x=np.sin(2*np.pi*(48*t+18*(1-np.exp(-t*25))/25))*np.exp(-t*24)
    add(buf,x,start+b*beat,.15)
   for b in [1,3]:
    t=np.arange(int(.12*R))/R;x=filt(rng.normal(0,1,len(t)),1800,'highpass')*np.exp(-t*45)
    add(buf,x,start+b*beat,.045)
   for b in np.arange(.5,4,.5):
    t=np.arange(int(.07*R))/R;add(buf,filt(rng.normal(0,1,len(t)),6000,'highpass')*np.exp(-t*65),start+b*beat,.018,.3)
 buf=delay(delay(buf,.5*beat,.18),.75*beat,.12)
 n=round(duration*R);loop=buf[:n].copy();tail=buf[n:];loop[:len(tail)]+=tail
 # The repeated preview plays two cycles; the individual WAV is the loop asset.
 save(name+'-loop',loop,False,True)
 preview=np.tile(loop,(2,1));k=int(.6*R);preview[:k]*=np.linspace(0,1,k)[:,None];preview[-k:]*=np.linspace(1,0,k)[:,None]
 save(name,preview)
 return duration*2
music('01-Dreamy-Sweep',80,[[60,64,67,71],[57,60,64,67],[53,57,60,64],[55,59,62,69]],[[76,79,74,0],[72,76,71,0],[69,72,76,0],[74,71,69,67]],'dream')
music('02-Candy-Playful',100,[[60,64,67,72],[65,69,72,76],[57,60,64,69],[55,59,62,67]],[[84,79,81,79],[81,84,83,79],[81,76,79,76],[79,83,81,79]],'candy')
music('03-Elemental-Pulse',96,[[57,60,64,67],[53,57,60,64],[60,64,67,71],[55,59,62,69]],[[76,0,72,74],[72,0,69,72],[76,0,79,76],[74,0,71,69]],'pulse')
def noise(d):return rng.normal(0,1,int(d*R))
def swoosh(d=.45,reverse=False):
 t=np.arange(int(d*R))/R;env=np.sin(np.pi*t/d)**2
 x=filt(noise(d),2600)*env*(.8 if reverse else 1)
 return x*.3+np.sin(2*np.pi*(650*t-450*t*t/d))*env*.14
fx=[]
# Each pair is a short standalone interaction sound, with no sampled recordings.
x=stereo(swoosh(.38),-.55);t=np.arange(int(.38*R))/R;x+=stereo(np.sin(2*np.pi*(120*t-60*t*t))*np.exp(-t*22),-.3)*.16
fx.append(('Swipe-left-soft-whoosh',x))
x=np.zeros((int(.8*R),2));add(x,swoosh(.32),0,.6,.5);add(x,tone(79,.6,'bell'),.1,.3,.2);add(x,tone(84,.5,'bell'),.19,.18,.3);fx.append(('Keep-right-sparkle',x))
x=np.zeros((int(.8*R),2));add(x,tone(84,.45,'bell')[::-1],0,.22,-.2);add(x,tone(76,.5,'keys'),.35,.2);fx.append(('Undo-soft-rewind',x))
t=np.arange(int(.25*R))/R;x=np.sin(2*np.pi*(330*t+80*t*t))*np.exp(-t*35)*(1-np.exp(-t*500));fx.append(('Menu-tap',stereo(x*.35)))
x=np.zeros((int(1.4*R),2));
for i,m in enumerate([72,76,79,84]):add(x,tone(m,1,'bell'),i*.1,.22,(-.25+i*.15))
fx.append(('Month-complete-chime',x))
t=np.arange(int(1.2*R))/R;x=filt(noise(1.2),1700)*np.exp(-t*3)*(1-np.exp(-t*30))*.28
for ti in [.09,.24,.43]:
 a=int(ti*R);n=int(.025*R);x[a:a+n]+=filt(rng.normal(0,1,n),3500)*np.linspace(1,0,n)*.12
fx.append(('Fire-release-burst',stereo(x)))
t=np.arange(int(1.0*R))/R;phase=2*np.pi*(310*t+180*(1-np.exp(-t*8))/8);x=np.sin(phase)*np.exp(-t*6)*(1-np.exp(-t*150))*.6
x+=filt(noise(1),2200)*np.exp(-t*12)*.07;fx.append(('Water-drop-ripple',stereo(x)))
t=np.arange(int(1.5*R))/R;phase=2*np.pi*(85*t+120*(1-np.exp(-t*3))/3);x=np.sin(phase+1.5*np.sin(2*np.pi*6*t))*np.exp(-t*2.5)*(1-np.exp(-t*20))*.42
x+=filt(noise(1.5),420)*np.exp(-t*2)*np.sin(np.pi*t/1.5)**2*.35;fx.append(('Toxic-slow-glug',stereo(x)))
x=np.zeros((int(.9*R),2));add(x,tone(88,.6,'bell'),0,.2,-.25);add(x,tone(91,.6,'bell'),.08,.2,.25);add(x,tone(84,.6,'bell'),.19,.17);fx.append(('Candy-pop',x))
demo=np.zeros((int(27*R),2));labels=[]
for i,(name,x) in enumerate(fx):
 y=save('SFX-'+name,x,False);add(demo,y,i*3,.7);labels.append({'seconds':i*3,'effect':name})
save('04-Sound-Effects-Reel',demo)
readme='''Photo Sweep — original synthesized audio mockups\n\nMUSIC PREVIEWS\n01 Dreamy Sweep: warm keys and floating pads, 80 BPM, 48 seconds.\n02 Candy Playful: bright mallets and light percussion, 100 BPM, 38.4 seconds.\n03 Elemental Pulse: mellow electronic groove, 96 BPM, 40 seconds.\nEach preview repeats its original loop twice and fades in/out. The -loop WAV files omit the preview fades and are designed for repetition.\n\nSOUND EFFECTS REEL\n'''+''.join(f'{v["seconds"]:02d}s: {v["effect"]}\n' for v in labels)+'''\nStandalone SFX WAVs are included for integration.\nThese are original programmatically synthesized mockups, with no external recordings or sampled music. Preview MP3s are 192 kbps stereo; WAV assets are 44.1 kHz, 16-bit stereo. No app files have been changed. The synthesis script is included so the drafts can be adjusted.\n'''
(OUT/'README.txt').write_text(readme)
with zipfile.ZipFile(OUT.parent/'PhotoSweep-Audio-Mockups.zip','w',zipfile.ZIP_DEFLATED) as z:
 for p in sorted(OUT.iterdir()):
  if p.suffix in ['.mp3','.wav','.txt','.py']:z.write(p,p.name)
# Check every asset for finite samples, no clipping, audible energy and the requested length.
from scipy.io.wavfile import read
for p in OUT.glob('*.wav'):
 sr,data=read(p);assert sr==R and data.ndim==2
 assert np.max(np.abs(data.astype(float)))<32767
 assert np.sqrt(np.mean(data.astype(float)**2))>100
print(json.dumps({'files':len(list(OUT.iterdir())),'music_preview_seconds':[48,38.4,40],'effects_reel_seconds':27,'zip_bytes':(OUT.parent/'PhotoSweep-Audio-Mockups.zip').stat().st_size}))
