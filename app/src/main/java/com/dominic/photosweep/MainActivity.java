package com.dominic.photosweep;

import android.Manifest;
import android.app.Activity;
import android.app.PendingIntent;
import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.RenderEffect;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.LruCache;
import android.util.Size;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.security.MessageDigest;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    private static final int PERMISSION_REQUEST = 31;
    private static final int TRASH_REQUEST = 32;
    private static final int RESTORE_REQUEST = 33;
    private static final long SEVEN_DAYS = 7L * 24 * 60 * 60 * 1000;
    private static final int TRASH_LIMIT = 20;
    private static final String[] THEME_NAMES = {
            "Midnight · teal and navy", "Lagoon · sea glass", "Dusk · violet glow",
            "Web Hero · red and blue", "Armored Gold · crimson and gold",
            "Thunder · indigo and silver", "Gamma · emerald and violet",
            "Vibrant Shield · blue and amber", "Cosmic · plum and aqua",
            "Scarlet Magic · ruby and rose", "Stealth · charcoal and lime",
            "Nebula · violet and cyan", "Solar · orange and sky",
            "Coral · sunset pink", "Forest · moss and mint", "Royal · sapphire and gold",
            "Berry · plum and raspberry", "Citrus · tangerine and lime",
            "Candy Land · pastel sweets", "Toxic · neon ooze", "Space · starfield",
            "Fire · glowing embers", "Water · ocean currents", "Ice · crystal frost",
            "Earth · stone and leaves", "Lightning · electric sky"
    };
    private static final int[][] THEME_COLORS = {
            {0xFF0F1C2F, 0xFF23384D, 0xFF40D2BC, 0xFFFF7580, 0xFFF7CC80},
            {0xFF07343D, 0xFF14545D, 0xFF64EBD8, 0xFFFF9882, 0xFFFFE3A5},
            {0xFF231B33, 0xFF41314E, 0xFFB29BFF, 0xFFFF8492, 0xFFFFCB81},
            {0xFF142B55, 0xFF244672, 0xFFFA6570, 0xFFFFB277, 0xFF84BCFF},
            {0xFF421B29, 0xFF693144, 0xFFFFCD66, 0xFFFF8586, 0xFF8DE5DF},
            {0xFF1D244B, 0xFF333E70, 0xFFC1D7FF, 0xFFFF8B9A, 0xFFFFCB76},
            {0xFF1D2A27, 0xFF354A3B, 0xFF9EE477, 0xFFFF9AB6, 0xFFCFABF5},
            {0xFF132D45, 0xFF254961, 0xFF8EC9FF, 0xFFFF8B8C, 0xFFFFCA73},
            {0xFF241B3B, 0xFF3B305B, 0xFF8CE8E2, 0xFFFF8CA3, 0xFFD7A9FF},
            {0xFF391D32, 0xFF59314D, 0xFFFF8CB2, 0xFFFFB18E, 0xFFFFD487},
            {0xFF202831, 0xFF34434A, 0xFFB5EE74, 0xFFFF9490, 0xFFE8D88A},
            {0xFF202548, 0xFF353D69, 0xFF9CE1FF, 0xFFFF8FB4, 0xFFCCB2FF},
            {0xFF33253A, 0xFF55445D, 0xFFFFBE74, 0xFFFF8F91, 0xFF88DDF5},
            {0xFF442636, 0xFF724158, 0xFFFFA0A7, 0xFFFFD2A2, 0xFFFFE1C1},
            {0xFF142E24, 0xFF345642, 0xFF84DCA4, 0xFFFFB89A, 0xFFE5D1A4},
            {0xFF172A50, 0xFF2D4672, 0xFFFFCE75, 0xFFFF9B9B, 0xFF98C5FF},
            {0xFF382038, 0xFF663052, 0xFFFF75B5, 0xFFFFA199, 0xFFD7B0F2},
            {0xFF3D2918, 0xFF714B25, 0xFFFFA63E, 0xFFFF8F8E, 0xFFBDE85A},
            {0xFF392D4E, 0xFF6B527A, 0xFFFFBAE5, 0xFFFF91BB, 0xFFFFDC93},
            {0xFF172E29, 0xFF315746, 0xFFAEF76A, 0xFFFFA482, 0xFFD7F67A},
            {0xFF101B3B, 0xFF263458, 0xFFA8C8FF, 0xFFFF96D7, 0xFFACFFF1},
            {0xFF391E26, 0xFF683631, 0xFFFFA45F, 0xFFFF7272, 0xFFFFD176},
            {0xFF122B42, 0xFF255572, 0xFF85DFFF, 0xFFFFA9A4, 0xFFB4E9FF},
            {0xFF1D3345, 0xFF39596D, 0xFFB4EFFF, 0xFFFFA8AD, 0xFFE4F9FF},
            {0xFF2D3024, 0xFF535D3C, 0xFFC0DF86, 0xFFFFA789, 0xFFE8D596},
            {0xFF1A2143, 0xFF333B6D, 0xFFB9B8FF, 0xFFFFA3B6, 0xFFFFDF8A}
    };
    private static final int[] THEME_DISPLAY_ORDER = {
            0, 1, 2, 13, 14, 15, 16, 17,
            3, 4, 5, 6, 7, 8, 9, 10, 11, 12,
            18, 19, 20, 21, 22, 23, 24, 25
    };
    private static int requiredThemeLevel(int index) {
        if (index < 3 || (index >= 13 && index <= 17)) return 1;
        return index < 13 ? index - 1 : index - 6;
    }
    private static final int[] THEME_BACKDROP_IDS = {
            R.drawable.theme_00, R.drawable.theme_01, R.drawable.theme_02, R.drawable.theme_03,
            R.drawable.theme_04, R.drawable.theme_05, R.drawable.theme_06, R.drawable.theme_07,
            R.drawable.theme_08, R.drawable.theme_09, R.drawable.theme_10, R.drawable.theme_11,
            R.drawable.theme_12, R.drawable.theme_13, R.drawable.theme_14, R.drawable.theme_15,
            R.drawable.theme_16, R.drawable.theme_17, R.drawable.theme_18, R.drawable.theme_19,
            R.drawable.space_nebula, R.drawable.theme_21, R.drawable.theme_22, R.drawable.theme_23,
            R.drawable.theme_24, R.drawable.theme_25
    };
    private int INK = Color.rgb(237, 248, 249);
    private int MUTED = Color.rgb(170, 193, 205);
    private int BG = Color.rgb(15, 28, 47);
    private int PANEL = Color.rgb(35, 56, 77);
    private int GREEN = Color.rgb(64, 210, 188);
    private int RED = Color.rgb(255, 117, 128);
    private int GOLD = Color.rgb(247, 204, 128);
    private static final int SAMPLE_RATE = 22050;

    private static class Photo {
        long id, timestamp, size;
        Uri uri;
        String month;
        int year;
    }

    private static class TrashEntry {
        long id, trashedAt, timestamp;
        Uri uri;
        TrashEntry(long id, long trashedAt, long timestamp) {
            this.id = id; this.trashedAt = trashedAt; this.timestamp = timestamp;
            this.uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id);
        }
    }

    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final ExecutorService duplicateWorker = Executors.newSingleThreadExecutor();
    private final LruCache<Long, Bitmap> previews = new LruCache<Long, Bitmap>(24) {
        @Override protected int sizeOf(Long key, Bitmap value) { return Math.max(1, value.getByteCount() / (1024 * 1024)); }
    };
    private final ArrayList<Photo> photos = new ArrayList<>();
    private final HashSet<Long> duplicates = new HashSet<>();
    private final ArrayList<TrashEntry> trashEntries = new ArrayList<>();
    private final ArrayList<TrashEntry> evictionQueue = new ArrayList<>();
    private Set<String> reviewed = new HashSet<>();
    private Set<String> rewarded = new HashSet<>();
    private Set<String> keptIds = new HashSet<>();
    private Set<String> trashedIds = new HashSet<>();
    private LinearLayout root;
    private FrameLayout host;
    private int xp, keptCount, trashedCount, restoredCount;
    private int selectedYear = -1;
    private String selectedMonth;
    private boolean reviewing, loading, duplicateScanning;
    private boolean showingTrash, deletingOld;
    private boolean showingSettings, soundEnabled, musicEnabled, statsExpanded, swipeHintSeen, adminMode;
    private int themeChoice, musicVolume;
    private AudioTrack musicTrack;
    private Bitmap spaceBackdrop;
    private final Bitmap[] themeBackdrops = new Bitmap[THEME_NAMES.length];
    private Bitmap candySprites;
    private SensorManager sensorManager;
    private Sensor gravitySensor;
    private TextureBackdrop activeBackdrop;
    private float gravityX = 0, gravityY = 1;
    private final SensorEventListener tiltListener = new SensorEventListener() {
        @Override public void onAccuracyChanged(Sensor sensor, int accuracy) { }
        @Override public void onSensorChanged(SensorEvent event) {
            if (activeBackdrop == null || themeChoice != 18) return;
            int rotation = getWindowManager().getDefaultDisplay().getRotation();
            float x = event.values[0], y = event.values[1];
            if (rotation == android.view.Surface.ROTATION_90) { float t = x; x = -y; y = t; }
            else if (rotation == android.view.Surface.ROTATION_270) { float t = x; x = y; y = -t; }
            else if (rotation == android.view.Surface.ROTATION_180) { x = -x; y = -y; }
            // Sensor acceleration points opposite the direction a loose object falls.
            gravityX = Math.max(-1, Math.min(1, -x / 7f));
            gravityY = Math.max(-1, Math.min(1, y / 7f));
        }
    };
    private final Handler uiHandler = new Handler(Looper.getMainLooper());
    private long pendingTrash = -1;
    private long pendingRestore = -1;
    private long lastKept = -1;
    private int generation;
    private float touchX, touchY;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        if (sensorManager != null) gravitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
        themeChoice = getPreferences(MODE_PRIVATE).getInt("theme", 0);
        adminMode = getPreferences(MODE_PRIVATE).getBoolean("admin_mode", false);
        soundEnabled = getPreferences(MODE_PRIVATE).getBoolean("sound_enabled", true);
        musicEnabled = getPreferences(MODE_PRIVATE).getBoolean("music_enabled", false);
        musicVolume = getPreferences(MODE_PRIVATE).getInt("music_volume", 18);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        reviewed = new HashSet<>(getPreferences(MODE_PRIVATE).getStringSet("reviewed", Collections.emptySet()));
        rewarded = new HashSet<>(getPreferences(MODE_PRIVATE).getStringSet("rewarded", Collections.emptySet()));
        keptIds = new HashSet<>(getPreferences(MODE_PRIVATE).getStringSet("kept_ids", reviewed));
        trashedIds = new HashSet<>(getPreferences(MODE_PRIVATE).getStringSet("trashed_ids", Collections.emptySet()));
        swipeHintSeen = getPreferences(MODE_PRIVATE).getBoolean("swipe_hint_seen",
                !getPreferences(MODE_PRIVATE).getStringSet("seen_swipe_overlays", Collections.emptySet()).isEmpty());
        statsExpanded = getPreferences(MODE_PRIVATE).getBoolean("stats_expanded", false);
        xp = getPreferences(MODE_PRIVATE).getInt("xp", 0);
        applyTheme();
        keptCount = keptIds.size();
        trashedCount = trashedIds.size();
        restoredCount = getPreferences(MODE_PRIVATE).getInt("restored_count", 0);
        loadTrashEntries();
        scheduleCleanup();
        render();
        if (hasAccess()) loadPhotos();
    }

    @Override protected void onResume() {
        super.onResume();
        if (gravitySensor != null) sensorManager.registerListener(tiltListener, gravitySensor, SensorManager.SENSOR_DELAY_GAME);
        if (musicEnabled) updateMusic();
        if (root != null) {
            loadTrashEntries();
            render();
            if (hasAccess() && canManage()) cleanupTrash();
        }
    }

    @Override protected void onPause() {
        if (sensorManager != null) sensorManager.unregisterListener(tiltListener);
        stopMusic();
        super.onPause();
    }

    @Override public void onDestroy() {
        stopMusic();
        for (Bitmap bitmap : themeBackdrops) if (bitmap != null && !bitmap.isRecycled()) bitmap.recycle();
        if (candySprites != null && !candySprites.isRecycled()) candySprites.recycle();
        generation++;
        io.shutdownNow();
        duplicateWorker.shutdownNow();
        super.onDestroy();
    }

    private void applyTheme() {
        if (themeChoice < 0 || themeChoice >= THEME_COLORS.length ||
                (!adminMode && xp / 500 + 1 < requiredThemeLevel(themeChoice))) themeChoice = 0;
        for (int i = 0; i < themeBackdrops.length; i++) if (i != themeChoice && themeBackdrops[i] != null) {
            themeBackdrops[i].recycle(); themeBackdrops[i] = null;
        }
        int[] palette = THEME_COLORS[themeChoice];
        BG = palette[0]; PANEL = palette[1]; GREEN = palette[2]; RED = palette[3]; GOLD = palette[4];
        INK = Color.rgb(237, 248, 249); MUTED = Color.rgb(170, 193, 205);
        if (getWindow() != null) getWindow().setNavigationBarColor(BG);
    }

    private boolean hasAccess() {
        if (Build.VERSION.SDK_INT <= 32) return checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        return checkSelfPermission(Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED ||
                (Build.VERSION.SDK_INT >= 34 && checkSelfPermission(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED) == PackageManager.PERMISSION_GRANTED);
    }

    private boolean canManage() {
        return Build.VERSION.SDK_INT >= 31 && MediaStore.canManageMedia(this);
    }

    private void requestMediaManagement() {
        if (Build.VERSION.SDK_INT < 31) {
            Toast.makeText(this, "Prompt-free Trash needs Android 12 or newer", Toast.LENGTH_LONG).show(); return;
        }
        try {
            Intent intent = new Intent(Settings.ACTION_REQUEST_MANAGE_MEDIA);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Open Settings → Apps → Special access → Manage media", Toast.LENGTH_LONG).show();
        }
    }

    private void requestAccess() {
        if (Build.VERSION.SDK_INT >= 34) requestPermissions(new String[]{Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED}, PERMISSION_REQUEST);
        else if (Build.VERSION.SDK_INT >= 33) requestPermissions(new String[]{Manifest.permission.READ_MEDIA_IMAGES}, PERMISSION_REQUEST);
        else requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_REQUEST);
    }

    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST) {
            if (hasAccess()) loadPhotos(); else render();
        }
    }

    private void loadPhotos() {
        if (!hasAccess() || loading) return;
        loading = true;
        final int token = ++generation;
        render();
        io.execute(() -> {
            ArrayList<Photo> found = new ArrayList<>();
            String[] columns = {MediaStore.Images.Media._ID, MediaStore.Images.Media.DATE_TAKEN,
                    MediaStore.Images.Media.DATE_ADDED, MediaStore.Images.Media.SIZE};
            try (Cursor cursor = getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    columns, null, null, MediaStore.Images.Media.DATE_ADDED + " DESC")) {
                if (cursor != null) {
                    while (cursor.moveToNext()) {
                        Photo p = new Photo();
                        p.id = cursor.getLong(0);
                        p.timestamp = cursor.getLong(1);
                        if (p.timestamp <= 0) p.timestamp = cursor.getLong(2) * 1000L;
                        p.size = cursor.getLong(3);
                        p.uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, p.id);
                        Date date = new Date(p.timestamp);
                        p.year = Integer.parseInt(new SimpleDateFormat("yyyy", Locale.US).format(date));
                        p.month = new SimpleDateFormat("yyyy-MM", Locale.US).format(date);
                        found.add(p);
                    }
                }
            } catch (Exception ignored) { }
            found.sort((a, b) -> Long.compare(b.timestamp, a.timestamp));
            runOnUiThread(() -> {
                if (token != generation || isDestroyed()) return;
                photos.clear(); photos.addAll(found);
                duplicates.clear(); loading = false;
                render();
                if (canManage()) cleanupTrash();
                scanDuplicates(new ArrayList<>(found), token);
            });
        });
    }

    private void scanDuplicates(List<Photo> snapshot, int token) {
        duplicateScanning = true;
        if (reviewing) render();
        duplicateWorker.execute(() -> {
            Map<Long, ArrayList<Photo>> sizes = new HashMap<>();
            for (Photo p : snapshot) if (p.size > 0) sizes.computeIfAbsent(p.size, k -> new ArrayList<>()).add(p);
            HashSet<Long> found = new HashSet<>();
            byte[] buffer = new byte[64 * 1024];
            for (ArrayList<Photo> group : sizes.values()) {
                if (token != generation || Thread.currentThread().isInterrupted()) return;
                if (group.size() < 2) continue;
                Map<String, ArrayList<Long>> hashes = new HashMap<>();
                for (Photo p : group) {
                    if (token != generation || Thread.currentThread().isInterrupted()) return;
                    try (InputStream stream = getContentResolver().openInputStream(p.uri)) {
                        if (stream == null) continue;
                        MessageDigest digest = MessageDigest.getInstance("SHA-256");
                        int count;
                        while ((count = stream.read(buffer)) != -1) digest.update(buffer, 0, count);
                        String key = Arrays.toString(digest.digest());
                        hashes.computeIfAbsent(key, k -> new ArrayList<>()).add(p.id);
                    } catch (Exception ignored) { }
                }
                for (ArrayList<Long> matches : hashes.values()) if (matches.size() > 1) found.addAll(matches);
            }
            runOnUiThread(() -> {
                if (token != generation || isDestroyed()) return;
                duplicates.clear(); duplicates.addAll(found);
                duplicateScanning = false;
                render();
            });
        });
    }

    private void render() {
        host = new FrameLayout(this);
        activeBackdrop = new TextureBackdrop();
        host.addView(activeBackdrop, new FrameLayout.LayoutParams(-1, -1));
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(22), dp(20), dp(22), dp(16));
        host.addView(root, new FrameLayout.LayoutParams(-1, -1));
        setContentView(host);
        if (!hasAccess()) { intro(); return; }
        if (loading) { heading("Photo Sweep", "Gathering your photos…"); return; }
        if (showingSettings) { settingsScreen(); return; }
        if (showingTrash) { trashScreen(); return; }
        if (reviewing && selectedMonth != null) { reviewScreen(); return; }
        if (selectedYear != -1) { monthsScreen(); return; }
        yearsScreen();
    }

    private void intro() {
        spacer(60);
        label("✦", 56, GREEN, true);
        spacer(26);
        label("Photo Sweep", 36, INK, true);
        spacer(10);
        label("A calmer way to clear your camera roll.", 18, MUTED, false);
        spacer(34);
        label("Browse by year and month. Swipe right to keep, left to move to Trash. Exact copies get a Duplicate bubble.", 16, INK, false);
        spacer(32);
        button(root, "Choose photo access", GREEN, Color.WHITE, this::requestAccess);
        spacer(18);
        label("To skip a confirmation on every swipe, enable Manage media once after photo access.", 13, MUTED, false);
    }

    private void heading(String title, String subtitle) {
        TextView eyebrow = label("✦  PHOTO SWEEP", 12, GREEN, true);
        eyebrow.setLetterSpacing(.16f);
        spacer(9);
        label(title, 32, INK, true);
        spacer(5);
        label(subtitle, 15, MUTED, false);
        spacer(21);
    }

    private void yearsScreen() {
        LinearLayout top = new LinearLayout(this); top.setGravity(Gravity.CENTER_VERTICAL);
        root.addView(top, new LinearLayout.LayoutParams(-1, dp(57)));
        TextView brand = new TextView(this); brand.setText("PHOTO SWEEP"); brand.setLetterSpacing(.13f);
        brand.setTextColor(GREEN); brand.setTextSize(15); brand.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        top.addView(brand, new LinearLayout.LayoutParams(0, -2, 1));
        TextView gear = new TextView(this); gear.setText("⚙"); gear.setTextSize(28); gear.setTextColor(INK);
        gear.setGravity(Gravity.CENTER); gear.setContentDescription("Options and sound settings");
        gear.setBackground(themeButton(PANEL, 16)); top.addView(gear, new LinearLayout.LayoutParams(dp(52), dp(52)));
        gear.setOnClickListener(v -> { showingSettings = true; render(); });
        label("Your photos", 28, INK, true); spacer(13);
        LinearLayout statsToggle = new LinearLayout(this); statsToggle.setGravity(Gravity.CENTER_VERTICAL);
        statsToggle.setPadding(dp(16), dp(8), dp(16), dp(8)); statsToggle.setBackground(rounded(PANEL, 17));
        root.addView(statsToggle, new LinearLayout.LayoutParams(-1, dp(54)));
        TextView progressTitle = new TextView(this);
        progressTitle.setText("✦  Level " + (xp / 500 + 1) + "  ·  " + (xp % 500) + "/500 XP");
        progressTitle.setTextColor(INK); progressTitle.setTextSize(16); progressTitle.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        statsToggle.addView(progressTitle, new LinearLayout.LayoutParams(0, -2, 1));
        TextView chevron = new TextView(this); chevron.setText(statsExpanded ? "Stats  ▴" : "Stats  ▾");
        chevron.setTextColor(GREEN); chevron.setTextSize(14); statsToggle.addView(chevron);
        statsToggle.setOnClickListener(v -> {
            statsExpanded = !statsExpanded;
            getPreferences(MODE_PRIVATE).edit().putBoolean("stats_expanded", statsExpanded).apply(); render();
        });
        if (statsExpanded) {
            spacer(9); levelPanel(); spacer(10);
            LinearLayout stats = new LinearLayout(this);
            root.addView(stats, new LinearLayout.LayoutParams(-1, dp(80)));
            statTile(stats, Integer.toString(keptCount), "Kept", GREEN);
            statTile(stats, Integer.toString(trashedCount), "Trashed", RED);
            statTile(stats, Integer.toString(restoredCount), "Restored", GOLD);
        }
        spacer(16);
        label("Browse by year", 21, INK, true);
        spacer(12);
        if (!canManage()) {
            label("Enable one-time media access to swipe to Trash without repeated prompts.", 14, MUTED, false);
            spacer(8);
            button(root, "Enable prompt-free Trash", PANEL, INK, this::requestMediaManagement);
            spacer(16);
        }
        LinkedHashMap<Integer, int[]> years = new LinkedHashMap<>();
        for (Photo p : photos) {
            int[] counts = years.computeIfAbsent(p.year, k -> new int[2]);
            counts[0]++;
            if (!reviewed.contains(Long.toString(p.id))) counts[1]++;
        }
        if (years.isEmpty()) { label("No photos are available. Check your photo access.", 17, MUTED, false); }
        ScrollView scroll = new ScrollView(this);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        LinearLayout list = new LinearLayout(this); list.setOrientation(LinearLayout.VERTICAL); scroll.addView(list);
        for (Map.Entry<Integer, int[]> entry : years.entrySet()) {
            int year = entry.getKey(); int[] count = entry.getValue();
            tile(list, Integer.toString(year), count[0] + " photos  •  " + count[1] + " to review", () -> { selectedYear = year; render(); });
        }
        button(root, "Recently trashed  ·  " + trashEntries.size() + "/20", PANEL, GOLD, () -> { showingTrash = true; render(); });
    }

    private void levelPanel() {
        int level = xp / 500 + 1;
        LinearLayout box = new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(19), dp(18), dp(19), dp(17)); box.setBackground(rounded(PANEL, 24));
        root.addView(box, new LinearLayout.LayoutParams(-1, dp(170)));
        LinearLayout levelRow = new LinearLayout(this); levelRow.setGravity(Gravity.CENTER_VERTICAL); box.addView(levelRow);
        TextView medal = new TextView(this); medal.setText(String.format(Locale.US, "%02d", level)); medal.setTextColor(BG);
        medal.setTextSize(25); medal.setGravity(Gravity.CENTER); medal.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        medal.setBackground(rounded(GREEN, 48)); levelRow.addView(medal, new LinearLayout.LayoutParams(dp(66), dp(66)));
        LinearLayout names = new LinearLayout(this); names.setOrientation(LinearLayout.VERTICAL); names.setPadding(dp(15), 0, 0, 0);
        levelRow.addView(names);
        TextView label = new TextView(this); label.setText("LEVEL " + level); label.setTextColor(GREEN);
        label.setTextSize(14); label.setTypeface(Typeface.DEFAULT, Typeface.BOLD); names.addView(label);
        TextView name = new TextView(this); name.setText(level < 3 ? "Photo Explorer" : level < 7 ? "Photo Pathfinder" : "Gallery Guardian");
        name.setTextColor(INK); name.setTextSize(19); name.setTypeface(Typeface.DEFAULT, Typeface.BOLD); names.addView(name);
        TextView points = new TextView(this); points.setText((xp % 500) + " / 500 XP to next level");
        points.setTextColor(INK); points.setTextSize(15);
        LinearLayout.LayoutParams pointsLp = new LinearLayout.LayoutParams(-1, -2); pointsLp.topMargin = dp(12); box.addView(points, pointsLp);
        FrameLayout bar = new FrameLayout(this);
        LinearLayout.LayoutParams barLp = new LinearLayout.LayoutParams(-1, dp(12)); barLp.topMargin = dp(11); box.addView(bar, barLp);
        View track = new View(this); track.setBackground(rounded(BG, 8)); bar.addView(track, new FrameLayout.LayoutParams(-1, -1));
        View fill = new View(this); fill.setBackground(rounded(GREEN, 8)); bar.addView(fill, new FrameLayout.LayoutParams(0, -1));
        bar.post(() -> { FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) fill.getLayoutParams();
            lp.width = Math.max(dp(3), bar.getWidth() * (xp % 500) / 500); fill.setLayoutParams(lp); });
    }

    private void statTile(LinearLayout parent, String count, String title, int accent) {
        LinearLayout box = new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(12), dp(10), dp(6), dp(8)); box.setBackground(rounded(PANEL, 18));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -1, 1); lp.rightMargin = dp(7); parent.addView(box, lp);
        TextView number = new TextView(this); number.setText(count); number.setTextColor(accent); number.setTextSize(25); number.setTypeface(Typeface.DEFAULT, Typeface.BOLD); box.addView(number);
        TextView label = new TextView(this); label.setText(title); label.setTextColor(MUTED); label.setTextSize(13); box.addView(label);
    }

    private void settingsScreen() {
        back("Photo Sweep", () -> { showingSettings = false; render(); });
        heading("Options", "Make each sweep feel like yours");
        ScrollView scroll = new ScrollView(this); root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        LinearLayout list = new LinearLayout(this); list.setOrientation(LinearLayout.VERTICAL); scroll.addView(list);
        sectionTitle(list, "TESTING");
        settingSwitch(list, "Admin mode", "Preview all themes without earning XP or reviewing photos", adminMode, value -> {
            if (value) getPreferences(MODE_PRIVATE).edit().putInt("theme_before_admin", themeChoice).apply();
            adminMode = value;
            getPreferences(MODE_PRIVATE).edit().putBoolean("admin_mode", value).apply();
            if (!value && xp / 500 + 1 < requiredThemeLevel(themeChoice)) {
                themeChoice = getPreferences(MODE_PRIVATE).getInt("theme_before_admin", 0);
                getPreferences(MODE_PRIVATE).edit().putInt("theme", themeChoice).apply();
            }
            applyTheme(); render();
        });
        sectionTitle(list, "THEMES");
        int level = xp / 500 + 1;
        for (int position = 0; position < THEME_DISPLAY_ORDER.length; position++) {
            if (position == 0) sectionTitle(list, "BASIC COLORS");
            else if (position == 8) sectionTitle(list, "HERO COLORS");
            else if (position == 18) sectionTitle(list, "WORLDS & ELEMENTS");
            final int choice = THEME_DISPLAY_ORDER[position];
            int requiredLevel = requiredThemeLevel(choice);
            boolean unlocked = adminMode || level >= requiredLevel;
            themeTile(list, choice, unlocked, requiredLevel, () -> {
                if (!unlocked) return;
                themeChoice = choice; getPreferences(MODE_PRIVATE).edit().putInt("theme", choice).apply();
                applyTheme(); render();
            });
        }
        sectionTitle(list, "AUDIO");
        settingSwitch(list, "Swipe sounds", "Coin chime for Keep, soft sweep for Trash", soundEnabled, value -> {
            soundEnabled = value; getPreferences(MODE_PRIVATE).edit().putBoolean("sound_enabled", value).apply();
            if (value) playEffect(true);
        });
        settingSwitch(list, "Gentle music", "A quiet loop while the app is open", musicEnabled, value -> {
            musicEnabled = value; getPreferences(MODE_PRIVATE).edit().putBoolean("music_enabled", value).apply(); updateMusic();
        });
        TextView volume = new TextView(this); volume.setText("Music volume  ·  " + musicVolume + "%");
        volume.setTextColor(INK); volume.setTextSize(16);
        LinearLayout.LayoutParams volumeLp = new LinearLayout.LayoutParams(-1, -2); volumeLp.topMargin = dp(17); list.addView(volume, volumeLp);
        SeekBar slider = new SeekBar(this); slider.setMax(50); slider.setProgress(musicVolume);
        slider.setProgressTintList(android.content.res.ColorStateList.valueOf(GREEN));
        list.addView(slider, new LinearLayout.LayoutParams(-1, dp(52)));
        slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar bar, int value, boolean user) {
                if (!user) return;
                musicVolume = value; volume.setText("Music volume  ·  " + value + "%");
                getPreferences(MODE_PRIVATE).edit().putInt("music_volume", value).apply();
                if (musicTrack != null) musicTrack.setVolume(value / 100f);
            }
            @Override public void onStartTrackingTouch(SeekBar bar) { }
            @Override public void onStopTrackingTouch(SeekBar bar) { }
        });
        TextView note = new TextView(this); note.setText("Sounds use your phone's media volume. Music stops when you leave Photo Sweep.");
        note.setTextColor(MUTED); note.setTextSize(13); list.addView(note);
    }

    private void sectionTitle(LinearLayout parent, String title) {
        TextView label = new TextView(this); label.setText(title); label.setTextColor(GREEN);
        label.setLetterSpacing(.12f); label.setTextSize(13); label.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2); lp.topMargin = dp(18); lp.bottomMargin = dp(12); parent.addView(label, lp);
    }

    private void themeTile(LinearLayout parent, int index, boolean unlocked, int requiredLevel, Runnable action) {
        LinearLayout row = new LinearLayout(this); row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(14), dp(8), dp(14), dp(8)); row.setBackground(rounded(PANEL, 16));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(70)); lp.bottomMargin = dp(8); parent.addView(row, lp);
        LinearLayout copy = new LinearLayout(this); copy.setOrientation(LinearLayout.VERTICAL);
        row.addView(copy, new LinearLayout.LayoutParams(0, -2, 1));
        TextView name = new TextView(this); name.setText((themeChoice == index ? "✓  " : "") + THEME_NAMES[index]);
        name.setTextSize(16); name.setTextColor(INK); name.setTypeface(Typeface.DEFAULT, Typeface.BOLD); copy.addView(name);
        TextView detail = new TextView(this); detail.setText(unlocked ? "Available" : "Unlock at level " + requiredLevel);
        detail.setTextSize(12); detail.setTextColor(unlocked ? GREEN : MUTED); copy.addView(detail);
        if (unlocked) {
            for (int color : new int[]{THEME_COLORS[index][2], THEME_COLORS[index][3], THEME_COLORS[index][4]}) {
                View swatch = new View(this); swatch.setBackground(rounded(color, 15));
                LinearLayout.LayoutParams circle = new LinearLayout.LayoutParams(dp(13), dp(13));
                circle.leftMargin = dp(3); row.addView(swatch, circle);
            }
        } else {
            TextView lock = new TextView(this); lock.setText("🔒"); lock.setTextSize(16); row.addView(lock);
        }
        row.setOnClickListener(v -> action.run());
    }

    private interface ToggleAction { void changed(boolean enabled); }
    private void settingSwitch(LinearLayout parent, String title, String detail, boolean checked, ToggleAction action) {
        LinearLayout row = new LinearLayout(this); row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(17), dp(13), dp(15), dp(13)); row.setBackground(rounded(PANEL, 18));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(88)); lp.bottomMargin = dp(10); parent.addView(row, lp);
        LinearLayout copy = new LinearLayout(this); copy.setOrientation(LinearLayout.VERTICAL); row.addView(copy, new LinearLayout.LayoutParams(0, -2, 1));
        TextView name = new TextView(this); name.setText(title); name.setTextSize(17); name.setTextColor(INK); name.setTypeface(Typeface.DEFAULT, Typeface.BOLD); copy.addView(name);
        TextView sub = new TextView(this); sub.setText(detail); sub.setTextSize(12); sub.setTextColor(MUTED); copy.addView(sub);
        Switch toggle = new Switch(this); toggle.setChecked(checked); toggle.setThumbTintList(android.content.res.ColorStateList.valueOf(checked ? GREEN : MUTED));
        row.addView(toggle); toggle.setOnCheckedChangeListener((button, value) -> {
            toggle.setThumbTintList(android.content.res.ColorStateList.valueOf(value ? GREEN : MUTED)); action.changed(value);
        });
        row.setOnClickListener(v -> toggle.setChecked(!toggle.isChecked()));
    }

    private void monthsScreen() {
        back("All years", () -> { selectedYear = -1; selectedMonth = null; render(); });
        heading(Integer.toString(selectedYear), "Choose a month");
        LinkedHashMap<String, int[]> months = new LinkedHashMap<>();
        for (Photo p : photos) if (p.year == selectedYear) {
            int[] counts = months.computeIfAbsent(p.month, k -> new int[2]);
            counts[0]++; if (!reviewed.contains(Long.toString(p.id))) counts[1]++;
        }
        ScrollView scroll = new ScrollView(this); root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        LinearLayout list = new LinearLayout(this); list.setOrientation(LinearLayout.VERTICAL); scroll.addView(list);
        for (Map.Entry<String, int[]> entry : months.entrySet()) {
            String month = entry.getKey(); int[] count = entry.getValue();
            String name = new SimpleDateFormat("MMMM", Locale.getDefault()).format(new Date(selectedYear - 1900, Integer.parseInt(month.substring(5)) - 1, 1));
            tile(list, name, count[0] + " photos  •  " + count[1] + " to review", () -> { selectedMonth = month; lastKept = -1; reviewing = true; render(); });
        }
    }

    private List<Photo> monthPhotos() {
        ArrayList<Photo> result = new ArrayList<>();
        for (Photo p : photos) if (p.month.equals(selectedMonth)) result.add(p);
        return result;
    }

    private void trashScreen() {
        back("Photo Sweep", () -> { showingTrash = false; render(); });
        heading("Recently trashed", "Last 20 photos · up to 7 days");
        if (trashEntries.isEmpty()) {
            spacer(36); label("Trash is empty", 21, INK, true);
            return;
        }
        ScrollView scroll = new ScrollView(this);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        LinearLayout list = new LinearLayout(this); list.setOrientation(LinearLayout.VERTICAL); scroll.addView(list);
        for (TrashEntry entry : new ArrayList<>(trashEntries)) {
            LinearLayout row = new LinearLayout(this); row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(10), dp(10), dp(10), dp(10)); row.setBackground(rounded(PANEL, 20));
            LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(-1, dp(100)); rowLp.bottomMargin = dp(10); list.addView(row, rowLp);
            ImageView thumbnail = new ImageView(this); thumbnail.setScaleType(ImageView.ScaleType.CENTER_CROP);
            thumbnail.setBackground(rounded(PANEL, 12)); row.addView(thumbnail, new LinearLayout.LayoutParams(dp(80), dp(80)));
            loadPreview(entry.id, entry.uri, thumbnail);
            LinearLayout info = new LinearLayout(this); info.setOrientation(LinearLayout.VERTICAL);
            info.setPadding(dp(12), 0, dp(4), 0); row.addView(info, new LinearLayout.LayoutParams(0, -2, 1));
            TextView title = new TextView(this); title.setText(DateFormat.getDateInstance(DateFormat.MEDIUM).format(new Date(entry.timestamp)));
            title.setTextColor(INK); title.setTypeface(Typeface.DEFAULT, Typeface.BOLD); title.setTextSize(14); info.addView(title);
            TextView days = new TextView(this);
            long timeLeft = Math.max(0, SEVEN_DAYS - (System.currentTimeMillis() - entry.trashedAt));
            days.setText(Math.max(1, (timeLeft + 86_399_999) / 86_400_000) + " days left");
            days.setTextColor(MUTED); days.setTextSize(12); info.addView(days);
            Button restore = new Button(this); restore.setText("Restore"); restore.setAllCaps(false);
            restore.setTextColor(INK); restore.setTextSize(13); restore.setBackground(themeButton(PANEL, 12));
            restore.setOnClickListener(v -> restore(entry)); row.addView(restore, new LinearLayout.LayoutParams(dp(90), dp(46)));
        }
    }

    private void reviewScreen() {
        back("Months", () -> { reviewing = false; render(); });
        List<Photo> month = monthPhotos();
        Photo current = null; int remaining = 0;
        for (Photo p : month) if (!reviewed.contains(Long.toString(p.id))) { remaining++; if (current == null) current = p; }
        String monthName = new SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(new Date(selectedYear - 1900, Integer.parseInt(selectedMonth.substring(5)) - 1, 1));
        heading(monthName, remaining + " of " + month.size() + " left to review");
        View track = new View(this); track.setBackground(rounded(PANEL, 4));
        root.addView(track, new LinearLayout.LayoutParams(-1, dp(5)));
        View fill = new View(this); fill.setBackground(rounded(GREEN, 4));
        FrameLayout progress = new FrameLayout(this);
        root.removeView(track); progress.addView(track, new FrameLayout.LayoutParams(-1, dp(5)));
        FrameLayout.LayoutParams progressFill = new FrameLayout.LayoutParams(-1, dp(5));
        progress.addView(fill, progressFill);
        LinearLayout.LayoutParams progressLayout = new LinearLayout.LayoutParams(-1, dp(5));
        progressLayout.bottomMargin = dp(17); root.addView(progress, progressLayout);
        final int remainingCount = remaining;
        progress.post(() -> { FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) fill.getLayoutParams();
            lp.width = (int) (progress.getWidth() * (month.size() == 0 ? 0 : (month.size() - remainingCount) / (float) month.size()));
            fill.setLayoutParams(lp); });
        if (current == null) {
            spacer(65); label("All caught up ✨", 28, INK, true); spacer(12);
            label("This month is clear. Kept photos are still in your gallery.", 16, MUTED, false);
            if (lastKept != -1) {
                spacer(20);
                button(root, "Undo last keep", PANEL, GREEN, () -> {
                    reviewed.remove(Long.toString(lastKept)); lastKept = -1; saveReviewed(); render();
                });
            }
            spacer(30);
            button(root, "Review this month again", GREEN, Color.WHITE, () -> {
                for (Photo p : month) reviewed.remove(Long.toString(p.id)); lastKept = -1; saveReviewed(); render();
            });
            return;
        }
        Photo shown = current;
        FrameLayout stage = new FrameLayout(this);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(-1, 0, 1);
        cardParams.bottomMargin = dp(20); root.addView(stage, cardParams);
        FrameLayout card = new FrameLayout(this);
        card.setBackground(rounded(PANEL, 25)); card.setElevation(dp(8));
        card.setClipToOutline(true);
        stage.addView(card, new FrameLayout.LayoutParams(-1, -1));
        ImageView backdrop = new ImageView(this); backdrop.setScaleType(ImageView.ScaleType.CENTER_CROP);
        backdrop.setAlpha(.55f);
        if (Build.VERSION.SDK_INT >= 31) backdrop.setRenderEffect(RenderEffect.createBlurEffect(dp(20), dp(20), Shader.TileMode.CLAMP));
        card.addView(backdrop, new FrameLayout.LayoutParams(-1, -1));
        ImageView photo = new ImageView(this); photo.setScaleType(ImageView.ScaleType.FIT_CENTER);
        card.addView(photo, new FrameLayout.LayoutParams(-1, -1));
        loadPreview(shown, photo);
        loadPreview(shown, backdrop);
        SwipeEffect effect = new SwipeEffect();
        card.addView(effect, new FrameLayout.LayoutParams(-1, -1));
        if (duplicates.contains(shown.id)) {
            TextView bubble = pill("✦ Duplicate", GOLD, Color.rgb(89, 64, 27));
            FrameLayout.LayoutParams badge = new FrameLayout.LayoutParams(-2, -2, Gravity.TOP | Gravity.END);
            badge.setMargins(dp(12), dp(12), dp(12), 0); card.addView(bubble, badge);
        }
        TextView date = pill(DateFormat.getDateInstance(DateFormat.MEDIUM).format(new Date(shown.timestamp)), Color.rgb(33, 57, 75), INK);
        FrameLayout.LayoutParams dateParams = new FrameLayout.LayoutParams(-2, -2, Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        dateParams.topMargin = dp(18); card.addView(date, dateParams);
        LinearLayout overlay = new LinearLayout(this); overlay.setGravity(Gravity.CENTER);
        overlay.setClickable(false);
        FrameLayout.LayoutParams overlayLp = new FrameLayout.LayoutParams(-1, dp(64), Gravity.CENTER);
        overlayLp.setMargins(dp(22), 0, dp(22), 0); card.addView(overlay, overlayLp);
        TextView trashAction = swipeOverlayLabel("←  Trash", RED);
        TextView keepAction = swipeOverlayLabel("Keep  →", GREEN);
        overlay.addView(trashAction, new LinearLayout.LayoutParams(0, -2, 1));
        overlay.addView(keepAction, new LinearLayout.LayoutParams(0, -2, 1));
        if (!swipeHintSeen) {
            swipeHintSeen = true;
            getPreferences(MODE_PRIVATE).edit().putBoolean("swipe_hint_seen", true)
                    .remove("seen_swipe_overlays").apply();
            overlay.setAlpha(1f);
            uiHandler.postDelayed(() -> overlay.animate().alpha(0f).setDuration(400)
                    .withEndAction(() -> overlay.setVisibility(View.GONE)).start(), 2300);
        } else overlay.setVisibility(View.GONE);
        card.setOnTouchListener((v, e) -> {
            if (e.getAction() == MotionEvent.ACTION_DOWN) {
                touchX = e.getRawX(); touchY = e.getRawY();
                return true;
            }
            if (e.getAction() == MotionEvent.ACTION_MOVE) {
                float dx = e.getRawX() - touchX;
                card.setTranslationX(dx); card.setRotation(Math.max(-13, Math.min(13, dx / dp(28))));
                effect.progress = Math.max(-1f, Math.min(1f, dx / (card.getWidth() * .62f)));
                effect.invalidate();
                trashAction.setAlpha(dx < -dp(12) ? 1f : .45f);
                keepAction.setAlpha(dx > dp(12) ? 1f : .45f);
                return true;
            }
            if (e.getAction() == MotionEvent.ACTION_UP) {
                float dx = e.getRawX() - touchX;
                if (Math.abs(dx) > dp(85) && Math.abs(dx) > Math.abs(e.getRawY() - touchY) * 1.2f) {
                    if (dx < 0 && !canManage()) {
                        card.animate().translationX(0).rotation(0).setDuration(210).start();
                        effect.progress = 0; effect.invalidate(); requestMediaManagement();
                    } else {
                        effect.progress = dx > 0 ? 1 : -1; effect.invalidate();
                        card.animate().translationX((dx > 0 ? 1 : -1) * stage.getWidth() * 1.2f)
                                .rotation(dx > 0 ? 16 : -16).alpha(0).setDuration(260)
                                .withEndAction(() -> { if (dx > 0) keep(shown); else trash(shown); }).start();
                    }
                } else {
                    effect.progress = 0; effect.invalidate();
                    card.animate().translationX(0).rotation(0).setDuration(200).start();
                }
                return true;
            }
            if (e.getAction() == MotionEvent.ACTION_CANCEL) {
                effect.progress = 0; effect.invalidate();
                card.animate().translationX(0).rotation(0).setDuration(200).start();
                return true;
            }
            return true;
        });
        if (lastKept != -1) {
            TextView undo = label("Undo last keep", 14, GREEN, true);
            undo.setPadding(0, dp(9), 0, 0);
            undo.setOnClickListener(v -> { reviewed.remove(Long.toString(lastKept)); lastKept = -1; saveReviewed(); render(); });
        }
        if (duplicateScanning) { spacer(6); label("Checking for exact duplicates…", 12, MUTED, false); }
    }

    private void loadPreview(Photo p, ImageView view) {
        loadPreview(p.id, p.uri, view);
    }

    private void loadPreview(long id, Uri uri, ImageView view) {
        Bitmap cached = previews.get(id);
        if (cached != null) { view.setImageBitmap(cached); return; }
        io.execute(() -> {
            try {
                Bitmap bitmap = getContentResolver().loadThumbnail(uri, new Size(1200, 1200), null);
                if (bitmap != null) {
                    previews.put(id, bitmap);
                    runOnUiThread(() -> { if (!isDestroyed() && view.getParent() != null) view.setImageBitmap(bitmap); });
                }
            } catch (Exception ignored) { }
        });
    }

    private class SwipeEffect extends View {
        final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        float progress;
        SwipeEffect() { super(MainActivity.this); setClickable(false); }
        @Override protected void onDraw(Canvas canvas) {
            float w = getWidth(), h = getHeight(), amount = Math.abs(progress);
            if (amount < .01f || w <= 0) return;
            boolean keep = progress > 0;
            int accent = keep ? GREEN : RED;
            int style = themeChoice < 18 ? themeChoice % 5 : themeChoice - 17;
            float edge = keep ? w * (1 - amount * .78f) : w * amount * .78f;
            float left = keep ? edge : 0, right = keep ? w : edge;
            int haze = Color.argb((int)(110 * amount), Color.red(accent), Color.green(accent), Color.blue(accent));
            paint.setShader(new LinearGradient(left, 0, right + 1, 0,
                    keep ? new int[]{Color.TRANSPARENT, haze} : new int[]{haze, Color.TRANSPARENT},
                    null, Shader.TileMode.CLAMP));
            canvas.drawRect(left, 0, right, h, paint); paint.setShader(null);
            paint.setColor(Color.argb((int)(210 * amount), Color.red(accent), Color.green(accent), Color.blue(accent)));
            canvas.drawRect(edge - dp(2), 0, edge + dp(2), h, paint);
            for (int i = 0; i < 36; i++) {
                float x = left + (right - left) * ((i * 37 % 41) / 41f);
                float y = h * ((i * 23 % 37) / 37f);
                float size = dp(2 + i % 5) * (.35f + amount);
                int color = (style == 1 && i % 3 == 0) ? GOLD : (style == 3 && i % 3 == 0) ? Color.WHITE : accent;
                paint.setColor(Color.argb((int)((90 + i % 4 * 34) * amount),
                        Color.red(color), Color.green(color), Color.blue(color)));
                if (style == 1 || style == 5) { // candy drops or water bubbles
                    paint.setStyle(style == 5 ? Paint.Style.STROKE : Paint.Style.FILL);
                    paint.setStrokeWidth(dp(1.5f)); canvas.drawCircle(x, y, size * (style == 1 ? 1.8f : 2.2f), paint);
                    paint.setStyle(Paint.Style.FILL);
                } else if (style == 2) { // toxic drips
                    canvas.drawCircle(x, y, size * 1.3f, paint);
                    canvas.drawRoundRect(x - size * .4f, y, x + size * .4f, y + size * 4, size, size, paint);
                } else if (style == 3) { // shooting stars
                    canvas.drawCircle(x, y, size * .7f, paint);
                    paint.setStrokeWidth(dp(1)); canvas.drawLine(x, y, x + (keep ? -1 : 1) * size * 6, y + size, paint);
                } else if (style == 4) { // fire sparks
                    Path flame = new Path(); flame.moveTo(x, y - size * 3); flame.quadTo(x + size * 2, y, x, y + size);
                    flame.quadTo(x - size * 2, y, x, y - size * 3); canvas.drawPath(flame, paint);
                } else if (style == 6) { // ice crystals
                    Path shard = new Path(); shard.moveTo(x, y - size * 3); shard.lineTo(x + size, y);
                    shard.lineTo(x, y + size * 3); shard.lineTo(x - size, y); shard.close(); canvas.drawPath(shard, paint);
                } else if (style == 7) { // leaves and earth flecks
                    canvas.drawOval(x - size, y - size * 2, x + size, y + size * 2, paint);
                    paint.setStrokeWidth(dp(1)); canvas.drawLine(x, y - size * 2, x, y + size * 2, paint);
                } else if (style == 8) { // electric bolts
                    Path bolt = new Path(); bolt.moveTo(x, y - size * 3); bolt.lineTo(x + size, y - size);
                    bolt.lineTo(x - size * .4f, y); bolt.lineTo(x + size * .7f, y + size * 2);
                    paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(dp(2)); canvas.drawPath(bolt, paint);
                    paint.setStyle(Paint.Style.FILL);
                } else { // sparkles for simple palettes
                    canvas.drawCircle(x, y, size * .5f, paint);
                    paint.setStrokeWidth(dp(1)); canvas.drawLine(x - size * 2, y, x + size * 2, y, paint);
                    canvas.drawLine(x, y - size * 2, x, y + size * 2, paint);
                }
            }
        }
    }

    private void keep(Photo p) {
        if (pendingTrash != -1) return;
        playEffect(true);
        lastKept = p.id;
        if (keptIds.add(Long.toString(p.id))) keptCount++;
        int earned = awardXp(p.id, 10);
        saveStats();
        reviewed.add(Long.toString(p.id)); saveReviewed(); render();
        if (earned > 0) floatXp(earned);
    }

    private int awardXp(long id, int points) {
        if (!rewarded.add(Long.toString(id))) return 0;
        xp += points; saveStats(); return points;
    }

    private void saveStats() {
        getPreferences(MODE_PRIVATE).edit()
                .putInt("xp", xp).putInt("restored_count", restoredCount)
                .putStringSet("rewarded", new HashSet<>(rewarded))
                .putStringSet("kept_ids", new HashSet<>(keptIds))
                .putStringSet("trashed_ids", new HashSet<>(trashedIds)).apply();
    }

    private void floatXp(int gained) {
        if (host == null) return;
        final FrameLayout currentHost = host;
        TextView badge = pill("✦  +" + gained + " XP", GREEN, Color.rgb(11, 47, 52));
        badge.setElevation(dp(12));
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(-2, -2, Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        lp.topMargin = dp(145); currentHost.addView(badge, lp);
        badge.setTranslationY(dp(26)); badge.setAlpha(0);
        badge.animate().translationY(0).alpha(1).setDuration(260)
                .withEndAction(() -> badge.animate().translationY(-dp(48)).alpha(0).setStartDelay(750)
                        .setDuration(500).withEndAction(() -> currentHost.removeView(badge)).start()).start();
    }

    private void trash(Photo p) {
        if (pendingTrash != -1 || !canManage()) return;
        try {
            PendingIntent request = MediaStore.createTrashRequest(getContentResolver(), Collections.singletonList(p.uri), true);
            pendingTrash = p.id;
            startIntentSenderForResult(request.getIntentSender(), TRASH_REQUEST, null, 0, 0, 0);
        } catch (Exception e) {
            pendingTrash = -1;
            Toast.makeText(this, "Could not move photo to Trash", Toast.LENGTH_SHORT).show(); render();
        }
    }

    private void restore(TrashEntry entry) {
        if (pendingRestore != -1 || !canManage()) { if (!canManage()) requestMediaManagement(); return; }
        try {
            pendingRestore = entry.id;
            PendingIntent request = MediaStore.createTrashRequest(getContentResolver(), Collections.singletonList(entry.uri), false);
            startIntentSenderForResult(request.getIntentSender(), RESTORE_REQUEST, null, 0, 0, 0);
        } catch (Exception e) {
            pendingRestore = -1; Toast.makeText(this, "Could not restore photo", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadTrashEntries() {
        trashEntries.clear();
        evictionQueue.clear();
        try {
            JSONArray array = new JSONArray(getPreferences(MODE_PRIVATE).getString("trash_entries", "[]"));
            for (int i = 0; i < array.length(); i++) {
                JSONObject item = array.getJSONObject(i);
                trashEntries.add(new TrashEntry(item.getLong("id"), item.getLong("at"), item.getLong("date")));
            }
            JSONArray evicted = new JSONArray(getPreferences(MODE_PRIVATE).getString("trash_evictions", "[]"));
            for (int i = 0; i < evicted.length(); i++) {
                JSONObject item = evicted.getJSONObject(i);
                evictionQueue.add(new TrashEntry(item.getLong("id"), item.getLong("at"), item.getLong("date")));
            }
        } catch (Exception ignored) { }
        trimRecoveryWindow();
        saveTrashEntries();
    }

    private void trimRecoveryWindow() {
        trashEntries.sort((a, b) -> Long.compare(b.trashedAt, a.trashedAt));
        HashSet<Long> seen = new HashSet<>();
        for (int i = 0; i < trashEntries.size();) {
            if (!seen.add(trashEntries.get(i).id)) trashEntries.remove(i); else i++;
        }
        long now = System.currentTimeMillis();
        for (int i = trashEntries.size() - 1; i >= 0; i--) {
            TrashEntry entry = trashEntries.get(i);
            if (i >= TRASH_LIMIT || now - entry.trashedAt >= SEVEN_DAYS) {
                if (evictionQueue.stream().noneMatch(e -> e.id == entry.id)) evictionQueue.add(entry);
                trashEntries.remove(i);
            }
        }
    }

    private void saveTrashEntries() {
        JSONArray array = new JSONArray();
        JSONArray evicted = new JSONArray();
        try {
            for (TrashEntry entry : trashEntries) {
                JSONObject item = new JSONObject();
                item.put("id", entry.id); item.put("at", entry.trashedAt); item.put("date", entry.timestamp);
                array.put(item);
            }
            for (TrashEntry entry : evictionQueue) {
                JSONObject item = new JSONObject();
                item.put("id", entry.id); item.put("at", entry.trashedAt); item.put("date", entry.timestamp);
                evicted.put(item);
            }
        } catch (Exception ignored) { }
        getPreferences(MODE_PRIVATE).edit().putString("trash_entries", array.toString())
                .putString("trash_evictions", evicted.toString()).commit();
    }

    private void scheduleCleanup() {
        JobScheduler scheduler = (JobScheduler) getSystemService(JOB_SCHEDULER_SERVICE);
        if (scheduler == null) return;
        JobInfo job = new JobInfo.Builder(701, new ComponentName(this, CleanupService.class))
                .setPeriodic(24L * 60 * 60 * 1000).setPersisted(true).build();
        scheduler.schedule(job);
    }

    private void cleanupTrash() {
        if (deletingOld || pendingTrash != -1 || pendingRestore != -1 || !canManage()) return;
        trimRecoveryWindow(); saveTrashEntries();
        ArrayList<TrashEntry> expired = new ArrayList<>(evictionQueue);
        if (expired.isEmpty()) return;
        deletingOld = true;
        io.execute(() -> {
            ArrayList<Long> deleted = new ArrayList<>();
            for (TrashEntry entry : expired) {
                try {
                    if (getContentResolver().delete(entry.uri, null, null) > 0) deleted.add(entry.id);
                } catch (Exception ignored) { }
            }
            runOnUiThread(() -> {
                deletingOld = false;
                if (deleted.isEmpty() || isDestroyed()) return;
                evictionQueue.removeIf(entry -> deleted.contains(entry.id));
                for (Long id : deleted) previews.remove(id);
                saveTrashEntries(); render();
            });
        });
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == TRASH_REQUEST) {
            long id = pendingTrash; pendingTrash = -1;
            int earned = 0;
            if (resultCode == RESULT_OK) {
                playEffect(false);
                if (lastKept == id) lastKept = -1;
                Photo moved = null;
                for (Photo p : photos) if (p.id == id) { moved = p; break; }
                if (moved != null) {
                    trashEntries.add(0, new TrashEntry(id, System.currentTimeMillis(), moved.timestamp));
                    trimRecoveryWindow();
                    saveTrashEntries();
                }
                photos.removeIf(p -> p.id == id);
                reviewed.remove(Long.toString(id)); saveReviewed();
                duplicates.remove(id);
                if (trashedIds.add(Long.toString(id))) trashedCount++;
                earned = awardXp(id, 15);
                saveStats();
            }
            render();
            if (earned > 0) floatXp(earned);
            if (resultCode == RESULT_OK) cleanupTrash();
        } else if (requestCode == RESTORE_REQUEST) {
            long id = pendingRestore; pendingRestore = -1;
            if (resultCode == RESULT_OK) {
                trashEntries.removeIf(e -> e.id == id); saveTrashEntries();
                restoredCount++; saveStats();
                loadPhotos();
            }
            render();
        }
    }

    public static class CleanupService extends JobService {
        @Override public boolean onStartJob(JobParameters params) {
            new Thread(() -> {
                if (Build.VERSION.SDK_INT >= 31 && MediaStore.canManageMedia(this)) {
                    android.content.SharedPreferences prefs = getSharedPreferences("MainActivity", MODE_PRIVATE);
                    try {
                        JSONArray array = new JSONArray(prefs.getString("trash_entries", "[]"));
                        JSONArray queued = new JSONArray(prefs.getString("trash_evictions", "[]"));
                        JSONArray remaining = new JSONArray();
                        JSONArray retry = new JSONArray();
                        long now = System.currentTimeMillis();
                        for (int i = 0; i < array.length(); i++) {
                            JSONObject item = array.getJSONObject(i);
                            boolean expired = now - item.getLong("at") >= SEVEN_DAYS || i >= TRASH_LIMIT;
                            if (expired) queued.put(item); else remaining.put(item);
                        }
                        for (int i = 0; i < queued.length(); i++) {
                            JSONObject item = queued.getJSONObject(i);
                            Uri uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, item.getLong("id"));
                            try {
                                if (getContentResolver().delete(uri, null, null) <= 0) retry.put(item);
                            } catch (Exception e) { retry.put(item); }
                        }
                        prefs.edit().putString("trash_entries", remaining.toString())
                                .putString("trash_evictions", retry.toString()).commit();
                    } catch (Exception ignored) { }
                }
                jobFinished(params, false);
            }).start();
            return true;
        }
        @Override public boolean onStopJob(JobParameters params) { return true; }
    }

    private void updateMusic() {
        if (!musicEnabled || musicVolume == 0) { stopMusic(); return; }
        if (musicTrack != null) { musicTrack.setVolume(musicVolume / 100f); return; }
        try {
            byte[] loop = synthMusic();
            AudioTrack track = new AudioTrack.Builder()
                    .setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
                    .setAudioFormat(new AudioFormat.Builder().setSampleRate(SAMPLE_RATE)
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
                    .setBufferSizeInBytes(loop.length).setTransferMode(AudioTrack.MODE_STATIC).build();
            if (track.getState() != AudioTrack.STATE_INITIALIZED || track.write(loop, 0, loop.length) != loop.length) {
                track.release(); return;
            }
            track.setLoopPoints(0, loop.length / 2, -1);
            track.setVolume(musicVolume / 100f);
            track.play(); musicTrack = track;
        } catch (Exception ignored) { stopMusic(); }
    }

    private void stopMusic() {
        if (musicTrack == null) return;
        try { musicTrack.pause(); musicTrack.flush(); musicTrack.release(); } catch (Exception ignored) { }
        musicTrack = null;
    }

    private void playEffect(boolean keep) {
        if (!soundEnabled) return;
        try {
            byte[] samples = synthEffect(keep);
            AudioTrack track = new AudioTrack.Builder()
                    .setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
                    .setAudioFormat(new AudioFormat.Builder().setSampleRate(SAMPLE_RATE)
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
                    .setBufferSizeInBytes(samples.length).setTransferMode(AudioTrack.MODE_STATIC).build();
            if (track.getState() != AudioTrack.STATE_INITIALIZED || track.write(samples, 0, samples.length) != samples.length) {
                track.release(); return;
            }
            track.setVolume(.38f); track.play();
            uiHandler.postDelayed(() -> { try { track.stop(); track.release(); } catch (Exception ignored) { } }, 900);
        } catch (Exception ignored) { }
    }

    private byte[] synthEffect(boolean keep) {
        int count = (int) (SAMPLE_RATE * (keep ? .52f : .44f));
        byte[] pcm = new byte[count * 2];
        for (int i = 0; i < count; i++) {
            double t = i / (double) SAMPLE_RATE, duration = count / (double) SAMPLE_RATE;
            double envelope = Math.min(1, t * 90) * Math.pow(Math.max(0, 1 - t / duration), keep ? 2.1 : 1.6);
            double wave;
            if (keep) {
                double note = t < .12 ? 784 : 1174.66;
                wave = .54 * Math.sin(2 * Math.PI * note * t) + .24 * Math.sin(2 * Math.PI * note * 2.01 * t);
            } else {
                double noise = Math.sin(i * 12.9898) * 43758.5453;
                noise = (noise - Math.floor(noise)) * 2 - 1;
                wave = .40 * noise + .23 * Math.sin(2 * Math.PI * (180 - 120 * t) * t);
            }
            short sample = (short) (Math.max(-1, Math.min(1, wave * envelope)) * 24000);
            pcm[i * 2] = (byte) sample; pcm[i * 2 + 1] = (byte) (sample >> 8);
        }
        return pcm;
    }

    private byte[] synthMusic() {
        int count = SAMPLE_RATE * 12;
        byte[] pcm = new byte[count * 2];
        double[] first = {174.61, 261.63, 329.63};
        double[] second = {146.83, 220.00, 293.66};
        for (int i = 0; i < count; i++) {
            double t = i / (double) SAMPLE_RATE;
            double blend = (1 - Math.cos(2 * Math.PI * t / 12)) / 2;
            double swell = .42 + .10 * Math.sin(2 * Math.PI * t / 5);
            double edge = Math.min(1, Math.min(t, 12 - t) * 2);
            double wave = 0;
            for (int note = 0; note < first.length; note++) {
                wave += ((1 - blend) * Math.sin(2 * Math.PI * first[note] * t)
                        + blend * Math.sin(2 * Math.PI * second[note] * t)) / (note + 2.3);
            }
            short sample = (short) (Math.max(-1, Math.min(1, wave * swell * edge)) * 15000);
            pcm[i * 2] = (byte) sample; pcm[i * 2 + 1] = (byte) (sample >> 8);
        }
        return pcm;
    }

    private void saveReviewed() { getPreferences(MODE_PRIVATE).edit().putStringSet("reviewed", new HashSet<>(reviewed)).apply(); }
    private class TextureBackdrop extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final float[] sweetX = new float[18], sweetY = new float[18];
        private final float[] speedX = new float[18], speedY = new float[18];
        private final float[] spin = new float[18];
        private long lastFrame;
        private boolean initialized;
        TextureBackdrop() { super(MainActivity.this); }
        @Override protected void onDraw(Canvas canvas) {
            float w = getWidth(), h = getHeight();
            if (w <= 0 || h <= 0) return;
            paint.setColor(BG); canvas.drawColor(BG);
            if (themeBackdrops[themeChoice] == null) {
                BitmapFactory.Options opts = new BitmapFactory.Options(); opts.inScaled = false;
                themeBackdrops[themeChoice] = BitmapFactory.decodeResource(getResources(), THEME_BACKDROP_IDS[themeChoice], opts);
            }
            Bitmap art = themeBackdrops[themeChoice];
            if (art != null) {
                float scale = Math.max(w / art.getWidth(), h / art.getHeight());
                float dw = art.getWidth() * scale, dh = art.getHeight() * scale;
                canvas.drawBitmap(art, null, new RectF((w - dw) / 2, (h - dh) / 2, (w + dw) / 2, (h + dh) / 2), paint);
            }
            canvas.drawColor(0x38000000);
            if (themeChoice == 18) {
                canvas.drawColor(0x500D0922);
                drawCandies(canvas, w, h);
            } else {
                drawThemeMotion(canvas, w, h, android.os.SystemClock.uptimeMillis() / 1000f);
                if (isAttachedToWindow()) postInvalidateDelayed(65);
            }
        }
        private float loop(float value, float span) {
            return (value % span + span) % span;
        }
        private void drawThemeMotion(Canvas canvas, float w, float h, float time) {
            int theme = themeChoice;
            float unit = dp(1);
            paint.setShader(null);
            paint.setStyle(Paint.Style.FILL);
            if (theme == 19) { // Toxic: bubbles rise while drops slide down the edges.
                for (int i = 0; i < 15; i++) {
                    float x = w * (.07f + ((i * 67) % 89) / 100f);
                    float y = h - loop(time * (18 + i % 4 * 9) * unit + i * h / 13, h + 80 * unit);
                    paint.setColor(0x668BFF51); paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(1.6f * unit);
                    canvas.drawCircle(x + (float)Math.sin(time + i) * 9 * unit, y, (5 + i % 5 * 3) * unit, paint);
                }
                paint.setStyle(Paint.Style.FILL); paint.setColor(0x88ABFC4E);
                for (int i = 0; i < 4; i++) canvas.drawRoundRect(new RectF(w * (i + 1) / 5, 0,
                        w * (i + 1) / 5 + 4 * unit, loop(time * (12 + i * 4) * unit + i * 71 * unit, h)), 4 * unit, 4 * unit, paint);
            } else if (theme == 20) { // Space: star twinkle and occasional shooting stars.
                for (int i = 0; i < 30; i++) {
                    float x = ((i * 173) % 991) / 991f * w, y = ((i * 311) % 997) / 997f * h;
                    paint.setColor(Color.argb(30 + (int)(65 * (.5 + .5 * Math.sin(time * 2 + i))), 195, 228, 255));
                    canvas.drawCircle(x, y, (i % 5 == 0 ? 2.2f : 1f) * unit, paint);
                }
                float flight = loop(time * 125 * unit, w + h);
                paint.setColor(0x9CB9E7FF); paint.setStrokeWidth(2 * unit);
                canvas.drawLine(flight - h * .35f, flight, flight - h * .35f - 36 * unit, flight - 36 * unit, paint);
            } else if (theme == 21) { // Fire: embers rise and flicker.
                for (int i = 0; i < 24; i++) {
                    float x = w * ((i * 59 % 97) / 97f) + (float)Math.sin(time * 2 + i) * 11 * unit;
                    float y = h - loop(time * (22 + i % 6 * 9) * unit + i * h / 19, h + 30 * unit);
                    paint.setColor(i % 3 == 0 ? 0x99FFDC62 : 0x88FF7950);
                    canvas.drawCircle(x, y, (1.5f + i % 4) * unit, paint);
                }
            } else if (theme == 22) { // Water: spreading ripples and slow currents.
                paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(1.5f * unit);
                for (int i = 0; i < 9; i++) {
                    float x = w * ((i * 37 % 93) / 93f), y = h * ((i * 29 % 89) / 89f);
                    float ripple = loop(time * 22 * unit + i * 17 * unit, 75 * unit);
                    paint.setColor(Color.argb((int)(85 * (1 - ripple / (75 * unit))), 138, 231, 255));
                    canvas.drawOval(x - ripple, y - ripple * .32f, x + ripple, y + ripple * .32f, paint);
                }
                paint.setStyle(Paint.Style.FILL);
            } else if (theme == 23) { // Ice: gently falling six armed snow crystals.
                paint.setColor(0x99DDF6FF); paint.setStrokeWidth(1.4f * unit);
                for (int i = 0; i < 16; i++) {
                    float x = w * ((i * 47 % 97) / 97f) + (float)Math.sin(time + i) * 9 * unit;
                    float y = loop(time * (13 + i % 4 * 5) * unit + i * h / 14, h + 30 * unit) - 15 * unit;
                    float r = (3 + i % 4) * unit;
                    for (int a = 0; a < 3; a++) {
                        double angle = a * Math.PI / 3;
                        float dx = (float)Math.cos(angle) * r, dy = (float)Math.sin(angle) * r;
                        canvas.drawLine(x - dx, y - dy, x + dx, y + dy, paint);
                    }
                }
            } else if (theme == 24) { // Earth: leaves drift sideways and downward.
                for (int i = 0; i < 16; i++) {
                    float x = loop(i * w / 12 + time * (10 + i % 3 * 6) * unit, w + 30 * unit) - 15 * unit;
                    float y = loop(i * h / 14 + time * 13 * unit, h + 30 * unit) - 15 * unit;
                    canvas.save(); canvas.rotate((float)Math.sin(time + i) * 45, x, y);
                    paint.setColor(i % 2 == 0 ? 0x8898CD65 : 0x99D8BB77);
                    canvas.drawOval(x - 5 * unit, y - 2 * unit, x + 5 * unit, y + 2 * unit, paint);
                    canvas.restore();
                }
            } else if (theme == 25) { // Lightning: brief branching flashes, then darkness.
                float flash = loop(time, 4.2f);
                if (flash < .16f || (flash > .28f && flash < .36f)) {
                    float x = w * (.3f + (int)(time / 4.2f) % 4 * .13f);
                    paint.setColor(0xBBD7E6FF); paint.setStrokeWidth(2 * unit);
                    for (int i = 0; i < 6; i++) {
                        float y = h * (i + 1) / 8;
                        float next = x + (i % 2 == 0 ? 24 : -34) * unit;
                        canvas.drawLine(x, y, next, h * (i + 2) / 8, paint);
                        if (i == 3) canvas.drawLine(x, y, x - 31 * unit, y + 36 * unit, paint);
                        x = next;
                    }
                }
            } else if (theme == 3) { // Web hero: threads grow from corners.
                paint.setColor(0x607EBEFF); paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(unit);
                float radius = (42 + 8 * (float)Math.sin(time)) * unit;
                for (int corner = 0; corner < 2; corner++) {
                    float x = corner == 0 ? 0 : w, y = corner == 0 ? 0 : h;
                    for (int i = 1; i <= 3; i++) canvas.drawArc(x - radius * i, y - radius * i,
                            x + radius * i, y + radius * i, corner == 0 ? 0 : 180, 90, false, paint);
                    for (int i = 0; i <= 4; i++) canvas.drawLine(x, y, x + (corner == 0 ? 1 : -1) * radius * 3 * i / 4,
                            y + (corner == 0 ? 1 : -1) * radius * 3 * (4 - i) / 4, paint);
                }
                paint.setStyle(Paint.Style.FILL);
            } else if (theme == 4 || theme == 12) { // Armor / Solar: rotating warm rays.
                paint.setColor(0x44FFD081); paint.setStrokeWidth(2 * unit);
                float cx = w * .78f, cy = h * .22f;
                for (int i = 0; i < 12; i++) {
                    double a = i * Math.PI / 6 + time * .18;
                    canvas.drawLine(cx + (float)Math.cos(a) * 35 * unit, cy + (float)Math.sin(a) * 35 * unit,
                            cx + (float)Math.cos(a) * 55 * unit, cy + (float)Math.sin(a) * 55 * unit, paint);
                }
            } else if (theme == 5 || theme == 11) { // Thunder / Nebula: pulsing arcs or drifting stardust.
                paint.setColor(theme == 5 ? 0x66C2D5FF : 0x6699E9FF);
                for (int i = 0; i < 16; i++) {
                    float x = loop(i * w / 13 + time * (theme == 5 ? 19 : 8) * unit, w);
                    float y = h * ((i * 37 % 97) / 97f);
                    canvas.drawCircle(x, y, (2 + i % 3) * unit * (1 + .25f * (float)Math.sin(time * 3 + i)), paint);
                }
            } else if (theme == 6 || theme == 10) { // Gamma / Stealth: pulsing green nodes.
                paint.setColor(0x558FEB70); paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(1.5f * unit);
                for (int i = 0; i < 8; i++) {
                    float x = w * ((i * 31 % 89) / 89f), y = h * ((i * 53 % 91) / 91f);
                    canvas.drawCircle(x, y, (10 + 5 * (float)Math.sin(time * 2 + i)) * unit, paint);
                }
                paint.setStyle(Paint.Style.FILL);
            } else if (theme == 7) { // Shield: concentric waves.
                paint.setColor(0x558AC7F5); paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(2 * unit);
                for (int i = 0; i < 4; i++) canvas.drawCircle(w * .5f, h * .5f,
                        loop(time * 18 * unit + i * 34 * unit, 140 * unit), paint);
                paint.setStyle(Paint.Style.FILL);
            } else if (theme == 8 || theme == 9) { // Cosmic / Scarlet: wisps orbit or rose petals drift.
                paint.setColor(theme == 8 ? 0x608EEBE4 : 0x77FF99B8);
                for (int i = 0; i < 14; i++) {
                    float x = w * ((i * 47 % 97) / 97f) + (float)Math.sin(time * .6 + i) * 12 * unit;
                    float y = loop(i * h / 12 - time * (theme == 8 ? 9 : -13) * unit, h);
                    canvas.drawOval(x - 3 * unit, y - 6 * unit, x + 3 * unit, y + 6 * unit, paint);
                }
            } else { // Basic palettes: matching colored dust, each with its own flow.
                int direction = theme % 3;
                paint.setColor(Color.argb(75, Color.red(GREEN), Color.green(GREEN), Color.blue(GREEN)));
                for (int i = 0; i < 18; i++) {
                    float x = loop(w * ((i * 67 % 97) / 97f) + (direction == 1 ? time * 11 * unit : 0), w);
                    float y = loop(h * ((i * 41 % 91) / 91f) + (direction == 2 ? -time * 12 * unit : time * 7 * unit), h);
                    canvas.drawCircle(x, y, (1 + i % 3) * unit, paint);
                }
            }
            paint.setStyle(Paint.Style.FILL); paint.setAlpha(255);
        }
        private void drawCandies(Canvas canvas, float w, float h) {
            if (candySprites == null) candySprites = BitmapFactory.decodeResource(getResources(), R.drawable.candy_sprites);
            if (candySprites == null) return;
            if (!initialized) {
                for (int i = 0; i < sweetX.length; i++) {
                    sweetX[i] = w * (.1f + ((i * 47) % 83) / 100f);
                    sweetY[i] = h * (((i * 31) % 97) / 100f);
                    spin[i] = i * 27;
                }
                initialized = true;
            }
            long now = android.os.SystemClock.uptimeMillis();
            float dt = lastFrame == 0 ? .016f : Math.min(.04f, (now - lastFrame) / 1000f);
            lastFrame = now;
            float gx = gravityX, gy = gravityY;
            if (Math.abs(gx) + Math.abs(gy) < .12f) gy = .35f;
            float radius = dp(23);
            for (int i = 0; i < sweetX.length; i++) {
                speedX[i] = (speedX[i] + gx * dp(540) * dt) * .992f;
                speedY[i] = (speedY[i] + gy * dp(540) * dt) * .992f;
                sweetX[i] += speedX[i] * dt;
                sweetY[i] += speedY[i] * dt;
                if (sweetX[i] < radius) { sweetX[i] = radius; speedX[i] *= -.28f; }
                if (sweetX[i] > w - radius) { sweetX[i] = w - radius; speedX[i] *= -.28f; }
                if (sweetY[i] < radius) { sweetY[i] = radius; speedY[i] *= -.28f; }
                if (sweetY[i] > h - radius) { sweetY[i] = h - radius; speedY[i] *= -.28f; }
                for (int j = 0; j < i; j++) {
                    float dx = sweetX[i] - sweetX[j], dy = sweetY[i] - sweetY[j];
                    float dist = (float) Math.sqrt(dx * dx + dy * dy);
                    if (dist > 1 && dist < radius * 1.65f) {
                        float shift = (radius * 1.65f - dist) * .5f;
                        sweetX[i] += dx / dist * shift; sweetY[i] += dy / dist * shift;
                        sweetX[j] -= dx / dist * shift; sweetY[j] -= dy / dist * shift;
                        speedX[i] *= .85f; speedY[i] *= .85f;
                    }
                }
                spin[i] += speedX[i] * dt * .08f;
                int sprite = i % 8, tileW = candySprites.getWidth() / 4, tileH = candySprites.getHeight() / 2;
                Rect source = new Rect(sprite % 4 * tileW, sprite / 4 * tileH,
                        (sprite % 4 + 1) * tileW, (sprite / 4 + 1) * tileH);
                canvas.save(); canvas.rotate(spin[i], sweetX[i], sweetY[i]);
                paint.setAlpha(210);
                canvas.drawBitmap(candySprites, source, new RectF(sweetX[i] - radius, sweetY[i] - radius,
                        sweetX[i] + radius, sweetY[i] + radius), paint);
                paint.setAlpha(255); canvas.restore();
            }
            if (isAttachedToWindow()) postInvalidateDelayed(32);
        }
    }
    private int dp(float n) { return (int) (n * getResources().getDisplayMetrics().density + 0.5f); }
    private GradientDrawable rounded(int color, int radius) {
        GradientDrawable bg = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{blend(color, Color.WHITE, .09f), color, blend(color, BG, .14f)});
        bg.setCornerRadius(dp(radius));
        bg.setStroke(dp(1), blend(GOLD, color, .68f)); return bg;
    }
    private int blend(int a, int b, float amount) {
        return Color.rgb((int)(Color.red(a) * (1 - amount) + Color.red(b) * amount),
                (int)(Color.green(a) * (1 - amount) + Color.green(b) * amount),
                (int)(Color.blue(a) * (1 - amount) + Color.blue(b) * amount));
    }
    private Drawable themeButton(int color, int radius) {
        final int selected = themeChoice;
        return new Drawable() {
            final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
            @Override public void draw(Canvas canvas) {
                android.graphics.Rect bounds = getBounds();
                RectF box = new RectF(bounds);
                Path clip = new Path(); clip.addRoundRect(box, dp(radius), dp(radius), Path.Direction.CW);
                canvas.save(); canvas.clipPath(clip);
                p.setColor(color); canvas.drawRect(box, p);
                p.setShader(new LinearGradient(box.left, box.top, box.right, box.bottom,
                        new int[]{blend(color, Color.WHITE, .20f), color, blend(color, BG, .16f)},
                        null, Shader.TileMode.CLAMP));
                canvas.drawRect(box, p); p.setShader(null);
                Bitmap art = themeBackdrops[selected];
                if (art != null) {
                    int left = art.getWidth() / 5, top = art.getHeight() / 3;
                    p.setAlpha(54);
                    canvas.drawBitmap(art, new Rect(left, top, art.getWidth() - left, top + art.getHeight() / 3), box, p);
                    p.setAlpha(255);
                }
                p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(dp(1.3f));
                p.setColor(blend(GOLD, color, .45f)); canvas.drawRoundRect(box, dp(radius), dp(radius), p);
                p.setStyle(Paint.Style.FILL); canvas.restore();
            }
            @Override public void setAlpha(int alpha) { p.setAlpha(alpha); invalidateSelf(); }
            @Override public void setColorFilter(android.graphics.ColorFilter filter) { p.setColorFilter(filter); invalidateSelf(); }
            @Override public int getOpacity() { return android.graphics.PixelFormat.TRANSLUCENT; }
        };
    }
    private void spacer(int height) { View gap = new View(this); root.addView(gap, new LinearLayout.LayoutParams(1, dp(height))); }
    private TextView label(String value, int size, int color, boolean bold) {
        TextView text = new TextView(this); text.setText(value); text.setTextSize(size); text.setTextColor(color);
        if (bold) text.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        root.addView(text); return text;
    }
    private TextView pill(String value, int bg, int color) {
        TextView text = new TextView(this); text.setText(value); text.setTextSize(13); text.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        text.setTextColor(color); text.setPadding(dp(13), dp(8), dp(13), dp(8)); text.setBackground(rounded(bg, 30)); return text;
    }
    private TextView swipeOverlayLabel(String value, int color) {
        TextView text = new TextView(this); text.setText(value); text.setTextSize(21);
        text.setTextColor(color); text.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        text.setGravity(Gravity.CENTER); text.setShadowLayer(dp(6), 0, dp(2), Color.BLACK);
        return text;
    }
    private Button button(LinearLayout parent, String value, int bg, int color, Runnable action) {
        Button button = new Button(this); button.setText(value); button.setAllCaps(false); button.setTextSize(16);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD); button.setTextColor(color); button.setBackground(themeButton(bg, 18));
        button.setOnClickListener(v -> action.run());
        parent.addView(button, new LinearLayout.LayoutParams(-1, dp(55))); return button;
    }
    private void back(String value, Runnable action) {
        TextView back = label("←  " + value, 18, INK, true);
        back.setGravity(Gravity.CENTER_VERTICAL);
        back.setPadding(dp(18), 0, dp(18), 0);
        back.setMinWidth(dp(150)); back.setHeight(dp(56));
        back.setBackground(themeButton(PANEL, 18));
        back.setOnClickListener(v -> action.run());
        spacer(15);
    }
    private void tile(LinearLayout parent, String title, String detail, Runnable action) {
        LinearLayout row = new LinearLayout(this); row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(14), dp(13), dp(17), dp(13)); row.setBackground(rounded(PANEL, 18));
        row.setElevation(dp(2));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2); lp.bottomMargin = dp(12); parent.addView(row, lp);
        View accent = new View(this); accent.setBackground(rounded(GREEN, 5));
        LinearLayout.LayoutParams accentLp = new LinearLayout.LayoutParams(dp(7), dp(57)); accentLp.rightMargin = dp(17); row.addView(accent, accentLp);
        LinearLayout copy = new LinearLayout(this); copy.setOrientation(LinearLayout.VERTICAL); row.addView(copy, new LinearLayout.LayoutParams(0, -2, 1));
        TextView name = new TextView(this); name.setText(title); name.setTextSize(22); name.setTypeface(Typeface.DEFAULT, Typeface.BOLD); name.setTextColor(INK); copy.addView(name);
        TextView sub = new TextView(this); sub.setText(detail); sub.setTextSize(14); sub.setTextColor(MUTED);
        LinearLayout.LayoutParams subLp = new LinearLayout.LayoutParams(-1, -2); subLp.topMargin = dp(4); copy.addView(sub, subLp);
        TextView arrow = new TextView(this); arrow.setText("›"); arrow.setTextColor(GREEN); arrow.setTextSize(28); row.addView(arrow);
        row.setOnClickListener(v -> action.run());
    }
}
