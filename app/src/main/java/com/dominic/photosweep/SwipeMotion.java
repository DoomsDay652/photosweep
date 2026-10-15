package com.dominic.photosweep;

/** Pure timing/trajectory math shared by the swipe renderer and its regression checks. */
final class SwipeMotion {
    /** Spring tether in density-independent units; fixed substeps keep 60/120 Hz consistent. */
    static final class Body {
        float x,y,vx,vy,angle,omega;
        private float gripX,gripY,targetX,targetY,inertia,accumulator;
        void begin(float grabX,float grabY,float width,float height) {
            x=y=vx=vy=angle=omega=targetX=targetY=accumulator=0;
            gripX=Math.max(-width*.48f,Math.min(width*.48f,grabX));
            gripY=Math.max(-height*.48f,Math.min(height*.48f,grabY));
            inertia=Math.max(1400,(width*width+height*height)/12f);
        }
        void target(float dx,float dy) { targetX=dx;targetY=dy; }
        void step(float seconds) {
            accumulator+=Math.max(0,Math.min(.05f,seconds));
            final float h=1f/240;
            while(accumulator>=h) {
                float c=(float)Math.cos(angle),s=(float)Math.sin(angle);
                float rx=c*gripX-s*gripY,ry=s*gripX+c*gripY;
                float ex=targetX+gripX-x-rx,ey=targetY+gripY-y-ry;
                float fx=1900*ex-68*(vx-omega*ry),fy=1900*ey-68*(vy+omega*rx);
                vx+=fx*h;vy+=fy*h;
                float upright=(float)Math.atan2(Math.sin(angle),Math.cos(angle));
                omega+=((rx*fy-ry*fx)/inertia-7*upright-4.8f*omega)*h;
                omega=Math.max(-9,Math.min(9,omega));
                x+=vx*h;y+=vy*h;angle+=omega*h;
                // A short tether keeps a fast flick responsive even on a delayed frame.
                c=(float)Math.cos(angle);s=(float)Math.sin(angle);
                ex=targetX+gripX-x-(c*gripX-s*gripY);
                ey=targetY+gripY-y-(s*gripX+c*gripY);
                float lag=(float)Math.hypot(ex,ey);
                if(lag>38){x+=ex*(1-38/lag);y+=ey*(1-38/lag);}
                accumulator-=h;
            }
        }
        float degrees() { return (float)Math.toDegrees(angle); }
        float releaseDegrees() { return Math.max(-100,Math.min(100,(float)Math.toDegrees(omega)*.18f)); }
        float gripLag() {
            float c=(float)Math.cos(angle),s=(float)Math.sin(angle);
            return (float)Math.hypot(targetX+gripX-x-(c*gripX-s*gripY),targetY+gripY-y-(s*gripX+c*gripY));
        }
    }
    static float clamp(float value) { return Math.max(0f, Math.min(1f, value)); }
    static float ease(float value) { float t = clamp(value); return t * t * (3f - 2f * t); }
    static float followFactor(float milliseconds,float response) { return 1f-(float)Math.exp(-Math.max(0,milliseconds)/Math.max(1,response)); }
    static float deepestBite(float previous,float cut,float photoWidth) {
        return Math.max(clamp(previous),clamp(cut/Math.max(1,photoWidth)));
    }
    static float freshBite(float previous,float cut,float photoWidth) {
        return Math.max(0,deepestBite(previous,cut,photoWidth)-clamp(previous))*Math.max(1,photoWidth);
    }
    static float portalFront(float centerX,float radiusX,float centerY,float radiusY,float y) {
        float v=Math.max(-1,Math.min(1,(y-centerY)/Math.max(1,radiusY)));
        return centerX-radiusX*(float)Math.sqrt(Math.max(0,1-v*v));
    }
    static float portalDepth(float distance,float reach) {
        return 1f-(float)Math.exp(-Math.max(0,distance)/Math.max(1,reach));
    }
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
    static float cardScale(float phase, boolean keep) { return 1f; }
    static float cardAlpha(float phase, boolean keep) { return 1f; }
    static float cardVertical(float phase, boolean keep) { return 0f; }
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






