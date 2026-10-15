package com.dominic.photosweep;

/** Device-local, earned-only rewards. No purchases, timers or expiring balances. */
final class ThemeProgression {
    static final int XP_PER_LEVEL = 200;
    static int level(int xp) { return Math.max(0, xp) / XP_PER_LEVEL + 1; }
    static int reviewXp(int uniqueReviews) { return 5 + (uniqueReviews > 0 && uniqueReviews % 10 == 0 ? 10 : 0); }
    static int migratedXp(int oldXp) { return (int)Math.min(Integer.MAX_VALUE - 2000L, Math.max(0L, oldXp) * 200L / 500L); }
    static int addLevels(int xp, int count) { return (int)Math.min(Integer.MAX_VALUE - 2000L, (long)Math.max(0,xp) + Math.max(0,count)*200L); }
    // 0 Silver, 1 Gold, 2 Diamond. Nine misses make the next roll a Diamond.
    static int roll(int percentile, int misses) {
        if (percentile < 0 || percentile >= 100) throw new IllegalArgumentException("percentile");
        return misses >= 9 || percentile >= 95 ? 2 : percentile >= 70 ? 1 : 0;
    }
}
