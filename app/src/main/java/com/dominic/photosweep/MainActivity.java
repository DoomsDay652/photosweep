package com.dominic.photosweep;

import android.Manifest;
import androidx.activity.ComponentActivity;
import androidx.activity.OnBackPressedCallback;
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
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
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

public class MainActivity extends ComponentActivity {
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
            R.drawable.theme_00, R.drawable.theme_01, R.drawable.theme_02, 0,
            0, 0, 0, 0,
            0, 0, 0, R.drawable.theme_11,
            R.drawable.theme_12, R.drawable.theme_13, R.drawable.theme_14, R.drawable.theme_15,
            R.drawable.theme_16, R.drawable.theme_17, R.drawable.theme_18, R.drawable.theme_19,
            R.drawable.space_nebula, R.drawable.theme_21, R.drawable.theme_22, R.drawable.theme_23,
            R.drawable.theme_24, R.drawable.theme_25,
            R.drawable.sakura_academy, R.drawable.spirit_sky, R.drawable.japan_lanterns, R.drawable.mexico_plaza,
            R.drawable.sakura_academy, R.drawable.spirit_sky, R.drawable.japan_lanterns, R.drawable.mexico_plaza,
            0,
            0,
            0,
            0,
            0,
            0,
            0,
            0,
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
            0,
            0,
            0,
            0,
            0,
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
    private AccountController accounts;
    private final HashSet<Long> duplicates = new HashSet<>();
    private final ArrayList<TrashEntry> trashEntries = new ArrayList<>();
    private final ArrayList<TrashEntry> evictionQueue = new ArrayList<>();
    private Set<String> reviewed = new HashSet<>();
    private Set<String> rewarded = new HashSet<>();
    private Set<String> keptIds = new HashSet<>();
    private Set<String> trashedIds = new HashSet<>();
    private LinearLayout root;
    private TextView headingSubtitle;
    private FrameLayout host;
    private int renderedTheme = -1;
    private boolean hasResumed, skipMediaReloadOnResume;
    private ReviewPage reviewPage;
    private final LruCache<Long, Bitmap> reviewPhotos = new LruCache<Long, Bitmap>(32 * 1024) {
        @Override protected int sizeOf(Long key, Bitmap value) {
            return Math.max(1, (value.getByteCount() + 1023) / 1024);
        }
    };
    private long prefetchedReviewId = -1;
    private class ReviewPage {
        final LinearLayout content = root;
        final String month = selectedMonth;
        final boolean full = fullScreenReview;
        final int theme = themeChoice;
        long photoId = -1;
        FrameLayout stage, card, progress;
        ImageView image;
        SwipeEffect effect;
        PhotoElementBorderView border;
        TextView date, count, duplicate, scanning;
        View fill, undo;
    }
    private int xp, keptCount, trashedCount, restoredCount;
    private int selectedYear = -1;
    private String selectedMonth;
    private boolean reviewing, loading, duplicateScanning, reloadPhotosPending;
    private boolean fullScreenReview, reviewActionRunning;
    private boolean showingTrash, deletingOld;
    private boolean showingSettings, showingThemes, soundEnabled, musicEnabled, statsExpanded, swipeHintSeen, adminMode;
    private int optionsSection;
    private boolean arachnophobiaMode, animateThemeChange;
    private int themeChoice, musicVolume, masterVolume, vfxVolume;
    private int swipeStyle, swipeIntensity = 55, swipeSpeed = 100;
    private android.animation.ValueAnimator swipePreviewAnimator;
    private SwipeEffect swipePreviewEffect;
    private AudioController audio;
    private TextView radioBubble, radioTrackLabel;
    private final Runnable fadeRadioBubble = () -> {
        TextView bubble = radioBubble;
        if (bubble != null) bubble.animate().alpha(0f).setDuration(2400).withEndAction(() -> {
            if (host != null) host.removeView(bubble);
            if (radioBubble == bubble) radioBubble = null;
        }).start();
    };
    private PlayUpdateController playUpdates;
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
            if (activeBackdrop == null || (themeChoice != 18 && themeChoice != 19 && themeChoice != 22)) return;
            int rotation = getWindowManager().getDefaultDisplay().getRotation();
            float x = event.values[0], y = event.values[1];
            if (rotation == android.view.Surface.ROTATION_90) { float t = x; x = -y; y = t; }
            else if (rotation == android.view.Surface.ROTATION_270) { float t = x; x = y; y = -t; }
            else if (rotation == android.view.Surface.ROTATION_180) { x = -x; y = -y; }
            // Sensor Y points toward the device top; canvas Y points downward.
            gravityX = Math.max(-1, Math.min(1, -x / 7f));
            gravityY = candyGravityY(y);
            activeBackdrop.invalidate();
        }
    };
    private static float candyGravityY(float screenY) {
        // At rest an upright phone reports positive sensor Y. Canvas gravity
        // is positive downward; negative values support an inverted phone.
        return Math.max(-1f, Math.min(1f, screenY / 7f));
    }
    private final Handler uiHandler = new Handler(Looper.getMainLooper());
    private long pendingTrash = -1;
    private long pendingRestore = -1;
    private ReviewUndo lastUndo, pendingTrashUndo;
    private boolean pendingRestoreUndo;
    private int generation;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        playUpdates = new PlayUpdateController(this, state);
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() {
                if (handleAppBack()) return;
                setEnabled(false);
                try { getOnBackPressedDispatcher().onBackPressed(); }
                finally { setEnabled(true); }
            }
        });
        audio = new AudioController(this, this::showRadioTrack);
        accounts = new AccountController(this, () -> { if (!isDestroyed() && root != null) render(); });
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        if (sensorManager != null) {
            gravitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
            if (gravitySensor == null) gravitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }
        themeChoice = getPreferences(MODE_PRIVATE).getInt("theme", 0);
        adminMode = getPreferences(MODE_PRIVATE).getBoolean("admin_mode", false);
        soundEnabled = getPreferences(MODE_PRIVATE).getBoolean("sound_enabled", true);
        musicEnabled = getPreferences(MODE_PRIVATE).getBoolean("music_enabled", false);
        musicVolume = readVolume("music_volume", 18);
        masterVolume = readVolume("master_volume", 100);
        vfxVolume = readVolume("vfx_volume", 100);
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
            showingThemes = state.getBoolean("showingThemes");
            optionsSection = state.getInt("optionsSection");
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
        // Start loading before the first render so saved review state is not
        // mistaken for an empty, completed month during Activity recreation.
        if (hasAccess()) loadPhotos(); else render();
    }

    @Override protected void onResume() {
        super.onResume();
        playUpdates.onResume();
        if (gravitySensor != null) sensorManager.registerListener(tiltListener, gravitySensor, SensorManager.SENSOR_DELAY_GAME);
        audio.setMix(masterVolume / 100f, vfxVolume / 100f);
        audio.resume(musicEnabled, musicVolume / 100f);
        // onCreate already loaded the library. Android photo-action results
        // update local state before onResume; do not scan the gallery again.
        if (!hasResumed) { hasResumed = true; return; }
        if (skipMediaReloadOnResume) { skipMediaReloadOnResume = false; return; }
        if (root != null && pendingTrash == -1 && pendingRestore == -1 && !reviewActionRunning) {
            loadTrashEntries();
            reviewPhotos.evictAll(); prefetchedReviewId = -1;
            if (reviewPage != null) reviewPage.photoId = -1;
            if (hasAccess()) loadPhotos(); else render();
        }
    }

    @Override protected void onPause() {
        clearRadioBubble();
        skipMediaReloadOnResume = pendingTrash != -1 || pendingRestore != -1 || playUpdates.isFlowActive();
        playUpdates.onPause();
        if (sensorManager != null) sensorManager.unregisterListener(tiltListener);
        stopMusic();
        clearSwipePreview();
        super.onPause();
    }

    @Override public void onDestroy() {
        clearRadioBubble();
        if (playUpdates != null) playUpdates.close();
        if (accounts != null) accounts.close();
        if (audio != null) audio.close();
        closePhotoZoom();
        for (Bitmap bitmap : themeBackdrops) if (bitmap != null && !bitmap.isRecycled()) bitmap.recycle();
        for (Bitmap bitmap : themeEffects) if (bitmap != null && !bitmap.isRecycled()) bitmap.recycle();
        if (fireBackgroundFrames != null && !fireBackgroundFrames.isRecycled()) fireBackgroundFrames.recycle();
        if (fireSparkSprites != null && !fireSparkSprites.isRecycled()) fireSparkSprites.recycle();
        for (Bitmap bitmap : swipeSprites.values()) if (!bitmap.isRecycled()) bitmap.recycle();
        swipeSprites.clear();
        if (candySprites != null && !candySprites.isRecycled()) candySprites.recycle();
        generation++;
        previews.evictAll(); reviewPhotos.evictAll();
        io.shutdownNow();
        duplicateWorker.shutdownNow();
        super.onDestroy();
    }

    private boolean handleAppBack() {
        if (reviewActionRunning) return true;
        if (zoomOverlay != null) closePhotoZoom();
        else if (fullScreenReview) { fullScreenReview = false; render(); }
        else if (showingThemes) { showingThemes = false; render(); }
        else if (showingSettings && optionsSection != 0) { optionsSection = 0; render(); }
        else if (showingSettings) { showingSettings = false; render(); }
        else if (showingTrash) { showingTrash = false; render(); }
        else if (reviewing) { reviewing = false; fullScreenReview = false; render(); }
        else if (selectedYear != -1) { selectedYear = -1; selectedMonth = null; render(); }
        else return false;
        return true;
    }

    @Override protected void onSaveInstanceState(Bundle state) {
        playUpdates.saveState(state);
        rememberScroll();
        state.putInt("selectedYear", selectedYear);
        state.putString("selectedMonth", selectedMonth);
        state.putBoolean("reviewing", reviewing);
        state.putBoolean("fullScreenReview", fullScreenReview);
        state.putBoolean("showingSettings", showingSettings);
        state.putBoolean("showingThemes", showingThemes);
        state.putInt("optionsSection", optionsSection);
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

    private ScrollView boundedScroll() {
        ScrollView scroll = new ScrollView(this);
        // Menus must stay inside their viewport; only review cards can draw beyond it.
        scroll.setClipChildren(true);
        scroll.setClipToPadding(true);
        // Clip the ScrollView's own render layer too: elevated controls can otherwise
        // escape child clipping and overlap the fixed page heading.
        scroll.addOnLayoutChangeListener((view, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) ->
                view.setClipBounds(new Rect(0, 0, right - left, bottom - top)));
        return scroll;
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
        if (!PlayPolicy.themeAllowed(themeChoice) || themeChoice < 0 || themeChoice >= THEME_COLORS.length ||
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
            new android.app.AlertDialog.Builder(this).setTitle("Optional media management")
                .setMessage("This allows Photo Sweep to move or restore photos without a confirmation for each photo and remove expired Trash entries automatically. You can decline and still review photos using Android confirmations. Change this anytime in Android Settings.")
                .setNegativeButton("Keep confirmations", null).setPositiveButton("Open Android Settings", (d,w) -> {
                    Intent intent = new Intent(Settings.ACTION_REQUEST_MANAGE_MEDIA, Uri.parse("package:" + getPackageName())); startActivity(intent);
                }).show();
        } catch (Exception e) {
            Toast.makeText(this, "Open Settings → Apps → Special access → Manage media", Toast.LENGTH_LONG).show();
        }
    }

    private void requestAccess() {
        new android.app.AlertDialog.Builder(this).setTitle("Choose photo access")
            .setMessage("Photo Sweep reads photos you allow so you can review, keep or move them to Android Trash. Photos are processed on this device and are not uploaded. You can allow selected photos, allow all photos, or decline. Android asks for confirmation before Trash or Restore unless you separately allow Manage media.")
            .setNegativeButton("Not now", null).setPositiveButton("Continue", (d,w) -> requestPhotoPermission()).show();
    }

    private void requestPhotoPermission() {
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
        if (photos.isEmpty()) render();
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
        boolean full = reviewing && fullScreenReview && !showingSettings && !showingThemes && !showingTrash;
        root.setPadding(safeInsets.left + (full ? 0 : dp(22)), safeInsets.top + (full ? 0 : dp(20)),
                safeInsets.right + (full ? 0 : dp(22)), safeInsets.bottom + (full ? 0 : dp(16)));
    }
    private void render() {
        if (reviewActionRunning) return; // Keep asynchronous updates out of a held/swiping card.
        if (updateReviewPage()) return;
        reviewPage = null;
        clearSwipePreview();
        rememberScroll();
        LinearLayout previousRoot = root;
        boolean transition = animateThemeChange;
        animateThemeChange = false;
        activeScroll = null;
        activeScrollPage = null;
        if (host == null) {
            host = new FrameLayout(this);
            host.setClipChildren(false);
            host.setClipToPadding(false);
            setContentView(host);
            host.setOnApplyWindowInsetsListener((view, insets) -> {
                safeInsets = insets.getInsets(android.view.WindowInsets.Type.systemBars() | android.view.WindowInsets.Type.displayCutout());
                applyContentInsets();
                return insets;
            });
        }
        if (renderedTheme != themeChoice) {
            // The world survives navigation and photo decisions. Only changing
            // the selected theme replaces its background and motion state.
            for (int i = host.getChildCount() - 1; i >= 0; i--) {
                View child = host.getChildAt(i);
                if (child instanceof TextureBackdrop || child instanceof ThemeMotionOverlay) host.removeViewAt(i);
            }
            activeBackdrop = new TextureBackdrop();
            host.addView(activeBackdrop, 0, new FrameLayout.LayoutParams(-1, -1));
            if (hasThemeMotion(themeChoice) && themeChoice != 18 && themeChoice != 19 && themeChoice != 21 && themeChoice != 22)
                host.addView(new ThemeMotionOverlay(), 1, new FrameLayout.LayoutParams(-1, -1));
            renderedTheme = themeChoice;
        }
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        boolean swipeOverflow = reviewing && !showingSettings && !showingThemes && !showingTrash;
        root.setClipChildren(!swipeOverflow);
        root.setClipToPadding(!swipeOverflow);
        applyContentInsets();
        host.addView(root, new FrameLayout.LayoutParams(-1, -1));
        if (showingThemes) themesScreen();
        else if (showingSettings) settingsScreen();
        else if (!hasAccess()) intro();
        else if (loading && photos.isEmpty()) heading("Photo Sweep", "Gathering your photos…");
        else if (showingTrash) trashScreen();
        else if (reviewing && selectedMonth != null) reviewScreen();
        else if (selectedYear != -1) monthsScreen();
        else yearsScreen();
        if (!showingThemes && !showingSettings && !showingTrash && !reviewing && selectedYear == -1)
            addVersionLabel();
        if (previousRoot != null) {
            LinearLayout nextRoot = root;
            nextRoot.getViewTreeObserver().addOnPreDrawListener(new android.view.ViewTreeObserver.OnPreDrawListener() {
                @Override public boolean onPreDraw() {
                    nextRoot.getViewTreeObserver().removeOnPreDrawListener(this);
                    host.removeView(previousRoot);
                    return true;
                }
            });
        }
        if (transition) { root.setAlpha(0f); root.animate().alpha(1f).setDuration(260).start(); }
        host.requestApplyInsets();
        if (radioBubble != null) radioBubble.bringToFront();
    }

    private void showRadioTrack(String title) {
        if (isDestroyed() || host == null) return;
        if (radioTrackLabel != null) radioTrackLabel.setText("Now playing · " + title);
        clearRadioBubble();
        TextView bubble = new TextView(this);
        bubble.setText("♫  " + title + "\nD.G.S. Radio");
        bubble.setTextSize(13); bubble.setTextColor(INK);
        bubble.setPadding(dp(16), dp(11), dp(16), dp(11));
        bubble.setMaxWidth(dp(240));
        GradientDrawable background = new GradientDrawable();
        background.setColor(PANEL); background.setCornerRadius(dp(22));
        background.setStroke(dp(1), GREEN);
        bubble.setBackground(background); bubble.setElevation(dp(8));
        bubble.setClickable(false); bubble.setFocusable(false);
        bubble.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        FrameLayout.LayoutParams placement = new FrameLayout.LayoutParams(-2, -2, Gravity.TOP | Gravity.END);
        placement.setMargins(dp(16), safeInsets.top + dp(92), safeInsets.right + dp(18), 0);
        radioBubble = bubble;
        host.addView(bubble, placement);
        bubble.setAlpha(0f); bubble.animate().alpha(1f).setDuration(350).start();
        uiHandler.postDelayed(fadeRadioBubble, 3800);
    }

    private void clearRadioBubble() {
        uiHandler.removeCallbacks(fadeRadioBubble);
        if (radioBubble != null) {
            radioBubble.animate().cancel();
            if (host != null) host.removeView(radioBubble);
            radioBubble = null;
        }
    }

    private String photoDate(Photo photo) {
        return DateFormat.getDateInstance(DateFormat.MEDIUM).format(new Date(photo.timestamp));
    }

    /** Update the existing review page, including Undo, without restarting the world or layout. */
    private boolean updateReviewPage() {
        ReviewPage page = reviewPage;
        if (page == null || page.content != root || !reviewing || showingSettings || showingThemes || showingTrash
                || !hasAccess() || page.theme != themeChoice || page.full != fullScreenReview
                || !page.month.equals(selectedMonth)) return false;
        List<Photo> month = monthPhotos();
        Photo current = null; int remaining = 0;
        for (Photo photo : month) if (!reviewed.contains(Long.toString(photo.id))) {
            remaining++; if (current == null) current = photo;
        }
        if (current == null) return false;
        page.date.setText(photoDate(current));
        page.count.setText(page.full ? remaining + "/" + month.size() + " left"
                : remaining + " of " + month.size() + " left to review");
        if (page.fill != null) {
            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) page.fill.getLayoutParams();
            lp.width = Math.round(page.progress.getWidth() * (month.size() - remaining) / (float) month.size());
            page.fill.setLayoutParams(lp);
        }
        page.undo.setVisibility(canUndoLastPhoto() ? View.VISIBLE : View.INVISIBLE);
        if (page.duplicate != null) page.duplicate.setVisibility(duplicates.contains(current.id) ? View.VISIBLE : View.GONE);
        if (page.scanning != null) page.scanning.setVisibility(duplicateScanning ? View.VISIBLE : View.GONE);
        if (page.photoId != current.id) {
            page.card.animate().cancel();
            page.card.setTranslationX(0); page.card.setRotation(0); page.card.setAlpha(1f);
            page.effect.cancel();
            if (page.border != null) page.border.stop();
            page.image.setImageDrawable(null);
            loadReviewPhoto(current, page.image);
            attachSwipeGesture(page.card, page.stage, page.effect, page.border, current,
                    page.full ? null : () -> { fullScreenReview = true; render(); });
            page.photoId = current.id;
        }
        return true;
    }

    private void addVersionLabel() {
        TextView version = new TextView(this);
        version.setText("v" + BuildConfig.VERSION_NAME + " · Build " + BuildConfig.VERSION_CODE);
        version.setTextSize(11);
        version.setTextColor(MUTED);
        version.setGravity(Gravity.END);
        version.setPadding(0, dp(6), dp(4), 0);
        version.setContentDescription("Photo Sweep version " + BuildConfig.VERSION_NAME
                + ", build " + BuildConfig.VERSION_CODE);
        root.addView(version, new LinearLayout.LayoutParams(-1, -2));
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
        button(root, "Privacy policy", PANEL, INK, () -> accounts.showPrivacy());
        spacer(18);
        label("Photo access is your choice. Manage media is optional.", 13, MUTED, false);
        button(root, "Account & settings", PANEL, INK, () -> { showingSettings = true; render(); });
        LinearLayout body = new LinearLayout(this); body.setOrientation(LinearLayout.VERTICAL);
        while (root.getChildCount() > 0) { View child = root.getChildAt(0); root.removeView(child); body.addView(child); }
        ScrollView scroll = boundedScroll(); scroll.setFillViewport(true); scroll.addView(body);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
    }

    private TextView heading(String title, String subtitle) {
        TextView eyebrow = label("✦  PHOTO SWEEP", 12, GREEN, true);
        eyebrow.setLetterSpacing(.16f);
        spacer(9);
        TextView titleView = label(title, 32, INK, true);
        spacer(5);
        headingSubtitle = label(subtitle, 15, MUTED, false);
        spacer(21);
        return titleView;
    }

    private void yearsScreen() {
        LinearLayout top = new LinearLayout(this); top.setGravity(Gravity.CENTER_VERTICAL);
        root.addView(top, new LinearLayout.LayoutParams(-1, dp(64)));
        TextView brand = new TextView(this); brand.setText("PHOTO SWEEP"); brand.setLetterSpacing(.08f);
        brand.setTextColor(GREEN); brand.setTextSize(12); brand.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        top.addView(brand, new LinearLayout.LayoutParams(0, -2, 1));
        topAction(top, "✦", "Themes", GREEN, "Themes and animation settings",
                () -> { showingThemes = true; render(); });
        topAction(top, "⚙", "Settings", INK, "Options and sound settings",
                () -> { showingSettings = true; render(); });
        topAction(top, "🗑", "Trash", GOLD, "Recently trashed, " + ReviewNavigation.photoCount(trashEntries.size()),
                () -> { showingTrash = true; render(); });
        label(accounts == null ? "Your photos" : accounts.photosHeading(), 28, INK, true); spacer(13);
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
        ScrollView scroll = boundedScroll();
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        trackScroll(scroll, "years");
        LinearLayout list = new LinearLayout(this); list.setOrientation(LinearLayout.VERTICAL); scroll.addView(list);
        for (Map.Entry<Integer, int[]> entry : years.entrySet()) {
            int year = entry.getKey(); int[] count = entry.getValue();
            tile(list, Integer.toString(year), ReviewNavigation.photoCount(count[0]) + "  •  " + count[1] + " to review", () -> { selectedYear = year; render(); });
        }
    }

    private void topAction(LinearLayout row, String symbol, String title, int color,
                           String description, Runnable action) {
        LinearLayout button = new LinearLayout(this);
        button.setOrientation(LinearLayout.VERTICAL); button.setGravity(Gravity.CENTER);
        button.setBackground(themeButton(PANEL, 16));
        button.setContentDescription(description); button.setFocusable(true);
        TextView icon = new TextView(this); icon.setText(symbol); icon.setTextSize(23);
        icon.setTextColor(color); icon.setGravity(Gravity.CENTER);
        icon.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        button.addView(icon, new LinearLayout.LayoutParams(-1, dp(29)));
        TextView name = new TextView(this); name.setText(title); name.setTextSize(11);
        name.setTextColor(color); name.setGravity(Gravity.CENTER); name.setMaxLines(1);
        name.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        button.addView(name, new LinearLayout.LayoutParams(-1, -2));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(64), dp(58));
        lp.leftMargin = dp(7); row.addView(button, lp);
        button.setOnClickListener(v -> action.run());
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

    private LinearLayout optionsList(String scrollKey) {
        ScrollView scroll = boundedScroll();
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        trackScroll(scroll, scrollKey);
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(list);
        return list;
    }

    private void openOptionsSection(int section) {
        optionsSection = section;
        render();
    }

    private void settingsScreen() {
        if (optionsSection == 0) {
            back("Photo Sweep", () -> { showingSettings = false; render(); });
            heading("Options", "Choose what you want to adjust");
            LinearLayout list = optionsList("options");
            tile(list, "Account & support", "Sign in, support and app information", () -> openOptionsSection(2));
            tile(list, "Audio", "Swipe sounds and music", () -> openOptionsSection(1));
            tile(list, "Privacy & permissions", "Photo access and media controls", () -> openOptionsSection(3));
            tile(list, "Testing & progress", "Admin preview and local reset", () -> openOptionsSection(4));
            return;
        }
        String[] titles = {"", "Audio", "Account & support", "Privacy & permissions", "Testing & progress"};
        back("Options", () -> { optionsSection = 0; render(); });
        heading(titles[optionsSection], "Photo Sweep options");
        LinearLayout list = optionsList("options:" + optionsSection);
        switch (optionsSection) {
            case 1: addAudioOptions(list); break;
            case 2: addAccountOptions(list); break;
            case 3: addPrivacyOptions(list); break;
            case 4: addTestingOptions(list); break;
            default: optionsSection = 0; render(); break;
        }
    }

    private void themesScreen() {
        back("Photo Sweep", () -> { showingThemes = false; render(); });
        heading("Themes", "Choose your look and swipe animations");
        LinearLayout list = optionsList("themes");
        addSwipeControls(list);
        addThemeOptions(list);
    }

    private void addTestingOptions(LinearLayout list) {
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
        sectionTitle(list, "LOCAL PROGRESS");
        Button reset = new Button(this); reset.setText("Reset local progress"); reset.setTextColor(INK);
        reset.setBackground(rounded(PANEL, 12)); list.addView(reset, new LinearLayout.LayoutParams(-1, dp(52)));
        reset.setOnClickListener(v -> confirmProgressReset());
    }

    private void addThemeOptions(LinearLayout list) {
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
        addThemeChoices(worlds, new int[]{27, 30, 31}, level);

        addCountryThemes(tier3, true, level);
    }

    private void addAccountOptions(LinearLayout list) {
        sectionTitle(list, "ACCOUNT & SUPPORT");
        accounts.addControls(list, PANEL, INK, GREEN, MUTED, RED);
        TextView account = new TextView(this); account.setText("Photos and progress stay on this device.");
        account.setTextColor(INK); account.setTextSize(16); list.addView(account);
        TextView support = new TextView(this);
        support.setText("Accounts are optional. Purchases use your Google Play account. Ads and payments are coming later.");
        support.setTextColor(MUTED); support.setTextSize(14); support.setPadding(0, dp(8), 0, dp(16)); list.addView(support);
        button(list, "Account & support details", PANEL, INK, this::showSupportDetails);
    }

    private void addPrivacyOptions(LinearLayout list) {
        sectionTitle(list, "PRIVACY & PERMISSIONS");
        button(list, "Privacy policy", PANEL, INK, () -> accounts.showPrivacy());
        button(list, "Change photo access", PANEL, INK, this::requestAccess);
        button(list, "Android app permissions", PANEL, INK, () -> startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + getPackageName()))));
        button(list, "Optional prompt-free Trash", PANEL, INK, this::requestMediaManagement);
    }

    private void addAudioOptions(LinearLayout list) {
        sectionTitle(list, "AUDIO");
        settingSwitch(list, "Swipe sounds", "Soft, bubbly feedback while you swipe", soundEnabled, value -> {
            soundEnabled = value; getPreferences(MODE_PRIVATE).edit().putBoolean("sound_enabled", value).apply();
            if (value) playEffect(true);
        });
        settingSwitch(list, "D.G.S. Radio", "Lo-fi beats and dreamscape synths", musicEnabled, value -> {
            musicEnabled = value; getPreferences(MODE_PRIVATE).edit().putBoolean("music_enabled", value).apply(); updateMusic();
            if (radioTrackLabel != null) radioTrackLabel.setText((value ? "Now playing · " : "Track · ") + audio.currentTitle());
            if (!value) clearRadioBubble();
        });
        radioTrackLabel = new TextView(this);
        radioTrackLabel.setText((musicEnabled ? "Now playing · " : "Track · ") + audio.currentTitle());
        radioTrackLabel.setTextColor(INK); radioTrackLabel.setTextSize(16);
        radioTrackLabel.setPadding(0, dp(14), 0, dp(8));
        list.addView(radioTrackLabel, new LinearLayout.LayoutParams(-1, -2));
        button(list, "Next track →", PANEL, INK, () -> {
            audio.nextTrack();
            if (!musicEnabled) {
                musicEnabled = true;
                getPreferences(MODE_PRIVATE).edit().putBoolean("music_enabled", true).apply();
                updateMusic();
                render();
            }
        });
        settingSwitch(list, "Loop current track", "Repeat this song instead of changing tracks", audio.isRepeatTrack(), audio::setRepeatTrack);
        addVolumeControl(list, "Master volume", "master_volume", masterVolume, value -> masterVolume = value);
        addVolumeControl(list, "Music volume", "music_volume", musicVolume, value -> musicVolume = value);
        addVolumeControl(list, "VFX sound volume", "vfx_volume", vfxVolume, value -> vfxVolume = value);
        TextView note = new TextView(this); note.setText("Sounds use your phone's media volume. Music stops when you leave Photo Sweep.");
        note.setTextColor(MUTED); note.setTextSize(13); list.addView(note);
    }

    private int readVolume(String key, int fallback) {
        return Math.max(0, Math.min(100, getPreferences(MODE_PRIVATE).getInt(key, fallback)));
    }

    private void addVolumeControl(LinearLayout list, String title, String key, int initial,
                                  java.util.function.IntConsumer changed) {
        TextView label = new TextView(this);
        label.setText(title + "  ·  " + initial + "%");
        label.setTextColor(INK); label.setTextSize(16);
        LinearLayout.LayoutParams labelLp = new LinearLayout.LayoutParams(-1, -2);
        labelLp.topMargin = dp(17); list.addView(label, labelLp);
        SeekBar slider = new SeekBar(this);
        slider.setMax(100); slider.setProgress(initial);
        slider.setContentDescription(title);
        slider.setProgressTintList(android.content.res.ColorStateList.valueOf(GREEN));
        slider.setThumbTintList(android.content.res.ColorStateList.valueOf(GREEN));
        list.addView(slider, new LinearLayout.LayoutParams(-1, dp(52)));
        slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar bar, int value, boolean user) {
                if (!user) return;
                changed.accept(value);
                label.setText(title + "  ·  " + value + "%");
                getPreferences(MODE_PRIVATE).edit().putInt(key, value).apply();
                updateMusic();
            }
            @Override public void onStartTrackingTouch(SeekBar bar) { }
            @Override public void onStopTrackingTouch(SeekBar bar) {
                if (key.equals("vfx_volume")) playEffect(true);
            }
        });
    }

    private void addThemeChoices(LinearLayout list, int[] choices, int level) {
        for (int choice : choices) {
            if (!PlayPolicy.themeAllowed(choice)) continue;
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
        new android.app.AlertDialog.Builder(this).setTitle("Reset local progress?")
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
                .setMessage("ACCOUNTS\nUse Google or an existing email account, or continue as a guest. Photos and progress stay on this device.\n\nPURCHASES\nAd removal and support purchases are coming later. No ads or payments are active yet.\n\nRESTORE\nUse Restore purchases with the Google Play account that made the purchase. Photo Sweep sign-in does not change your Play account. Consumable support purchases cannot be restored.")
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
        row.setOnClickListener(v -> { playSound(R.raw.bubble_tap, .16f); action.run(); });
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
        ScrollView scroll = boundedScroll(); root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
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
        playSound(R.raw.bubble_undo, .28f);
    }

    private void trashScreen() {
        back("Photo Sweep", () -> { showingTrash = false; render(); });
        heading("Recently trashed", "7-day recovery window · Android controls final removal");
        label("Photo Sweep tracks the latest 20 photos for up to 7 days. With Manage media enabled, expired or older entries can be permanently removed. Without it, Android controls final removal.", 13, MUTED, false);
        if (trashEntries.isEmpty()) {
            spacer(36); label("Trash is empty", 21, INK, true);
            return;
        }
        ScrollView scroll = boundedScroll();
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

    private boolean hasPhotoElementBorder() {
        return (themeTier(themeChoice) == 3 || countryCollection(themeChoice) >= 0) && swipeStyle != 9;
    }

    private void reviewScreen() {
        List<Photo> month = monthPhotos();
        Photo current = null; int remaining = 0;
        for (Photo p : month) if (!reviewed.contains(Long.toString(p.id))) { remaining++; if (current == null) current = p; }
        if (fullScreenReview && current != null) { fullScreenReviewScreen(current, remaining, month.size()); return; }
        if (fullScreenReview) { fullScreenReview = false; applyContentInsets(); }
        back("Months", () -> { reviewing = false; fullScreenReview = false; render(); });
        String monthName = ReviewNavigation.title(selectedMonth, true);
        TextView dateHeading = heading(current == null ? monthName : photoDate(current), remaining + " of " + month.size() + " left to review");
        TextView remainingHeading = headingSubtitle;
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
        ReviewPage page = new ReviewPage(); reviewPage = page;
        page.date = dateHeading; page.count = remainingHeading; page.fill = fill; page.progress = progress; page.photoId = shown.id;
        FrameLayout stage = new FrameLayout(this); stage.setClipChildren(false); stage.setClipToPadding(false); page.stage = stage;
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(-1, 0, 1);
        cardParams.bottomMargin = dp(20); root.addView(stage, cardParams);
        FrameLayout card = new FrameLayout(this);
        card.setBackgroundColor(Color.TRANSPARENT);
        card.setClipChildren(false); card.setClipToPadding(false); page.card = card;
        card.setContentDescription("Photo. Tap for full-screen review, swipe left to Trash or right to Keep");
        stage.addView(card, new FrameLayout.LayoutParams(-1, -1));
        ImageView photo = new ImageView(this); photo.setScaleType(ImageView.ScaleType.FIT_CENTER);
        photo.setPadding(dp(28), dp(20), dp(28), dp(42)); page.image = photo;
        photo.setContentDescription("Tap for full-screen photo review");
        card.addView(new PhotoShadowView(photo), new FrameLayout.LayoutParams(-1, -1));
        card.addView(photo, new FrameLayout.LayoutParams(-1, -1));
        loadReviewPhoto(shown, photo);
        PhotoElementBorderView fireBorder = null;
        if (hasPhotoElementBorder()) {
            fireBorder = new PhotoElementBorderView(photo, themeChoice);
            card.addView(fireBorder, new FrameLayout.LayoutParams(-1, -1));
        }
        SwipeEffect effect = new SwipeEffect();
        effect.setElevation(dp(18)); stage.addView(effect, new FrameLayout.LayoutParams(-1, -1));
        {
            TextView bubble = pill("✦ Duplicate", GOLD, Color.rgb(89, 64, 27));
            FrameLayout.LayoutParams badge = new FrameLayout.LayoutParams(-2, -2, Gravity.TOP | Gravity.END);
            badge.setMargins(dp(12), dp(12), dp(12), 0); card.addView(bubble, badge);
            page.duplicate = bubble; bubble.setVisibility(duplicates.contains(shown.id) ? View.VISIBLE : View.GONE);
        }
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
        attachSwipeGesture(card, stage, effect, fireBorder, shown, () -> { fullScreenReview = true; render(); });
        page.card = card; page.effect = effect; page.border = fireBorder;
        {
            TextView undo = label("Undo last photo", 14, GREEN, true);
            page.undo = undo; undo.setVisibility(canUndoLastPhoto() ? View.VISIBLE : View.INVISIBLE);
            undo.setPadding(0, dp(9), 0, 0);
            undo.setOnClickListener(v -> undoLastPhoto());
        }
        addMonthNavigation();
        spacer(6); page.scanning = label("Checking for exact duplicates…", 12, MUTED, false);
        page.scanning.setVisibility(duplicateScanning ? View.VISIBLE : View.GONE);
    }

    private void attachSwipeGesture(FrameLayout card, FrameLayout stage, SwipeEffect effect,
                                    PhotoElementBorderView fireBorder, Photo shown, Runnable tap) {
        final boolean[] committed = {false}, multitouch = {false}; final float[] start = new float[2]; final int[] position = new int[2];
        card.setOnTouchListener((view, event) -> {
            if (committed[0] || reviewActionRunning || pendingTrash != -1 || pendingRestore != -1) return true;
            int action = event.getActionMasked();
            if (action == MotionEvent.ACTION_DOWN) {
                multitouch[0] = false;
                start[0] = event.getRawX(); start[1] = event.getRawY(); stage.getLocationOnScreen(position);
                if (fireBorder != null) fireBorder.ignite();
                effect.hold(start[0] - position[0], start[1] - position[1]); return true;
            }
            float dx = event.getRawX() - start[0], dy = event.getRawY() - start[1];
            if (action == MotionEvent.ACTION_POINTER_DOWN || action == MotionEvent.ACTION_CANCEL) {
                multitouch[0] = action == MotionEvent.ACTION_POINTER_DOWN;
                if (fireBorder != null) fireBorder.cool();
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
                    {
                        committed[0] = true; reviewActionRunning = true;
                        if (fireBorder != null) fireBorder.flare(dx > 0);
                        effect.release(dx > 0);
                        card.animate().translationX((dx > 0 ? 1 : -1) * (host.getWidth() + card.getWidth() / 2f))
                                .rotation(dx > 0 ? 16 : -16).setDuration(SwipeMotion.duration(swipeSpeed))
                                .withEndAction(() -> {
                                    effect.cancel();
                                    if (fireBorder != null) fireBorder.stop();
                                    reviewActionRunning = false;
                                    if (isDestroyed()) return;
                                    if (dx > 0) keep(shown); else trash(shown);
                                }).start();
                    }
                } else {
                    if (fireBorder != null) fireBorder.cool();
                    effect.cancel(); card.animate().translationX(0).rotation(0).setDuration(200).start();
                    if (tap != null && Math.abs(dx) < dp(12) && Math.abs(dy) < dp(12)) tap.run();
                }
                return true;
            }
            return true;
        });
    }
    private void fullScreenReviewScreen(Photo shown, int remaining, int total) {
        ReviewPage page = new ReviewPage(); reviewPage = page; page.photoId = shown.id;
        FrameLayout stage = new FrameLayout(this); stage.setBackgroundColor(Color.BLACK);
        stage.setClipChildren(false); stage.setClipToPadding(false); page.stage = stage;
        root.addView(stage, new LinearLayout.LayoutParams(-1, -1));
        FrameLayout card = new FrameLayout(this); page.card = card;
        card.setClipChildren(false); card.setClipToPadding(false);
        FrameLayout.LayoutParams photoArea = new FrameLayout.LayoutParams(-1, -1);
        photoArea.setMargins(0, dp(58), 0, dp(52)); stage.addView(card, photoArea);
        ImageView image = new ImageView(this); image.setScaleType(ImageView.ScaleType.FIT_CENTER);
        image.setPadding(dp(28), dp(20), dp(28), dp(42)); page.image = image;
        image.setContentDescription("Whole photo. Swipe right to Keep or left to Trash");
        card.addView(new PhotoShadowView(image), new FrameLayout.LayoutParams(-1, -1));
        card.addView(image, new FrameLayout.LayoutParams(-1, -1)); loadReviewPhoto(shown, image);
        PhotoElementBorderView fireBorder = null;
        if (hasPhotoElementBorder()) {
            fireBorder = new PhotoElementBorderView(image, themeChoice);
            card.addView(fireBorder, new FrameLayout.LayoutParams(-1, -1));
        }
        SwipeEffect effect = new SwipeEffect(); effect.setElevation(dp(18)); stage.addView(effect, new FrameLayout.LayoutParams(-1, -1));
        page.effect = effect; page.border = fireBorder;
        attachSwipeGesture(card, stage, effect, fireBorder, shown, null);
        LinearLayout toolbar = new LinearLayout(this); toolbar.setGravity(Gravity.CENTER_VERTICAL);
        toolbar.setPadding(dp(8), dp(6), dp(8), dp(6)); toolbar.setBackgroundColor(0xC0141A22); toolbar.setElevation(dp(24));
        FrameLayout.LayoutParams top = new FrameLayout.LayoutParams(-1, dp(58), Gravity.TOP); stage.addView(toolbar, top);
        Button exit = new Button(this); exit.setText("Exit full screen"); exit.setTextSize(12); exit.setTextColor(INK); exit.setBackground(rounded(PANEL, 10));
        toolbar.addView(exit, new LinearLayout.LayoutParams(dp(124), -1));
        exit.setOnClickListener(v -> { if (!reviewActionRunning) { fullScreenReview = false; render(); } });
        LinearLayout details = new LinearLayout(this); details.setOrientation(LinearLayout.VERTICAL); details.setGravity(Gravity.CENTER);
        toolbar.addView(details, new LinearLayout.LayoutParams(0, -1, 1));
        TextView date = new TextView(this); date.setText(photoDate(shown)); date.setTextColor(INK); date.setTextSize(13); details.addView(date); page.date = date;
        TextView count = new TextView(this); count.setText(remaining + "/" + total + " left");
        count.setTextColor(MUTED); count.setTextSize(12); details.addView(count); page.count = count;
        Button inspect = new Button(this); inspect.setText("Zoom"); inspect.setTextSize(12); inspect.setTextColor(INK); inspect.setBackground(rounded(PANEL, 10));
        toolbar.addView(inspect, new LinearLayout.LayoutParams(dp(65), -1));
        inspect.setOnClickListener(v -> {
            if (!reviewActionRunning) for (Photo photo : monthPhotos()) if (photo.id == page.photoId) { showPhotoZoom(photo); break; }
        });
        LinearLayout footer = new LinearLayout(this); footer.setGravity(Gravity.CENTER_VERTICAL); footer.setBackgroundColor(0xB0141A22); footer.setElevation(dp(24));
        FrameLayout.LayoutParams bottom = new FrameLayout.LayoutParams(-1, dp(52), Gravity.BOTTOM); stage.addView(footer, bottom);
        TextView hint = new TextView(this); hint.setText("← Trash     Keep →"); hint.setTextColor(INK); hint.setTextSize(13); hint.setGravity(Gravity.CENTER);
        footer.addView(hint, new LinearLayout.LayoutParams(0, -1, 1));
        {
            Button undo = new Button(this); undo.setText("Undo last photo");
            page.undo = undo; undo.setVisibility(canUndoLastPhoto() ? View.VISIBLE : View.INVISIBLE); undo.setTextSize(12); undo.setTextColor(INK); undo.setBackground(rounded(PANEL, 10));
            footer.addView(undo, new LinearLayout.LayoutParams(dp(144), dp(44))); undo.setOnClickListener(v -> { if (!reviewActionRunning) undoLastPhoto(); });
        }
    }
    private Bitmap decodeReviewPhoto(Photo photo) {
        Bitmap cached = reviewPhotos.get(photo.id);
        if (cached != null && !cached.isRecycled()) return cached;
        try {
            Bitmap decoded = ImageDecoder.decodeBitmap(ImageDecoder.createSource(getContentResolver(), photo.uri), (decoder, info, source) -> {
                int w = info.getSize().getWidth(), h = info.getSize().getHeight();
                float scale = PhotoFit.scale(w, h);
                decoder.setTargetSize(Math.max(1, Math.round(w * scale)), Math.max(1, Math.round(h * scale)));
                decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE);
            });
            if (!isDestroyed()) reviewPhotos.put(photo.id, decoded);
            return decoded;
        } catch (Exception ignored) { return null; }
    }

    private void loadReviewPhoto(Photo photo, ImageView view) {
        view.setTag(photo.id);
        Bitmap cached = reviewPhotos.get(photo.id);
        if (cached != null && !cached.isRecycled()) { view.setImageBitmap(cached); invalidatePhotoDecorations(view); }
        else {
            Bitmap preview = previews.get(photo.id); if (preview != null) view.setImageBitmap(preview);
            io.execute(() -> {
                if (isDestroyed()) return;
                Bitmap decoded = decodeReviewPhoto(photo);
                runOnUiThread(() -> {
                    // The same ImageView is reused for the next card. A late
                    // decode must never put the previous photo back on screen.
                    if (decoded == null || isDestroyed() || !view.isAttachedToWindow() || !Long.valueOf(photo.id).equals(view.getTag())) return;
                    view.setImageBitmap(decoded);
                    invalidatePhotoDecorations(view);
                });
            });
        }
        // Decode only the next undecided photo into a bounded cache so a
        // completed swipe can replace the image without an empty loading frame.
        boolean found = false;
        for (Photo next : monthPhotos()) {
            if (next.id == photo.id) { found = true; continue; }
            if (found && !reviewed.contains(Long.toString(next.id))) {
                if (prefetchedReviewId != next.id) {
                    prefetchedReviewId = next.id;
                    io.execute(() -> { if (!isDestroyed()) decodeReviewPhoto(next); });
                }
                break;
            }
        }
    }

    private void invalidatePhotoDecorations(ImageView view) {
        if (view.getParent() instanceof FrameLayout) {
            FrameLayout parent = (FrameLayout) view.getParent();
            for (int i = 0; i < parent.getChildCount(); i++) {
                View sibling = parent.getChildAt(i);
                if (sibling instanceof PhotoShadowView || sibling instanceof PhotoElementBorderView) sibling.invalidate();
            }
        }
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
            if (id == R.drawable.fx_water_drop || id == R.drawable.fx_toxic_drop) options.inSampleSize = 4;
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
    private boolean visiblePhotoBounds(ImageView photo, RectF bounds) {
        android.graphics.drawable.Drawable drawable = photo.getDrawable();
        if (drawable == null || drawable.getIntrinsicWidth() <= 0 || drawable.getIntrinsicHeight() <= 0
                || photo.getWidth() <= 0 || photo.getHeight() <= 0) return false;
        // This is the same matrix ImageView uses to paint its drawable. It includes
        // FIT_CENTER's letterboxing and tracks a new bitmap or orientation layout.
        bounds.set(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
        Matrix matrix = photo.getImageMatrix();
        matrix.mapRect(bounds);
        bounds.offset(photo.getLeft() + photo.getPaddingLeft(), photo.getTop() + photo.getPaddingTop());
        return !bounds.isEmpty();
    }

    /** Cast a soft shadow from the visible image, leaving letterboxed space transparent. */
    private class PhotoShadowView extends View {
        private final ImageView photo;
        private final RectF bounds = new RectF();
        private final Paint shadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        PhotoShadowView(ImageView photo) {
            super(MainActivity.this);
            this.photo = photo;
            setLayerType(View.LAYER_TYPE_SOFTWARE, null);
            setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
            shadowPaint.setColor(0xFF211B1B);
            shadowPaint.setShadowLayer(dp(18), 0, dp(7), 0xC0000000);
        }

        @Override protected void onDraw(Canvas canvas) {
            if (!visiblePhotoBounds(photo, bounds)) {
                postInvalidateDelayed(250);
                return;
            }
            canvas.drawRoundRect(bounds, dp(5), dp(5), shadowPaint);
        }
    }

    /** Keep every elemental border on the photo itself, never on the review stage. */
    private class PhotoElementBorderView extends View {
        private static final int IDLE = 0, IGNITING = 1, BURNING = 2, COOLING = 3;
        private final ImageView photo;
        private final int element;
        private final Paint flamePaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        private final Rect source = new Rect();
        private final RectF destination = new RectF();
        private final RectF outline = new RectF();
        private final float[] sourceX = new float[4], sourceY = new float[4];
        private final float[] targetX = new float[4], targetY = new float[4];
        private final Path leftRoute = new Path(), rightRoute = new Path(), reveal = new Path();
        private final Paint revealStroke = new Paint(Paint.ANTI_ALIAS_FLAG);
        // The painted rim moves within the 6x2 AI sprite sheet. These anchors
        // were measured at several points along every frame's bright inner edge.
        private final int[] burnLeft = {52,47,42,38,32,26,53,46,41,39,32,28};
        private final int[] burnRight = {231,229,221,217,212,207,230,229,220,218,213,207};
        private final int[] touchLeft = {54,48,44,41,34,29,53,48,44,41,40,32};
        private final int[] touchRight = {232,230,222,218,212,207,230,226,221,218,213,209};
        private final Paint burstPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        private int phase = IDLE;
        private long phaseStarted;
        private long releaseAt;
        private boolean releaseKeep;
        private float coolFrom = 1f;

        PhotoElementBorderView(ImageView photo, int element) {
            super(MainActivity.this);
            this.photo = photo;
            this.element = element;
            setClickable(false);
            setFocusable(false);
            setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
            setVisibility(View.GONE);
            revealStroke.setStyle(Paint.Style.STROKE);
            revealStroke.setStrokeCap(Paint.Cap.ROUND);
            revealStroke.setStrokeJoin(Paint.Join.ROUND);
        }
        private float progress(long now) {
            if (phase == IGNITING) return Math.min(1f, (now - phaseStarted) / 850f);
            if (phase == BURNING) return 1f;
            if (phase == COOLING) return coolFrom * Math.max(0f, 1f - (now - phaseStarted) / 650f);
            return 0f;
        }
        void ignite() {
            releaseAt = 0;
            phase = IGNITING;
            phaseStarted = android.os.SystemClock.uptimeMillis();
            setVisibility(View.VISIBLE);
            invalidate();
        }
        void flare(boolean keep) {
            long now = android.os.SystemClock.uptimeMillis();
            coolFrom = progress(now);
            phase = COOLING;
            phaseStarted = releaseAt = now;
            releaseKeep = keep;
            invalidate();
        }
        void cool() {
            long now = android.os.SystemClock.uptimeMillis();
            coolFrom = progress(now);
            releaseAt = 0;
            phase = COOLING;
            phaseStarted = now;
            invalidate();
        }
        void stop() { releaseAt = 0; phase = IDLE; setVisibility(View.GONE); }

        private void drawCountryBorder(Canvas canvas, int country, long now, float growth) {
            if (!visiblePhotoBounds(photo, outline)) return;
            int[][] colors = {
                    {0xfff5eee5, 0xffdc3443, 0xffffb8cb}, {0xff16824b, 0xfff8eed0, 0xffdc4735},
                    {0xffdf3e4e, 0xfff9f4e8, 0xff335fa9}, {0xffb8293d, 0xffffcb45, 0xffc54836},
                    {0xff1c9856, 0xffffd944, 0xff2368b0}, {0xff3456a4, 0xfffaf1e3, 0xffc64651},
                    {0xff34845c, 0xfff7eee2, 0xffc84445}, {0xfff1f1ef, 0xffda4050, 0xff326cb2}};
            int save = canvas.save(); clipGrowth(canvas, growth, dp(48));
            float radius = dp(8), time = now / 1000f;
            Path rim = new Path(); rim.addRoundRect(outline, radius, radius, Path.Direction.CW);
            flamePaint.setShader(null); flamePaint.setStyle(Paint.Style.STROKE); flamePaint.setStrokeWidth(dp(7));
            flamePaint.setColor(colors[country][0]); flamePaint.setAlpha(235);
            canvas.drawPath(rim, flamePaint);
            flamePaint.setStrokeWidth(dp(country == 4 ? 4 : 2.5f));
            flamePaint.setColor(colors[country][1]);
            float dash = dp(country == 2 ? 12 : country == 3 ? 20 : 7);
            flamePaint.setPathEffect(new android.graphics.DashPathEffect(new float[]{dash, dp(country == 0 ? 18 : 5)}, time * dp(5)));
            canvas.drawPath(rim, flamePaint); flamePaint.setPathEffect(null);
            RectF outer = new RectF(outline); outer.inset(-dp(5), -dp(5));
            flamePaint.setColor(colors[country][2]); flamePaint.setStrokeWidth(dp(1.5f));
            canvas.drawRoundRect(outer, radius, radius, flamePaint);
            // Country-specific artwork follows the photo perimeter, not a shared lantern frame.
            Bitmap motif = effectSprite(COUNTRY_ANIMATED_THEMES[country]);
            PathMeasure route = new PathMeasure(rim, false); float length = route.getLength();
            float[] point = new float[2], tangent = new float[2];
            int count = country == 3 || country == 7 ? 10 : 16;
            flamePaint.setStyle(Paint.Style.FILL); flamePaint.setAlpha(245);
            if (motif != null) for (int i = 0; i < count; i++) {
                route.getPosTan(length * (i + .5f) / count, point, tangent);
                float size = dp(country == 0 || country == 7 ? 23 : country == 2 ? 18 : 21);
                float height = size * motif.getHeight() / motif.getWidth();
                canvas.save();
                canvas.rotate((float)Math.toDegrees(Math.atan2(tangent[1], tangent[0])) + 90
                        + (float)Math.sin(time * 1.6f + i) * (country == 3 ? 12 : 5), point[0], point[1]);
                canvas.drawBitmap(motif, null, new RectF(point[0]-size/2, point[1]-height/2,
                        point[0]+size/2, point[1]+height/2), flamePaint);
                canvas.restore();
            }
            drawCountryFlag(canvas, flamePaint, country, outline.left + dp(15), outline.top + dp(4), dp(26));
            drawCountryFlag(canvas, flamePaint, country, outline.right - dp(15), outline.bottom - dp(4), dp(26));
            flamePaint.setStyle(Paint.Style.FILL); flamePaint.setAlpha(255); canvas.restoreToCount(save);
        }

        private int elementArt() {
            switch (element) {
                case 18: return R.drawable.photo_border_candy;
                case 19: return R.drawable.photo_border_toxic;
                case 20: return R.drawable.photo_border_space;
                case 22: return R.drawable.photo_border_water;
                case 23: return R.drawable.photo_border_ice;
                case 24: return R.drawable.photo_border_earth;
                case 25: return R.drawable.photo_border_lightning;
                case 27: case 30: case 40: return R.drawable.photo_border_blossom;
                case 26: case 31: case 57: return R.drawable.photo_border_spirit;
                case 32: case 33: case 50: case 51: case 52: case 53: case 54: case 55:
                    return R.drawable.photo_border_festival;
                case 34: return R.drawable.photo_border_web;
                case 36: return R.drawable.photo_border_lightning;
                case 39: case 42: return R.drawable.photo_border_space;
                case 35: case 37: case 38: case 41: case 43: return R.drawable.photo_border_hero;
                case 58: return R.drawable.photo_border_water;
                case 56: case 59: case 60: return R.drawable.photo_border_anime;
                default: return 0;
            }
        }

        private float[] artAnchors() {
            // Measured from each generated asset's alpha opening at its middle
            // row/column (source size 887 x 1774), not from the outer PNG box.
            switch (element) {
                case 18: return new float[]{169, 720, 147, 1604};
                case 19: return new float[]{192, 694, 148, 1616};
                case 20: return new float[]{159, 737, 137, 1612};
                case 22: return new float[]{169, 738, 225, 1579};
                case 23: return new float[]{144, 744, 299, 1465};
                case 24: return new float[]{160, 757, 180, 1574};
                case 25: case 36: return new float[]{196, 692, 166, 1603};
                case 27: case 30: case 40: return new float[]{187, 738, 168, 1552};
                case 26: case 31: case 57: return new float[]{151, 738, 224, 1558};
                case 32: case 33: case 50: case 51: case 52: case 53: case 54: case 55:
                    return new float[]{175, 762, 102, 1547};
                case 34: return new float[]{132, 754, 180, 1549};
                case 39: case 42: return new float[]{159, 737, 137, 1612};
                case 35: case 37: case 38: case 41: case 43:
                    return new float[]{115, 786, 87, 1626};
                case 58: return new float[]{169, 738, 225, 1579};
                case 56: case 59: case 60: return new float[]{159, 728, 258, 1453};
                default: return new float[]{0, 0, 0, 0};
            }
        }

        private void clipGrowth(Canvas canvas, float growth, float thickness) {
            float r = Math.min(dp(6), Math.min(outline.width(), outline.height()) * .1f);
            float l = outline.left, t = outline.top, right = outline.right, b = outline.bottom;
            // Two symmetric paths begin at the bottom middle. Their rounded
            // corners climb the photo and meet at its top middle.
            leftRoute.reset(); leftRoute.moveTo(outline.centerX(), b);
            leftRoute.lineTo(l + r, b); leftRoute.quadTo(l, b, l, b - r);
            leftRoute.lineTo(l, t + r); leftRoute.quadTo(l, t, l + r, t);
            leftRoute.lineTo(outline.centerX(), t);
            rightRoute.reset(); rightRoute.moveTo(outline.centerX(), b);
            rightRoute.lineTo(right - r, b); rightRoute.quadTo(right, b, right, b - r);
            rightRoute.lineTo(right, t + r); rightRoute.quadTo(right, t, right - r, t);
            rightRoute.lineTo(outline.centerX(), t);
            reveal.reset();
            PathMeasure measure = new PathMeasure(leftRoute, false);
            measure.getSegment(0, measure.getLength() * growth, reveal, true);
            measure.setPath(rightRoute, false);
            measure.getSegment(0, measure.getLength() * growth, reveal, true);
            revealStroke.setStrokeWidth(thickness);
            Path coverage = new Path();
            revealStroke.getFillPath(reveal, coverage);
            canvas.clipPath(coverage);
        }

        @Override protected void onDraw(Canvas canvas) {
            if (phase == IDLE) return;
            long now = android.os.SystemClock.uptimeMillis();
            float growth = progress(now);
            if (growth <= 0f) {
                if (phase == COOLING && now - phaseStarted >= 650) stop();
                else if (isAttachedToWindow()) postInvalidateOnAnimation();
                return;
            }
            int country = countryCollection(element);
            if (country >= 0) {
                if (phase == IGNITING && now - phaseStarted >= 850) { phase = BURNING; phaseStarted = now; }
                drawCountryBorder(canvas, country, now, growth);
                if (isAttachedToWindow()) postInvalidateOnAnimation();
                return;
            }
            Bitmap sheet;
            long age = now - phaseStarted;
            int frame;
            if (phase == IGNITING && age >= 850) { phase = BURNING; phaseStarted = now; age = 0; }
            boolean fire = element == 21;
            if (!fire) {
                sheet = swipeAsset(elementArt());
                frame = 0;
            } else if (phase == BURNING) {
                sheet = swipeAsset(R.drawable.fire_border_frames);
                frame = (int)((age / 105) % 12);
            } else {
                sheet = swipeAsset(R.drawable.fire_touch_frames);
                frame = phase == IGNITING ? 6 + (int)Math.min(5, age / 140)
                        : (int)Math.max(0, 5 - Math.min(5, age / 105));
            }
            if (sheet == null || sheet.isRecycled()) return;
            int frameWidth = fire ? sheet.getWidth() / 6 : sheet.getWidth();
            int frameHeight = fire ? sheet.getHeight() / 2 : sheet.getHeight();
            source.set(fire ? (frame % 6) * frameWidth : 0, fire ? (frame / 6) * frameHeight : 0,
                    (fire ? frame % 6 + 1 : 1) * frameWidth, (fire ? frame / 6 + 1 : 1) * frameHeight);
            if (!visiblePhotoBounds(photo, outline)) {
                postInvalidateDelayed(250);
                return;
            }
            // Normalize each moving sprite rim to the exact same photo outline.
            // The first row's horizontal rim is around y=101/424; the second
            // row is painted 32 pixels higher. Its x position also drifts left
            // by up to 26 pixels through each six-frame sequence.
            boolean burningSheet = phase == BURNING;
            float[] anchors = fire ? null : artAnchors();
            float left = fire ? (burningSheet ? burnLeft[frame] : touchLeft[frame]) * frameWidth / 256f : anchors[0] * frameWidth / 887f;
            float right = fire ? (burningSheet ? burnRight[frame] : touchRight[frame]) * frameWidth / 256f : anchors[1] * frameWidth / 887f;
            float top = fire ? (frame >= 6 ? (burningSheet ? 69 : 74) : (burningSheet ? 101 : 102)) * frameHeight / 512f : anchors[2] * frameHeight / 1774f;
            float bottom = fire ? (frame >= 6 ? (burningSheet ? 391 : 393) : (burningSheet ? 423 : 424)) * frameHeight / 512f : anchors[3] * frameHeight / 1774f;
            float unit = getResources().getDisplayMetrics().density * (fire ? .42f : .14f);
            float cornerX = fire ? 32 * frameWidth / 256f : frameWidth * .12f;
            float cornerTop = fire ? 43 * frameHeight / 512f : frameHeight * .07f;
            float cornerBottom = fire ? 34 * frameHeight / 512f : frameHeight * .07f;
            sourceX[0] = 0; sourceX[1] = left + cornerX;
            sourceX[2] = right - cornerX; sourceX[3] = frameWidth;
            sourceY[0] = 0; sourceY[1] = top + cornerTop;
            sourceY[2] = bottom - cornerBottom; sourceY[3] = frameHeight;
            targetX[0] = outline.left - left * unit;
            targetX[1] = outline.left + cornerX * unit;
            targetX[2] = outline.right - cornerX * unit;
            targetX[3] = outline.right + (frameWidth - right) * unit;
            targetY[0] = outline.top - top * unit;
            targetY[1] = outline.top + cornerTop * unit;
            targetY[2] = outline.bottom - cornerBottom * unit;
            targetY[3] = outline.bottom + (frameHeight - bottom) * unit;
            // Very small photos still get a non-inverted center section.
            if (targetX[1] > targetX[2]) targetX[1] = targetX[2] = outline.centerX();
            if (targetY[1] > targetY[2]) targetY[1] = targetY[2] = outline.centerY();
            int frameLeft = source.left, frameTop = source.top;
            int save = canvas.save();
            clipGrowth(canvas, growth, fire ? dp(92) : dp(55));
            if (!fire) flamePaint.setAlpha((int)(220 + 30 * Math.sin(now / 280.0)));
            for (int y = 0; y < 3; y++) for (int x = 0; x < 3; x++) {
                if (x == 1 && y == 1) continue; // Never paint over the photo interior.
                source.set(frameLeft + Math.round(sourceX[x]), frameTop + Math.round(sourceY[y]),
                        frameLeft + Math.round(sourceX[x + 1]), frameTop + Math.round(sourceY[y + 1]));
                destination.set(targetX[x], targetY[y], targetX[x + 1], targetY[y + 1]);
                if (!destination.isEmpty()) canvas.drawBitmap(sheet, source, destination, flamePaint);
            }
            canvas.restoreToCount(save);
            if (fire && releaseAt != 0) {
                float progress = Math.min(1f, (android.os.SystemClock.uptimeMillis() - releaseAt) / 460f);
                if (progress < 1f) {
                    Bitmap burst = swipeAsset(R.drawable.fire_release_burst);
                    if (burst != null && !burst.isRecycled()) {
                        float width = Math.min(outline.width() * .65f, dp(260));
                        float height = Math.min(outline.height() * .75f, dp(500));
                        float direction = releaseKeep ? 1 : -1;
                        float center = outline.centerX() + direction * outline.width() * progress * .28f;
                        burstPaint.setAlpha((int)(190 * (1 - progress) * (1 - progress)));
                        canvas.drawBitmap(burst, null, new RectF(center - width / 2, outline.centerY() - height / 2,
                                center + width / 2, outline.centerY() + height / 2), burstPaint);
                    }
                } else releaseAt = 0;
            }
            if (phase != IDLE && isAttachedToWindow()) postInvalidateOnAnimation();
        }
    }
    private class SwipeEffect extends SwipeVfxView {
        SwipeEffect() {
            super(MainActivity.this, MainActivity.this::selectedSwipeSprite,
                    countryCollection(themeChoice) >= 0 && (swipeStyle == 0 || swipeStyle == 8)
                            ? COUNTRY_FLAGS[countryCollection(themeChoice)] : null,
                    selectedSwipeProfile(),
                    (!canCustomizeSwipe() || swipeStyle != 9) && !(themeChoice == 21 && swipeStyle == 0),
                    swipeIntensity, swipeSpeed);
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
        playMonthComplete();
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
        if (pendingTrash != -1 || pendingRestore != -1) return;
        try {
            PendingIntent request = MediaStore.createTrashRequest(getContentResolver(), Collections.singletonList(p.uri), true);
            pendingTrash = p.id; pendingTrashUndo = snapshot(p, true);
            startIntentSenderForResult(request.getIntentSender(), TRASH_REQUEST, null, 0, 0, 0);
        } catch (Exception e) {
            pendingTrash = -1; pendingTrashUndo = null; skipMediaReloadOnResume = false;
            if (reviewPage != null) reviewPage.photoId = -1;
            Toast.makeText(this, "Could not move photo to Trash", Toast.LENGTH_SHORT).show(); render();
        }
    }

    private void restore(TrashEntry entry) {
        if (pendingRestore != -1 || pendingTrash != -1) return;
        try {
            pendingRestore = entry.id;
            PendingIntent request = MediaStore.createTrashRequest(getContentResolver(), Collections.singletonList(entry.uri), false);
            startIntentSenderForResult(request.getIntentSender(), RESTORE_REQUEST, null, 0, 0, 0);
        } catch (Exception e) {
            pendingRestore = -1; pendingRestoreUndo = false; skipMediaReloadOnResume = false; Toast.makeText(this, "Could not restore photo", Toast.LENGTH_SHORT).show();
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
                saveTrashEntries(); if (showingTrash) render();
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
            if (resultCode != RESULT_OK && reviewPage != null) reviewPage.photoId = -1;
            render();
            if (earned > 0) floatXp(earned);
            if (resultCode == RESULT_OK) { playMonthComplete(); cleanupTrash(); }
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
        if (audio != null) {
            audio.setMix(masterVolume / 100f, vfxVolume / 100f);
            audio.configure(musicEnabled, musicVolume / 100f);
        }
    }

    private void stopMusic() {
        if (audio != null) audio.pause();
    }

    private void playSound(int resource, float volume) {
        if (soundEnabled && audio != null) audio.effect(resource, volume);
    }

    private void playEffect(boolean keep) {
        playSound(keep ? R.raw.bubble_keep : R.raw.bubble_trash, .30f);
        int accent;
        switch (selectedSwipeProfile()) {
            case FIRE: accent = R.raw.bubble_fire; break;
            case WATER: accent = R.raw.bubble_water; break;
            case TOXIC: accent = R.raw.bubble_toxic; break;
            case CANDY: accent = R.raw.bubble_candy; break;
            default: return;
        }
        playSound(accent, .13f);
    }

    private void playMonthComplete() {
        if (selectedMonth == null || showingTrash) return;
        for (Photo photo : monthPhotos())
            if (!reviewed.contains(Long.toString(photo.id))) return;
        uiHandler.postDelayed(() -> {
            if (!isFinishing() && !isDestroyed() && hasWindowFocus())
                playSound(R.raw.bubble_complete, .23f);
        }, 380);
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
        @Override protected void onSizeChanged(int w, int h, int oldW, int oldH) {
            super.onSizeChanged(w, h, oldW, oldH);
            if (initialized && oldW > 0 && oldH > 0 && w > 0 && h > 0) {
                for (int i = 0; i < sweetX.length; i++) {
                    sweetX[i] *= w / (float) oldW;
                    sweetY[i] *= h / (float) oldH;
                }
                lastFrame = 0;
            }
        }
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
            if (themeChoice == 19 || themeChoice == 22) {
                drawLiquid(canvas, w, h, android.os.SystemClock.uptimeMillis() / 1000f, themeChoice == 19);
                if (isAttachedToWindow()) postInvalidateDelayed(32);
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
        private final LiquidPlane liquidPlane = new LiquidPlane();
        private final Path liquidFill = new Path(), liquidLine = new Path(), ooze = new Path();
        private final float[] dropPrevious = new float[6], rippleStarted = new float[6];
        private long liquidLastFrame, liquidStarted;
        private float liquidAngle = (float)Math.PI / 2;
        private float slimeVelocity;
        private float liquidWave(float fraction, float time, boolean toxic) {
            if (!toxic) return (float)Math.sin(fraction*12+time*1.3f)*dp(2)*(float)Math.sin(Math.PI*fraction);
            float amplitude = dp(4) + Math.min(dp(10), Math.abs(slimeVelocity)*dp(18));
            return (float)Math.sin(fraction*Math.PI*2)*amplitude*(float)Math.sin(Math.PI*fraction)
                    * (float)Math.cos(time*.6f);
        }
        private void drawSlimeGlob(Canvas canvas, float x, float y, float radiusX, float radiusY) {
            // One cohesive shape and material before and after the neck pinches off.
            ooze.reset(); ooze.moveTo(x,y-radiusY);
            ooze.cubicTo(x-radiusX*.8f,y-radiusY,x-radiusX,y-radiusY*.2f,x-radiusX,y+radiusY*.15f);
            ooze.cubicTo(x-radiusX,y+radiusY*1.2f,x+radiusX,y+radiusY*1.2f,x+radiusX,y+radiusY*.15f);
            ooze.cubicTo(x+radiusX,y-radiusY*.2f,x+radiusX*.8f,y-radiusY,x,y-radiusY); ooze.close();
            paint.setStyle(Paint.Style.FILL);
            paint.setShader(new LinearGradient(x-radiusX,y,x+radiusX,y+radiusY,
                    0xddb4df48,0xdd668e28,Shader.TileMode.CLAMP)); canvas.drawPath(ooze,paint); paint.setShader(null);
            paint.setColor(0x66e1ff90); canvas.drawOval(x-radiusX*.6f,y-radiusY*.35f,
                    x-radiusX*.25f,y+radiusY*.35f,paint);
        }
        private void drawLiquid(Canvas canvas, float w, float h, float ignored, boolean toxic) {
            long now = android.os.SystemClock.uptimeMillis();
            if (liquidStarted == 0) liquidStarted = now;
            float time = (now-liquidStarted)/1000f;
            float dt = liquidLastFrame == 0 ? .032f : Math.min(.08f, (now-liquidLastFrame)/1000f);
            liquidLastFrame = now;
            // Ignore an almost-flat phone's weak/noisy screen-plane gravity.
            // Slime follows gravity with a damped, heavy response rather than locking in place.
            if (Math.hypot(gravityX, gravityY) > .45f) {
                float target = (float)Math.atan2(gravityY, gravityX);
                float difference = (float)Math.atan2(Math.sin(target-liquidAngle), Math.cos(target-liquidAngle));
                if (toxic) {
                    slimeVelocity += (difference*1.8f - slimeVelocity*2.4f)*dt;
                    liquidAngle += slimeVelocity*dt;
                } else liquidAngle += difference * (1-(float)Math.exp(-dt/.32f));
            } else if (toxic) slimeVelocity *= (float)Math.exp(-dt*2.4f);
            liquidPlane.update(w, h, (float)Math.cos(liquidAngle), (float)Math.sin(liquidAngle), .25f);
            float nx = liquidPlane.nx, ny = liquidPlane.ny, tx = -ny, ty = nx;
            float ax = liquidPlane.ax, ay = liquidPlane.ay, bx = liquidPlane.bx, by = liquidPlane.by;
            // Wave displacement stays tiny; the underlying plane preserves one quarter of the area.
            liquidFill.reset();
            for (int i = 0; i < liquidPlane.points; i++) {
                float x = liquidPlane.polygon[i*2], y = liquidPlane.polygon[i*2+1];
                if (i == 0) liquidFill.moveTo(x,y); else liquidFill.lineTo(x,y);
            }
            liquidFill.close();
            if (toxic) {
                // The visible pool body follows its slow wave, not just a decorative line.
                liquidFill.reset();
                float lengthSquared = (bx-ax)*(bx-ax)+(by-ay)*(by-ay);
                for (int i = 0; i < liquidPlane.points; i++) {
                    int j = (i+1)%liquidPlane.points;
                    float x = liquidPlane.polygon[i*2], y = liquidPlane.polygon[i*2+1];
                    float nextX = liquidPlane.polygon[j*2], nextY = liquidPlane.polygon[j*2+1];
                    if (i == 0) liquidFill.moveTo(x,y);
                    boolean surface = Math.abs(nx*x+ny*y-liquidPlane.level)<.05f
                            && Math.abs(nx*nextX+ny*nextY-liquidPlane.level)<.05f;
                    if (surface) for (int step = 1; step <= 48; step++) {
                        float t = step/48f, px = x+(nextX-x)*t, py = y+(nextY-y)*t;
                        float fraction = ((px-ax)*(bx-ax)+(py-ay)*(by-ay))/Math.max(1,lengthSquared);
                        float wave = liquidWave(fraction,time,true);
                        liquidFill.lineTo(px+nx*wave,py+ny*wave);
                    } else liquidFill.lineTo(nextX,nextY);
                }
                liquidFill.close();
            }
            paint.setStyle(Paint.Style.FILL); paint.setAlpha(255);
            float cx = (ax+bx)*.5f, cy = (ay+by)*.5f, depth = Math.max(w,h)*.35f;
            paint.setShader(new LinearGradient(cx, cy, cx+nx*depth, cy+ny*depth,
                    toxic ? 0xcb7cad2e : 0x995abedb, toxic ? 0xed1b351a : 0xdf164b70, Shader.TileMode.CLAMP));
            canvas.drawPath(liquidFill, paint); paint.setShader(null);
            liquidLine.reset();
            for (int i = 0; i <= 48; i++) {
                float t = i/48f, wave = liquidWave(t,time,toxic);
                float x = ax+(bx-ax)*t+nx*wave, y = ay+(by-ay)*t+ny*wave;
                if (i == 0) liquidLine.moveTo(x,y); else liquidLine.lineTo(x,y);
            }
            paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(dp(toxic ? 3 : 1.5f));
            paint.setColor(toxic ? 0xccb3e768 : 0xbbe0f8ff); canvas.drawPath(liquidLine, paint);
            paint.setStyle(Paint.Style.FILL);
            int count = toxic ? 3 : dropPrevious.length;
            for (int i = 0; i < count; i++) {
                float duration = toxic ? 18+i*3 : 5+i*.7f;
                float phase = loop(time/duration+i*.173f, 1f);
                float fraction = .12f + i * .76f / Math.max(1,count-1);
                float impactX = ax+(bx-ax)*fraction, impactY = ay+(by-ay)*fraction;
                float hit = toxic ? .9f : .76f;
                if (phase >= hit && dropPrevious[i] < hit) rippleStarted[i] = time;
                dropPrevious[i] = phase;
                float radius = dp(toxic ? 6 : 4);
                if (toxic) {
                    float x = w*(.17f+i*.33f), maxLength = Math.min(h*.32f,dp(180));
                    float releaseY = dp(16)+maxLength, bulb = radius*1.6f;
                    // The same glob grows on the strand and falls from its exact release position.
                    float amount = Math.min(1,phase/.68f);
                    float globY = dp(16)+amount*amount*maxLength;
                    float fall = Math.max(0,(phase-.68f)/(hit-.68f));
                    float surfaceY = ny > .15f ? (liquidPlane.level-nx*x)/ny : h;
                    surfaceY = Math.max(releaseY+bulb,Math.min(h+bulb,surfaceY));
                    if (phase >= .68f) {
                        float submergedY = surfaceY + bulb*2/Math.max(.25f,ny);
                        globY = releaseY+(submergedY-releaseY)*fall*fall;
                    }
                    if (phase < .74f) {
                        float retract = phase < .68f ? 0 : (phase-.68f)/.06f;
                        float neckEnd = (phase < .68f ? globY : releaseY)*(1-retract);
                        float stem = dp(9)*(1-amount*.62f), neck = Math.max(dp(.4f),stem*(1-amount*.95f));
                        ooze.reset(); ooze.moveTo(x-dp(9),0);
                        ooze.cubicTo(x-stem,neckEnd*.45f,x-neck,neckEnd*.8f,x-neck,neckEnd);
                        ooze.lineTo(x+neck,neckEnd);
                        ooze.cubicTo(x+neck,neckEnd*.8f,x+stem,neckEnd*.45f,x+dp(9),0); ooze.close();
                        paint.setColor(0xbfa4d83a); canvas.drawPath(ooze,paint);
                        paint.setColor(0x44e1ff90); canvas.drawRoundRect(x-stem*.35f,0,x,neckEnd*.86f,dp(3),dp(3),paint);
                    }
                    if (phase < hit) {
                        float size = phase < .68f ? radius*(1+amount*.6f) : bulb;
                        float stretch = 1+.25f*(float)Math.sin(Math.PI*Math.min(1,fall));
                        int save = canvas.save(); canvas.clipOutPath(liquidFill);
                        drawSlimeGlob(canvas,x,globY,size/(float)Math.sqrt(stretch),size*stretch);
                        canvas.restoreToCount(save);
                    }
                    impactX = x; impactY = Math.min(h,surfaceY);
                } else if (phase < hit) {
                    float t = phase/hit, distance = (float)Math.hypot(w,h)+dp(24);
                    float remaining = distance*(1-t*t);
                    float x = impactX-nx*remaining, y = impactY-ny*remaining;
                    Bitmap drop = swipeAsset(R.drawable.fx_water_drop);
                    if (drop != null) {
                        paint.setColor(Color.WHITE); paint.setAlpha(190);
                        canvas.save(); canvas.rotate((float)Math.toDegrees(liquidAngle)-90,x,y);
                        canvas.drawBitmap(drop,null,new RectF(x-radius*2,y-radius*3.5f,x+radius*2,y+radius),paint);
                        canvas.restore(); paint.setAlpha(255);
                    }
                }
                float age = time-rippleStarted[i], life = toxic ? 4 : 1.8f;
                if (rippleStarted[i] > 0 && age >= 0 && age < life) {
                    float radiusRipple = dp(5)+age*dp(toxic ? 7 : 28);
                    int save = canvas.save(); canvas.clipPath(liquidFill);
                    canvas.rotate((float)Math.toDegrees(Math.atan2(by-ay,bx-ax)),impactX,impactY);
                    paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(dp(toxic ? 2 : 1.2f));
                    paint.setColor(Color.argb((int)((toxic ? 95 : 170)*(1-age/life)),toxic ? 198 : 190,239,toxic ? 107 : 255));
                    canvas.drawOval(impactX-radiusRipple,impactY-radiusRipple*.25f,
                            impactX+radiusRipple,impactY+radiusRipple*.25f,paint);
                    paint.setStyle(Paint.Style.FILL); canvas.restoreToCount(save);
                }
            }
            if (toxic) {
                int save = canvas.save(); canvas.clipPath(liquidFill);
                // Sparse, nearly stationary gas pockets form and subside; nothing races or wraps.
                for (int i = 0; i < 4; i++) {
                    float age = loop(time+i*4.7f, 22), life = age/22;
                    float size = dp(3+i%3)*(float)Math.sin(Math.PI*life);
                    float x = w*(.13f+i*.23f), y = h*(.88f-i%2*.055f)-life*dp(8);
                    paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(dp(1.2f)); paint.setColor(0x4497bc51);
                    canvas.drawCircle(x,y,size,paint);
                }
                paint.setStyle(Paint.Style.FILL); canvas.restoreToCount(save);
            }
            paint.setAlpha(255); paint.setShader(null);
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
    /** Clip a constant-volume liquid against screen gravity, without inversion switches. */
    static final class LiquidPlane {
        final float[] polygon = new float[12];
        int points;
        float nx = 0, ny = 1, level, ax, ay, bx, by;
        void update(float width, float height, float x, float y, float fraction) {
            float magnitude = (float)Math.hypot(x, y);
            nx = magnitude < .001f ? 0 : x / magnitude;
            ny = magnitude < .001f ? 1 : y / magnitude;
            float low = Math.min(0, Math.min(nx * width, Math.min(ny * height, nx * width + ny * height)));
            float high = Math.max(0, Math.max(nx * width, Math.max(ny * height, nx * width + ny * height)));
            float desired = width * height * fraction;
            for (int i = 0; i < 24; i++) {
                float middle = (low + high) * .5f;
                clip(width, height, middle);
                if (area() > desired) low = middle; else high = middle;
            }
            level = (low + high) * .5f; clip(width, height, level);
            // The two boundary intersections form the free surface.
            int found = 0;
            for (int i = 0; i < points; i++) {
                float px = polygon[i*2], py = polygon[i*2+1];
                if (Math.abs(nx*px + ny*py - level) < .05f) {
                    if (found == 0) { ax = px; ay = py; found = 1; }
                    else if (Math.hypot(px-ax, py-ay) > .1f) { bx = px; by = py; break; }
                }
            }
        }
        private void clip(float width, float height, float threshold) {
            points = 0;
            for (int i = 0; i < 4; i++) {
                float x = i == 1 || i == 2 ? width : 0, y = i >= 2 ? height : 0;
                int j = (i + 1) % 4;
                float nextX = j == 1 || j == 2 ? width : 0, nextY = j >= 2 ? height : 0;
                float a = nx*x + ny*y - threshold, b = nx*nextX + ny*nextY - threshold;
                if (a >= 0) { polygon[points*2] = x; polygon[points*2+1] = y; points++; }
                if ((a >= 0) != (b >= 0)) {
                    float t = a / (a-b);
                    polygon[points*2] = x + (nextX-x)*t;
                    polygon[points*2+1] = y + (nextY-y)*t; points++;
                }
            }
        }
        float area() {
            float sum = 0;
            for (int i = 0; i < points; i++) {
                int j = (i+1)%points;
                sum += polygon[i*2]*polygon[j*2+1] - polygon[j*2]*polygon[i*2+1];
            }
            return Math.abs(sum)*.5f;
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
        button.setOnClickListener(v -> { playSound(R.raw.bubble_tap, .16f); action.run(); });
        parent.addView(button, new LinearLayout.LayoutParams(-1, dp(55))); return button;
    }
    private void back(String value, Runnable action) {
        TextView back = label("←  " + value, 18, INK, true);
        back.setGravity(Gravity.CENTER_VERTICAL);
        back.setPadding(dp(18), 0, dp(18), 0);
        back.setMinWidth(dp(150)); back.setHeight(dp(56));
        back.setBackground(themeButton(PANEL, 18));
        back.setOnClickListener(v -> { playSound(R.raw.bubble_tap, .16f); action.run(); });
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
        row.setOnClickListener(v -> { playSound(R.raw.bubble_tap, .16f); action.run(); });
    }
}
