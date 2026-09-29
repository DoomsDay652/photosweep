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
            {0xFF092730, 0xFF18464B, 0xFF5DE5C2, 0xFFFF8484, 0xFFFFD382},
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
            {0xFF382538, 0xFF60405A, 0xFFFFA7AC, 0xFFFFD2A2, 0xFFFFDEB4},
            {0xFF152D28, 0xFF315143, 0xFF94E1AC, 0xFFFFA4A0, 0xFFE3D39A},
            {0xFF172A50, 0xFF2D4672, 0xFFFFCE75, 0xFFFF9B9B, 0xFF98C5FF},
            {0xFF33243C, 0xFF5D365D, 0xFFFF91BF, 0xFFFFA199, 0xFFD7B0F2},
            {0xFF372B25, 0xFF65503C, 0xFFFFB46B, 0xFFFF8F8E, 0xFFCEF087},
            {0xFF392D4E, 0xFF6B527A, 0xFFFFBAE5, 0xFFFF91BB, 0xFFFFDC93},
            {0xFF172E29, 0xFF315746, 0xFFAEF76A, 0xFFFFA482, 0xFFD7F67A},
            {0xFF101B3B, 0xFF263458, 0xFFA8C8FF, 0xFFFF96D7, 0xFFACFFF1},
            {0xFF391E26, 0xFF683631, 0xFFFFA45F, 0xFFFF7272, 0xFFFFD176},
            {0xFF122B42, 0xFF255572, 0xFF85DFFF, 0xFFFFA9A4, 0xFFB4E9FF},
            {0xFF1D3345, 0xFF39596D, 0xFFB4EFFF, 0xFFFFA8AD, 0xFFE4F9FF},
            {0xFF2D3024, 0xFF535D3C, 0xFFC0DF86, 0xFFFFA789, 0xFFE8D596},
            {0xFF1A2143, 0xFF333B6D, 0xFFB9B8FF, 0xFFFFA3B6, 0xFFFFDF8A}
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
    private final Handler uiHandler = new Handler(Looper.getMainLooper());
    private long pendingTrash = -1;
    private long pendingRestore = -1;
    private long lastKept = -1;
    private int generation;
    private float touchX, touchY;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
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
        if (musicEnabled) updateMusic();
        if (root != null) {
            loadTrashEntries();
            render();
            if (hasAccess() && canManage()) cleanupTrash();
        }
    }

    @Override protected void onPause() {
        stopMusic();
        super.onPause();
    }

    @Override public void onDestroy() {
        stopMusic();
        generation++;
        io.shutdownNow();
        duplicateWorker.shutdownNow();
        super.onDestroy();
    }

    private void applyTheme() {
        if (themeChoice < 0 || themeChoice >= THEME_COLORS.length ||
                (!adminMode && themeChoice >= 3 && xp / 500 + 1 < themeChoice - 1)) themeChoice = 0;
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
        host.addView(new TextureBackdrop(), new FrameLayout.LayoutParams(-1, -1));
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
        gear.setBackground(rounded(PANEL, 16)); top.addView(gear, new LinearLayout.LayoutParams(dp(52), dp(52)));
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
        button(root, "Recently trashed  ·  " + trashEntries.size() + "/20", Color.rgb(62, 72, 91), Color.rgb(255, 240, 213), () -> { showingTrash = true; render(); });
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
        View track = new View(this); track.setBackground(rounded(Color.rgb(18, 39, 56), 8)); bar.addView(track, new FrameLayout.LayoutParams(-1, -1));
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
            if (!value && themeChoice >= 3 && xp / 500 + 1 < themeChoice - 1) {
                themeChoice = getPreferences(MODE_PRIVATE).getInt("theme_before_admin", 0);
                getPreferences(MODE_PRIVATE).edit().putInt("theme", themeChoice).apply();
            }
            applyTheme(); render();
        });
        sectionTitle(list, "THEMES");
        int level = xp / 500 + 1;
        for (int i = 0; i < THEME_NAMES.length; i++) {
            if (i == 0) sectionTitle(list, "ORIGINAL");
            else if (i == 3) sectionTitle(list, "HERO COLORS");
            else if (i == 13) sectionTitle(list, "MORE COLORS");
            else if (i == 18) sectionTitle(list, "WORLDS & ELEMENTS");
            final int choice = i;
            int requiredLevel = i < 3 ? 1 : i - 1;
            boolean unlocked = adminMode || level >= requiredLevel;
            themeTile(list, i, unlocked, requiredLevel, () -> {
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
            thumbnail.setBackground(rounded(Color.rgb(51, 96, 107), 12)); row.addView(thumbnail, new LinearLayout.LayoutParams(dp(80), dp(80)));
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
            restore.setTextColor(GREEN); restore.setTextSize(13); restore.setBackground(rounded(Color.rgb(42, 91, 94), 12));
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
        View track = new View(this); track.setBackground(rounded(Color.rgb(35, 56, 77), 4));
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
        card.setBackground(rounded(Color.rgb(29, 50, 68), 25)); card.setElevation(dp(8));
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
        try {
            JSONArray array = new JSONArray(getPreferences(MODE_PRIVATE).getString("trash_entries", "[]"));
            for (int i = 0; i < array.length(); i++) {
                JSONObject item = array.getJSONObject(i);
                trashEntries.add(new TrashEntry(item.getLong("id"), item.getLong("at"), item.getLong("date")));
            }
        } catch (Exception ignored) { }
    }

    private void saveTrashEntries() {
        JSONArray array = new JSONArray();
        try {
            for (TrashEntry entry : trashEntries) {
                JSONObject item = new JSONObject();
                item.put("id", entry.id); item.put("at", entry.trashedAt); item.put("date", entry.timestamp);
                array.put(item);
            }
        } catch (Exception ignored) { }
        getPreferences(MODE_PRIVATE).edit().putString("trash_entries", array.toString()).commit();
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
        long now = System.currentTimeMillis();
        ArrayList<TrashEntry> expired = new ArrayList<>();
        for (int i = 0; i < trashEntries.size(); i++) {
            TrashEntry entry = trashEntries.get(i);
            if (now - entry.trashedAt >= SEVEN_DAYS || i >= TRASH_LIMIT) expired.add(entry);
        }
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
                trashEntries.removeIf(entry -> deleted.contains(entry.id));
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
                        JSONArray remaining = new JSONArray();
                        long now = System.currentTimeMillis();
                        for (int i = 0; i < array.length(); i++) {
                            JSONObject item = array.getJSONObject(i);
                            boolean expired = now - item.getLong("at") >= SEVEN_DAYS || i >= TRASH_LIMIT;
                            if (expired) {
                                Uri uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, item.getLong("id"));
                                try {
                                    if (getContentResolver().delete(uri, null, null) <= 0) remaining.put(item);
                                } catch (Exception e) { remaining.put(item); }
                            } else remaining.put(item);
                        }
                        prefs.edit().putString("trash_entries", remaining.toString()).commit();
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
        TextureBackdrop() { super(MainActivity.this); }
        @Override protected void onDraw(Canvas canvas) {
            float w = getWidth(), h = getHeight();
            paint.setShader(new LinearGradient(0, 0, w, h,
                    new int[]{BG, PANEL, BG},
                    null, Shader.TileMode.CLAMP));
            canvas.drawRect(0, 0, w, h, paint); paint.setShader(null);
            if (themeChoice == 20) {
                if (spaceBackdrop == null) spaceBackdrop = BitmapFactory.decodeResource(getResources(), R.drawable.space_nebula);
                if (spaceBackdrop != null) {
                    paint.setAlpha(185);
                    canvas.drawBitmap(spaceBackdrop, new Rect(0, 0, spaceBackdrop.getWidth(), spaceBackdrop.getHeight()),
                            new RectF(0, 0, w, h), paint);
                    paint.setAlpha(255);
                }
                return;
            }
            int style = themeChoice < 18 ? themeChoice % 5 : themeChoice - 17;
            if (themeChoice == 18) {
                paint.setStrokeWidth(dp(13));
                for (int i = -6; i < 16; i++) {
                    paint.setColor(i % 2 == 0 ? 0x23FFB8D9 : 0x2699E5F2);
                    canvas.drawLine(i * dp(90), 0, i * dp(90) + h * .37f, h, paint);
                }
            } else if (themeChoice == 19) {
                paint.setColor(0x2841FF84);
                for (int i = 0; i < 8; i++) {
                    float x = w * ((i * 37 % 83) / 83f);
                    canvas.drawCircle(x, h * ((i * 29 % 101) / 101f), dp(22 + i * 6), paint);
                }
            } else if (themeChoice == 22) {
                paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(dp(2));
                for (int i = 0; i < 14; i++) {
                    paint.setColor(0x2A9CEBFF);
                    Path wave = new Path(); float y = h * i / 13f;
                    wave.moveTo(0, y);
                    for (int j = 0; j < 6; j++) wave.quadTo(w * (j + .5f) / 6f, y - dp(18), w * (j + 1f) / 6f, y);
                    canvas.drawPath(wave, paint);
                }
                paint.setStyle(Paint.Style.FILL);
            }
            int count = themeChoice >= 18 ? 74 : 128;
            for (int i = 0; i < count; i++) {
                float x = ((i * 137L + 73) % 1000) / 1000f * w;
                float y = ((i * 263L + 117) % 1000) / 1000f * h;
                float size = dp(2 + i % 4);
                paint.setColor(Color.argb(35 + i % 4 * 12, Color.red(GREEN), Color.green(GREEN), Color.blue(GREEN)));
                if (style == 5 || style == 1) canvas.drawCircle(x, y, size * 1.4f, paint);
                else if (style == 6) {
                    Path crystal = new Path(); crystal.moveTo(x, y - size * 2); crystal.lineTo(x + size, y);
                    crystal.lineTo(x, y + size * 2); crystal.lineTo(x - size, y); crystal.close(); canvas.drawPath(crystal, paint);
                } else if (style == 8) {
                    canvas.drawLine(x, y, x + size, y + size * 3, paint);
                } else if (style == 7) {
                    canvas.drawOval(x - size, y - size * 2, x + size, y + size * 2, paint);
                } else if (style == 4) {
                    canvas.drawOval(x - size, y - size * 2, x + size, y + size, paint);
                } else canvas.drawCircle(x, y, dp(0.9f + i % 3 * .4f), paint);
            }
        }
    }
    private int dp(float n) { return (int) (n * getResources().getDisplayMetrics().density + 0.5f); }
    private GradientDrawable rounded(int color, int radius) {
        GradientDrawable bg = new GradientDrawable(); bg.setColor(color); bg.setCornerRadius(dp(radius));
        bg.setStroke(dp(1), Color.argb(38, 139, 200, 207)); return bg;
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
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD); button.setTextColor(color); button.setBackground(rounded(bg, 18));
        button.setOnClickListener(v -> action.run());
        parent.addView(button, new LinearLayout.LayoutParams(-1, dp(55))); return button;
    }
    private void back(String value, Runnable action) {
        TextView back = label("←  " + value, 18, INK, true);
        back.setGravity(Gravity.CENTER_VERTICAL);
        back.setPadding(dp(18), 0, dp(18), 0);
        back.setMinWidth(dp(150)); back.setHeight(dp(56));
        back.setBackground(rounded(Color.rgb(41, 64, 84), 18));
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
