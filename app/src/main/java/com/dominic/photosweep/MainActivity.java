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
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.LruCache;
import android.util.Size;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
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
    private static final int CLEANUP_REQUEST = 34;
    private static final long SEVEN_DAYS = 7L * 24 * 60 * 60 * 1000;
    private static final int TRASH_LIMIT = 20;
    private static final int INK = Color.rgb(31, 43, 55);
    private static final int MUTED = Color.rgb(105, 118, 130);
    private static final int BG = Color.rgb(247, 249, 250);
    private static final int GREEN = Color.rgb(37, 142, 111);
    private static final int RED = Color.rgb(209, 85, 97);

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
    private LinearLayout root;
    private int selectedYear = -1;
    private String selectedMonth;
    private boolean reviewing, loading, duplicateScanning;
    private boolean showingTrash, deletingOld;
    private long pendingTrash = -1;
    private long pendingRestore = -1;
    private final ArrayList<Long> pendingCleanup = new ArrayList<>();
    private long lastKept = -1;
    private int generation;
    private float touchX, touchY;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        reviewed = new HashSet<>(getPreferences(MODE_PRIVATE).getStringSet("reviewed", Collections.emptySet()));
        loadTrashEntries();
        scheduleCleanup();
        render();
        if (hasAccess()) loadPhotos();
    }

    @Override protected void onResume() {
        super.onResume();
        if (root != null) {
            loadTrashEntries();
            render();
            if (hasAccess() && canManage()) cleanupTrash();
        }
    }

    @Override public void onDestroy() {
        generation++;
        io.shutdownNow();
        duplicateWorker.shutdownNow();
        super.onDestroy();
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
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(22), dp(22), dp(22), dp(16));
        root.setBackgroundColor(BG);
        setContentView(root);
        if (!hasAccess()) { intro(); return; }
        if (loading) { heading("Photo Sweep", "Gathering your photos…"); return; }
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
        heading("Photo Sweep", "Pick a year to tidy up");
        button(root, "Recently trashed  ·  " + trashEntries.size() + "/20", Color.rgb(231, 241, 238), GREEN, () -> { showingTrash = true; render(); });
        spacer(12);
        if (!canManage()) {
            label("Enable one-time media access to swipe to Trash without repeated prompts.", 14, MUTED, false);
            spacer(8);
            button(root, "Enable prompt-free Trash", INK, Color.WHITE, this::requestMediaManagement);
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
        button(root, "Change access", Color.WHITE, INK, this::requestAccess);
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
        heading("Recently trashed", "Restore within 7 days  •  last 20 photos");
        if (trashEntries.isEmpty()) {
            spacer(36); label("Nothing in your Trash yet.", 21, INK, true);
            spacer(8); label("Swipe left on a photo to place it here.", 15, MUTED, false);
            return;
        }
        ScrollView scroll = new ScrollView(this);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        LinearLayout list = new LinearLayout(this); list.setOrientation(LinearLayout.VERTICAL); scroll.addView(list);
        for (TrashEntry entry : new ArrayList<>(trashEntries)) {
            LinearLayout row = new LinearLayout(this); row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(10), dp(10), dp(10), dp(10)); row.setBackground(rounded(Color.WHITE, 20));
            LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(-1, dp(100)); rowLp.bottomMargin = dp(10); list.addView(row, rowLp);
            ImageView thumbnail = new ImageView(this); thumbnail.setScaleType(ImageView.ScaleType.CENTER_CROP);
            thumbnail.setBackground(rounded(BG, 12)); row.addView(thumbnail, new LinearLayout.LayoutParams(dp(80), dp(80)));
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
            restore.setTextColor(GREEN); restore.setTextSize(13); restore.setBackground(rounded(Color.rgb(231, 241, 238), 12));
            restore.setOnClickListener(v -> restore(entry)); row.addView(restore, new LinearLayout.LayoutParams(dp(90), dp(46)));
        }
        spacer(8);
        label("Older items are permanently deleted after 7 days. When this list reaches 20, the oldest item is deleted to make room.", 13, MUTED, false);
    }

    private void reviewScreen() {
        back("Months", () -> { reviewing = false; render(); });
        List<Photo> month = monthPhotos();
        Photo current = null; int remaining = 0;
        for (Photo p : month) if (!reviewed.contains(Long.toString(p.id))) { remaining++; if (current == null) current = p; }
        String monthName = new SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(new Date(selectedYear - 1900, Integer.parseInt(selectedMonth.substring(5)) - 1, 1));
        heading(monthName, remaining + " of " + month.size() + " left to review");
        View track = new View(this); track.setBackground(rounded(Color.rgb(225, 232, 232), 4));
        root.addView(track, new LinearLayout.LayoutParams(-1, dp(5)));
        View fill = new View(this); fill.setBackground(rounded(GREEN, 4));
        FrameLayout progress = new FrameLayout(this);
        root.removeView(track); progress.addView(track, new FrameLayout.LayoutParams(-1, dp(5)));
        FrameLayout.LayoutParams progressFill = new FrameLayout.LayoutParams(-1, dp(5));
        progress.addView(fill, progressFill);
        LinearLayout.LayoutParams progressLayout = new LinearLayout.LayoutParams(-1, dp(5));
        progressLayout.bottomMargin = dp(17); root.addView(progress, progressLayout);
        progress.post(() -> { FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) fill.getLayoutParams();
            lp.width = (int) (progress.getWidth() * (month.size() == 0 ? 0 : (month.size() - remaining) / (float) month.size()));
            fill.setLayoutParams(lp); });
        if (current == null) {
            spacer(65); label("All caught up ✨", 28, INK, true); spacer(12);
            label("This month is clear. Kept photos are still in your gallery.", 16, MUTED, false);
            if (lastKept != -1) {
                spacer(20);
                button(root, "Undo last keep", Color.WHITE, GREEN, () -> {
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
        card.setBackground(rounded(Color.WHITE, 25)); card.setElevation(dp(8));
        stage.addView(card, new FrameLayout.LayoutParams(-1, -1));
        ImageView photo = new ImageView(this); photo.setScaleType(ImageView.ScaleType.FIT_CENTER);
        card.addView(photo, new FrameLayout.LayoutParams(-1, -1));
        loadPreview(shown, photo);
        SwipeEffect effect = new SwipeEffect();
        card.addView(effect, new FrameLayout.LayoutParams(-1, -1));
        if (duplicates.contains(shown.id)) {
            TextView bubble = pill("✦ Duplicate", Color.rgb(255, 236, 183), Color.rgb(105, 75, 21));
            FrameLayout.LayoutParams badge = new FrameLayout.LayoutParams(-2, -2, Gravity.TOP | Gravity.END);
            badge.setMargins(dp(12), dp(12), dp(12), 0); card.addView(bubble, badge);
        }
        TextView date = pill(DateFormat.getDateInstance(DateFormat.MEDIUM).format(new Date(shown.timestamp)), Color.WHITE, INK);
        FrameLayout.LayoutParams dateParams = new FrameLayout.LayoutParams(-2, -2, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        dateParams.bottomMargin = dp(12); card.addView(date, dateParams);
        card.setOnTouchListener((v, e) -> {
            if (e.getAction() == MotionEvent.ACTION_DOWN) { touchX = e.getRawX(); touchY = e.getRawY(); return true; }
            if (e.getAction() == MotionEvent.ACTION_MOVE) {
                float dx = e.getRawX() - touchX;
                card.setTranslationX(dx); card.setRotation(Math.max(-13, Math.min(13, dx / dp(28))));
                effect.progress = Math.max(-1f, Math.min(1f, dx / (card.getWidth() * .62f)));
                effect.invalidate();
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
                card.animate().translationX(0).rotation(0).setDuration(200).start(); return true;
            }
            return true;
        });
        LinearLayout actions = new LinearLayout(this); actions.setOrientation(LinearLayout.HORIZONTAL);
        root.addView(actions);
        Button trash = button(actions, "← Trash", Color.WHITE, RED, () -> { if (canManage()) trash(shown); else requestMediaManagement(); });
        Button keep = button(actions, "Keep →", GREEN, Color.WHITE, () -> keep(shown));
        LinearLayout.LayoutParams half = new LinearLayout.LayoutParams(0, dp(55), 1);
        half.rightMargin = dp(7); trash.setLayoutParams(half);
        LinearLayout.LayoutParams other = new LinearLayout.LayoutParams(0, dp(55), 1);
        other.leftMargin = dp(7); keep.setLayoutParams(other);
        spacer(14);
        label("Swipe left to Trash   •   right to Keep", 13, MUTED, false);
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
            if (progress > 0) {
                float glowWidth = w * (.1f + .75f * amount);
                paint.setShader(new LinearGradient(w - glowWidth, 0, w, 0,
                        new int[]{Color.TRANSPARENT, Color.argb((int)(125 * amount), 255,255,255), Color.argb((int)(210 * amount), 255,255,255)},
                        null, Shader.TileMode.CLAMP));
                canvas.drawRect(w - glowWidth, 0, w, h, paint); paint.setShader(null);
                paint.setColor(Color.argb((int)(230 * amount), 255, 255, 255));
                canvas.drawRect(w - dp(4), 0, w, h, paint);
            } else if (progress < 0) {
                float edge = w * (1 - amount * .72f);
                paint.setColor(Color.argb((int)(175 * amount), 247, 249, 250));
                canvas.drawRect(edge, 0, w, h, paint);
                for (int i = 0; i < 27; i++) {
                    float x = edge + (w - edge) * ((i * 37 % 29) / 29f);
                    float y = h * ((i * 13 % 31) / 31f);
                    float size = dp(5 + (i * 7 % 12)) * amount;
                    paint.setColor(Color.argb((int)(180 * amount), 247, 249, 250));
                    Path chip = new Path(); chip.moveTo(x, y); chip.lineTo(x + size, y - size / 2);
                    chip.lineTo(x + size * 1.4f, y + size); chip.close(); canvas.drawPath(chip, paint);
                }
            }
        }
    }

    private void keep(Photo p) {
        if (pendingTrash != -1) return;
        lastKept = p.id;
        reviewed.add(Long.toString(p.id)); saveReviewed(); render();
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
        ArrayList<Uri> expired = new ArrayList<>(); pendingCleanup.clear();
        for (int i = 0; i < trashEntries.size(); i++) {
            TrashEntry entry = trashEntries.get(i);
            if (now - entry.trashedAt >= SEVEN_DAYS || i >= TRASH_LIMIT) {
                expired.add(entry.uri); pendingCleanup.add(entry.id);
            }
        }
        if (expired.isEmpty()) return;
        try {
            deletingOld = true;
            PendingIntent request = MediaStore.createDeleteRequest(getContentResolver(), expired);
            startIntentSenderForResult(request.getIntentSender(), CLEANUP_REQUEST, null, 0, 0, 0);
        } catch (Exception e) {
            deletingOld = false; pendingCleanup.clear();
        }
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == TRASH_REQUEST) {
            long id = pendingTrash; pendingTrash = -1;
            if (resultCode == RESULT_OK) {
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
            }
            render();
            if (resultCode == RESULT_OK) cleanupTrash();
        } else if (requestCode == RESTORE_REQUEST) {
            long id = pendingRestore; pendingRestore = -1;
            if (resultCode == RESULT_OK) {
                trashEntries.removeIf(e -> e.id == id); saveTrashEntries();
                loadPhotos();
            }
            render();
        } else if (requestCode == CLEANUP_REQUEST) {
            deletingOld = false;
            if (resultCode == RESULT_OK) {
                trashEntries.removeIf(e -> pendingCleanup.contains(e.id));
                for (Long id : pendingCleanup) previews.remove(id);
                saveTrashEntries();
            }
            pendingCleanup.clear(); render();
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
                                    MediaStore.createDeleteRequest(getContentResolver(), Collections.singletonList(uri)).send();
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

    private void saveReviewed() { getPreferences(MODE_PRIVATE).edit().putStringSet("reviewed", new HashSet<>(reviewed)).apply(); }
    private int dp(float n) { return (int) (n * getResources().getDisplayMetrics().density + 0.5f); }
    private GradientDrawable rounded(int color, int radius) {
        GradientDrawable bg = new GradientDrawable(); bg.setColor(color); bg.setCornerRadius(dp(radius)); return bg;
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
    private Button button(LinearLayout parent, String value, int bg, int color, Runnable action) {
        Button button = new Button(this); button.setText(value); button.setAllCaps(false); button.setTextSize(16);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD); button.setTextColor(color); button.setBackground(rounded(bg, 16));
        button.setOnClickListener(v -> action.run());
        parent.addView(button, new LinearLayout.LayoutParams(-1, dp(55))); return button;
    }
    private void back(String value, Runnable action) {
        TextView back = label("‹  " + value, 16, GREEN, true);
        back.setPadding(0, 0, 0, dp(18)); back.setOnClickListener(v -> action.run());
    }
    private void tile(LinearLayout parent, String title, String detail, Runnable action) {
        LinearLayout row = new LinearLayout(this); row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(dp(19), dp(17), dp(19), dp(17)); row.setBackground(rounded(Color.WHITE, 18));
        row.setElevation(dp(2));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2); lp.bottomMargin = dp(12); parent.addView(row, lp);
        TextView name = new TextView(this); name.setText(title + "    ›"); name.setTextSize(22); name.setTypeface(Typeface.DEFAULT, Typeface.BOLD); name.setTextColor(INK); row.addView(name);
        TextView sub = new TextView(this); sub.setText(detail); sub.setTextSize(14); sub.setTextColor(MUTED);
        LinearLayout.LayoutParams subLp = new LinearLayout.LayoutParams(-1, -2); subLp.topMargin = dp(5); row.addView(sub, subLp);
        row.setOnClickListener(v -> action.run());
    }
}
