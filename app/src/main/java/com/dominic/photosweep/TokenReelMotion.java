package com.dominic.photosweep;

/** Presentation only: token odds and payout are decided before the reels start. */
final class TokenReelMotion {
    static int duration(int reel) { return 1650 + Math.max(0,Math.min(2,reel))*450; }
    static int target(float start, int token, int reel) {
        if(token<0 || token>2) throw new IllegalArgumentException("token");
        return (int)Math.ceil(start/3f)*3 + (6+Math.max(0,Math.min(2,reel))*2)*3 + token;
    }
    static float position(float start, int target, float elapsed, int reel) {
        float t=Math.max(0,Math.min(1,elapsed/duration(reel)));
        float remaining=1-t;
        return start+(target-start)*(1-remaining*remaining*remaining*remaining);
    }
    static int symbol(int index) { return Math.floorMod(index,3); }
}
