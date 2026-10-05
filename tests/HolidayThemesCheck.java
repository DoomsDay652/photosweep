package com.dominic.photosweep;

/** Guards saved selections, collection bounds, and per-holiday interaction profiles. */
public final class HolidayThemesCheck {
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
    public static void main(String[] args) {
        check(HolidayThemes.collection(61) == -1, "existing final theme stays outside holiday collection");
        check(SwipeTheme.forTheme(61) == SwipeTheme.Kind.YIN_YANG, "existing saved Yin Yang ID unchanged");
        check(SwipeTheme.forTheme(50) == SwipeTheme.Kind.USA, "existing country animation ID unchanged");
        check(HolidayThemes.collection(-1) == -1 && HolidayThemes.collection(HolidayThemes.end()) == -1,
                "reject invalid IDs at both ends");
        java.util.Set<SwipeTheme.Kind> profiles = new java.util.HashSet<>();
        for (int h = 0; h < HolidayThemes.NAMES.length; h++) {
            int still = HolidayThemes.still(h), animated = HolidayThemes.animated(h);
            check(HolidayThemes.collection(still) == h && HolidayThemes.collection(animated) == h,
                    "both saved variants resolve to one holiday card");
            check(!HolidayThemes.animatedTheme(still) && HolidayThemes.animatedTheme(animated),
                    "still selection never starts background motion");
            check(PlayPolicy.themeAllowed(still) && PlayPolicy.themeAllowed(animated), "both holiday choices selectable");
            check(SwipeTheme.forTheme(still) == SwipeTheme.forTheme(animated), "matching motif for both choices");
            SwipeTheme.Kind profile = SwipeTheme.forTheme(still);
            check(profile != SwipeTheme.Kind.SPARKLE && profiles.add(profile), "distinct holiday swipe profile");
            check(!SwipeTheme.description(profile).contains("sparkle burst"), "holiday-specific interaction copy");
        }
        check(!PlayPolicy.themeAllowed(HolidayThemes.end()), "no unavailable resource may be selected");
        check(HolidayThemes.reflective(7) && HolidayThemes.reflective(11), "remembrance holidays stay gentle");
        check(!HolidayThemes.reflective(3), "July 4 celebration profile");
        System.out.println("PASS: 12 collections, 24 saved IDs, original selections, selectable variants, distinct profiles, still-motion and bounds checks.");
    }
}
