package com.dominic.photosweep;

import static org.junit.Assert.*;

import android.app.Instrumentation;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;
import android.net.Uri;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import androidx.lifecycle.Lifecycle;
import androidx.test.core.app.ActivityScenario;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.By;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject2;
import org.junit.Test;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

/** Ten isolated, real Activity sessions using synthetic photos on an emulator only. */
public class TenSessionSmokeTest {
    private final Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
    private final Context context = instrumentation.getTargetContext();
    private final SharedPreferences preferences = context.getSharedPreferences("MainActivity", Context.MODE_PRIVATE);
    private ActivityScenario<MainActivity> scenario;
    private static final int[] THEMES = {0, 18, 19, 20, 21, 22, 23, 24, 50, 61};

    @Test public void session01_midnight() throws Exception { runSession(0); }
    @Test public void session02_candy() throws Exception { runSession(1); }
    @Test public void session03_toxic() throws Exception { runSession(2); }
    @Test public void session04_space() throws Exception { runSession(3); }
    @Test public void session05_fire() throws Exception { runSession(4); }
    @Test public void session06_water() throws Exception { runSession(5); }
    @Test public void session07_ice() throws Exception { runSession(6); }
    @Test public void session08_earth() throws Exception { runSession(7); }
    @Test public void session09_country() throws Exception { runSession(8); }
    @Test public void session10_yinYang() throws Exception { runSession(9); }

