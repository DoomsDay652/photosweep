package com.dominic.photosweep;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.os.Handler;
import android.os.Looper;
import java.io.IOException;
import java.util.function.Consumer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Local radio with one current/one prepared next track; menus never restart playback. */
final class AudioController implements AutoCloseable {
    private final Context context;
    private final AudioManager manager;
    private final AudioFocusRequest focus;
    private final SoundPool pool;
    private final Map<Integer, Integer> effects = new HashMap<>();
    private final Set<Integer> loaded = new HashSet<>();
    private final int[] streams = new int[4];
    private final float[] streamGains = new float[4];
    private int nextStream;
    private static final int[] TRACKS = {R.raw.dreamy_sweep, R.raw.paper_lantern,
            R.raw.nebula_drift, R.raw.midnight_polaroid};
    private static final String[] TITLES = {"Dreamy Sweep", "Paper Lantern", "Nebula Drift", "Midnight Polaroid"};
    private final Consumer<String> trackChanged;
    private final SharedPreferences preferences;
    private MediaPlayer music, nextMusic;
    private boolean musicPrepared, nextPrepared, announced, repeatTrack;
    private int track, restorePosition;
    private boolean foreground, enabled, focused, ducked, closed;
    private float volume, masterVolume = 1f, effectsVolume = 1f;

    AudioController(Context context, Consumer<String> trackChanged) {
        this.context = context.getApplicationContext();
        this.trackChanged = trackChanged;
        preferences = this.context.getSharedPreferences("radio", Context.MODE_PRIVATE);
        track = Math.floorMod(preferences.getInt("track", 0), TRACKS.length);
        restorePosition = Math.max(0, preferences.getInt("position", 0));
        repeatTrack = preferences.getBoolean("repeat", false);
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
        this.volume = Math.max(0, Math.min(1f, volume));
        if (!foreground || !enabled || this.volume * masterVolume == 0) {
            pausePlayer();
            abandonFocus();
            return;
        }
        if (!focused && manager != null) {
            focused = manager.requestAudioFocus(focus) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
        }
        if (focused) startPlayer();
    }

    private boolean canPlay() {
        return !closed && foreground && enabled && volume * masterVolume > 0 && focused;
    }

    private void startPlayer() {
        if (!canPlay()) return;
        try {
            if (music == null) { music = preparePlayer(track); return; }
            if (!musicPrepared) return;
            applyVolume();
            music.setLooping(repeatTrack);
            if (!music.isPlaying()) music.start();
            if (!announced) { announced = true; trackChanged.accept(currentTitle()); }
            prepareNext();
        } catch (IllegalStateException ignored) {
            releasePlayer(); abandonFocus();
        }
    }

