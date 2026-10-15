package com.dominic.photosweep;

public class PhotoInteractionCheck {
    static void check(boolean ok,String message) { if(!ok)throw new AssertionError(message); }
    public static void main(String[] args) {
        int supported=0;
        for(SwipeTheme.Kind kind:SwipeTheme.Kind.values()) {
            if(!PhotoInteraction.supports(kind))continue;
            supported++;
            // Contact must stay inside the strip replaced by the photo mesh, even on small previews.
            for(float height:new float[]{60,180,550,1200})
                for(int frame=0;frame<80;frame++)for(int row=0;row<=100;row++) {
                    float edge=PhotoInteraction.contact(kind,height*row/100,height,frame*.1f);
                    check(Float.isFinite(edge) && edge>=0 && edge<65,"contact outside mask: "+kind);
                }
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
        System.out.println("Six-theme contact bounds and keep-mesh continuity passed");
    }
}
