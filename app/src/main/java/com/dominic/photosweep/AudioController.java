package com.dominic.photosweep;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.os.Handler;
import android.os.Looper;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** One reusable player and a small effect pool; navigation never restarts music. */
final class AudioController implements AutoCloseable {
    private final Context context;
    private final AudioManager manager;
    private final AudioFocusRequest focus;
    private final SoundPool pool;
    private final Map<Integer, Integer> effects = new HashMap<>();
    private final Set<Integer> loaded = new HashSet<>();
    private final int[] streams = new int[4];
    private int nextStream;
    private MediaPlayer music;
    private boolean foreground, enabled, focused, ducked, closed;
    private float volume;

    AudioController(Context context) {
        this.context = context.getApplicationContext();
        manager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        AudioAttributes musicAttributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build();
        focus = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(musicAttributes)
                .setOnAudioFocusChangeListener(this::focusChanged, new Handler(Looper.getMainLooper()))
                .build();
        pool = new SoundPool.Builder().setMaxStreams(4)
                .setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()).build();
        pool.setOnLoadCompleteListener((soundPool, soundId, status) -> {
            if (!closed && status == 0) loaded.add(soundId);
        });
        for (int resource : new int[] {R.raw.bubble_trash, R.raw.bubble_keep, R.raw.bubble_undo,
                R.raw.bubble_tap, R.raw.bubble_complete, R.raw.bubble_fire, R.raw.bubble_water,
                R.raw.bubble_toxic, R.raw.bubble_candy}) {
            effects.put(resource, pool.load(this.context, resource, 1));
        }
    }

    void resume(boolean enabled, float volume) {
        foreground = true;
        configure(enabled, volume);
    }

    void configure(boolean enabled, float volume) {
        if (closed) return;
        this.enabled = enabled;
        this.volume = Math.max(0, Math.min(.5f, volume));
        if (!foreground || !enabled || this.volume == 0) {
            pausePlayer();
            abandonFocus();
            return;
        }
        if (!focused && manager != null) {
            focused = manager.requestAudioFocus(focus) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
        }
        if (focused) startPlayer();
    }

    private void startPlayer() {
        if (closed || !foreground || !enabled || volume == 0 || !focused) return;
        try {
            if (music == null) {
                music = MediaPlayer.create(context, R.raw.dreamy_sweep);
                if (music == null) return;
                music.setLooping(true);
                music.setOnErrorListener((player, what, extra) -> {
                    releasePlayer();
                    abandonFocus();
                    return true;
                });
            }
            applyVolume();
            if (!music.isPlaying()) music.start();
        } catch (IllegalStateException ignored) {
            releasePlayer();
            abandonFocus();
        }
    }

    private void applyVolume() {
        if (music != null) {
            float level = volume * (ducked ? .2f : 1f);
            music.setVolume(level, level);
        }
    }

    private void focusChanged(int change) {
        if (closed) return;
        switch (change) {
            case AudioManager.AUDIOFOCUS_GAIN:
                focused = true; ducked = false;
                startPlayer();
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                ducked = true; applyVolume();
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                focused = false; pausePlayer();
                break;
            case AudioManager.AUDIOFOCUS_LOSS:
                focused = false; pausePlayer(); abandonFocus();
                break;
            default: break;
        }
    }

    void effect(int resource, float volume) {
        if (closed) return;
        Integer id = effects.get(resource);
        if (id != null && loaded.contains(id)) {
            float level = Math.max(0, Math.min(.4f, volume));
            streams[nextStream] = pool.play(id, level, level, 1, 0, 1f);
            nextStream = (nextStream + 1) % streams.length;
        }
    }

    void pause() {
        foreground = false;
        pausePlayer();
        abandonFocus();
        for (int i = 0; i < streams.length; i++) {
            pool.stop(streams[i]); streams[i] = 0;
        }
    }

    private void pausePlayer() {
        if (music != null) try { if (music.isPlaying()) music.pause(); }
        catch (IllegalStateException ignored) { releasePlayer(); }
    }

    private void abandonFocus() {
        if (manager != null) manager.abandonAudioFocusRequest(focus);
        focused = false; ducked = false;
    }

    private void releasePlayer() {
        if (music != null) { music.release(); music = null; }
    }

    @Override public void close() {
        if (closed) return;
        pause(); closed = true;
        releasePlayer(); pool.release(); loaded.clear(); effects.clear();
    }
}
