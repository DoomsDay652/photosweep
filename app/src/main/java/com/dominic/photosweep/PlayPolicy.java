package com.dominic.photosweep;

final class PlayPolicy {
    private PlayPolicy() {}
    static boolean themeAllowed(int index) {
        return index >= 0 && index < HolidayThemes.end() && !(index >= 3 && index <= 10)
                && !(index >= 34 && index <= 41) && !(index >= 56 && index <= 60) && index != 26;
    }
    static boolean accountReady(String apiKey, String appId, String projectId, String privacy, String deletion) {
        return !apiKey.isEmpty() && !appId.isEmpty() && !projectId.isEmpty()
                && validUrl(privacy) && validUrl(deletion);
    }
    static boolean validUrl(String url) {
        try {
            java.net.URI uri = java.net.URI.create(url);
            return "https".equals(uri.getScheme()) && uri.getHost() != null
                    && !uri.getHost().contains("example.") && !"localhost".equals(uri.getHost());
        } catch (IllegalArgumentException e) { return false; }
    }
}
