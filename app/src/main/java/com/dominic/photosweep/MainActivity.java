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
import android.graphics.ImageDecoder;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
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
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
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
            "Earth · stone and leaves", "Lightning · electric sky",
            "Sakura Academy · twilight blossoms", "Spirit Sky · floating sanctuary",
            "Japan · lantern street", "Mexico · colorful plaza",
            "Sakura Breeze · drifting petals", "Spirit Lights · wandering wisps",
            "Japan · glowing lanterns", "Mexico · marigold celebration",
            "Web Hero · living webs",
            "Armored Gold · radiant armor",
            "Thunder · storm arcs",
            "Gamma · pulsing energy",
            "Vibrant Shield · energy waves",
            "Cosmic · orbiting wisps",
            "Scarlet Magic · drifting petals",
            "Stealth · glowing nodes",
            "Nebula · stardust",
            "Solar · moving rays",
            "United States · waterfront lights",
            "Spain · tiled twilight plaza",
            "Brazil · tropical bay",
            "France · Paris twilight",
            "Italy · Venetian evening",
            "South Korea · hanok courtyard",
            "United States · drifting stars",
            "Spain · dancing fans",
            "Brazil · tropical breeze",
            "France · lavender breeze",
            "Italy · olive breeze",
            "South Korea · floating lanterns",
            "My Hero Academia · hero city",
            "Bleach · spirit courtyard",
            "One Piece · pirate seas",
            "Naruto · hidden village",
            "DragonBall · capsule skyline",
            "Yin Yang · ink and ivory"
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
            {0xFF1A2143, 0xFF333B6D, 0xFFB9B8FF, 0xFFFFA3B6, 0xFFFFDF8A},
            {0xFF24213F, 0xFF433353, 0xFFFFB5D2, 0xFFFF9C9F, 0xFFE4CEFF},
            {0xFF142B3D, 0xFF29465A, 0xFF8CE9D8, 0xFFFFA5B5, 0xFFFFD783},
            {0xFF191F3B, 0xFF34334D, 0xFFFFBE8C, 0xFFFF9090, 0xFFFFD995},
            {0xFF123342, 0xFF285369, 0xFF7DE8DB, 0xFFFF9C9A, 0xFFFFCC73},
            {0xFF24213F, 0xFF433353, 0xFFFFB5D2, 0xFFFF9C9F, 0xFFE4CEFF},
            {0xFF142B3D, 0xFF29465A, 0xFF8CE9D8, 0xFFFFA5B5, 0xFFFFD783},
            {0xFF191F3B, 0xFF34334D, 0xFFFFBE8C, 0xFFFF9090, 0xFFFFD995},
            {0xFF123342, 0xFF285369, 0xFF7DE8DB, 0xFFFF9C9A, 0xFFFFCC73},
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
            {0xFF14233E, 0xFF293A58, 0xFF8EC9FF, 0xFFFF909C, 0xFFF8E1AA},
            {0xFF302334, 0xFF513647, 0xFFFFCA79, 0xFFFF959B, 0xFF96DAE6},
            {0xFF123438, 0xFF245452, 0xFF8DE89D, 0xFFFFB197, 0xFFFFD86E},
            {0xFF202A46, 0xFF384565, 0xFFBDADFF, 0xFFFFABBC, 0xFFFFDFBA},
            {0xFF223238, 0xFF43554D, 0xFFACDFBA, 0xFFFFADA0, 0xFFFFD4A1},
            {0xFF25263F, 0xFF45405B, 0xFFA9DDF0, 0xFFFFABB4, 0xFFFFD8B0},
            {0xFF14233E, 0xFF293A58, 0xFF8EC9FF, 0xFFFF909C, 0xFFF8E1AA},
            {0xFF302334, 0xFF513647, 0xFFFFCA79, 0xFFFF959B, 0xFF96DAE6},
            {0xFF123438, 0xFF245452, 0xFF8DE89D, 0xFFFFB197, 0xFFFFD86E},
            {0xFF202A46, 0xFF384565, 0xFFBDADFF, 0xFFFFABBC, 0xFFFFDFBA},
            {0xFF223238, 0xFF43554D, 0xFFACDFBA, 0xFFFFADA0, 0xFFFFD4A1},
            {0xFF25263F, 0xFF45405B, 0xFFA9DDF0, 0xFFFFABB4, 0xFFFFD8B0},
            {0xFF102E35, 0xFF254A4C, 0xFF74EBC6, 0xFFFF9297, 0xFFFFD36F},
            {0xFF192B43, 0xFF31425E, 0xFF9CDFFF, 0xFFFFA183, 0xFFE8EAF6},
            {0xFF133548, 0xFF28566A, 0xFF83E5F2, 0xFFFF9993, 0xFFFFD270},
            {0xFF1D3133, 0xFF3A4C45, 0xFFAFE093, 0xFFFFAB7F, 0xFFFFD579},
            {0xFF172E52, 0xFF304B71, 0xFFFFC46A, 0xFFFF9096, 0xFF90DFFF},
            {0xFF15171D, 0xFF34363D, 0xFFE1E8E6, 0xFFD99B9F, 0xFFD8C69A}
    };
    private static final int[] COLOR_THEMES = {0, 1, 2, 13, 14, 15, 16, 17};
    private static final int[] HERO_THEMES = {3, 4, 5, 6, 7, 8, 9, 10, 11, 12};
    private static final int[] ANIMATED_THEMES = {18, 19, 20, 21, 22, 23, 24, 25};
    private static final int[] ANIME_ANIMATED_THEMES = {27, 30, 31, 56, 57, 58, 59, 60};
    private static final int[] HERO_ANIMATED_THEMES = {34, 35, 36, 37, 38, 39, 40, 41, 42, 43};
    private static final String[] COUNTRY_NAMES = {"Japan", "Mexico", "United States", "Spain", "Brazil", "France", "Italy", "South Korea"};
    private static final String[] COUNTRY_FLAGS = {"🇯🇵", "🇲🇽", "🇺🇸", "🇪🇸", "🇧🇷", "🇫🇷", "🇮🇹", "🇰🇷"};
    private static final String[] COUNTRY_KEYS = {"japan", "mexico", "us", "spain", "brazil", "france", "italy", "korea"};
    private static final String[] COUNTRY_SCENES = {"Lantern streets at dusk", "Colorful twilight plazas", "Waterfront city lights", "Tiled twilight plazas", "Tropical coastal bays", "Paris at twilight", "Venetian evening canals", "Hanok courtyards"};
    private static final String[] COUNTRY_MOTIONS = {"Flags and lanterns", "Flags and marigolds", "Flags and stars", "Flags and fans", "Flags and tropical leaves", "Flags and lavender", "Flags and olive branches", "Flags and paper lanterns"};
    private static final int[] COUNTRY_STILL_THEMES = {28, 29, 44, 45, 46, 47, 48, 49};
    private static final int[] COUNTRY_ANIMATED_THEMES = {32, 33, 50, 51, 52, 53, 54, 55};
    private static final String[] SWIPE_STYLE_NAMES = {"Theme matched", "Sparkles", "Petals", "Bubbles", "Embers", "Lightning", "Comets", "Energy rings", "Country celebration", "Off"};
    private static final int[] TIER_UNLOCK_LEVELS = {1, 3, 6};
    private static int motionStyle(int index) {
        if (index == 26) return 30;
        if (index == 27) return 31;
        return index >= 34 && index <= 43 ? index - 31 : index;
    }
    private static int themeTier(int index) {
        if (isColorTheme(index)) return 1;
        if (index == 61) return 2;
        for (int hero : HERO_THEMES) if (hero == index) return 2;
        for (int country : COUNTRY_STILL_THEMES) if (country == index) return 2;
        return 3;
    }
    private static boolean hasThemeMotion(int index) {
        return themeTier(index) == 3;
    }
    private static boolean isColorTheme(int index) {
        return index < 3 || (index >= 13 && index <= 17);
    }
    private static int requiredThemeLevel(int index) {
        return TIER_UNLOCK_LEVELS[themeTier(index) - 1];
    }
    private static final int[] THEME_BACKDROP_IDS = {
            R.drawable.theme_00, R.drawable.theme_01, R.drawable.theme_02, R.drawable.theme_03,
            R.drawable.theme_04, R.drawable.theme_05, R.drawable.theme_06, R.drawable.theme_07,
            R.drawable.theme_08, R.drawable.theme_09, R.drawable.theme_10, R.drawable.theme_11,
            R.drawable.theme_12, R.drawable.theme_13, R.drawable.theme_14, R.drawable.theme_15,
            R.drawable.theme_16, R.drawable.theme_17, R.drawable.theme_18, R.drawable.theme_19,
            R.drawable.space_nebula, R.drawable.theme_21, R.drawable.theme_22, R.drawable.theme_23,
            R.drawable.theme_24, R.drawable.theme_25,
            R.drawable.sakura_academy, R.drawable.spirit_sky, R.drawable.japan_lanterns, R.drawable.mexico_plaza,
            R.drawable.sakura_academy, R.drawable.spirit_sky, R.drawable.japan_lanterns, R.drawable.mexico_plaza,
            R.drawable.theme_03,
            R.drawable.theme_04,
            R.drawable.theme_05,
            R.drawable.theme_06,
            R.drawable.theme_07,
            R.drawable.theme_08,
            R.drawable.theme_09,
            R.drawable.theme_10,
            R.drawable.theme_11,
            R.drawable.theme_12,
            R.drawable.country_us,
            R.drawable.country_spain,
            R.drawable.country_brazil,
            R.drawable.country_france,
            R.drawable.country_italy,
            R.drawable.country_korea,
            R.drawable.country_us,
            R.drawable.country_spain,
            R.drawable.country_brazil,
            R.drawable.country_france,
            R.drawable.country_italy,
            R.drawable.country_korea,
            R.drawable.anime_myhero,
            R.drawable.anime_bleach,
            R.drawable.anime_onepiece,
            R.drawable.anime_naruto,
            R.drawable.anime_dragonball,
            R.drawable.theme_yin_yang
    };
    private static final int[] THEME_FX_IDS = {
            R.drawable.fx_nebula,
            R.drawable.fx_water,
            R.drawable.fx_cosmic,
            R.drawable.fx_web_spider,
            R.drawable.fx_gold,
            R.drawable.fx_thunder,
            R.drawable.fx_gamma,
            R.drawable.fx_shield,
            R.drawable.fx_cosmic,
            R.drawable.fx_petal,
            R.drawable.fx_stealth,
            R.drawable.fx_nebula,
            R.drawable.fx_solar,
            R.drawable.fx_petal,
            R.drawable.fx_leaf,
            R.drawable.fx_gold,
            R.drawable.fx_petal,
            R.drawable.fx_solar,
            R.drawable.fx_candy,
            R.drawable.fx_toxic,
            R.drawable.fx_shooting_star,
            R.drawable.fx_flame,
            R.drawable.fx_water,
            R.drawable.fx_ice,
            R.drawable.fx_leaf,
            R.drawable.fx_lightning,
            R.drawable.fx_petal,
            R.drawable.fx_cosmic,
            0,
            0,
            R.drawable.fx_petal,
            R.drawable.fx_cosmic,
            R.drawable.fx_japan_lantern,
            R.drawable.fx_marigold,
            R.drawable.fx_web_spider,
            R.drawable.fx_gold,
            R.drawable.fx_thunder,
            R.drawable.fx_gamma,
            R.drawable.fx_shield,
            R.drawable.fx_cosmic,
            R.drawable.fx_petal,
            R.drawable.fx_stealth,
            R.drawable.fx_nebula,
            R.drawable.fx_solar,
            0,
            0,
            0,
            0,
            0,
            0,
            R.drawable.fx_us_star,
            R.drawable.fx_spain_fan,
            R.drawable.fx_brazil_leaf,
            R.drawable.fx_france_lavender,
            R.drawable.fx_italy_olive,
            R.drawable.fx_korea_lantern,
            R.drawable.fx_gamma,
            R.drawable.fx_cosmic,
            R.drawable.fx_water,
            R.drawable.fx_leaf,
            R.drawable.fx_solar,
            R.drawable.fx_yin_yang
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
    private final LruCache<Long, Bitmap> previews = new LruCache<Long, Bitmap>(24 * 1024) {
        @Override protected int sizeOf(Long key, Bitmap value) { return Math.max(1, (value.getByteCount() + 1023) / 1024); }
    };
    private final ArrayList<Photo> photos = new ArrayList<>();
    private final HashMap<String, ArrayList<Photo>> photosByMonth = new HashMap<>();
    private final AppServices services = AppServices.offline();
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
    private boolean reviewing, loading, duplicateScanning, reloadPhotosPending;
    private boolean fullScreenReview, reviewActionRunning;
    private Bitmap reviewBitmap;
    private long reviewBitmapId = -1;
    private boolean showingTrash, deletingOld;
    private boolean showingSettings, soundEnabled, musicEnabled, statsExpanded, swipeHintSeen, adminMode;
    private boolean arachnophobiaMode, animateThemeChange;
    private int themeChoice, musicVolume;
    private int swipeStyle, swipeIntensity = 55, swipeSpeed = 100;
    private android.animation.ValueAnimator swipePreviewAnimator;
    private SwipeEffect swipePreviewEffect;
    private AudioTrack musicTrack;
    private final Bitmap[] themeBackdrops = new Bitmap[THEME_NAMES.length];
    private final Bitmap[] themeEffects = new Bitmap[THEME_NAMES.length];
    private Bitmap fireBackgroundFrames, fireSparkSprites;
    private final HashMap<String, Integer> scrollPositions = new HashMap<>();
    private ScrollView activeScroll;
    private String activeScrollPage;
    private Bitmap candySprites;
    private SensorManager sensorManager;
    private Sensor gravitySensor;
    private TextureBackdrop activeBackdrop;
    private android.graphics.Insets safeInsets = android.graphics.Insets.NONE;
    private FrameLayout zoomOverlay;
    private Bitmap zoomBitmap;
    private final java.util.Random cometRandom = new java.util.Random();
    private long nextCometAt, cometStart, cometDuration;
    private boolean cometFlying;
    private float cometFromX, cometFromY, cometToX, cometToY, cometAngle;
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
            // Upright sensor Y is positive. Keep screen gravity downward even when inverted.
            gravityX = Math.max(-1, Math.min(1, -x / 7f));
            gravityY = candyGravityY(y);
        }
    };
    private static float candyGravityY(float screenY) {
        return Math.max(.35f, Math.min(1f, .35f + screenY / 7f));
    }
    private final Handler uiHandler = new Handler(Looper.getMainLooper());
    private long pendingTrash = -1;
    private long pendingRestore = -1;
    private ReviewUndo lastUndo, pendingTrashUndo;
    private boolean pendingRestoreUndo;
    private int generation;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        if (sensorManager != null) gravitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
        themeChoice = getPreferences(MODE_PRIVATE).getInt("theme", 0);
        adminMode = getPreferences(MODE_PRIVATE).getBoolean("admin_mode", false);
        soundEnabled = getPreferences(MODE_PRIVATE).getBoolean("sound_enabled", true);
        musicEnabled = getPreferences(MODE_PRIVATE).getBoolean("music_enabled", false);
        musicVolume = getPreferences(MODE_PRIVATE).getInt("music_volume", 18);
        arachnophobiaMode = getPreferences(MODE_PRIVATE).getBoolean("arachnophobia_mode", false);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        reviewed = new HashSet<>(getPreferences(MODE_PRIVATE).getStringSet("reviewed", Collections.emptySet()));
        rewarded = new HashSet<>(getPreferences(MODE_PRIVATE).getStringSet("rewarded", Collections.emptySet()));
        keptIds = new HashSet<>(getPreferences(MODE_PRIVATE).getStringSet("kept_ids", reviewed));
        trashedIds = new HashSet<>(getPreferences(MODE_PRIVATE).getStringSet("trashed_ids", Collections.emptySet()));
        swipeHintSeen = getPreferences(MODE_PRIVATE).getBoolean("swipe_hint_seen",
                !getPreferences(MODE_PRIVATE).getStringSet("seen_swipe_overlays", Collections.emptySet()).isEmpty());
        statsExpanded = getPreferences(MODE_PRIVATE).getBoolean("stats_expanded", false);
        xp = getPreferences(MODE_PRIVATE).getInt("xp", 0);
        if (state != null) {
            selectedYear = state.getInt("selectedYear", -1);
            selectedMonth = state.getString("selectedMonth");
            reviewing = state.getBoolean("reviewing");
            fullScreenReview = state.getBoolean("fullScreenReview");
            showingSettings = state.getBoolean("showingSettings");
            showingTrash = state.getBoolean("showingTrash");
            pendingTrash = state.getLong("pendingTrash", -1);
            pendingRestore = state.getLong("pendingRestore", -1);
            pendingRestoreUndo = state.getBoolean("pendingRestoreUndo");
            lastUndo = readUndoState(state.getBundle("lastUndo"));
            pendingTrashUndo = readUndoState(state.getBundle("pendingTrashUndo"));
            Bundle savedScroll = state.getBundle("scrollPositions");
            if (savedScroll != null) for (String key : savedScroll.keySet())
                scrollPositions.put(key, savedScroll.getInt(key));
        }
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
        clearSwipePreview();
        super.onPause();
    }

    @Override public void onDestroy() {
        stopMusic();
        closePhotoZoom();
        for (Bitmap bitmap : themeBackdrops) if (bitmap != null && !bitmap.isRecycled()) bitmap.recycle();
        for (Bitmap bitmap : themeEffects) if (bitmap != null && !bitmap.isRecycled()) bitmap.recycle();
        if (fireBackgroundFrames != null && !fireBackgroundFrames.isRecycled()) fireBackgroundFrames.recycle();
        if (fireSparkSprites != null && !fireSparkSprites.isRecycled()) fireSparkSprites.recycle();
        for (Bitmap bitmap : swipeSprites.values()) if (!bitmap.isRecycled()) bitmap.recycle();
        swipeSprites.clear();
        if (candySprites != null && !candySprites.isRecycled()) candySprites.recycle();
        reviewBitmap = null; reviewBitmapId = -1;
        generation++;
        previews.evictAll();
        io.shutdownNow();
        duplicateWorker.shutdownNow();
        super.onDestroy();
    }

    @Override public void onBackPressed() {
        if (reviewActionRunning) return;
        if (zoomOverlay != null) closePhotoZoom();
        else if (fullScreenReview) { fullScreenReview = false; render(); }
        else super.onBackPressed();
    }

    @Override protected void onSaveInstanceState(Bundle state) {
        rememberScroll();
        state.putInt("selectedYear", selectedYear);
        state.putString("selectedMonth", selectedMonth);
        state.putBoolean("reviewing", reviewing);
        state.putBoolean("fullScreenReview", fullScreenReview);
        state.putBoolean("showingSettings", showingSettings);
        state.putBoolean("showingTrash", showingTrash);
        state.putLong("pendingTrash", pendingTrash);
        state.putLong("pendingRestore", pendingRestore);
        state.putBoolean("pendingRestoreUndo", pendingRestoreUndo);
        if (lastUndo != null) state.putBundle("lastUndo", undoState(lastUndo));
        if (pendingTrashUndo != null) state.putBundle("pendingTrashUndo", undoState(pendingTrashUndo));
        Bundle savedScroll = new Bundle();
        for (Map.Entry<String, Integer> entry : scrollPositions.entrySet())
            savedScroll.putInt(entry.getKey(), entry.getValue());
        state.putBundle("scrollPositions", savedScroll);
        super.onSaveInstanceState(state);
    }

    private void rememberScroll() {
        if (activeScroll != null && activeScrollPage != null)
            scrollPositions.put(activeScrollPage, activeScroll.getScrollY());
    }

    private void trackScroll(ScrollView scroll, String page) {
        activeScroll = scroll;
        activeScrollPage = page;
        int position = scrollPositions.getOrDefault(page, 0);
        scroll.post(() -> { if (activeScroll == scroll) scroll.scrollTo(0, position); });
        scroll.setOnScrollChangeListener((view, x, y, oldX, oldY) -> scrollPositions.put(page, y));
    }

    private void applyTheme() {
        // Retired Sakura Academy shares its artwork with Sakura Breeze; preserve old selections.
        if (themeChoice == 26) {
            themeChoice = 30;
            getPreferences(MODE_PRIVATE).edit().putInt("theme", themeChoice).apply();
        }
        if (themeChoice < 0 || themeChoice >= THEME_COLORS.length ||
                (!adminMode && xp / 500 + 1 < requiredThemeLevel(themeChoice))) themeChoice = 0;
        for (int i = 0; i < themeBackdrops.length; i++) if (i != themeChoice) {
            if (themeBackdrops[i] != null) { themeBackdrops[i].recycle(); themeBackdrops[i] = null; }
            if (themeEffects[i] != null) { themeEffects[i].recycle(); themeEffects[i] = null; }
        }
        loadSwipePreferences();
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
        if (!hasAccess()) return;
        if (loading) { reloadPhotosPending = true; return; }
        loading = true; reloadPhotosPending = false;
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
                        java.time.YearMonth date = java.time.YearMonth.from(java.time.Instant.ofEpochMilli(p.timestamp)
                                .atZone(java.time.ZoneId.systemDefault()));
                        p.year = date.getYear(); p.month = date.toString();
                        found.add(p);
                    }
                }
            } catch (Exception ignored) { }
            found.sort((a, b) -> Long.compare(b.timestamp, a.timestamp));
            runOnUiThread(() -> {
                if (token != generation || isDestroyed()) return;
                photos.clear(); photos.addAll(found); rebuildMonthIndex();
                duplicates.clear(); loading = false;
                render();
                if (reloadPhotosPending) { loadPhotos(); return; }
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
                        while ((count = stream.read(buffer)) != -1) {
                            if (token != generation || Thread.currentThread().isInterrupted()) return;
                            digest.update(buffer, 0, count);
                        }
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

    private void applyContentInsets() {
        if (root == null) return;
        boolean full = reviewing && fullScreenReview && !showingSettings && !showingTrash;
        root.setPadding(safeInsets.left + (full ? 0 : dp(22)), safeInsets.top + (full ? 0 : dp(20)),
                safeInsets.right + (full ? 0 : dp(22)), safeInsets.bottom + (full ? 0 : dp(16)));
    }
    private void render() {
        clearSwipePreview();
        rememberScroll();
        if (zoomOverlay != null && zoomOverlay.getParent() instanceof android.view.ViewGroup)
            ((android.view.ViewGroup) zoomOverlay.getParent()).removeView(zoomOverlay);
        FrameLayout previousHost = host;
        boolean transition = animateThemeChange && previousHost != null && previousHost.getParent() != null;
        animateThemeChange = false;
        activeScroll = null;
        activeScrollPage = null;
        host = new FrameLayout(this);
        activeBackdrop = new TextureBackdrop();
        host.addView(activeBackdrop, new FrameLayout.LayoutParams(-1, -1));
        if (hasThemeMotion(themeChoice) && themeChoice != 18 && themeChoice != 21)
            host.addView(new ThemeMotionOverlay(), new FrameLayout.LayoutParams(-1, -1));
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(22), dp(20), dp(22), dp(16));
        applyContentInsets();
        host.setOnApplyWindowInsetsListener((view, insets) -> {
            if (view == host) {
                safeInsets = insets.getInsets(android.view.WindowInsets.Type.systemBars() | android.view.WindowInsets.Type.displayCutout());
                applyContentInsets();
            }
            return insets;
        });
        host.addView(root, new FrameLayout.LayoutParams(-1, -1));
        if (!hasAccess()) intro();
        else if (loading) heading("Photo Sweep", "Gathering your photos…");
        else if (showingSettings) settingsScreen();
        else if (showingTrash) trashScreen();
        else if (reviewing && selectedMonth != null) reviewScreen();
        else if (selectedYear != -1) monthsScreen();
        else yearsScreen();
        if (transition) {
            FrameLayout content = findViewById(android.R.id.content);
            host.setAlpha(0f);
            content.addView(host, new FrameLayout.LayoutParams(-1, -1));
            host.animate().alpha(1f).setDuration(260).withEndAction(() -> content.removeView(previousHost)).start();
        } else setContentView(host);
        host.requestApplyInsets();
        if (zoomOverlay != null) {
            FrameLayout content = findViewById(android.R.id.content);
            content.addView(zoomOverlay, new FrameLayout.LayoutParams(-1, -1));
        }
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
        TextView trash = new TextView(this); trash.setText("🗑"); trash.setTextSize(25); trash.setTextColor(GOLD);
        trash.setGravity(Gravity.CENTER); trash.setContentDescription("Recently trashed, " + ReviewNavigation.photoCount(trashEntries.size()));
        trash.setBackground(themeButton(PANEL, 16));
        LinearLayout.LayoutParams trashLp = new LinearLayout.LayoutParams(dp(52), dp(52)); trashLp.rightMargin = dp(8);
        top.addView(trash, trashLp);
        trash.setOnClickListener(v -> { showingTrash = true; render(); });
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
        trackScroll(scroll, "years");
        LinearLayout list = new LinearLayout(this); list.setOrientation(LinearLayout.VERTICAL); scroll.addView(list);
        for (Map.Entry<Integer, int[]> entry : years.entrySet()) {
            int year = entry.getKey(); int[] count = entry.getValue();
            tile(list, Integer.toString(year), ReviewNavigation.photoCount(count[0]) + "  •  " + count[1] + " to review", () -> { selectedYear = year; render(); });
        }
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
        trackScroll(scroll, "settings");
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
            applyTheme(); animateThemeChange = true; render();
        });
        Button reset = new Button(this); reset.setText("Reset account progress"); reset.setTextColor(INK);
        reset.setBackground(rounded(PANEL, 12)); list.addView(reset, new LinearLayout.LayoutParams(-1, dp(52)));
        reset.setOnClickListener(v -> confirmProgressReset());
        sectionTitle(list, "THEMES");
        int level = xp / 500 + 1;
        LinearLayout tier1 = themeGroup(list, "tier1", "TIER 1 · COLORS",
                "Level " + TIER_UNLOCK_LEVELS[0] + " · color palettes", isColorTheme(themeChoice));
        addThemeChoices(tier1, COLOR_THEMES, level);
        LinearLayout tier2 = themeGroup(list, "tier2", "TIER 2 · DISTINCTIVE",
                "Level " + TIER_UNLOCK_LEVELS[1] + " · still illustrated artwork", themeTier(themeChoice) == 2);
        LinearLayout heroes = themeGroup(tier2, "heroes", "HERO & FANTASY", "Still hero and fantasy artwork", true);
        addThemeChoices(heroes, HERO_THEMES, level);
        addThemeChoices(themeGroup(tier2, "balance", "BALANCE", "Ink and ivory koi garden", themeChoice == 61), new int[]{61}, level);
        addCountryThemes(tier2, false, level);
        LinearLayout tier3 = themeGroup(list, "tier3", "TIER 3 · ANIMATED",
                "Level " + TIER_UNLOCK_LEVELS[2] + " · moving worlds and effects", themeTier(themeChoice) == 3);
        addThemeChoices(themeGroup(tier3, "heroes3", "HERO & FANTASY", "Animated hero powers", themeChoice >= 34 && themeChoice <= 43), HERO_ANIMATED_THEMES, level);
        LinearLayout worlds = themeGroup(tier3, "worlds", "WORLDS & ELEMENTS", "Animated settings", true);
        addThemeChoices(worlds, ANIMATED_THEMES, level);
        addThemeChoices(themeGroup(tier3, "anime3", "ANIME", "Hero cities, spirit worlds, pirates and ninja villages", isAnimeTheme(themeChoice)), ANIME_ANIMATED_THEMES, level);
        addCountryThemes(tier3, true, level);
        addSwipeControls(list);
        sectionTitle(list, "ACCOUNT & SUPPORT");
        TextView account = new TextView(this); account.setText("Account · " + services.account.displayName());
        account.setTextColor(INK); account.setTextSize(16); list.addView(account);
        TextView support = new TextView(this);
        support.setText("Your photos stay on this device. Accounts are optional. Support purchases and ad removal will become available after store setup; this version has no ads.");
        support.setTextColor(MUTED); support.setTextSize(14); support.setPadding(0, dp(8), 0, dp(16)); list.addView(support);
        Button setup = new Button(this); setup.setText("Account & support details"); setup.setTextColor(INK);
        setup.setBackground(rounded(PANEL, 12)); list.addView(setup, new LinearLayout.LayoutParams(-1, dp(52)));
        setup.setOnClickListener(v -> showSupportDetails());
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

    private void addThemeChoices(LinearLayout list, int[] choices, int level) {
        for (int choice : choices) {
            int requiredLevel = requiredThemeLevel(choice);
            boolean unlocked = adminMode || level >= requiredLevel;
            themeTile(list, choice, unlocked, requiredLevel, () -> {
                if (!unlocked) return;
                themeChoice = choice; getPreferences(MODE_PRIVATE).edit().putInt("theme", choice).apply();
                applyTheme(); animateThemeChange = true; render();
            });
            if (choice == 34) settingSwitch(list, "Arachnophobia mode", "Hide the spider in animated Web Hero", arachnophobiaMode, value -> {
                arachnophobiaMode = value;
                getPreferences(MODE_PRIVATE).edit().putBoolean("arachnophobia_mode", value).apply();
                if (activeBackdrop != null) activeBackdrop.resetSpider();
            });
        }
    }

    private void sectionTitle(LinearLayout parent, String title) {
        TextView label = new TextView(this); label.setText(title); label.setTextColor(GREEN);
        label.setLetterSpacing(.12f); label.setTextSize(13); label.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2); lp.topMargin = dp(18); lp.bottomMargin = dp(12); parent.addView(label, lp);
    }

    private LinearLayout themeGroup(LinearLayout parent, String key, String title, String description, boolean openByDefault) {
        LinearLayout header = new LinearLayout(this); header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(16), dp(10), dp(14), dp(10)); header.setBackground(rounded(PANEL, 16));
        LinearLayout.LayoutParams headerLp = new LinearLayout.LayoutParams(-1, dp(68));
        headerLp.bottomMargin = dp(8); parent.addView(header, headerLp);
        LinearLayout labels = new LinearLayout(this); labels.setOrientation(LinearLayout.VERTICAL);
        header.addView(labels, new LinearLayout.LayoutParams(0, -2, 1));
        TextView name = new TextView(this); name.setText(title); name.setTextColor(GREEN);
        name.setTextSize(14); name.setTypeface(Typeface.DEFAULT, Typeface.BOLD); labels.addView(name);
        TextView detail = new TextView(this); detail.setText(description); detail.setTextColor(MUTED);
        detail.setTextSize(12); labels.addView(detail);
        TextView arrow = new TextView(this); arrow.setTextColor(INK); arrow.setTextSize(23);
        header.addView(arrow);
        LinearLayout content = new LinearLayout(this); content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(10), 0, 0, dp(6)); parent.addView(content);
        boolean expanded = getPreferences(MODE_PRIVATE).getBoolean("theme_group_" + key, openByDefault);
        content.setVisibility(expanded ? View.VISIBLE : View.GONE);
        arrow.setText(expanded ? "⌃" : "⌄");
        header.setOnClickListener(v -> {
            boolean show = content.getVisibility() != View.VISIBLE;
            content.setVisibility(show ? View.VISIBLE : View.GONE);
            arrow.setText(show ? "⌃" : "⌄");
            getPreferences(MODE_PRIVATE).edit().putBoolean("theme_group_" + key, show).apply();
        });
        return content;
    }

    private static int countryCollection(int theme) {
        for (int i = 0; i < COUNTRY_NAMES.length; i++)
            if (COUNTRY_STILL_THEMES[i] == theme || COUNTRY_ANIMATED_THEMES[i] == theme) return i;
        return -1;
    }
    private static boolean isAnimeTheme(int theme) {
        for (int anime : ANIME_ANIMATED_THEMES) if (anime == theme) return true;
        return false;
    }
    private void loadSwipePreferences() {
        String suffix = "_" + themeChoice;
        swipeStyle = Math.max(0, Math.min(SWIPE_STYLE_NAMES.length - 1, getPreferences(MODE_PRIVATE).getInt("swipe_style" + suffix, 0)));
        swipeIntensity = Math.max(15, Math.min(100, getPreferences(MODE_PRIVATE).getInt("swipe_intensity" + suffix, 55)));
        swipeSpeed = Math.max(50, Math.min(200, getPreferences(MODE_PRIVATE).getInt("swipe_speed" + suffix, 100)));
    }
    private void saveSwipePreferences() {
        String suffix = "_" + themeChoice;
        getPreferences(MODE_PRIVATE).edit().putInt("swipe_style" + suffix, swipeStyle)
                .putInt("swipe_intensity" + suffix, swipeIntensity).putInt("swipe_speed" + suffix, swipeSpeed).apply();
    }
    private Bitmap effectSprite(int selected) {
        if (selected < 0 || selected >= THEME_FX_IDS.length || THEME_FX_IDS[selected] == 0) return null;
        if (motionStyle(selected) == 3 && arachnophobiaMode) return null;
        if (themeEffects[selected] == null) {
            BitmapFactory.Options options = new BitmapFactory.Options(); options.inScaled = false;
            themeEffects[selected] = BitmapFactory.decodeResource(getResources(), THEME_FX_IDS[selected], options);
        }
        return themeEffects[selected];
    }
    private void confirmProgressReset() {
        new android.app.AlertDialog.Builder(this).setTitle("Reset account progress?")
                .setMessage("Start again at level 1 with 0 XP, clear review history and statistics, and turn off Admin mode. Themes and swipe preferences return to defaults. Your photos and existing Trash recovery timers stay unchanged. This resets progress on this device only.")
                .setNegativeButton("Cancel", null).setPositiveButton("Reset progress", (dialog, which) -> {
                    if (pendingTrash != -1 || pendingRestore != -1 || reviewActionRunning) {
                        Toast.makeText(this, "Finish the current photo action first", Toast.LENGTH_SHORT).show(); return;
                    }
                    android.content.SharedPreferences prefs = getPreferences(MODE_PRIVATE);
                    android.content.SharedPreferences.Editor editor = prefs.edit();
                    for (String key : prefs.getAll().keySet()) if (ResetPolicy.clears(key)) editor.remove(key);
                    editor.apply();
                    reviewed.clear(); rewarded.clear(); keptIds.clear(); trashedIds.clear();
                    xp = 0; keptCount = 0; trashedCount = 0; restoredCount = 0;
                    lastUndo = null; pendingTrashUndo = null; adminMode = false; themeChoice = 0;
                    statsExpanded = false; swipeHintSeen = false; fullScreenReview = false;
                    scrollPositions.clear(); activeScrollPage = null; closePhotoZoom(); applyTheme(); render();
                    Toast.makeText(this, "Progress reset · level 1", Toast.LENGTH_SHORT).show();
                }).show();
    }
    private void showSupportDetails() {
        new android.app.AlertDialog.Builder(this).setTitle("Account & Support")
                .setMessage("ACCOUNT\nGuest · photos stay on your device. Optional Google sign-in needs provider configuration.\n\nSUPPORT DEVELOPMENT\nSmall, medium, and large support purchases are planned. Prices will come from Google Play in your local currency.\n\nREMOVE ADS\nA one-time purchase will remove all ads. There are no ads in this release. Owned purchases will be restorable through Google Play.\n\nSETUP STATUS\nPlay Console products, AdMob placements, sign-in configuration, and purchase verification are not configured yet. No payment is collected by this release.")
                .setPositiveButton("Done", null).show();
    }
    private boolean canCustomizeSwipe() { return hasThemeMotion(themeChoice) || countryCollection(themeChoice) >= 0 || themeChoice == 61; }
    private void addSwipeControls(LinearLayout list) {
        sectionTitle(list, "SWIPE ANIMATIONS");
        boolean enabled = canCustomizeSwipe();
        TextView note = new TextView(this);
        note.setText(enabled ? "Customize " + THEME_NAMES[themeChoice] + ". " + SwipeTheme.description(SwipeTheme.forTheme(themeChoice)) + ". Each theme remembers its own swipe settings."
                : "Choose a country, Yin Yang, or Tier 3 theme at level " + TIER_UNLOCK_LEVELS[2] + " to customize swipe effects.");
        note.setTextColor(MUTED); note.setTextSize(13); note.setPadding(0, 0, 0, dp(8)); list.addView(note);
        android.widget.Spinner picker = new android.widget.Spinner(this);
        android.widget.ArrayAdapter<String> choices = new android.widget.ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, SWIPE_STYLE_NAMES) {
            @Override public View getView(int position, View convertView, android.view.ViewGroup parent) {
                TextView view = (TextView) super.getView(position, convertView, parent);
                view.setTextColor(INK); view.setTextSize(15); view.setPadding(dp(12), dp(10), dp(12), dp(10)); return view;
            }
            @Override public View getDropDownView(int position, View convertView, android.view.ViewGroup parent) {
                TextView view = (TextView) super.getDropDownView(position, convertView, parent);
                view.setTextColor(INK); view.setBackgroundColor(PANEL); view.setTextSize(15);
                view.setPadding(dp(12), dp(12), dp(12), dp(12)); return view;
            }
        };
        choices.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        picker.setAdapter(choices); picker.setSelection(swipeStyle); picker.setEnabled(enabled);
        picker.setBackground(rounded(PANEL, 12)); list.addView(picker, new LinearLayout.LayoutParams(-1, dp(52)));
        picker.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                if (!canCustomizeSwipe() || position == swipeStyle) return;
                swipeStyle = position; saveSwipePreferences();
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) { }
        });
        TextView intensity = new TextView(this); intensity.setText("Intensity · " + swipeIntensity + "%");
        intensity.setTextColor(INK); intensity.setTextSize(14); list.addView(intensity);
        SeekBar density = new SeekBar(this); density.setMax(85); density.setProgress(swipeIntensity - 15); density.setEnabled(enabled);
        density.setProgressTintList(android.content.res.ColorStateList.valueOf(GREEN)); list.addView(density, new LinearLayout.LayoutParams(-1, dp(48)));
        density.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar bar, int value, boolean user) {
                if (!user) return; swipeIntensity = value + 15; intensity.setText("Intensity · " + swipeIntensity + "%"); saveSwipePreferences();
            }
            @Override public void onStartTrackingTouch(SeekBar bar) { }
            @Override public void onStopTrackingTouch(SeekBar bar) { }
        });
        TextView speed = new TextView(this); speed.setText("Speed · " + swipeSpeed + "%");
        speed.setTextColor(INK); speed.setTextSize(14); list.addView(speed);
        SeekBar tempo = new SeekBar(this); tempo.setMax(150); tempo.setProgress(swipeSpeed - 50); tempo.setEnabled(enabled);
        tempo.setProgressTintList(android.content.res.ColorStateList.valueOf(GREEN)); list.addView(tempo, new LinearLayout.LayoutParams(-1, dp(48)));
        tempo.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar bar, int value, boolean user) {
                if (!user) return; swipeSpeed = value + 50; speed.setText("Speed · " + swipeSpeed + "%"); saveSwipePreferences();
            }
            @Override public void onStartTrackingTouch(SeekBar bar) { }
            @Override public void onStopTrackingTouch(SeekBar bar) { }
        });
        LinearLayout previews = new LinearLayout(this); list.addView(previews);
        for (boolean keep : new boolean[]{false, true}) {
            Button preview = new Button(this); preview.setText(keep ? "Preview Keep →" : "← Preview Trash");
            preview.setTextColor(INK); preview.setTextSize(13); preview.setBackground(rounded(PANEL, 12));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(52), 1); lp.setMargins(dp(3), 0, dp(3), 0);
            previews.addView(preview, lp);
            preview.setOnClickListener(v -> previewSwipe(keep));
        }
    }
    private void clearSwipePreview() {
        if (swipePreviewAnimator != null) swipePreviewAnimator.cancel();
        if (swipePreviewEffect != null && swipePreviewEffect.getParent() instanceof android.view.ViewGroup)
            ((android.view.ViewGroup) swipePreviewEffect.getParent()).removeView(swipePreviewEffect);
        swipePreviewEffect = null;
    }
    private void previewSwipe(boolean keep) {
        clearSwipePreview();
        FrameLayout target = host; SwipeEffect effect = new SwipeEffect(); swipePreviewEffect = effect;
        target.addView(effect, new FrameLayout.LayoutParams(-1, -1));
        android.animation.ValueAnimator animation = android.animation.ValueAnimator.ofFloat(0f, keep ? 1f : -1f);
        animation.setDuration(900L * 100 / swipeSpeed);
        animation.addUpdateListener(value -> { effect.progress = (float)value.getAnimatedValue(); effect.invalidate(); });
        animation.addListener(new android.animation.AnimatorListenerAdapter() {
            private boolean cancelled;
            @Override public void onAnimationCancel(android.animation.Animator value) { cancelled = true; }
            @Override public void onAnimationEnd(android.animation.Animator value) {
                if (swipePreviewAnimator == value) swipePreviewAnimator = null;
                if (cancelled) { target.removeView(effect); return; }
                effect.release(keep);
                uiHandler.postDelayed(() -> { target.removeView(effect); if (swipePreviewEffect == effect) swipePreviewEffect = null; }, SwipeMotion.duration(swipeSpeed) + 40);
            }
        });
        swipePreviewAnimator = animation; animation.start();
    }

    private void addCountryThemes(LinearLayout parent, boolean animated, int level) {
        String suffix = animated ? "3" : "2";
        LinearLayout countries = themeGroup(parent, "countries" + suffix, "COUNTRIES",
                COUNTRY_NAMES.length + " country collections", countryCollection(themeChoice) >= 0);
        for (int i = 0; i < COUNTRY_NAMES.length; i++) {
            int choice = animated ? COUNTRY_ANIMATED_THEMES[i] : COUNTRY_STILL_THEMES[i];
            addThemeChoices(themeGroup(countries, COUNTRY_KEYS[i] + suffix, COUNTRY_NAMES[i].toUpperCase(Locale.ROOT),
                    animated ? COUNTRY_MOTIONS[i] : COUNTRY_SCENES[i], themeChoice == choice), new int[]{choice}, level);
        }
    }

    private void themeTile(LinearLayout parent, int index, boolean unlocked, int requiredLevel, Runnable action) {
        LinearLayout row = new LinearLayout(this); row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(14), dp(8), dp(14), dp(8)); row.setBackground(rounded(PANEL, 16));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(70)); lp.bottomMargin = dp(8); parent.addView(row, lp);
        LinearLayout copy = new LinearLayout(this); copy.setOrientation(LinearLayout.VERTICAL);
        row.addView(copy, new LinearLayout.LayoutParams(0, -2, 1));
        TextView name = new TextView(this); name.setText((themeChoice == index ? "✓  " : "") + THEME_NAMES[index]);
        name.setTextSize(16); name.setTextColor(INK); name.setTypeface(Typeface.DEFAULT, Typeface.BOLD); copy.addView(name);
        TextView detail = new TextView(this); detail.setText(unlocked ? "Tier " + themeTier(index) + " · " + (hasThemeMotion(index) ? "Animated effects" : themeTier(index) == 1 ? "Color palette" : "Illustrated background") : "Unlock at level " + requiredLevel);
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
        trackScroll(scroll, "months:" + selectedYear);
        LinearLayout list = new LinearLayout(this); list.setOrientation(LinearLayout.VERTICAL); scroll.addView(list);
        for (Map.Entry<String, int[]> entry : months.entrySet()) {
            String month = entry.getKey(); int[] count = entry.getValue();
            String name = ReviewNavigation.title(month, false);
            tile(list, name, ReviewNavigation.photoCount(count[0]) + "  •  " + count[1] + " to review", () -> { openReviewMonth(month); });
        }
    }

    private void rebuildMonthIndex() {
        photosByMonth.clear();
        for (Photo photo : photos) photosByMonth.computeIfAbsent(photo.month, key -> new ArrayList<>()).add(photo);
    }

    private List<Photo> monthPhotos() {
        List<Photo> month = photosByMonth.get(selectedMonth);
        return month == null ? Collections.emptyList() : month;
    }

    private void openReviewMonth(String month) {
        selectedMonth = month; selectedYear = Integer.parseInt(month.substring(0, 4));
        reviewing = true; showingTrash = false; showingSettings = false; lastUndo = null; render();
    }

    private void addMonthNavigation() {
        String previous = ReviewNavigation.adjacent(photosByMonth.keySet(), selectedMonth, false);
        String next = ReviewNavigation.adjacent(photosByMonth.keySet(), selectedMonth, true);
        if (previous == null && next == null) return;
        spacer(14);
        LinearLayout navigation = new LinearLayout(this);
        root.addView(navigation, new LinearLayout.LayoutParams(-1, dp(58)));
        if (previous != null) monthNavigationButton(navigation, previous, false);
        if (next != null) monthNavigationButton(navigation, next, true);
    }

    private void monthNavigationButton(LinearLayout row, String month, boolean next) {
        Button control = button(row, (next ? "Next month →" : "← Previous month") + "\n"
                + java.time.YearMonth.parse(month).format(java.time.format.DateTimeFormatter.ofPattern("MMM yyyy", Locale.getDefault())),
                PANEL, INK, () -> openReviewMonth(month));
        control.setTextSize(14); control.setPadding(dp(5), 0, dp(5), 0); control.setMaxLines(2);
        control.setContentDescription("Review " + (next ? "next" : "previous") + " month, " + ReviewNavigation.title(month, true));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, -1, 1);
        if (next && row.getChildCount() > 1) params.leftMargin = dp(8); control.setLayoutParams(params);
    }

    private Bundle undoState(ReviewUndo action) {
        Bundle undo = new Bundle(); undo.putLong("id", action.id); undo.putString("month", action.month);
        undo.putLong("timestamp", action.timestamp); undo.putBoolean("trashed", action.trashed);
        undo.putBoolean("reviewed", action.wasReviewed); undo.putBoolean("kept", action.wasKept);
        undo.putBoolean("wasTrashed", action.wasTrashed); undo.putInt("xp", action.earnedXp); return undo;
    }

    private ReviewUndo readUndoState(Bundle undo) {
        if (undo == null || undo.getString("month") == null) return null;
        ReviewUndo action = new ReviewUndo(undo.getLong("id"), undo.getString("month"), undo.getLong("timestamp"),
                undo.getBoolean("trashed"), undo.getBoolean("reviewed"), undo.getBoolean("kept"), undo.getBoolean("wasTrashed"));
        action.earnedXp = undo.getInt("xp"); return action;
    }

    private ReviewUndo snapshot(Photo photo, boolean trashed) {
        String id = Long.toString(photo.id);
        return new ReviewUndo(photo.id, photo.month, photo.timestamp, trashed, reviewed.contains(id), keptIds.contains(id), trashedIds.contains(id));
    }

    private boolean canUndoLastPhoto() {
        if (lastUndo == null || !lastUndo.month.equals(selectedMonth)) return false;
        return lastUndo.trashed ? trashEntries.stream().anyMatch(entry -> entry.id == lastUndo.id)
                : photos.stream().anyMatch(photo -> photo.id == lastUndo.id);
    }

    private void undoLastPhoto() {
        if (!canUndoLastPhoto() || pendingTrash != -1 || pendingRestore != -1) return;
        if (lastUndo.trashed) {
            if (!canManage()) { requestMediaManagement(); return; }
            for (TrashEntry entry : trashEntries) if (entry.id == lastUndo.id) {
                pendingRestoreUndo = true; restore(entry); return;
            }
        } else { finishUndo(); render(); }
    }

    private void finishUndo() {
        if (lastUndo == null) return;
        lastUndo.rollback(reviewed, keptIds, trashedIds, rewarded);
        xp = lastUndo.restoredXp(xp);
        keptCount = keptIds.size(); trashedCount = trashedIds.size();
        lastUndo = null; saveStats(); saveReviewed(); applyTheme();
    }

    private void trashScreen() {
        back("Photo Sweep", () -> { showingTrash = false; render(); });
        heading("Recently trashed", "Photos wiped after 7 days");
        label("The latest 20 photos can be restored. Older photos are wiped when Trash reaches its limit.", 13, MUTED, false);
        if (trashEntries.isEmpty()) {
            spacer(36); label("Trash is empty", 21, INK, true);
            return;
        }
        ScrollView scroll = new ScrollView(this);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        trackScroll(scroll, "trash");
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
            days.setText(ReviewNavigation.expiry(timeLeft));
            days.setTextColor(MUTED); days.setTextSize(12); info.addView(days);
            Button restore = new Button(this); restore.setText("Restore"); restore.setAllCaps(false);
            restore.setTextColor(INK); restore.setTextSize(13); restore.setBackground(themeButton(PANEL, 12));
            restore.setOnClickListener(v -> restore(entry)); row.addView(restore, new LinearLayout.LayoutParams(dp(90), dp(46)));
        }
    }

    private void reviewScreen() {
        List<Photo> month = monthPhotos();
        Photo current = null; int remaining = 0;
        for (Photo p : month) if (!reviewed.contains(Long.toString(p.id))) { remaining++; if (current == null) current = p; }
        if (fullScreenReview && current != null) { fullScreenReviewScreen(current, remaining, month.size()); return; }
        if (fullScreenReview) { fullScreenReview = false; applyContentInsets(); }
        back("Months", () -> { reviewing = false; fullScreenReview = false; render(); });
        String monthName = ReviewNavigation.title(selectedMonth, true);
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
            if (canUndoLastPhoto()) {
                spacer(20);
                button(root, "Undo last photo", PANEL, GREEN, this::undoLastPhoto);
            }
            spacer(30);
            button(root, "Review this month again", GREEN, Color.WHITE, () -> {
                for (Photo p : month) reviewed.remove(Long.toString(p.id)); lastUndo = null; saveReviewed(); render();
            });
            addMonthNavigation();
            return;
        }
        Photo shown = current;
        FrameLayout stage = new FrameLayout(this);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(-1, 0, 1);
        cardParams.bottomMargin = dp(20); root.addView(stage, cardParams);
        FrameLayout card = new FrameLayout(this);
        card.setBackground(rounded(PANEL, 25)); card.setElevation(dp(8));
        card.setClipToOutline(true);
        card.setContentDescription("Photo. Tap for full-screen review, swipe left to Trash or right to Keep");
        stage.addView(card, new FrameLayout.LayoutParams(-1, -1));
        ImageView photo = new ImageView(this); photo.setScaleType(ImageView.ScaleType.FIT_CENTER);
        photo.setContentDescription("Tap for full-screen photo review");
        card.addView(photo, new FrameLayout.LayoutParams(-1, -1));
        loadReviewPhoto(shown, photo);
        SwipeEffect effect = new SwipeEffect();
        effect.setElevation(dp(18)); stage.addView(effect, new FrameLayout.LayoutParams(-1, -1));
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
        TextView full = pill("⤢ Full screen", INK, PANEL); full.setContentDescription("Full-screen photo review with swiping");
        FrameLayout.LayoutParams fullLp = new FrameLayout.LayoutParams(-2, dp(44), Gravity.BOTTOM | Gravity.END);
        fullLp.setMargins(0, 0, dp(12), dp(12)); stage.addView(full, fullLp); full.setElevation(dp(22));
        full.setOnClickListener(v -> { if (!reviewActionRunning) { fullScreenReview = true; render(); } });
        attachSwipeGesture(card, stage, effect, shown, () -> { fullScreenReview = true; render(); });
        if (canUndoLastPhoto()) {
            TextView undo = label("Undo last photo", 14, GREEN, true);
            undo.setPadding(0, dp(9), 0, 0);
            undo.setOnClickListener(v -> undoLastPhoto());
        }
        addMonthNavigation();
        if (duplicateScanning) { spacer(6); label("Checking for exact duplicates…", 12, MUTED, false); }
    }

    private void attachSwipeGesture(FrameLayout card, FrameLayout stage, SwipeEffect effect, Photo shown, Runnable tap) {
        final boolean[] committed = {false}, multitouch = {false}; final float[] start = new float[2]; final int[] position = new int[2];
        card.setOnTouchListener((view, event) -> {
            if (committed[0] || reviewActionRunning || pendingTrash != -1 || pendingRestore != -1) return true;
            int action = event.getActionMasked();
            if (action == MotionEvent.ACTION_DOWN) {
                multitouch[0] = false;
                start[0] = event.getRawX(); start[1] = event.getRawY(); stage.getLocationOnScreen(position);
                effect.hold(start[0] - position[0], start[1] - position[1]); return true;
            }
            float dx = event.getRawX() - start[0], dy = event.getRawY() - start[1];
            if (action == MotionEvent.ACTION_POINTER_DOWN || action == MotionEvent.ACTION_CANCEL) {
                multitouch[0] = action == MotionEvent.ACTION_POINTER_DOWN;
                effect.cancel(); card.animate().translationX(0).rotation(0).setDuration(180).start(); return true;
            }
            if (multitouch[0]) {
                if (action == MotionEvent.ACTION_UP) multitouch[0] = false;
                return true;
            }
            if (action == MotionEvent.ACTION_MOVE) {
                card.setTranslationX(dx); card.setRotation(Math.max(-13, Math.min(13, dx / dp(28))));
                effect.progress = Math.max(-1, Math.min(1, dx / Math.max(1, card.getWidth() * .62f)));
                effect.follow(event.getRawX() - position[0], event.getRawY() - position[1]); return true;
            }
            if (action == MotionEvent.ACTION_UP) {
                if (SwipeMotion.shouldCommit(dx, dy, dp(85))) {
                    if (dx < 0 && !canManage()) {
                        effect.cancel(); card.animate().translationX(0).rotation(0).setDuration(200).start(); requestMediaManagement();
                    } else {
                        committed[0] = true; reviewActionRunning = true; effect.release(dx > 0);
                        card.animate().translationX((dx > 0 ? 1 : -1) * stage.getWidth() * 1.2f)
                                .rotation(dx > 0 ? 16 : -16).alpha(0).setDuration(SwipeMotion.duration(swipeSpeed))
                                .withEndAction(() -> {
                                    reviewActionRunning = false;
                                    if (isDestroyed()) return;
                                    if (dx > 0) keep(shown); else trash(shown);
                                }).start();
                    }
                } else {
                    effect.cancel(); card.animate().translationX(0).rotation(0).setDuration(200).start();
                    if (tap != null && Math.abs(dx) < dp(12) && Math.abs(dy) < dp(12)) tap.run();
                }
                return true;
            }
            return true;
        });
    }
    private void fullScreenReviewScreen(Photo shown, int remaining, int total) {
        FrameLayout stage = new FrameLayout(this); stage.setBackgroundColor(Color.BLACK);
        root.addView(stage, new LinearLayout.LayoutParams(-1, -1));
        FrameLayout card = new FrameLayout(this);
        FrameLayout.LayoutParams photoArea = new FrameLayout.LayoutParams(-1, -1);
        photoArea.setMargins(0, dp(58), 0, dp(52)); stage.addView(card, photoArea);
        ImageView image = new ImageView(this); image.setScaleType(ImageView.ScaleType.FIT_CENTER);
        image.setContentDescription("Whole photo. Swipe right to Keep or left to Trash");
        card.addView(image, new FrameLayout.LayoutParams(-1, -1)); loadReviewPhoto(shown, image);
        SwipeEffect effect = new SwipeEffect(); effect.setElevation(dp(18)); stage.addView(effect, new FrameLayout.LayoutParams(-1, -1));
        attachSwipeGesture(card, stage, effect, shown, null);
        LinearLayout toolbar = new LinearLayout(this); toolbar.setGravity(Gravity.CENTER_VERTICAL);
        toolbar.setPadding(dp(8), dp(6), dp(8), dp(6)); toolbar.setBackgroundColor(0xC0141A22); toolbar.setElevation(dp(24));
        FrameLayout.LayoutParams top = new FrameLayout.LayoutParams(-1, dp(58), Gravity.TOP); stage.addView(toolbar, top);
        Button exit = new Button(this); exit.setText("Exit full screen"); exit.setTextSize(12); exit.setTextColor(INK); exit.setBackground(rounded(PANEL, 10));
        toolbar.addView(exit, new LinearLayout.LayoutParams(dp(124), -1));
        exit.setOnClickListener(v -> { if (!reviewActionRunning) { fullScreenReview = false; render(); } });
        TextView count = new TextView(this); count.setText(ReviewNavigation.title(selectedMonth, false) + " · " + remaining + "/" + total + " left");
        count.setTextColor(INK); count.setTextSize(12); count.setGravity(Gravity.CENTER); toolbar.addView(count, new LinearLayout.LayoutParams(0, -1, 1));
        Button inspect = new Button(this); inspect.setText("Zoom"); inspect.setTextSize(12); inspect.setTextColor(INK); inspect.setBackground(rounded(PANEL, 10));
        toolbar.addView(inspect, new LinearLayout.LayoutParams(dp(65), -1));
        inspect.setOnClickListener(v -> { if (!reviewActionRunning) showPhotoZoom(shown); });
        LinearLayout footer = new LinearLayout(this); footer.setGravity(Gravity.CENTER_VERTICAL); footer.setBackgroundColor(0xB0141A22); footer.setElevation(dp(24));
        FrameLayout.LayoutParams bottom = new FrameLayout.LayoutParams(-1, dp(52), Gravity.BOTTOM); stage.addView(footer, bottom);
        TextView hint = new TextView(this); hint.setText("← Trash     Keep →"); hint.setTextColor(INK); hint.setTextSize(13); hint.setGravity(Gravity.CENTER);
        footer.addView(hint, new LinearLayout.LayoutParams(0, -1, 1));
        if (canUndoLastPhoto()) {
            Button undo = new Button(this); undo.setText("Undo last photo"); undo.setTextSize(12); undo.setTextColor(INK); undo.setBackground(rounded(PANEL, 10));
            footer.addView(undo, new LinearLayout.LayoutParams(dp(144), dp(44))); undo.setOnClickListener(v -> { if (!reviewActionRunning) undoLastPhoto(); });
        }
    }
    private void loadReviewPhoto(Photo photo, ImageView view) {
        if (reviewBitmapId == photo.id && reviewBitmap != null && !reviewBitmap.isRecycled()) { view.setImageBitmap(reviewBitmap); return; }
        Bitmap preview = previews.get(photo.id); if (preview != null) view.setImageBitmap(preview);
        final FrameLayout requestedHost = host;
        io.execute(() -> {
            if (isDestroyed() || requestedHost != host) return;
            Bitmap full = null;
            try {
                full = ImageDecoder.decodeBitmap(ImageDecoder.createSource(getContentResolver(), photo.uri), (decoder, info, source) -> {
                    int w = info.getSize().getWidth(), h = info.getSize().getHeight();
                    float scale = PhotoFit.scale(w, h);
                    decoder.setTargetSize(Math.max(1, Math.round(w * scale)), Math.max(1, Math.round(h * scale)));
                    decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE);
                });
            } catch (Exception ignored) { }
            final Bitmap decoded = full;
            runOnUiThread(() -> {
                if (decoded == null) return;
                if (isDestroyed() || requestedHost != host || !view.isAttachedToWindow()) { decoded.recycle(); return; }
                reviewBitmap = decoded; reviewBitmapId = photo.id; view.setImageBitmap(decoded);
            });
        });
    }

    private void loadPreview(Photo p, ImageView view) {
        loadPreview(p.id, p.uri, view);
    }

    private void showPhotoZoom(Photo photo) {
        if (zoomOverlay != null) closePhotoZoom();
        FrameLayout overlay = new FrameLayout(this);
        overlay.setBackgroundColor(Color.BLACK);
        overlay.setClickable(true);
        ZoomPhotoView image = new ZoomPhotoView();
        overlay.addView(image, new FrameLayout.LayoutParams(-1, -1));
        Bitmap preview = previews.get(photo.id);
        if (preview != null) image.setBitmap(preview);
        TextView loading = new TextView(this);
        loading.setText("Loading full photo…"); loading.setTextColor(Color.WHITE);
        loading.setTextSize(15); loading.setGravity(Gravity.CENTER);
        overlay.addView(loading, new FrameLayout.LayoutParams(-1, -1));
        TextView close = new TextView(this);
        close.setText("✕"); close.setTextColor(Color.WHITE); close.setTextSize(30);
        close.setGravity(Gravity.CENTER); close.setContentDescription("Close full-screen photo");
        close.setBackground(themeButton(0xFF253342, 18));
        FrameLayout.LayoutParams closeLp = new FrameLayout.LayoutParams(dp(52), dp(52), Gravity.TOP | Gravity.END);
        closeLp.setMargins(0, safeInsets.top + dp(18), safeInsets.right + dp(18), 0); overlay.addView(close, closeLp);
        close.setOnClickListener(v -> closePhotoZoom());
        TextView hint = new TextView(this);
        hint.setText("Pinch to zoom  ·  Drag to inspect  ·  Double tap");
        hint.setTextColor(0xFFD2E2EF); hint.setTextSize(13); hint.setGravity(Gravity.CENTER);
        FrameLayout.LayoutParams hintLp = new FrameLayout.LayoutParams(-1, dp(48), Gravity.BOTTOM);
        hintLp.bottomMargin = safeInsets.bottom; overlay.addView(hint, hintLp);
        zoomOverlay = overlay;
        FrameLayout content = findViewById(android.R.id.content);
        content.addView(overlay, new FrameLayout.LayoutParams(-1, -1));
        io.execute(() -> {
            Bitmap full = null;
            try {
                ImageDecoder.Source source = ImageDecoder.createSource(getContentResolver(), photo.uri);
                full = ImageDecoder.decodeBitmap(source, (decoder, info, src) -> {
                    int longest = Math.max(info.getSize().getWidth(), info.getSize().getHeight());
                    int sample = 1;
                    while (longest / sample > 4096) sample *= 2;
                    decoder.setTargetSampleSize(sample);
                    decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE);
                });
            } catch (Exception ignored) { }
            Bitmap decoded = full;
            runOnUiThread(() -> {
                if (decoded != null && zoomOverlay == overlay && !isDestroyed()) {
                    zoomBitmap = decoded;
                    image.setBitmap(decoded);
                    loading.setVisibility(View.GONE);
                } else {
                    if (decoded != null) decoded.recycle();
                    if (zoomOverlay == overlay) loading.setText("Could not open this photo");
                }
            });
        });
    }

    private void closePhotoZoom() {
        if (zoomOverlay != null && zoomOverlay.getParent() instanceof android.view.ViewGroup)
            ((android.view.ViewGroup) zoomOverlay.getParent()).removeView(zoomOverlay);
        zoomOverlay = null;
        if (zoomBitmap != null) { zoomBitmap.recycle(); zoomBitmap = null; }
    }

    private class ZoomPhotoView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        private Bitmap bitmap;
        private float zoom = 1f, panX, panY, lastX, lastY;
        private final ScaleGestureDetector scaler = new ScaleGestureDetector(MainActivity.this,
                new ScaleGestureDetector.SimpleOnScaleGestureListener() {
                    @Override public boolean onScale(ScaleGestureDetector detector) {
                        zoomAt(detector.getScaleFactor(), detector.getFocusX(), detector.getFocusY());
                        return true;
                    }
                });
        private final GestureDetector taps = new GestureDetector(MainActivity.this,
                new GestureDetector.SimpleOnGestureListener() {
                    @Override public boolean onDoubleTap(MotionEvent event) {
                        zoomAt(zoom > 1.1f ? 1f / zoom : 2.5f, event.getX(), event.getY());
                        return true;
                    }
                });
        ZoomPhotoView() { super(MainActivity.this); }
        void setBitmap(Bitmap value) { bitmap = value; zoom = 1f; panX = panY = 0; invalidate(); }
        private void zoomAt(float factor, float x, float y) {
            float old = zoom;
            zoom = Math.max(1f, Math.min(6f, zoom * factor));
            float ratio = zoom / old;
            panX = ratio * panX + (1 - ratio) * (x - getWidth() / 2f);
            panY = ratio * panY + (1 - ratio) * (y - getHeight() / 2f);
            constrainPan(); invalidate();
        }
        private void constrainPan() {
            if (bitmap == null || getWidth() == 0 || getHeight() == 0) return;
            float base = Math.min(getWidth() / (float) bitmap.getWidth(), getHeight() / (float) bitmap.getHeight());
            float maxX = Math.max(0, (bitmap.getWidth() * base * zoom - getWidth()) / 2f);
            float maxY = Math.max(0, (bitmap.getHeight() * base * zoom - getHeight()) / 2f);
            panX = Math.max(-maxX, Math.min(maxX, panX));
            panY = Math.max(-maxY, Math.min(maxY, panY));
        }
        @Override protected void onDraw(Canvas canvas) {
            if (bitmap == null || bitmap.isRecycled()) return;
            constrainPan();
            float base = Math.min(getWidth() / (float) bitmap.getWidth(), getHeight() / (float) bitmap.getHeight());
            float width = bitmap.getWidth() * base * zoom, height = bitmap.getHeight() * base * zoom;
            float left = (getWidth() - width) / 2f + panX, top = (getHeight() - height) / 2f + panY;
            canvas.drawBitmap(bitmap, null, new RectF(left, top, left + width, top + height), paint);
        }
        @Override public boolean onTouchEvent(MotionEvent event) {
            taps.onTouchEvent(event);
            scaler.onTouchEvent(event);
            if (event.getActionMasked() == MotionEvent.ACTION_DOWN || event.getActionMasked() == MotionEvent.ACTION_POINTER_UP) {
                lastX = event.getX(); lastY = event.getY();
            } else if (event.getActionMasked() == MotionEvent.ACTION_MOVE && event.getPointerCount() == 1 && !scaler.isInProgress()) {
                panX += event.getX() - lastX; panY += event.getY() - lastY;
                lastX = event.getX(); lastY = event.getY();
                constrainPan(); invalidate();
            }
            return true;
        }
    }

    private void loadPreview(long id, Uri uri, ImageView view) {
        Bitmap cached = previews.get(id);
        if (cached != null) { view.setImageBitmap(cached); return; }
        final FrameLayout requestedHost = host;
        io.execute(() -> {
            if (isDestroyed() || requestedHost != host) return;
            Bitmap queued = previews.get(id);
            if (queued != null) {
                runOnUiThread(() -> { if (!isDestroyed() && view.isAttachedToWindow()) view.setImageBitmap(queued); });
                return;
            }
            try {
                Bitmap bitmap = getContentResolver().loadThumbnail(uri, new Size(900, 900), null);
                if (bitmap != null) {
                    if (isDestroyed()) { bitmap.recycle(); return; }
                    previews.put(id, bitmap);
                    runOnUiThread(() -> { if (!isDestroyed() && view.isAttachedToWindow()) view.setImageBitmap(bitmap); });
                }
            } catch (Exception ignored) { }
        });
    }

    private void drawCountryFlag(Canvas canvas, Paint paint, int country, float x, float y, float size) {
        int color = paint.getColor(), alpha = paint.getAlpha();
        float oldSize = paint.getTextSize(); Paint.Align align = paint.getTextAlign();
        android.graphics.Typeface face = paint.getTypeface();
        paint.setColor(Color.WHITE); paint.setAlpha(alpha); paint.setTypeface(android.graphics.Typeface.DEFAULT);
        paint.setTextSize(size); paint.setTextAlign(Paint.Align.CENTER);
        Paint.FontMetrics metrics = paint.getFontMetrics();
        canvas.drawText(COUNTRY_FLAGS[country], x, y - (metrics.ascent + metrics.descent) / 2, paint);
        paint.setColor(color); paint.setAlpha(alpha); paint.setTypeface(face); paint.setTextSize(oldSize); paint.setTextAlign(align);
    }

    private final Map<Integer, Bitmap> swipeSprites = new HashMap<>();
    private Bitmap swipeAsset(int id) {
        Bitmap bitmap = swipeSprites.get(id);
        if (bitmap == null) {
            BitmapFactory.Options options = new BitmapFactory.Options(); options.inScaled = false;
            bitmap = BitmapFactory.decodeResource(getResources(), id, options);
            if (bitmap != null) swipeSprites.put(id, bitmap);
        }
        return bitmap;
    }
    private Bitmap selectedSwipeSprite(boolean keep) {
        int style = canCustomizeSwipe() ? swipeStyle : 1;
        if (style == 0) {
            if (themeChoice == 61) return swipeAsset(keep ? R.drawable.fx_yin : R.drawable.fx_yang);
            int country = countryCollection(themeChoice);
            return effectSprite(country < 0 ? themeChoice : COUNTRY_ANIMATED_THEMES[country]);
        }
        if (style == 8) {
            int country = countryCollection(themeChoice);
            if (country >= 0) return effectSprite(COUNTRY_ANIMATED_THEMES[country]);
        }
        int[] resources = {R.drawable.fx_sparkle, R.drawable.fx_sparkle, R.drawable.fx_petal,
                R.drawable.fx_bubble, R.drawable.fx_flame, R.drawable.fx_lightning,
                R.drawable.fx_shooting_star, R.drawable.fx_ring, R.drawable.fx_sparkle, 0};
        return resources[style] == 0 ? null : swipeAsset(resources[style]);
    }
    private SwipeTheme.Kind selectedSwipeProfile() {
        if (swipeStyle == 0 && canCustomizeSwipe()) return SwipeTheme.forTheme(themeChoice);
        SwipeTheme.Kind[] kinds = {SwipeTheme.Kind.SPARKLE, SwipeTheme.Kind.SPARKLE, SwipeTheme.Kind.SAKURA,
                SwipeTheme.Kind.WATER, SwipeTheme.Kind.FIRE, SwipeTheme.Kind.LIGHTNING, SwipeTheme.Kind.SPACE,
                SwipeTheme.Kind.SHIELD, countryCollection(themeChoice) >= 0 ? SwipeTheme.forTheme(themeChoice) : SwipeTheme.Kind.SPARKLE, SwipeTheme.Kind.SPARKLE};
        return kinds[swipeStyle];
    }
    private class SwipeEffect extends SwipeVfxView {
        SwipeEffect() {
            super(MainActivity.this, MainActivity.this::selectedSwipeSprite,
                    countryCollection(themeChoice) >= 0 && (swipeStyle == 0 || swipeStyle == 8)
                            ? COUNTRY_FLAGS[countryCollection(themeChoice)] : null,
                    selectedSwipeProfile(),
                    !canCustomizeSwipe() || swipeStyle != 9, GREEN, RED, swipeIntensity, swipeSpeed);
        }
    }

    private void keep(Photo p) {
        if (pendingTrash != -1 || pendingRestore != -1) return;
        playEffect(true);
        lastUndo = snapshot(p, false);
        if (keptIds.add(Long.toString(p.id))) keptCount++;
        int earned = awardXp(p.id, 10); lastUndo.earnedXp = earned;
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
        if (pendingTrash != -1 || pendingRestore != -1 || !canManage()) return;
        try {
            PendingIntent request = MediaStore.createTrashRequest(getContentResolver(), Collections.singletonList(p.uri), true);
            pendingTrash = p.id; pendingTrashUndo = snapshot(p, true);
            startIntentSenderForResult(request.getIntentSender(), TRASH_REQUEST, null, 0, 0, 0);
        } catch (Exception e) {
            pendingTrash = -1; pendingTrashUndo = null;
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
            pendingRestore = -1; pendingRestoreUndo = false; Toast.makeText(this, "Could not restore photo", Toast.LENGTH_SHORT).show();
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
                    int state = TrashPolicy.state(getContentResolver(), entry.uri);
                    if (state == 0 || (state == 1 && getContentResolver().delete(entry.uri, null, null) > 0)) deleted.add(entry.id);
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
            ReviewUndo action = pendingTrashUndo; pendingTrashUndo = null;
            int earned = 0;
            if (resultCode == RESULT_OK) {
                playEffect(false);
                if (action != null) {
                    lastUndo = action;
                    trashEntries.add(0, new TrashEntry(id, System.currentTimeMillis(), action.timestamp));
                    trimRecoveryWindow(); saveTrashEntries();
                }
                photos.removeIf(p -> p.id == id); rebuildMonthIndex();
                reviewed.remove(Long.toString(id)); saveReviewed();
                duplicates.remove(id);
                if (trashedIds.add(Long.toString(id))) trashedCount++;
                earned = awardXp(id, 15);
                if (lastUndo != null && lastUndo.id == id) lastUndo.earnedXp = earned;
                saveStats();
            }
            render();
            if (earned > 0) floatXp(earned);
            if (resultCode == RESULT_OK) cleanupTrash();
        } else if (requestCode == RESTORE_REQUEST) {
            long id = pendingRestore; pendingRestore = -1;
            boolean undo = pendingRestoreUndo; pendingRestoreUndo = false;
            if (resultCode == RESULT_OK) {
                trashEntries.removeIf(e -> e.id == id); saveTrashEntries();
                if (undo) finishUndo(); else restoredCount++;
                saveStats();
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
                                int state = TrashPolicy.state(getContentResolver(), uri);
                                if (state < 0 || (state == 1 && getContentResolver().delete(uri, null, null) <= 0)) retry.put(item);
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
        private static final int FIRE_SPARK_COUNT = 28;
        private final java.util.Random fireRandom = new java.util.Random();
        private final long[] sparkStart = new long[FIRE_SPARK_COUNT];
        private final float[] sparkX = new float[FIRE_SPARK_COUNT], sparkSpeed = new float[FIRE_SPARK_COUNT];
        private final float[] sparkPhase = new float[FIRE_SPARK_COUNT], sparkDrift = new float[FIRE_SPARK_COUNT];
        private final float[] sparkWiggle = new float[FIRE_SPARK_COUNT];
        private boolean fireSparksReady;
        private final float[] sweetX = new float[18], sweetY = new float[18];
        private final float[] speedX = new float[18], speedY = new float[18];
        private final float[] spin = new float[18];
        private final java.util.Random spiderRandom = new java.util.Random();
        private long spiderStageStart, spiderStageDuration;
        private int spiderStage; // descend, hang, climb, wait
        private boolean spiderStarted;
        private float spiderX = -1, spiderDepth;
        private long lastFrame;
        private boolean initialized;
        TextureBackdrop() { super(MainActivity.this); }
        void resetSpider() { spiderStarted = false; invalidate(); }
        private void beginSpiderDescent(long start) {
            float next = .12f + spiderRandom.nextFloat() * .76f;
            for (int i = 0; i < 8 && Math.abs(next - spiderX) < .26f; i++)
                next = .12f + spiderRandom.nextFloat() * .76f;
            spiderX = next;
            spiderDepth = .08f + spiderRandom.nextFloat() * .24f;
            spiderStage = 0;
            spiderStageStart = start;
            spiderStageDuration = 650 + spiderRandom.nextInt(1550);
            spiderStarted = true;
        }
        @Override protected void onDraw(Canvas canvas) {
            float w = getWidth(), h = getHeight();
            if (w <= 0 || h <= 0) return;
            paint.setColor(BG); canvas.drawColor(BG);
            if (!isColorTheme(themeChoice) && themeBackdrops[themeChoice] == null) {
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
            if (themeChoice == 21) {
                paint.setShader(new LinearGradient(0, h * .76f, 0, h,
                        0x00141010, 0xF0141010, Shader.TileMode.CLAMP));
                canvas.drawRect(0, h * .76f, w, h, paint);
                paint.setShader(null);
                drawFireBackground(canvas, w, h);
                if (isAttachedToWindow()) postInvalidateDelayed(50);
            }
            int country = countryCollection(themeChoice);
            if (country >= 0 && !hasThemeMotion(themeChoice)) {
                Bitmap symbol = effectSprite(COUNTRY_ANIMATED_THEMES[country]);
                if (symbol != null) drawCountryEffects(canvas, symbol, w, h, 0f, country);
            }
            if (themeChoice == 18) {
                canvas.drawColor(0x500D0922);
                drawCandies(canvas, w, h);
            }
        }
        private void resetFireSpark(int i, long now, float w, float fireBase, boolean scatter) {
            sparkX[i] = fireRandom.nextFloat() * w;
            sparkSpeed[i] = dp(27 + fireRandom.nextInt(58));
            sparkPhase[i] = fireRandom.nextFloat() * 6.28f;
            sparkDrift[i] = dp(-8 + fireRandom.nextInt(17));
            sparkWiggle[i] = dp(7 + fireRandom.nextInt(16));
            float lifetime = (fireBase + dp(50)) / sparkSpeed[i];
            sparkStart[i] = now - (scatter ? (long)(fireRandom.nextFloat() * lifetime * 1000) : 0);
        }
        private void drawFireBackground(Canvas canvas, float w, float h) {
            BitmapFactory.Options options = new BitmapFactory.Options(); options.inScaled = false;
            if (fireBackgroundFrames == null)
                fireBackgroundFrames = BitmapFactory.decodeResource(getResources(), R.drawable.fire_background_frames, options);
            if (fireSparkSprites == null)
                fireSparkSprites = BitmapFactory.decodeResource(getResources(), R.drawable.fire_sparks, options);
            long now = android.os.SystemClock.uptimeMillis();
            float fireBase = h * .82f;
            canvas.save(); canvas.clipRect(0, 0, w, fireBase);
            if (fireBackgroundFrames != null) {
                final int cellW = 400, cellH = 208, inset = 8, frameW = 384, frameH = 192;
                int frame = (int)((now / 100) % 30);
                int left = (frame % 5) * cellW + inset, top = (frame / 5) * cellH + inset;
                Rect source = new Rect(left, top, left + frameW, top + frameH);
                float height = Math.min(h * .46f, dp(330));
                paint.setAlpha(145);
                canvas.drawBitmap(fireBackgroundFrames, source,
                        new RectF(-w * .025f, fireBase - height, w * 1.025f, fireBase), paint);
            }
            if (fireSparkSprites != null) {
                if (!fireSparksReady) {
                    for (int i = 0; i < FIRE_SPARK_COUNT; i++) resetFireSpark(i, now, w, fireBase, true);
                    fireSparksReady = true;
                }
                int cellW = fireSparkSprites.getWidth() / 4;
                int cellH = fireSparkSprites.getHeight() / 4;
                for (int i = 0; i < FIRE_SPARK_COUNT; i++) {
                    float age = (now - sparkStart[i]) / 1000f;
                    float y = fireBase + dp(15) - age * sparkSpeed[i];
                    if (y < -dp(25)) { resetFireSpark(i, now, w, fireBase, false); continue; }
                    float sway = (float)Math.sin(age * (2.2f + i % 4 * .35f) + sparkPhase[i]) * sparkWiggle[i]
                            + (float)Math.sin(age * 6.1f + sparkPhase[i] * 1.7f) * sparkWiggle[i] * .38f;
                    float x = sparkX[i] + sparkDrift[i] * age + sway;
                    float size = dp(10 + i % 4 * 2);
                    float fadeIn = Math.min(1f, (fireBase + dp(15) - y) / dp(85f));
                    float fadeOut = Math.min(1f, (y + dp(25)) / dp(120f));
                    paint.setAlpha((int)(190 * Math.max(0f, Math.min(fadeIn, fadeOut))));
                    int variant = i % 16;
                    int sx = (variant % 4) * cellW, sy = (variant / 4) * cellH;
                    Rect source = new Rect(sx + cellW / 5, sy + cellH / 7,
                            sx + cellW * 4 / 5, sy + cellH * 6 / 7);
                    canvas.save(); canvas.rotate((float)Math.sin(age * 3.2f + sparkPhase[i]) * 25, x, y);
                    canvas.drawBitmap(fireSparkSprites, source,
                            new RectF(x - size / 2, y - size, x + size / 2, y + size), paint);
                    canvas.restore();
                }
            }
            canvas.restore();
            paint.setAlpha(255);
        }
        // Drawn above the cards, so the world effects remain visible in every screen.
        // This View does not consume touches; swipes and buttons still reach the content.
        private float loop(float value, float span) {
            return (value % span + span) % span;
        }
        private void drawGeneratedEffect(Canvas canvas, float w, float h, float time) {
            int theme = themeChoice;
            if (theme == 18 || theme == 21 || !hasThemeMotion(theme) || THEME_FX_IDS[theme] == 0) return; // Fire draws behind the UI.
            if (motionStyle(theme) == 3 && arachnophobiaMode) return;
            if (themeEffects[theme] == null) {
                BitmapFactory.Options opts = new BitmapFactory.Options(); opts.inScaled = false;
                themeEffects[theme] = BitmapFactory.decodeResource(getResources(), THEME_FX_IDS[theme], opts);
            }
            Bitmap sprite = themeEffects[theme];
            if (sprite == null) return;
            float unit = dp(1);
            int country = countryCollection(theme);
            if (country >= 0) {
                drawCountryEffects(canvas, sprite, w, h, time, country);
                return;
            }
            if (theme >= 50) {
                drawExpandedCollectionEffects(canvas, sprite, w, h, time, theme);
                return;
            }
            if (motionStyle(theme) >= 30 && motionStyle(theme) <= 33) {
                drawCollectionEffects(canvas, sprite, w, h, time, motionStyle(theme));
                return;
            }
            theme = motionStyle(theme);
            if (theme == 3) {
                long now = android.os.SystemClock.uptimeMillis();
                if (!spiderStarted || now - spiderStageStart > 30000) beginSpiderDescent(now);
                while (now - spiderStageStart >= spiderStageDuration) {
                    spiderStageStart += spiderStageDuration;
                    if (spiderStage == 0) {
                        spiderStage = 1; spiderStageDuration = 650 + spiderRandom.nextInt(2200);
                    } else if (spiderStage == 1) {
                        spiderStage = 2; spiderStageDuration = 550 + spiderRandom.nextInt(1450);
                    } else if (spiderStage == 2) {
                        spiderStage = 3; spiderStageDuration = 850 + spiderRandom.nextInt(3700);
                    } else beginSpiderDescent(spiderStageStart);
                }
                if (spiderStage == 3) return;
                float height = Math.min(h * .28f, 154 * unit);
                float width = height * sprite.getWidth() / sprite.getHeight();
                float progress = (now - spiderStageStart) / (float) spiderStageDuration;
                float amount = spiderStage == 1 ? 1 : (float)(.5 - .5 * Math.cos(Math.PI * progress));
                if (spiderStage == 2) amount = 1 - amount;
                float x = w * spiderX + (float)Math.sin(now * .0017) * 3 * unit;
                float top = -height + (spiderDepth * h + height) * amount;
                paint.setColor(0x887FC9FF); paint.setStrokeWidth(1.4f * unit);
                if (top > 0) canvas.drawLine(x, 0, x, top + 12 * unit, paint);
                paint.setAlpha(200);
                canvas.drawBitmap(sprite, null, new RectF(x - width / 2, top,
                        x + width / 2, top + height), paint);
                paint.setAlpha(255);
                return;
            }
            if (theme == 20) {
                long now = android.os.SystemClock.uptimeMillis();
                if (nextCometAt == 0) nextCometAt = now + 3500 + cometRandom.nextInt(5000);
                if (cometFlying && now - cometStart >= cometDuration) {
                    cometFlying = false;
                    nextCometAt = now + 7000 + cometRandom.nextInt(12000);
                }
                if (!cometFlying && now >= nextCometAt) launchComet(now, w, h, 115 * unit);
                if (cometFlying) {
                    float travel = Math.min(1f, (now - cometStart) / (float) cometDuration);
                    float x = cometFromX + (cometToX - cometFromX) * travel;
                    float y = cometFromY + (cometToY - cometFromY) * travel;
                    float fade = Math.min(1f, Math.min(travel * 8, (1 - travel) * 8));
                    float size = 115 * unit;
                    canvas.save(); canvas.translate(x, y); canvas.rotate(cometAngle);
                    paint.setAlpha((int)(210 * fade));
                    canvas.drawBitmap(sprite, null, new RectF(-size / 2, -size / 2, size / 2, size / 2), paint);
                    canvas.restore();
                }
                paint.setAlpha(255);
                return;
            }
            if (theme == 25) {
                float flash = loop(time, 2.6f);
                if (flash > .55f) return;
            }
            int count = theme == 21 || theme == 22 || theme == 23 || theme == 24 ? 8 : 5;
            for (int i = 0; i < count; i++) {
                float span = (theme == 20 || theme == 21 || theme == 22 || theme == 23 || theme == 24) ? 1.8f : 1.2f;
                float x = w * (.08f + ((i * 67) % 85) / 100f)
                        + (float)Math.sin(time * .65f + i * 2.1f) * 20 * unit;
                float y = h * loop(i * .23f + time * (theme == 21 ? -.075f : .045f), span) - h * .2f;
                float size = (theme == 25 ? 86 : 33 + i % 3 * 12) * unit;
                if (theme == 20) size = (42 + i % 3 * 17) * unit;
                float width = size * sprite.getWidth() / sprite.getHeight();
                canvas.save();
                canvas.rotate(theme == 24 || theme == 9 ? (float)Math.sin(time + i) * 25 : 0, x, y);
                paint.setAlpha(theme == 25 ? 175 : theme >= 19 ? 135 : 95);
                canvas.drawBitmap(sprite, null, new RectF(x - width / 2, y - size / 2,
                        x + width / 2, y + size / 2), paint);
                canvas.restore();
            }
            paint.setAlpha(255);
        }
        private void launchComet(long now, float w, float h, float margin) {
            int path = cometRandom.nextInt(6);
            if (path < 2) {
                cometFromX = path == 0 ? -margin : w + margin;
                cometToX = path == 0 ? w + margin : -margin;
                cometFromY = h * (.12f + cometRandom.nextFloat() * .75f);
                cometToY = Math.max(0, Math.min(h, cometFromY + h * (cometRandom.nextFloat() - .5f) * .45f));
            } else {
                boolean fromTop = path == 2 || path == 4;
                boolean fromLeft = path == 2 || path == 5;
                float leftX = w * (.04f + cometRandom.nextFloat() * .25f);
                float rightX = w * (.71f + cometRandom.nextFloat() * .25f);
                cometFromX = fromLeft ? leftX : rightX;
                cometToX = fromLeft ? rightX : leftX;
                cometFromY = fromTop ? -margin : h + margin;
                cometToY = fromTop ? h + margin : -margin;
            }
            float direction = (float)Math.toDegrees(Math.atan2(cometToY - cometFromY, cometToX - cometFromX));
            cometAngle = direction - 135f; // The generated sprite's bright head points down and left.
            cometDuration = 600 + cometRandom.nextInt(1150);
            cometStart = now;
            cometFlying = true;
        }
        private void drawCollectionEffects(Canvas canvas, Bitmap sprite, float w, float h, float time, int theme) {
            float unit = getResources().getDisplayMetrics().density;
            // Slow, low-opacity decorations leave photo cards readable.
            int count = theme == 31 ? 6 : 9;
            for (int i = 0; i < count; i++) {
                float phase = i * .173f;
                float x = w * loop(phase * 2.13f + (float)Math.sin(time * .22f + i) * .06f, 1f);
                float y;
                if (theme == 31) y = h * (.12f + i * .125f) + (float)Math.sin(time * .35f + i) * 22 * unit;
                else y = h * loop(phase + time * (theme == 32 ? -.022f : .035f), 1.25f) - h * .12f;
                float size = (theme == 32 ? 34 : theme == 31 ? 28 : 18 + i % 3 * 5) * unit;
                canvas.save();
                canvas.rotate(theme == 32 ? (float)Math.sin(time * .6f + i) * 9 : time * 12 + i * 43, x, y);
                paint.setAlpha(theme == 31 ? (int)(80 + 45 * Math.sin(time * .8f + i)) : theme == 32 ? 140 : 125);
                float halfWidth = size * .5f, halfHeight = halfWidth * sprite.getHeight() / sprite.getWidth();
                canvas.drawBitmap(sprite, null, new RectF(x - halfWidth, y - halfHeight, x + halfWidth, y + halfHeight), paint);
                canvas.restore();
            }
            paint.setAlpha(255);
        }

        private void drawCountryEffects(Canvas canvas, Bitmap sprite, float w, float h, float time, int country) {
            float unit = getResources().getDisplayMetrics().density;
            for (int i = 0; i < 12; i++) {
                boolean flag = i % 2 == 0;
                float x = w * loop(i * .271f + (float)Math.sin(time * .2f + i) * .05f, 1f);
                float y = h * loop(i * .113f + time * .023f, 1.25f) - h * .12f;
                float size = (flag ? 30 : country == 0 || country == 7 ? 38 : 32) * unit;
                canvas.save(); canvas.rotate((float)Math.sin(time * .55f + i) * 12, x, y);
                paint.setAlpha(150);
                if (flag) drawCountryFlag(canvas, paint, country, x, y, size);
                else {
                    float halfW = size / 2, halfH = halfW * sprite.getHeight() / sprite.getWidth();
                    canvas.drawBitmap(sprite, null, new RectF(x-halfW, y-halfH, x+halfW, y+halfH), paint);
                }
                canvas.restore();
            }
            paint.setAlpha(255);
        }

        private void drawExpandedCollectionEffects(Canvas canvas, Bitmap sprite, float w, float h, float time, int theme) {
            float unit = getResources().getDisplayMetrics().density;
            boolean lantern = theme == 55, stars = theme == 50;
            int count = lantern ? 6 : theme == 51 ? 5 : 10;
            for (int i = 0; i < count; i++) {
                float x = w * loop(i * .271f + (float)Math.sin(time * .2f + i) * .05f, 1f);
                float y = stars ? h * (.08f + i * .087f) + (float)Math.sin(time * .3f + i) * 10 * unit
                        : h * loop(i * .163f + time * (lantern || theme == 57 || theme == 60 ? -.018f : .028f), 1.25f) - h * .12f;
                float size = (lantern ? 38 : theme == 51 ? 32 : theme == 53 || theme == 54 ? 28 : 20 + i % 3 * 7) * unit;
                float halfW = size / 2, halfH = halfW * sprite.getHeight() / sprite.getWidth();
                canvas.save();
                canvas.rotate(lantern ? (float)Math.sin(time * .5f + i) * 7 : stars ? 0 : time * 7 + i * 29, x, y);
                paint.setAlpha(stars ? (int)(65 + 65 * (.5f + .5f * Math.sin(time + i))) : 110);
                canvas.drawBitmap(sprite, null, new RectF(x - halfW, y - halfH, x + halfW, y + halfH), paint);
                canvas.restore();
            }
            paint.setAlpha(255);
            if (theme == 56 || theme == 57 || theme == 60) {
                paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(2 * unit);
                paint.setColor(Color.argb(60, Color.red(GREEN), Color.green(GREEN), Color.blue(GREEN)));
                float pulse = .5f + .5f * (float)Math.sin(time * .8f);
                for (int edge = 0; edge < 2; edge++) {
                    float x = edge == 0 ? 0 : w;
                    canvas.drawCircle(x, h * .45f, (45 + pulse * 50) * unit, paint);
                }
                paint.setStyle(Paint.Style.FILL);
            }
        }

        private void drawThemeMotion(Canvas canvas, float w, float h, float time) {
            if (!hasThemeMotion(themeChoice)) return;
            int theme = motionStyle(themeChoice);
            float unit = dp(1);
            paint.setShader(null);
            paint.setStyle(Paint.Style.FILL);
            if (theme >= 26) return; // Collection motion is rendered with its generated artwork.
            if (theme == 19) { // Toxic: bubbles rise while drops slide down the edges.
                for (int i = 0; i < 15; i++) {
                    float x = w * (.07f + ((i * 67) % 89) / 100f);
                    float y = h - loop(time * (18 + i % 4 * 9) * unit + i * h / 13, h + 80 * unit);
                    paint.setColor(0xB38BFF51); paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(2.5f * unit);
                    canvas.drawCircle(x + (float)Math.sin(time + i) * 9 * unit, y, (8 + i % 5 * 4) * unit, paint);
                }
                paint.setStyle(Paint.Style.FILL); paint.setColor(0x88ABFC4E);
                for (int i = 0; i < 4; i++) canvas.drawRoundRect(new RectF(w * (i + 1) / 5, 0,
                        w * (i + 1) / 5 + 4 * unit, loop(time * (12 + i * 4) * unit + i * 71 * unit, h)), 4 * unit, 4 * unit, paint);
            } else if (theme == 20) { // Space: star twinkle and occasional shooting stars.
                for (int i = 0; i < 30; i++) {
                    float x = ((i * 173) % 991) / 991f * w, y = ((i * 311) % 997) / 997f * h;
                    paint.setColor(Color.argb(75 + (int)(115 * (.5 + .5 * Math.sin(time * 2 + i))), 195, 228, 255));
                    canvas.drawCircle(x, y, (i % 5 == 0 ? 3.5f : 1.8f) * unit, paint);
                }
            } else if (theme == 22) { // Water: spreading ripples and slow currents.
                paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(3 * unit);
                for (int i = 0; i < 9; i++) {
                    float x = w * ((i * 37 % 93) / 93f), y = h * ((i * 29 % 89) / 89f);
                    float ripple = loop(time * 22 * unit + i * 17 * unit, 75 * unit);
                    paint.setColor(Color.argb((int)(180 * (1 - ripple / (75 * unit))), 138, 231, 255));
                    canvas.drawOval(x - ripple, y - ripple * .32f, x + ripple, y + ripple * .32f, paint);
                }
                paint.setStyle(Paint.Style.FILL);
            } else if (theme == 23) { // Ice: gently falling six armed snow crystals.
                paint.setColor(0xDDDDF6FF); paint.setStrokeWidth(2.4f * unit);
                for (int i = 0; i < 16; i++) {
                    float x = w * ((i * 47 % 97) / 97f) + (float)Math.sin(time + i) * 9 * unit;
                    float y = loop(time * (13 + i % 4 * 5) * unit + i * h / 14, h + 30 * unit) - 15 * unit;
                    float r = (6 + i % 5) * unit;
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
                    paint.setColor(i % 2 == 0 ? 0xCC98CD65 : 0xCCD8BB77);
                    canvas.drawOval(x - 10 * unit, y - 4 * unit, x + 10 * unit, y + 4 * unit, paint);
                    canvas.restore();
                }
            } else if (theme == 25) { // Lightning: brief branching flashes, then darkness.
                float flash = loop(time, 2.6f);
                if (flash < .25f || (flash > .38f && flash < .52f)) {
                    float x = w * (.3f + (int)(time / 2.6f) % 4 * .13f);
                    paint.setColor(0xEED7E6FF); paint.setStrokeWidth(3 * unit);
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
    private class ThemeMotionOverlay extends View {
        ThemeMotionOverlay() { super(MainActivity.this); setClickable(false); setFocusable(false); }
        @Override protected void onDraw(Canvas canvas) {
            if (activeBackdrop != null && hasThemeMotion(themeChoice) && themeChoice != 18) {
                float time = android.os.SystemClock.uptimeMillis() / 1000f;
                activeBackdrop.drawThemeMotion(canvas, getWidth(), getHeight(), time);
                activeBackdrop.drawGeneratedEffect(canvas, getWidth(), getHeight(), time);
                if (isAttachedToWindow()) postInvalidateDelayed(50);
            }
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
                if (art != null && !isColorTheme(selected)) {
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
