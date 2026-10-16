package com.dominic.photosweep;

/** Deterministic photo deformation in density-independent coordinates. No frame allocations. */
final class PhotoInteraction {
    static boolean supports(SwipeTheme.Kind kind) {
        switch (kind) {
            case FIRE: case WATER: case ICE: case TOXIC: case SPACE: case CANDY: return true;
            default: return false;
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
