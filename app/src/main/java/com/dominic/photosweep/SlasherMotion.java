package com.dominic.photosweep;

/** Release-only penetration at the captured pose and a back-facing walk into the room. */
final class SlasherMotion {
    static final int STAB_MS=3000, DOOR_MS=2800;
    float dx,dy,scale=1,angle,alpha=1,doorOpen,approach,drag,yaw,penetration,gait,walkTravel,stepLift;
    boolean handVisible,punctured,backFacing;
    static float ease(float value){float t=Math.max(0,Math.min(1,value));return t*t*(3-2*t);}
    void sample(boolean door,float phase,float width,float height,float cx,float cy) {
        float p=Math.max(0,Math.min(1,phase));
        dx=dy=angle=drag=doorOpen=approach=yaw=penetration=gait=walkTravel=stepLift=0;
        scale=alpha=1;handVisible=punctured=backFacing=false;
        if(door) {
            doorOpen=ease(p/.16f)*(1-ease((p-.88f)/.12f));
            float walk=ease((p-.16f)/.67f),steps=walk*6;
            int step=(int)steps;float foot=steps-step;
            float travel=(step+ease(foot))/6;walkTravel=travel;
            gait=(float)Math.sin(steps*Math.PI);
            float bounce=(float)Math.sin(foot*Math.PI);
            dx=(width*.28f-Math.min(width*.38f,height*.25f)*.10f-cx)*travel;
            stepLift=height*.012f*bounce*bounce*(1-walk);
            dy=(height*.58f-cy)*travel-stepLift;
            scale=1-.85f*walk;angle=gait*.075f*(1-walk*.5f);
            yaw=(float)Math.PI*ease((p-.28f)/.28f);backFacing=yaw>(float)Math.PI*.5f;
            alpha=1-ease((p-.81f)/.06f);
        } else {
            // The release pose stays exactly where the user put it until steel makes contact.
            // No escape run, yaw or rotation reset: an upside-down print is stabbed upside down.
            handVisible=p>=.08f && p<1;
            approach=ease((p-.08f)/.19f);
            punctured=p>=.27f;
            float drive=Math.max(0,Math.min(1,(p-.27f)/.09f));
            penetration=1-(1-drive)*(1-drive);
            drag=ease((p-.35f)/.65f);
            dx=(width+Math.max(0,width-cx)+width*.68f)*drag;
            dy=height*.12f*drag*drag;
            alpha=1-ease((p-.96f)/.04f);
        }
    }
}
