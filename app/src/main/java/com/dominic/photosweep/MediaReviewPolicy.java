package com.dominic.photosweep;

/** Stable review identities: existing photo keys stay unchanged across upgrades. */
final class MediaReviewPolicy {
    private static final long VIDEO = 1L << 62;
    private MediaReviewPolicy() { }
    static long key(long providerId, boolean video) {
        if (providerId < 0 || providerId >= VIDEO) throw new IllegalArgumentException("Media ID out of range");
        return video ? providerId | VIDEO : providerId;
    }
    static boolean isVideo(long key) { return key >= 0 && (key & VIDEO) != 0; }
    static long providerId(long key) { return isVideo(key) ? key & ~VIDEO : key; }
    static String count(int count, boolean video) {
        return count + (video ? (count == 1 ? " video" : " videos") : (count == 1 ? " photo" : " photos"));
    }
    static String duration(long milliseconds) {
        long seconds = Math.max(0, milliseconds) / 1000;
        return seconds >= 3600
                ? String.format(java.util.Locale.ROOT, "%d:%02d:%02d", seconds / 3600, seconds / 60 % 60, seconds % 60)
                : String.format(java.util.Locale.ROOT, "%d:%02d", seconds / 60, seconds % 60);
    }
}
