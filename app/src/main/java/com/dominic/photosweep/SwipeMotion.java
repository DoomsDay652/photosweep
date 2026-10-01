package com.dominic.photosweep;

/** Pure timing/trajectory math shared by the swipe renderer and its regression checks. */
final class SwipeMotion {
    static float clamp(float value) { return Math.max(0f, Math.min(1f, value)); }
    static float ease(float value) { float t = clamp(value); return t * t * (3f - 2f * t); }
    static int duration(int speed) { return Math.max(220, Math.min(620, 36000 / Math.max(50, speed))); }
    static float travel(float phase, boolean keep) { return (keep ? 1f : -1f) * ease(phase); }
    static boolean shouldCommit(float dx, float dy, float threshold) { return Math.abs(dx)>threshold && Math.abs(dx)>Math.abs(dy)*1.2f; }
    static float falling(float seconds) { return 32f * seconds * seconds; }
}
