package com.dominic.photosweep;

/** Provider boundary for the upcoming account, billing and advertising integrations. */
final class AppServices {
    enum Product {
        REMOVE_ADS("remove_ads", false), SUPPORT_SMALL("support_small", true),
        SUPPORT_MEDIUM("support_medium", true), SUPPORT_LARGE("support_large", true);
        final String playId;
        final boolean consumable;
        Product(String playId, boolean consumable) { this.playId = playId; this.consumable = consumable; }
    }
    interface Account {
        boolean signedIn();
        String displayName();
    }
    interface Purchases {
        boolean available();
        boolean ownsRemoveAds();
        // Prices must come from Play ProductDetails, never from hard-coded currency strings.
        String localizedPrice(Product product);
    }
    interface Ads {
        boolean configured();
        boolean consentAllowsAds();
    }

    final Account account;
    final Purchases purchases;
    final Ads ads;

    AppServices(Account account, Purchases purchases, Ads ads) {
        this.account = account; this.purchases = purchases; this.ads = ads;
    }

    boolean canShowAds(boolean reviewing, boolean showingTrash) {
        return ads.configured() && ads.consentAllowsAds() && !purchases.ownsRemoveAds()
                && !reviewing && !showingTrash;
    }

    static AppServices offline() {
        return new AppServices(new Account() {
            public boolean signedIn() { return false; }
            public String displayName() { return "Guest"; }
        }, new Purchases() {
            public boolean available() { return false; }
            public boolean ownsRemoveAds() { return false; }
            public String localizedPrice(Product product) { return null; }
        }, new Ads() {
            public boolean configured() { return false; }
            public boolean consentAllowsAds() { return false; }
        });
    }
}
