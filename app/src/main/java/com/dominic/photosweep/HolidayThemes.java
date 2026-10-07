package com.dominic.photosweep;

/** Append-only holiday IDs preserve every existing saved theme selection. */
final class HolidayThemes {
    static final int FIRST = 62;
    static final String[] KEYS = {"christmas", "halloween", "thanksgiving", "independence", "new_year",
            "mlk", "presidents", "memorial", "juneteenth", "labor", "columbus", "veterans"};
    static final String[] NAMES = {"Christmas", "Halloween", "Thanksgiving", "July 4th", "New Year's Day",
            "Martin Luther King Jr. Day", "Presidents' Day", "Memorial Day", "Juneteenth", "Labor Day", "Columbus Day", "Veterans Day"};
    static final String[] SYMBOLS = {"🎄", "🎃", "🍂", "🎆", "✨", "🕊", "🏛", "🌺", "⭐", "☀", "⛵", "🎗"};
    static final String[] SCENES = {"Snowy village lights", "Cozy pumpkin night", "Autumn harvest evening",
            "American waterfront celebration", "Midnight fireworks celebration", "A peaceful bridge of light", "Winter civic gardens",
            "Quiet remembrance garden", "Community celebration", "Golden summer town", "Twilight sailing horizon", "Lanterns of gratitude"};
    static final String[] MOTIONS = {"Drifting snowflakes", "Bobbing pumpkin lights", "Falling harvest leaves",
            "Red, white and blue fireworks", "Multicolor fireworks", "Floating peace doves",
            "Sapphire and gold glints", "Slow drifting poppies", "Rising celebration stars", "Swaying summer blooms",
            "Sailing emblems and soft ocean sway", "Gentle laurel lights"};
    static final String[] SEARCH = {"Christmas Day winter snow", "Halloween October pumpkin", "Thanksgiving Day harvest autumn",
            "Independence Day July 4 fourth fireworks", "New Year New Year's Day confetti", "MLK Martin Luther King Jr. peace",
            "Washington's Birthday Presidents' Day", "Memorial Day remembrance", "Juneteenth National Independence Day freedom",
            "Labor Day summer", "Columbus Day sailing", "Veterans Day gratitude remembrance"};
    private HolidayThemes() {}
    static int collection(int theme) { return theme >= FIRST && theme < end() ? (theme - FIRST) / 2 : -1; }
    static int end() { return FIRST + NAMES.length * 2; }
    static int still(int holiday) { return FIRST + holiday * 2; }
    static int animated(int holiday) { return still(holiday) + 1; }
    static boolean animatedTheme(int theme) { return collection(theme) >= 0 && (theme - FIRST) % 2 == 1; }
    static boolean reflective(int holiday) { return holiday == 5 || holiday == 7 || holiday == 11; }
}

