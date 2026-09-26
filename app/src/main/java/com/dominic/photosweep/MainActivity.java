package com.dominic.photosweep;

import android.Manifest;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.ContentUris;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
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

    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final ExecutorService duplicateWorker = Executors.newSingleThreadExecutor();
    private final LruCache<Long, Bitmap> previews = new LruCache<Long, Bitmap>(24) {
        @Override protected int sizeOf(Long key, Bitmap value) { return Math.max(1, value.getByteCount() / (1024 * 1024)); }
    };
    private final ArrayList<Photo> photos = new ArrayList<>();
    private final HashSet<Long> duplicates = new HashSet<>();
    private Set<String> reviewed = new HashSet<>();
    private LinearLayout root;
    private int selectedYear = -1;
    private String selectedMonth;
    private boolean reviewing, loading, duplicateScanning;
    private long pendingTrash = -1;
    private long lastKept = -1;
    private int generation;
    private float touchX, touchY;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        reviewed = new HashSet<>(getPreferences(MODE_PRIVATE).getStringSet("reviewed", Collections.emptySet()));
        render();
        if (hasAccess()) loadPhotos();
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
        label("Your photos stay on your phone. Android asks before anything moves to Trash.", 13, MUTED, false);
    }

    private void heading(String title, String subtitle) {
        label(title, 31, INK, true);
        spacer(5);
        label(subtitle, 15, MUTED, false);
        spacer(24);
    }

    private void yearsScreen() {
        heading("Photo Sweep", "Pick a year to tidy up");
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

    private void reviewScreen() {
        back("Months", () -> { reviewing = false; render(); });
        List<Photo> month = monthPhotos();
        Photo current = null; int remaining = 0;
        for (Photo p : month) if (!reviewed.contains(Long.toString(p.id))) { remaining++; if (current == null) current = p; }
        String monthName = new SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(new Date(selectedYear - 1900, Integer.parseInt(selectedMonth.substring(5)) - 1, 1));
        heading(monthName, remaining + " of " + month.size() + " left to review");
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
        FrameLayout card = new FrameLayout(this);
        card.setBackground(rounded(Color.WHITE, 22)); card.setElevation(dp(5));
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(-1, 0, 1);
        cardParams.bottomMargin = dp(20); root.addView(card, cardParams);
        ImageView photo = new ImageView(this); photo.setScaleType(ImageView.ScaleType.FIT_CENTER);
        card.addView(photo, new FrameLayout.LayoutParams(-1, -1));
        loadPreview(shown, photo);
        if (duplicates.contains(shown.id)) {
            TextView bubble = pill("✦ Duplicate", Color.rgb(255, 236, 183), Color.rgb(105, 75, 21));
            FrameLayout.LayoutParams badge = new FrameLayout.LayoutParams(-2, -2, Gravity.TOP | Gravity.END);
            badge.setMargins(dp(12), dp(12), dp(12), 0); card.addView(bubble, badge);
        }
        TextView date = pill(DateFormat.getDateInstance(DateFormat.MEDIUM).format(new Date(shown.timestamp)), Color.WHITE, INK);
        FrameLayout.LayoutParams dateParams = new FrameLayout.LayoutParams(-2, -2, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        dateParams.bottomMargin = dp(12); card.addView(date, dateParams);
        card.setOnTouchListener((v, e) -> {
            if (e.getAction() == MotionEvent.ACTION_DOWN) { touchX = e.getX(); touchY = e.getY(); return true; }
            if (e.getAction() == MotionEvent.ACTION_UP) {
                float dx = e.getX() - touchX;
                if (Math.abs(dx) > dp(70) && Math.abs(dx) > Math.abs(e.getY() - touchY) * 1.2f) {
                    if (dx > 0) keep(shown); else trash(shown);
                }
                return true;
            }
            return true;
        });
        LinearLayout actions = new LinearLayout(this); actions.setOrientation(LinearLayout.HORIZONTAL);
        root.addView(actions);
        Button trash = button(actions, "← Trash", Color.WHITE, RED, () -> trash(shown));
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
        Bitmap cached = previews.get(p.id);
        if (cached != null) { view.setImageBitmap(cached); return; }
        io.execute(() -> {
            try {
                Bitmap bitmap = getContentResolver().loadThumbnail(p.uri, new Size(1200, 1200), null);
                if (bitmap != null) {
                    previews.put(p.id, bitmap);
                    runOnUiThread(() -> { if (!isDestroyed() && view.getParent() != null) view.setImageBitmap(bitmap); });
                }
            } catch (Exception ignored) { }
        });
    }

    private void keep(Photo p) {
        if (pendingTrash != -1) return;
        lastKept = p.id;
        reviewed.add(Long.toString(p.id)); saveReviewed(); render();
    }

    private void trash(Photo p) {
        if (pendingTrash != -1) return;
        try {
            PendingIntent request = MediaStore.createTrashRequest(getContentResolver(), Collections.singletonList(p.uri), true);
            pendingTrash = p.id;
            startIntentSenderForResult(request.getIntentSender(), TRASH_REQUEST, null, 0, 0, 0);
        } catch (Exception e) {
            pendingTrash = -1;
            android.widget.Toast.makeText(this, "Could not open Trash confirmation", android.widget.Toast.LENGTH_SHORT).show();
        }
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == TRASH_REQUEST) {
            long id = pendingTrash; pendingTrash = -1;
            if (resultCode == RESULT_OK) {
                if (lastKept == id) lastKept = -1;
                photos.removeIf(p -> p.id == id);
                reviewed.remove(Long.toString(id)); saveReviewed();
                duplicates.remove(id); previews.remove(id);
            }
            render();
        }
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
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2); lp.bottomMargin = dp(12); parent.addView(row, lp);
        TextView name = new TextView(this); name.setText(title + "    ›"); name.setTextSize(22); name.setTypeface(Typeface.DEFAULT, Typeface.BOLD); name.setTextColor(INK); row.addView(name);
        TextView sub = new TextView(this); sub.setText(detail); sub.setTextSize(14); sub.setTextColor(MUTED);
        LinearLayout.LayoutParams subLp = new LinearLayout.LayoutParams(-1, -2); subLp.topMargin = dp(5); row.addView(sub, subLp);
        row.setOnClickListener(v -> action.run());
    }
}
