package com.dominic.photosweep;

/** Preserve the complete original aspect ratio, bounded for a high-quality display decode. */
final class PhotoFit {
    static float scale(int width, int height) {
        if(width<=0 || height<=0) return 1f;
        return (float)Math.min(1d, Math.min(3072d / Math.max(width,height),
                Math.sqrt(4194304d / ((double)width*height))));
    }
}