    private MediaPlayer preparePlayer(int index) {
        MediaPlayer player = new MediaPlayer();
        player.setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build());
        player.setOnPreparedListener(ready -> {
            if (closed || (ready != music && ready != nextMusic)) { ready.release(); return; }
            if (ready == music) {
                musicPrepared = true;
                if (restorePosition > 0) {
                    ready.seekTo(Math.min(restorePosition, Math.max(0, ready.getDuration()-1)));
                    restorePosition = 0;
                }
                startPlayer();
            } else { nextPrepared = true; linkNext(); }
        });
        player.setOnCompletionListener(this::completed);
        player.setOnErrorListener((failed, what, extra) -> {
            if (failed == music) { releasePlayer(); abandonFocus(); }
            else if (failed == nextMusic) releaseNext();
            return true;
        });
        try (android.content.res.AssetFileDescriptor asset = context.getResources().openRawResourceFd(TRACKS[index])) {
            player.setDataSource(asset.getFileDescriptor(), asset.getStartOffset(), asset.getLength());
            player.prepareAsync();
            return player;
        } catch (IOException | RuntimeException error) { player.release(); return null; }
    }

    private void prepareNext() {
        if (repeatTrack || music == null || !musicPrepared) return;
        if (nextMusic == null) nextMusic = preparePlayer((track+1)%TRACKS.length);
        linkNext();
    }

    private void linkNext() {
        if (musicPrepared && nextPrepared && music != null && nextMusic != null && !repeatTrack) {
            applyVolume();
            music.setNextMediaPlayer(nextMusic);
        }
    }

    private void completed(MediaPlayer finished) {
        if (closed || finished != music || repeatTrack) return;
        finished.setNextMediaPlayer(null);
        finished.release();
        music = nextMusic; musicPrepared = nextPrepared;
        nextMusic = null; nextPrepared = false;
        track = (track+1)%TRACKS.length; announced = false; restorePosition = 0;
        if (!canPlay()) pausePlayer();
        savePosition();
        startPlayer();
    }

    String currentTitle() { return TITLES[track]; }
    boolean isRepeatTrack() { return repeatTrack; }

    void setRepeatTrack(boolean repeat) {
        if (closed) return;
        repeatTrack = repeat;
        preferences.edit().putBoolean("repeat", repeat).apply();
        if (music != null && musicPrepared) {
            music.setNextMediaPlayer(null);
            music.setLooping(repeat);
        }
        if (repeat) releaseNext(); else prepareNext();
    }

    void nextTrack() {
        if (closed) return;
        releasePlayer(); track = (track+1)%TRACKS.length;
        restorePosition = 0; announced = false;
        savePosition(); startPlayer();
    }

    private void applyVolume() {
        float level = volume * masterVolume * (ducked ? .2f : 1f);
        if (music != null && musicPrepared) music.setVolume(level, level);
        if (nextMusic != null && nextPrepared) nextMusic.setVolume(level, level);
    }

    private void savePosition() {
        int position = restorePosition;
        if (music != null && musicPrepared) try { position = music.getCurrentPosition(); }
        catch (IllegalStateException ignored) { }
        preferences.edit().putInt("track", track).putInt("position", position).apply();
    }

    private void focusChanged(int change) {
        if (closed || !foreground) return;
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

    void setMix(float master, float effects) {
        if (closed) return;
        masterVolume = Math.max(0, Math.min(1f, master));
        effectsVolume = Math.max(0, Math.min(1f, effects));
        applyVolume();
        for (int i = 0; i < streams.length; i++) {
            float level = streamGains[i] * masterVolume * effectsVolume;
            if (streams[i] != 0) pool.setVolume(streams[i], level, level);
        }
    }

    void effect(int resource, float volume) {
        if (closed || !foreground || masterVolume == 0 || effectsVolume == 0) return;
        Integer id = effects.get(resource);
        if (id != null && loaded.contains(id)) {
            streamGains[nextStream] = Math.max(0, Math.min(.4f, volume));
            float level = streamGains[nextStream] * masterVolume * effectsVolume;
            streams[nextStream] = pool.play(id, level, level, 1, 0, 1f);
            nextStream = (nextStream + 1) % streams.length;
        }
    }

    void pause() {
        foreground = false;
        pausePlayer();
        savePosition();
        abandonFocus();
        for (int i = 0; i < streams.length; i++) {
            pool.stop(streams[i]); streams[i] = 0;
        }
    }

    private void pausePlayer() {
        if (nextMusic != null && nextPrepared) try { if (nextMusic.isPlaying()) nextMusic.pause(); }
        catch (IllegalStateException ignored) { releaseNext(); }
        if (music != null && musicPrepared) try { if (music.isPlaying()) music.pause(); }
        catch (IllegalStateException ignored) { releasePlayer(); }
    }

    private void abandonFocus() {
        if (manager != null) manager.abandonAudioFocusRequest(focus);
        focused = false; ducked = false;
    }

    private void releaseNext() {
        if (music != null && musicPrepared) try { music.setNextMediaPlayer(null); }
        catch (IllegalStateException ignored) { }
        if (nextMusic != null) { nextMusic.release(); nextMusic = null; }
        nextPrepared = false;
    }

    private void releasePlayer() {
        releaseNext();
        if (music != null) { music.release(); music = null; }
        musicPrepared = false;
    }

    @Override public void close() {
        if (closed) return;
        pause(); closed = true;
        releasePlayer(); pool.release(); loaded.clear(); effects.clear();
    }
}
