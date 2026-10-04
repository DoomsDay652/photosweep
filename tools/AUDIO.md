# Original audio for Photo Sweep 0.1.1

Dreamy Sweep is the approved 24-second, 80 BPM instrumental composition.
`create_audio_mockups.py` preserves its original synthesis and composition:
run it with Python, numpy, scipy and ffmpeg to regenerate the draft WAVs.
Convert `01-Dreamy-Sweep-loop.wav` to `dreamy_sweep.ogg` with ffmpeg libvorbis,
quality 5. Use the loop WAV, not the longer preview with fades.

`create_bubbly_audio.py` regenerates the nine bundled OGG interaction cues.
All music and effects are synthesized from original waveforms without recordings
or third-party samples. Music and effects are packaged in the app and work offline.

Dreamy Sweep plays for every theme. Keep/trash, navigation, undo and finishing a
month have gentle bubbly cues. Fire, Water, Toxic and Candy add quiet accents
matching the selected swipe style. Music remains optional and defaults to off;
existing preferences are preserved. Turn it on in Options > Audio.

Check on a real phone: enable music, change menus/themes, keep/trash/undo a
photo, finish a month, toggle effects, adjust volume to zero and back, leave and
return to the app, and interrupt playback with other audio or a call. Music
must continue across menus, stop in the background and respect audio focus.
