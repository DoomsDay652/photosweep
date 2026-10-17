package com.dominic.photosweep;

/** Stable theme IDs keep existing ownership intact while the library groups by subject. */
final class ThemeCatalog {
    static final int SLASHER=86, SUMMER=87, COUNT=88;
    static final String[] COLLECTIONS = {"All themes", "Colors & basics", "Nature & places", "Elements", "Countries", "Holidays & seasons", "Fantasy & space"};
    static int collection(int theme) {
        if (theme < 0 || theme >= COUNT) throw new IllegalArgumentException("theme");
        if (theme < 3 || (theme >= 13 && theme <= 17)) return 1;
        if (theme >= 62) return 5;
        if (theme == 28 || theme == 29 || theme == 32 || theme == 33 || (theme >= 44 && theme <= 55)) return 4;
        if (theme == 26 || theme == 27 || theme == 30 || theme == 31 || theme == 61) return 2;
        if (theme == 19 || (theme >= 21 && theme <= 25)) return 3;
        return 6;
    }
    static int cost(int tier) {
        if (tier < 1 || tier > 3) throw new IllegalArgumentException("tier");
        return tier == 1 ? 0 : tier == 2 ? 1 : 3;
    }
    static String price(int tier) { return tier == 1 ? "Free" : tier == 2 ? "1 Gold" : "3 Diamonds"; }
    static boolean visible(boolean owned, int availability) { return availability == 0 || (availability == 1) == owned; }
}
