package com.dominic.photosweep;

public class PhotoInteractionCheck {
    static void check(boolean ok,String message) { if(!ok)throw new AssertionError(message); }
    public static void main(String[] args) {
        int supported=0;
        for(SwipeTheme.Kind kind:SwipeTheme.Kind.values()) {
            if(!PhotoInteraction.supports(kind))continue;
            supported++;
            check(PhotoInteraction.keepSqueeze(kind,0)==1,"photo must be continuous at receiving lip: "+kind);
            float previous=1;
            for(int i=0;i<=100;i++) {
                float squeeze=PhotoInteraction.keepSqueeze(kind,i/100f);
                check(squeeze>0 && squeeze<=previous,"keep mesh must not invert or overshoot: "+kind);
                previous=squeeze;
            }
        }
        check(supported==6,"only the approved six themes use the new renderer");
        check(!PhotoInteraction.supports(SwipeTheme.Kind.SPARKLE),"other themes retain existing effects");
        System.out.println("Six-theme keep-mesh continuity passed");
    }
}
