package com.dominic.photosweep;

/** Deterministic contact geometry in density-independent coordinates. No frame allocations. */
final class PhotoInteraction {
    static boolean supports(SwipeTheme.Kind kind) {
        switch (kind) {
            case FIRE: case WATER: case ICE: case TOXIC: case SPACE: case CANDY: return true;
            default: return false;
        }
    }
    static float contact(SwipeTheme.Kind kind, float y, float height, float seconds) {
        float wave=(float)Math.sin(y*.065+seconds*2.2);
        switch(kind) {
            case FIRE: return 27+9*wave+5*(float)Math.sin(y*.19-seconds*4);
            case WATER: return 24+8*wave;
            case ICE: return 23+12*Math.abs(((y/29)%2)-1);
            case TOXIC: return 24+11*wave+8*(float)Math.sin(y*.17);
            case SPACE: return 18+26*(float)Math.exp(-Math.pow((y-height*.5)/Math.max(1,height*.34),2));
            case CANDY: return 25+6*(float)Math.sin(y*.22+seconds*6);
            default: return 0;
        }
    }
    static float keepSqueeze(SwipeTheme.Kind kind, float depth) {
        float d=SwipeMotion.clamp(depth);
        switch(kind) {
            case FIRE: return 1-.32f*d;
            case WATER: return 1-.12f*d;
            case ICE: case TOXIC: return 1-.03f*d;
            case SPACE: return 1-.84f*d;
            case CANDY: return 1-.48f*d;
            default: return 1-.06f*d;
        }
    }
    private PhotoInteraction() { }
}