    private void runSession(int index) throws Exception {
        androidx.test.uiautomator.Configurator.getInstance().setWaitForIdleTimeout(0);
        List<Uri> samples = new ArrayList<>();
        shell("pm grant " + context.getPackageName() + " android.permission.READ_MEDIA_IMAGES");
        preferences.edit().clear().putInt("theme", THEMES[index]).putBoolean("admin_mode", true)
                .putBoolean("swipe_hint_seen", true).commit();
        context.getSharedPreferences("radio", Context.MODE_PRIVATE).edit().clear().commit();
        try {
            samples.add(seedPhoto(index, 0)); samples.add(seedPhoto(index, 1));
            java.time.YearMonth fixtureMonth = null;
            for (Uri uri : samples) try (android.database.Cursor c = context.getContentResolver().query(uri,
                    new String[]{MediaStore.Images.Media.DATE_TAKEN, MediaStore.Images.Media.DATE_ADDED}, null, null, null)) {
                assertNotNull(c); assertTrue(c.moveToFirst());
                long indexedDate = c.getLong(0) > 0 ? c.getLong(0) : c.getLong(1) * 1000L;
                java.time.YearMonth indexedMonth = java.time.YearMonth.from(java.time.Instant.ofEpochMilli(indexedDate).atZone(java.time.ZoneId.systemDefault()));
                if (fixtureMonth == null) fixtureMonth = indexedMonth; else assertEquals("Fixtures share a month", fixtureMonth, indexedMonth);
            }
            scenario = ActivityScenario.launch(MainActivity.class);
            awaitText("Your photos");
            assertLabelFits("Themes"); assertLabelFits("Settings"); assertLabelFits("Trash");
            clickText("Themes"); awaitText("Choose your look and swipe animations");
            clickText("Preview Keep →"); back(); awaitText("Your photos");
            clickText("Trash"); awaitText("Trash is empty"); back(); awaitText("Your photos");
            clickText("Settings"); awaitText("Choose what you want to adjust");
            clickText("Audio"); awaitDescription("Master volume");
            adjustVolume("Master volume", .35f); adjustVolume("Music volume", .22f);
            adjustVolume("VFX sound volume", .65f);
            int master = preferences.getInt("master_volume", -1);
            int music = preferences.getInt("music_volume", -1);
            int vfx = preferences.getInt("vfx_volume", -1);
            assertTrue("Independent audio preferences", master > 25 && master < 45 && music > 12 && music < 32 && vfx > 55 && vfx < 75);
            clickText("D.G.S. Radio");
            scenario.moveToState(Lifecycle.State.CREATED); scenario.moveToState(Lifecycle.State.RESUMED);
            scenario.recreate(); awaitDescription("Master volume");
            assertSlider("Master volume", master); assertSlider("Music volume", music); assertSlider("VFX sound volume", vfx);
            adjustVolume("Master volume", 0); assertEquals(0, preferences.getInt("master_volume", -1));
            adjustVolume("Master volume", 1); assertEquals(100, preferences.getInt("master_volume", -1));
            back(); back(); awaitText("Your photos");
            clickText(Integer.toString(fixtureMonth.getYear())); awaitText("Choose a month");
            clickText(fixtureMonth.format(java.time.format.DateTimeFormatter.ofPattern("MMMM", java.util.Locale.getDefault())));
            awaitText("2 of 2 left to review");
            dragPhoto(.08f, false); awaitText("2 of 2 left to review");
            dragPhoto(.40f, false); awaitText("1 of 2 left to review");
            clickText("Undo last photo"); awaitText("2 of 2 left to review");
            clickDescription("Full-screen photo review with swiping");
            awaitDescription("Whole photo. Swipe right to Keep or left to Trash");
            scenario.recreate(); awaitDescription("Whole photo. Swipe right to Keep or left to Trash");
            dragPhoto(.40f, true); awaitText("1/2 left");
            clickText("Undo last photo"); awaitText("2/2 left");
            back(); awaitDescription("Full-screen photo review with swiping");
            dragPhoto(-.40f, false); acceptSystemPhotoPrompt(); awaitText("1 of 1 left to review");
            clickText("Undo last photo"); acceptSystemPhotoPrompt(); awaitText("2 of 2 left to review");
            back(); back(); awaitText("Your photos");
            assertEquals("Theme survives the session", THEMES[index], preferences.getInt("theme", -1));
        } finally {
            if (scenario != null) {
                java.io.File evidence = new java.io.File(context.getFilesDir(), "smoke-evidence");
                evidence.mkdirs();
                Bitmap screen = instrumentation.getUiAutomation().takeScreenshot();
                if (screen != null) {
                    try (OutputStream out = new java.io.FileOutputStream(new java.io.File(evidence, "session-" + (index+1) + ".png"))) {
                        screen.compress(Bitmap.CompressFormat.PNG, 100, out);
                    } finally { screen.recycle(); }
                }
                UiDevice.getInstance(instrumentation).dumpWindowHierarchy(new java.io.File(evidence, "session-" + (index+1) + ".xml"));
                // Copy evidence out before Gradle uninstalls the app. Keep it
                // outside shared media so later sessions never import it.
                shell("mkdir -p /data/local/tmp/PhotoSweepSmoke");
                for (String extension : new String[]{"png", "xml"}) {
                    String name = "session-" + (index+1) + "." + extension;
                    exportEvidence(new java.io.File(evidence, name), name);
                }
                scenario.close(); scenario = null;
            }
            android.os.Bundle cleanup = new android.os.Bundle();
            cleanup.putInt(MediaStore.QUERY_ARG_MATCH_TRASHED, MediaStore.MATCH_INCLUDE);
            for (Uri uri : samples) context.getContentResolver().delete(uri, cleanup);
        }
    }

    private void exportEvidence(java.io.File file, String name) throws Exception {
        android.os.ParcelFileDescriptor[] pipes = instrumentation.getUiAutomation()
                .executeShellCommandRw("dd of=/data/local/tmp/PhotoSweepSmoke/" + name);
        try (java.io.InputStream source = new java.io.FileInputStream(file);
             OutputStream target = new android.os.ParcelFileDescriptor.AutoCloseOutputStream(pipes[1])) {
            source.transferTo(target);
        }
        try (java.io.InputStream response = new android.os.ParcelFileDescriptor.AutoCloseInputStream(pipes[0])) {
            response.readAllBytes();
        }
    }

