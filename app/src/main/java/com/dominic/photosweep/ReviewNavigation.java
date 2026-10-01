package com.dominic.photosweep;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Locale;

/** Calendar navigation shared by review and month lists. Empty months are skipped. */
final class ReviewNavigation {
    private ReviewNavigation() { }

    static String adjacent(Collection<String> months, String current, boolean next) {
        String result = null;
        for (String month : months) {
            int direction = month.compareTo(current);
            if ((next && direction > 0) || (!next && direction < 0)) {
                if (result == null || (next ? month.compareTo(result) < 0 : month.compareTo(result) > 0)) result = month;
            }
        }
        return result;
    }

    static String title(String month, boolean includeYear) {
        return YearMonth.parse(month).format(DateTimeFormatter.ofPattern(includeYear ? "MMMM yyyy" : "MMMM", Locale.getDefault()));
    }

    static String photoCount(int count) { return count + (count == 1 ? " photo" : " photos"); }

    static String expiry(long timeLeftMillis) {
        if (timeLeftMillis <= 0) return "Waiting for cleanup";
        long days = (timeLeftMillis + 86_399_999L) / 86_400_000L;
        return "Recovery window: " + days + (days == 1 ? " day" : " days");
    }
}
