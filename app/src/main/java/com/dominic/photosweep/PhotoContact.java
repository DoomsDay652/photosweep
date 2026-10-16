package com.dominic.photosweep;

/** Swept material-point collision. Coordinates come only from the displayed photo, never the pointer. */
final class PhotoContact {
    static float coverage(float fromX,float fromY,float toX,float toY,float footprint,
                          float height,float[] edges,float[] hit) {
        if(height<=0 || edges.length<2)return 0;
        float lo=0,hi=1,dy=toY-fromY;
        if(Math.abs(dy)<.0001f) {
            if(toY<0 || toY>=height)return 0;
        } else {
            float a=-fromY/dy,b=(height-.001f-fromY)/dy;
            lo=Math.max(0,Math.min(a,b));hi=Math.min(1,Math.max(a,b));
            if(lo>hi)return 0;
        }
        // Sample on the boundary's row resolution, including the entire swept path and its endpoints.
        int steps=Math.max(1,Math.min(2048,(int)Math.ceil(Math.abs(dy)*(hi-lo)/height*(edges.length-1)*2)));
        float best=0;
        for(int i=0;i<=steps;i++) {
            float t=lo+(hi-lo)*i/steps,x=fromX+(toX-fromX)*t,y=fromY+dy*t;
            float row=Math.max(0,Math.min(edges.length-1,y/height*(edges.length-1)));
            int at=Math.min(edges.length-2,(int)row);
            float edge=edges[at]+(edges[at+1]-edges[at])*(row-at);
            float amount=SwipeMotion.clamp(.5f+(edge-x)/Math.max(1,footprint));
            if(amount>best){best=amount;hit[0]=edge;hit[1]=y;}
            if(best>=1)return 1;
        }
        return best;
    }
    /** Interpolate the displayed pose through its rotation, rather than cutting across the rotation arc. */
    static int buildSweep(float[] from,float[] to,float[] samples) {
        float a0=(float)Math.atan2(from[3],from[0]),a1=(float)Math.atan2(to[3],to[0]);
        float delta=a1-a0;
        while(delta>Math.PI)delta-=2*Math.PI;
        while(delta<-Math.PI)delta+=2*Math.PI;
        int steps=Math.max(1,Math.min(samples.length/6-1,(int)Math.ceil(Math.abs(delta)/(Math.PI/24))));
        float sx0=(float)Math.hypot(from[0],from[3]),sx1=(float)Math.hypot(to[0],to[3]);
        float sy0=(float)Math.hypot(from[1],from[4]),sy1=(float)Math.hypot(to[1],to[4]);
        float cx0=from[2]+(from[0]+from[1])*.5f,cy0=from[5]+(from[3]+from[4])*.5f;
        float cx1=to[2]+(to[0]+to[1])*.5f,cy1=to[5]+(to[3]+to[4])*.5f;
        for(int i=0;i<=steps;i++) {
            int at=i*6;float t=i/(float)steps,angle=a0+delta*t;
            float cos=(float)Math.cos(angle),sin=(float)Math.sin(angle);
            float sx=sx0+(sx1-sx0)*t,sy=sy0+(sy1-sy0)*t;
            samples[at]=sx*cos;samples[at+1]=-sy*sin;
            samples[at+3]=sx*sin;samples[at+4]=sy*cos;
            samples[at+2]=cx0+(cx1-cx0)*t-(samples[at]+samples[at+1])*.5f;
            samples[at+5]=cy0+(cy1-cy0)*t-(samples[at+3]+samples[at+4])*.5f;
        }
        System.arraycopy(from,0,samples,0,6);System.arraycopy(to,0,samples,steps*6,6);
        return steps;
    }
    private PhotoContact() { }
}