    private Uri seedPhoto(int session, int number) throws Exception {
        Calendar date = Calendar.getInstance(); date.clear(); date.set(2026, Calendar.JANUARY, 15, 12 + number, 0);
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME, "PhotoSweep-test-" + session + "-" + number + ".jpg");
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        values.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/PhotoSweepTests");
        values.put(MediaStore.Images.Media.DATE_TAKEN, date.getTimeInMillis());
        values.put(MediaStore.Images.Media.IS_PENDING, 1);
        Uri uri = context.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        assertNotNull(uri);
        Bitmap bitmap = Bitmap.createBitmap(number == 0 ? 480 : 360, number == 0 ? 360 : 480, Bitmap.Config.ARGB_8888);
        bitmap.eraseColor(Color.rgb(25 + session * 20, 100 + number * 70, 180));
        try (OutputStream stream = context.getContentResolver().openOutputStream(uri)) {
            assertTrue(bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream));
        } finally { bitmap.recycle(); }
        // Use the indexed capture/import date when navigating: Android may clear
        // DATE_TAKEN while scanning a generated image without camera metadata.
        values.clear(); values.put(MediaStore.Images.Media.IS_PENDING, 0);
        context.getContentResolver().update(uri, values, null, null); return uri;
    }

    private void shell(String command) throws Exception {
        try (android.os.ParcelFileDescriptor result = instrumentation.getUiAutomation().executeShellCommand(command);
             java.io.InputStream stream = new android.os.ParcelFileDescriptor.AutoCloseInputStream(result)) {
            stream.readAllBytes();
        }
    }

    private View find(View view, Predicate<View> match) {
        if (view.getVisibility() != View.VISIBLE) return null;
        if (match.test(view)) return view;
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = group.getChildCount() - 1; i >= 0; i--) {
                View found = find(group.getChildAt(i), match); if (found != null) return found;
            }
        }
        return null;
    }
    private Predicate<View> text(String value) {
        return v -> v instanceof TextView && ((TextView)v).getText().toString().equals(value);
    }
    private Predicate<View> description(String value) {
        return v -> value.contentEquals(v.getContentDescription() == null ? "" : v.getContentDescription());
    }
    private void await(Predicate<View> match, String name) {
        long end = SystemClock.uptimeMillis() + 10000;
        AtomicBoolean found = new AtomicBoolean();
        do {
            scenario.onActivity(a -> {
                View v = find(a.getWindow().getDecorView(), match);
                found.set(v != null && v.getWidth() > 0);
            });
            if (found.get()) return;
            SystemClock.sleep(100);
        } while (SystemClock.uptimeMillis() < end);
        AtomicReference<String> visible = new AtomicReference<>();
        scenario.onActivity(a -> {
            List<String> labels = new ArrayList<>(); collectText(a.getWindow().getDecorView(), labels);
            visible.set(labels.toString());
        });
        fail("Missing UI: " + name + "; available: " + visible.get());
    }
    private void collectText(View v, List<String> labels) {
        if (v.getVisibility() != View.VISIBLE) return;
        if (v instanceof TextView) labels.add(((TextView)v).getText().toString() + " [" + v.getWidth() + "x" + v.getHeight() + "]");
        if (v instanceof ViewGroup) for (int i=0; i<((ViewGroup)v).getChildCount(); i++) collectText(((ViewGroup)v).getChildAt(i), labels);
    }
    private void awaitText(String value) { await(text(value), value); }
    private void awaitDescription(String value) { await(description(value), value); }
    private void click(Predicate<View> match, String name) {
        await(match, name);
        scenario.onActivity(a -> {
            View v = find(a.getWindow().getDecorView(), match); assertNotNull(name, v);
            while (!v.isClickable() && v.getParent() instanceof View) v = (View)v.getParent();
            assertTrue("Clickable: " + name, v.performClick());
        });
        instrumentation.waitForIdleSync(); SystemClock.sleep(200);
    }
    private void clickText(String value) { click(text(value), value); }
    private void clickDescription(String value) { click(description(value), value); }
    private void back() { scenario.onActivity(a -> a.getOnBackPressedDispatcher().onBackPressed()); instrumentation.waitForIdleSync(); SystemClock.sleep(200); }
    private void assertLabelFits(String value) {
        scenario.onActivity(a -> {
            TextView label = (TextView)find(a.getWindow().getDecorView(), text(value)); assertNotNull(label);
            assertTrue("Readable " + value, label.getWidth() > 0 && label.getPaint().measureText(value) <= label.getWidth());
            assertTrue("48dp tap target " + value, ((View)label.getParent()).getHeight() >= 48 * a.getResources().getDisplayMetrics().density);
        });
    }
    private void adjustVolume(String name, float fraction) {
        scenario.onActivity(a -> {
            SeekBar slider = (SeekBar)find(a.getWindow().getDecorView(), description(name)); assertNotNull(name, slider);
            float x = slider.getPaddingLeft() + fraction * (slider.getWidth() - slider.getPaddingLeft() - slider.getPaddingRight());
            long now = SystemClock.uptimeMillis();
            MotionEvent down = MotionEvent.obtain(now, now, MotionEvent.ACTION_DOWN, x, slider.getHeight()/2f, 0);
            MotionEvent up = MotionEvent.obtain(now, now+80, MotionEvent.ACTION_UP, x, slider.getHeight()/2f, 0);
            slider.dispatchTouchEvent(down); slider.dispatchTouchEvent(up); down.recycle(); up.recycle();
        });
    }
    private void assertSlider(String name, int value) {
        scenario.onActivity(a -> assertEquals(name, value, ((SeekBar)find(a.getWindow().getDecorView(), description(name))).getProgress()));
    }
    private void dragPhoto(float fraction, boolean full) {
        long focusDeadline = SystemClock.uptimeMillis() + 10000;
        AtomicBoolean focused = new AtomicBoolean();
        do {
            scenario.onActivity(a -> focused.set(a.hasWindowFocus()));
            if (focused.get()) break;
            SystemClock.sleep(100);
        } while (SystemClock.uptimeMillis() < focusDeadline);
        assertTrue("Photo window has focus", focused.get());
        AtomicReference<Rect> position = new AtomicReference<>();
        scenario.onActivity(a -> {
            View photo = find(a.getWindow().getDecorView(), description(full ? "Whole photo. Swipe right to Keep or left to Trash" : "Photo. Tap for full-screen review, swipe left to Trash or right to Keep"));
            assertNotNull("Swipe target", photo); Rect r = new Rect(); assertTrue(photo.getGlobalVisibleRect(r)); position.set(r);
        });
        Rect r = position.get(); float x = r.centerX(), y = r.centerY(), dx = r.width() * fraction;
        long down = SystemClock.uptimeMillis();
        send(down, MotionEvent.ACTION_DOWN, x, y);
        for (int step=1; step<=12; step++) { SystemClock.sleep(25); send(down, MotionEvent.ACTION_MOVE, x+dx*step/12, y); }
        send(down, MotionEvent.ACTION_UP, x+dx, y); SystemClock.sleep(900);
    }
    private void send(long down, int action, float x, float y) {
        MotionEvent e = MotionEvent.obtain(down, SystemClock.uptimeMillis(), action, x, y, 0);
        e.setSource(android.view.InputDevice.SOURCE_TOUCHSCREEN);
        try { assertTrue("Swipe input delivered", instrumentation.getUiAutomation().injectInputEvent(e, true)); }
        finally { e.recycle(); }
    }
    private void acceptSystemPhotoPrompt() {
        UiDevice device = UiDevice.getInstance(instrumentation);
        long end = SystemClock.uptimeMillis()+2500;
        do {
            UiObject2 positive = device.findObject(By.res("android:id/button1"));
            if (positive != null) { positive.click(); return; }
            SystemClock.sleep(100);
        } while (SystemClock.uptimeMillis()<end);
        // Owned synthetic media can be trashed without a system confirmation.
    }
}
