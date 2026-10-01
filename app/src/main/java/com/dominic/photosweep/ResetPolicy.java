package com.dominic.photosweep;

/** Reset only local account progression; photo access and Trash timers must survive. */
final class ResetPolicy {
    static boolean clears(String key) {
        switch(key) {
            case "xp":case "reviewed":case "rewarded":case "kept_ids":case "trashed_ids":
            case "restored_count":case "admin_mode":case "theme":case "theme_before_admin":
            case "stats_expanded":case "swipe_hint_seen":case "seen_swipe_overlays":return true;
            default:return key.startsWith("swipe_style_") || key.startsWith("swipe_intensity_") || key.startsWith("swipe_speed_");
        }
    }
}
