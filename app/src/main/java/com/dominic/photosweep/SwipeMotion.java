package com.dominic.photosweep;

/** Pure timing/trajectory math shared by the swipe renderer and its regression checks. */
final class SwipeMotion {
    static float clamp(float value) { return Math.max(0f, Math.min(1f, value)); }
    static float ease(float value) { float t = clamp(value); return t * t * (3f - 2f * t); }
    static int duration(int speed) { return Math.max(220, Math.min(620, 36000 / Math.max(50, speed))); }
    // Shared semantic cues do not change with the selected theme or custom VFX.
    static final int KEEP_COLOR = 0xFF80E3BF, TRASH_COLOR = 0xFFFF959D;
    static int intent(float progress) { return Math.abs(progress) < .055f ? 0 : progress > 0 ? 1 : -1; }
    static float dragProgress(float dx, float dy, float width) {
        if (Math.abs(dx) <= Math.abs(dy) * 1.2f) return 0;
        return Math.max(-1f, Math.min(1f, dx / Math.max(1f, width * .62f)));
    }
    static int actionColor(boolean keep) { return keep ? KEEP_COLOR : TRASH_COLOR; }
    static String actionLabel(boolean keep) { return keep ? "Keep" : "Move to Trash"; }
    static int releaseDuration(int speed, boolean keep) { return duration(speed) + (keep ? 0 : 90); }
    static float cardScale(float phase, boolean keep) { return 1f + (keep ? .025f : 0f) * ease(phase); }
    static float cardAlpha(float phase, boolean keep) { return 1f; }
    static float cardVertical(float phase, boolean keep) { return (keep ? -18f : 0f) * ease(phase); }
    static float cardRotation(boolean keep) { return 0f; }
    static int grinderClipLeft(float translationX, int width, float edge) {
        return Math.max(0, Math.min(width, (int)Math.ceil(edge-translationX)));
    }
    static int keepClipRight(float translationX, int width, float edge, float scale) {
        float pivot=width/2f;
        return Math.max(0,Math.min(width,(int)Math.floor(pivot+(edge-translationX-pivot)/Math.max(.01f,scale))));
    }
    static float grinderProximity(float photoLeft, float edge, float range) {
        return clamp(1f-Math.max(0,photoLeft-edge)/Math.max(1,range));
    }
    static float pocketOpening(float restRight, float currentRight, float edge, float minimumTravel) {
        return ease((currentRight-restRight)/Math.max(1,Math.max(minimumTravel,edge-restRight)));
    }
    static float keepSleeveLeft(float photoLeft, float photoWidth, float receivingRight, float opening) {
        return Math.min(receivingRight,Math.max(photoLeft,receivingRight-Math.max(0,photoWidth)*.48f*clamp(opening)));
    }
    static float grinderShake(float seconds, float proximity) {
        return ease(proximity)*(float)(Math.sin(seconds*89)*.68+Math.sin(seconds*137)*.32);
    }
    static float spellHalfWidth(float photoWidth, float stageWidth, float padding) {
        return Math.max(1,Math.min(stageWidth*.46f,photoWidth*.5f+padding));
    }
    static float spellHalfHeight(float photoHeight, float stageHeight, float centerY, float padding) {
        float room=Math.max(1,Math.min(centerY,stageHeight-centerY)-padding);
        return Math.max(1,Math.min(room,photoHeight*.58f+padding));
    }
    static float travel(float phase, boolean keep) { return (keep ? 1f : -1f) * ease(phase); }
    static boolean shouldCommit(float dx, float dy, float threshold) { return Math.abs(dx)>threshold && Math.abs(dx)>Math.abs(dy)*1.2f; }
    static float falling(float seconds) { return 32f * seconds * seconds; }
}





