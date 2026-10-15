package com.dominic.photosweep;

/** Device-local, earned-only rewards. No purchases, timers or expiring balances. */
final class ThemeProgression {
    static final int XP_PER_LEVEL = 200;
    static int level(int xp) { return Math.max(0, xp) / XP_PER_LEVEL + 1; }
    static int reviewXp(int uniqueReviews) { return 5 + (uniqueReviews > 0 && uniqueReviews % 10 == 0 ? 10 : 0); }
    static int migratedXp(int oldXp) { return (int)Math.min(Integer.MAX_VALUE - 2000L, Math.max(0L, oldXp) * 200L / 500L); }
    static int addLevels(int xp, int count) { return (int)Math.min(Integer.MAX_VALUE - 2000L, (long)Math.max(0,xp) + Math.max(0,count)*200L); }
    static final int REWARD_TYPES = 5;
    // One independent roll: 1 Silver 50%, 3 Silver 20%, 1 Gold 20%, 2 Gold 5%, 1 Diamond 5%.
    static int roll(int percentile) {
        if (percentile < 0 || percentile >= 100) throw new IllegalArgumentException("percentile");
        return percentile<50?0:percentile<70?1:percentile<90?2:percentile<95?3:4;
    }
    private static void validateReward(int reward) {
        if(reward<0 || reward>=REWARD_TYPES)throw new IllegalArgumentException("reward");
    }
    static int tokenKind(int reward) { validateReward(reward);return reward<2?0:reward<4?1:2; }
    static int tokenCount(int reward) { validateReward(reward);return reward==1?3:reward==3?2:1; }
    static String rewardLabel(int reward) {
        int count=tokenCount(reward),kind=tokenKind(reward);
        return count + " " + (kind==0?"Silver":kind==1?"Gold":"Diamond") + (count==1?" token":" tokens");
    }
    static String reelLabel(int reward) {
        switch(reward) {
            case 0:return "SILVER";case 1:return "3 SILVER";case 2:return "GOLD";case 3:return "2 GOLD";case 4:return "DIAMOND";
            default:throw new IllegalArgumentException("reward");
        }
    }
    static int legacyReward(int token) { return token==0?0:token==1?2:token==2?4:-1; }
}
