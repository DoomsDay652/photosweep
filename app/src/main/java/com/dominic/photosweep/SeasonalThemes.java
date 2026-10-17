package com.dominic.photosweep;

/** Picker families are independent of saved theme IDs and background motion profiles. */
final class SeasonalThemes {
    static final int HALLOWEEN=1, SUMMER=12;
    static int group(int theme) {
        if(theme==ThemeCatalog.SLASHER)return HALLOWEEN;
        if(theme==ThemeCatalog.SUMMER)return SUMMER;
        return HolidayThemes.collection(theme);
    }
    static String name(int group){return group==SUMMER?"Summer":HolidayThemes.NAMES[group];}
    static int[] variants(int group) {
        if(group==HALLOWEEN)return new int[]{64,65,ThemeCatalog.SLASHER};
        if(group==SUMMER)return new int[]{ThemeCatalog.SUMMER};
        return new int[]{HolidayThemes.still(group),HolidayThemes.animated(group)};
    }
    static String label(int theme) {
        if(theme==64)return "Cozy scene";
        if(theme==65)return "Pumpkin night";
        if(theme==ThemeCatalog.SLASHER)return "Slasher";
        if(theme==ThemeCatalog.SUMMER)return "Beach Day";
        return HolidayThemes.animatedTheme(theme)?"Animated":"Still";
    }
    private SeasonalThemes(){}
}
