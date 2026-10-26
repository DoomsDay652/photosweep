package com.dominic.photosweep;

import android.content.Context;
import android.graphics.*;
import java.util.Arrays;
import java.util.Random;

/** Dorsal-view swimmer. Live paper, not pointer proximity, drives every bite. */
final class BeachSharkRenderer {
    static final String[] SPECIES={"Great white","Tiger shark","Hammerhead"};
    static final int X=64,Y=64;
    private static final int SWIM_X=40,SWIM_Y=12;
    private static final int[] BITE_RESTS={1450,900,1750,1150};
    private static final float[] LANES={.32f,.72f,.16f,.86f,.50f},SPINES={.506f,.455f,.400f};
    private static Bitmap[] art;
    private static int nextSpecies=new Random().nextInt(3);
    private static synchronized int chooseSpecies(){int result=nextSpecies;nextSpecies=(nextSpecies+1)%3;return result;}
    private final float[] pose=new float[9],previous=new float[9],vertices=new float[(X+1)*(Y+1)*2];
    private final boolean[] removed=new boolean[(X+1)*(Y+1)];
    private final int[] colors=new int[(X+1)*(Y+1)];
    private final float[] swimMesh=new float[(SWIM_X+1)*(SWIM_Y+1)*2];
    private long swimAt=-1;
    private float swimHeading,turnBend;
    private float swimX,swimY,swimDrive,strokePhase;
    private final int[] swimColors=new int[(SWIM_X+1)*(SWIM_Y+1)];
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
    private final RectF destination=new RectF();
    private final Rect fragment=new Rect();
    private Bitmap photo;
    private boolean prior,damaged,engaged,releasing,releaseBitten;
    private int species,bites;
    private long orbitStart=-1,lastNow,biteAt=-10000,releaseAt;
    private float noseX,noseY,heading,oldNoseX,oldNoseY,oldHeading,visibility;
    private float targetX,targetY,stageW,restLeft;
    private final Random routes=new Random();
    private float waypointX,waypointY,travelSpeed,turnSpeed;
    private long recoverUntil,approachAt;
    private int lastRecovery=-1;
    private boolean wasEngaged;
    private final float[] chipU=new float[24],chipV=new float[24],chipX=new float[24],chipY=new float[24];
    private int chipCount;
    private final Matrix releasePose=new Matrix(),carriedPose=new Matrix(),turn=new Matrix();
    private final float[] releaseValues=new float[9];
    private final float[] from=new float[6],to=new float[6];
    private float gripU,gripV,gripX,gripY,releaseNoseX,releaseNoseY,releaseHeading;
    private float releaseAngle,releaseMouthX,releaseMouthY,releaseExit;
    BeachSharkRenderer(Context context){
        if(art==null){
            BitmapFactory.Options options=new BitmapFactory.Options();options.inScaled=false;
            Bitmap atlas=BitmapFactory.decodeResource(context.getResources(),R.drawable.beach_sharks_dorsal_v8,options);
            int[] edges={0,Math.round(atlas.getHeight()*.391f),Math.round(atlas.getHeight()*.695f),atlas.getHeight()};
            art=new Bitmap[3];for(int n=0;n<3;n++)art[n]=Bitmap.createBitmap(atlas,0,edges[n],atlas.getWidth(),edges[n+1]-edges[n]);
        }
    }
    int species(){return species;}
    int biteCount(){return bites;}
    boolean hasDamage(){return damaged;}
    boolean missing(int col,int row){return removed[row*(X+1)+col];}
    float mouthX(){return noseX-stageW*.035f*(float)Math.cos(heading);}
    float mouthY(){return noseY-stageW*.035f*(float)Math.sin(heading);}
    float heading(){return heading;}
    boolean releasing(){return releasing;}
    boolean clamped(){return releaseBitten;}
    float gripU(){return gripU;}
    float gripV(){return gripV;}
    void carriedMatrix(Matrix result){result.set(carriedPose);}
    void clear(){
        photo=null;prior=false;damaged=false;bites=0;chipCount=0;biteAt=-10000;
        orbitStart=-1;lastNow=0;visibility=0;engaged=false;releasing=false;releaseBitten=false;Arrays.fill(removed,false);
        swimAt=-1;turnBend=0;swimDrive=0;strokePhase=0;recoverUntil=0;approachAt=0;travelSpeed=0;turnSpeed=0;lastRecovery=-1;wasEngaged=false;
    }
    void bind(Bitmap value,Matrix matrix){
        boolean fresh=value!=photo;
        if(fresh){clear();photo=value;if(value!=null)species=chooseSpecies();}
        if(releasing)return;
        matrix.getValues(pose);
        if(fresh){restLeft=Float.MAX_VALUE;for(int v=0;v<2;v++)for(int u=0;u<2;u++)restLeft=Math.min(restLeft,px(u,v,pose));}
        if(!prior)System.arraycopy(pose,0,previous,0,9);
    }
    private static float ease(float x){return SlasherMotion.ease(x);}
    private static float lerp(float a,float b,float p){return a+(b-a)*p;}
    private static float clamp(float x,float lo,float hi){return Math.max(lo,Math.min(hi,x));}
    private static float wrap(float a){while(a>(float)Math.PI)a-=2*(float)Math.PI;while(a<-(float)Math.PI)a+=2*(float)Math.PI;return a;}
    private float px(float u,float v,float[] m){return m[0]*u+m[1]*v+m[2];}
    private float py(float u,float v,float[] m){return m[3]*u+m[4]*v+m[5];}
    private boolean target(float w,float h){
        float left=Float.MAX_VALUE;
        for(int v=0;v<2;v++)for(int u=0;u<2;u++)left=Math.min(left,px(u,v,pose));
        if(left>w*.22f||left>restLeft-w*.018f)return false;
        float interest=LANES[bites%LANES.length],best=Float.MAX_VALUE;boolean found=false;
        for(int row=0;row<=Y;row++)for(int col=0;col<=X;col++){
            int i=row*(X+1)+col;if(removed[i])continue;float u=col/(float)X,v=row/(float)Y;
            float x=px(u,v,pose),y=py(u,v,pose);
            if(x<-w*.08f||x>w*.32f||y<h*.08f||y>h*.92f)continue;
            float score=Math.abs(v-interest)*w*.55f+x*.35f;
            if(score<best){best=score;targetX=x;targetY=y;found=true;}
        }
        return found;
    }
    void contact(long now,float w,float h){
        if(photo==null||photo.isRecycled()||releasing)return;
        stageW=w;float dt=lastNow==0?1/60f:clamp((now-lastNow)/1000f,0,.05f);lastNow=now;
        engaged=target(w,h);
        visibility=lerp(visibility,engaged?1:0,1-(float)Math.exp(-dt*7));
        if(orbitStart<0){orbitStart=now;noseX=-w*.30f;noseY=engaged?targetY:h*.5f;heading=0;}
        if(engaged && !wasEngaged && visibility<.5f && noseX<-w*.30f){
            noseX=-w*.30f;noseY=targetY;heading=0;turnSpeed=0;travelSpeed=0;
        }
        oldNoseX=noseX;oldNoseY=noseY;oldHeading=heading;
        if(engaged && !wasEngaged){approachAt=now;recoverUntil=0;}
        if(engaged){
            if(now>=recoverUntil){
                // Pursue surviving paper, with different recovery waypoints on
                // each pass instead of replaying an ellipse.
                waypointX=clamp(targetX+w*.085f,w*.09f,w*.39f);
                waypointY=targetY;
                if(approachAt==0)approachAt=now;
                if(now-approachAt>3000)recover(now,w,h,450);
            }
        }else{waypointX=-w*.55f;waypointY=clamp(noseY,h*.15f,h*.85f);}
        float dx=waypointX-noseX,dy=waypointY-noseY;
        float aim=(float)Math.atan2(dy,dx),error=wrap(aim-heading);
        float wantedTurn=clamp(error*4.5f,-4.2f,4.2f);
        turnSpeed=lerp(turnSpeed,wantedTurn,1-(float)Math.exp(-dt*7));
        heading+=turnSpeed*dt;
        float cruise=now<recoverUntil?.43f:.57f;
        float wantedSpeed=w*cruise*(.82f+.18f*(float)Math.cos((now-orbitStart)/1000f*1.7f+species));
        wantedSpeed*=1-.46f*clamp(Math.abs(error)/2.4f,0,1);
        travelSpeed=lerp(travelSpeed,wantedSpeed,1-(float)Math.exp(-dt*3));
        noseX+=(float)Math.cos(heading)*travelSpeed*dt;
        noseY+=(float)Math.sin(heading)*travelSpeed*dt;
        wasEngaged=engaged;
        // Occasional nips between circuits; the held print never enters a haul.
        int rest=BITE_RESTS[bites%4];
        if((engaged||visibility>.15f)&&now-biteAt>=rest){
            float motion=(float)Math.hypot(pose[2]-previous[2],pose[5]-previous[5])+(float)Math.hypot(noseX-oldNoseX,noseY-oldNoseY);
            int steps=Math.min(72,Math.max(1,(int)Math.ceil(motion/Math.max(1,w*.014f))));
            float a=w*.045f,b=w*.050f;
            boolean hit=false;
            for(int step=steps;step>=0&&!hit;step--){
                float t=step/(float)steps,angle=oldHeading+wrap(heading-oldHeading)*t;
                float x=lerp(oldNoseX,noseX,t)-w*.035f*(float)Math.cos(angle),y=lerp(oldNoseY,noseY,t)-w*.035f*(float)Math.sin(angle);
                for(int row=0;row<=Y&&!hit;row++)for(int col=0;col<=X;col++){
                    int i=row*(X+1)+col;if(removed[i])continue;float u=col/(float)X,v=row/(float)Y;
                    float sx=lerp(px(u,v,previous),px(u,v,pose),t),sy=lerp(py(u,v,previous),py(u,v,pose),t);
                    if(inside(sx,sy,x,y,a,b,angle)){
                        stamp(t,x,y,a,b,angle,-1);biteAt=now;bites++;recover(now,w,h,Math.max(520,BITE_RESTS[bites%4]/2));hit=true;break;
                    }
                }
            }
        }
        System.arraycopy(pose,0,previous,0,9);prior=true;
    }
    private void recover(long now,float w,float h,int duration){
        int route=routes.nextInt(4);
        if(route==lastRecovery)route=(route+1+routes.nextInt(3))%4;
        lastRecovery=route;
        float cy=py(.5f,.5f,pose),spread=h*(.13f+routes.nextFloat()*.10f);
        waypointX=w*(-.20f+routes.nextFloat()*.30f);
        waypointY=clamp(cy+(route%2==0?-spread:spread),h*.10f,h*.90f);
        if(route>=2)waypointX=-w*(.22f+routes.nextFloat()*.16f);
        recoverUntil=now+duration;approachAt=0;
    }
    private boolean inside(float x,float y,float cx,float cy,float a,float b,float angle){
        float c=(float)Math.cos(angle),s=(float)Math.sin(angle),dx=x-cx,dy=y-cy;
        float u=(dx*c+dy*s)/a,v=(-dx*s+dy*c)/b,theta=(float)Math.atan2(v,u);
        float teeth=1+.055f*(float)Math.cos(theta*17);return u*u+v*v<teeth*teeth;
    }
    private void stamp(float blend,float x,float y,float a,float b,float angle,int protect){
        chipCount=0;
        for(int row=0;row<=Y;row++)for(int col=0;col<=X;col++){
            int i=row*(X+1)+col;if(removed[i]||i==protect)continue;float u=col/(float)X,v=row/(float)Y;
            float sx=lerp(px(u,v,previous),px(u,v,pose),blend),sy=lerp(py(u,v,previous),py(u,v,pose),blend);
            if(inside(sx,sy,x,y,a,b,angle)){
                removed[i]=true;damaged=true;
                if(i%17==0&&chipCount<chipU.length){chipU[chipCount]=u;chipV[chipCount]=v;chipX[chipCount]=sx;chipY[chipCount++]=sy;}
            }
        }
    }
    void paper(Canvas canvas,Matrix matrix){drawPaper(canvas,matrix,0);}
    private void drawPaper(Canvas canvas,Matrix matrix,float fold){
        if(photo==null||photo.isRecycled())return;
        for(int row=0;row<=Y;row++)for(int col=0;col<=X;col++){
            int i=row*(X+1)+col;float u=col/(float)X,v=row/(float)Y;
            vertices[i*2]=u;vertices[i*2+1]=v;colors[i]=removed[i]?0x00FFFFFF:0xFFFFFFFF;
        }
        matrix.mapPoints(vertices);
        if(fold>0){
            float cx=releaseMouthX,cy=releaseMouthY;
            for(int i=0;i<vertices.length;i+=2){float distance=(float)Math.hypot(vertices[i]-cx,vertices[i+1]-cy),near=1-clamp(distance/120f,0,1);
                vertices[i+1]-=fold*near*near*(1-near)*30;}
        }
        paint.setAlpha(255);canvas.drawBitmapMesh(photo,X,Y,vertices,0,colors,0,paint);
    }
    void draw(Canvas canvas,long now,float w,float h,float alpha){
        if(photo==null||photo.isRecycled()||visibility<.002f)return;
        swimmer(canvas,noseX,noseY,heading,now,w,alpha*visibility);crumbs(canvas,now,w,h,alpha);
    }
    private void swimmer(Canvas canvas,float x,float y,float angle,long now,float w,float alpha){
        Bitmap sprite=art[species];float length=w*.64f,height=length*sprite.getHeight()/sprite.getWidth();
        float dt=swimAt<0?0:clamp((now-swimAt)/1000f,0,.1f);
        if(dt>0){
            float velocity=wrap(angle-swimHeading)/dt;
            turnBend=lerp(turnBend,clamp(velocity*.36f,-1.15f,1.15f),1-(float)Math.exp(-dt*5));
            float speed=(float)Math.hypot(x-swimX,y-swimY)/Math.max(1,w*dt);
            swimDrive=lerp(swimDrive,clamp(speed,0,1.3f),1-(float)Math.exp(-dt*4));
            strokePhase+=dt*(4.7f+species*.35f+swimDrive*2.8f);
        }
        swimAt=now;swimHeading=angle;swimX=x;swimY=y;
        float time=strokePhase/7.2f;
        // Integrate an articulated spine at constant arc length. Each skin section
        // stays perpendicular to that spine, so turns bend the animal instead of
        // rotating a stiff cutout or shearing its tail sideways.
        for(int col=0;col<=SWIM_X;col++){
            float u=col/(float)SWIM_X,travel=(u-.965f)*length;
            float cx=x,cy=y;int segments=Math.max(1,(int)(Math.abs(travel)/length*32));
            for(int k=0;k<segments;k++){
                float at=.965f+(u-.965f)*(k+.5f)/segments;
                float local=spineAngle(at,angle,time,turnBend);
                cx+=(float)Math.cos(local)*travel/segments;cy+=(float)Math.sin(local)*travel/segments;
            }
            float local=spineAngle(u,angle,time,turnBend),nx=-(float)Math.sin(local),ny=(float)Math.cos(local);
            float aft=clamp((.965f-u)/.965f,0,1);
            // A small fin bank follows the turn; tail propulsion grows aft.
            float bank=1-.10f*Math.abs(turnBend)*(1-aft);
            for(int row=0;row<=SWIM_Y;row++){
                int i=(row*(SWIM_X+1)+col)*2;float offset=(row/(float)SWIM_Y-SPINES[species])*height*bank;
                // Pectoral fins subtly yield under load instead of pumping like
                // wings. The centerline and ventral bite anchor remain fixed.
                float finLoad=(float)Math.exp(-Math.pow((u-.60f)/.15f,2));
                offset*=1-finLoad*.035f*(float)Math.sin(strokePhase-1.3f);
                swimMesh[i]=cx+nx*offset;swimMesh[i+1]=cy+ny*offset;
                float side=clamp(offset/(height*.35f),-1,1);
                float shade=clamp(.96f+side*turnBend*.045f+.018f*(float)Math.sin(u*16+row*.65f-strokePhase*.6f),.86f,1);
                int red=(int)(245*shade),green=(int)(251*shade),blue=(int)(255*shade);
                swimColors[i/2]=0xFF000000|red<<16|green<<8|blue;
            }
        }
        paint.setAlpha((int)(255*clamp(alpha,0,1)));canvas.drawBitmapMesh(sprite,SWIM_X,SWIM_Y,swimMesh,0,swimColors,0,paint);paint.setAlpha(255);
    }
    static float spineAngle(float u,float heading,float time,float bend){
        float aft=clamp((.965f-u)/.965f,0,1);
        return heading-bend*aft+.62f*aft*aft*(float)Math.sin(time*7.2f-aft*5.4f);
    }
    private void crumbs(Canvas canvas,long now,float w,float h,float alpha){
        float age=(now-biteAt)/1000f;if(age<0||age>.65f)return;
        for(int n=0;n<chipCount;n++){
            float angle=n*2.39996f,side=2+n%4,x=chipX[n]+(float)Math.cos(angle)*w*.11f*age;
            float y=chipY[n]+(float)Math.sin(angle)*w*.10f*age+h*.045f*age*age;
            int sx=(int)(chipU[n]*(photo.getWidth()-1)),sy=(int)(chipV[n]*(photo.getHeight()-1));
            fragment.set(sx,sy,Math.min(photo.getWidth(),sx+9),Math.min(photo.getHeight(),sy+9));destination.set(x-side,y-side,x+side,y+side);
            paint.setAlpha((int)(255*alpha*(1-age/.65f)));canvas.drawBitmap(photo,fragment,destination,paint);
        }paint.setAlpha(255);
    }
    void beginRelease(Bitmap value,Matrix matrix,long now,float w,float h){
        if(releasing)return;bind(value,matrix);releasing=true;releaseBitten=false;releaseAt=now;
        releasePose.set(matrix);carriedPose.set(matrix);matrix.getValues(releaseValues);
        releaseNoseX=orbitStart<0?-w*.35f:noseX;releaseNoseY=orbitStart<0?h*.5f:noseY;releaseHeading=heading;
        float best=Float.MAX_VALUE;int selected=-1;
        for(int row=0;row<=Y;row++)for(int col=0;col<=X;col++){
            int i=row*(X+1)+col;if(removed[i])continue;float u=col/(float)X,v=row/(float)Y,x=px(u,v,releaseValues),y=py(u,v,releaseValues);
            float score=x+Math.abs(y-h*.5f)*.15f+(x<0?w*3:0);
            if(score<best){best=score;selected=i;gripU=u;gripV=v;gripX=x;gripY=y;}
        }
        if(selected<0){gripU=.5f;gripV=.5f;gripX=px(.5f,.5f,releaseValues);gripY=py(.5f,.5f,releaseValues);}
        float right=gripX+w*.64f;
        for(int v=0;v<2;v++)for(int u=0;u<2;u++){
            float x=px(u,v,releaseValues),y=py(u,v,releaseValues),dx=x-gripX,dy=y-gripY;
            right=Math.max(right,Math.max(x,gripX+dx*(float)Math.cos(-.20f)-dy*(float)Math.sin(-.20f)));
        }
        releaseExit=right+w*.06f;
        visibility=1;
    }
    void drawRelease(Canvas canvas,float phase,float w,float h){
        if(!releasing||photo==null)return;stageW=w;float p=clamp(phase,0,1);long now=releaseAt+(long)(p*SeasonalRenderer.TRASH_MS);
        float approach=ease(p/.27f),haul=(float)Math.pow(clamp((p-.42f)/.58f,0,1),1.6),bank=ease((p-.37f)/.28f);
        releaseAngle=(float)Math.PI*bank;
        releaseMouthX=gripX-releaseExit*haul;
        releaseMouthY=gripY-h*.085f*(float)Math.sin(haul*Math.PI);
        // Approach on a curved swim path with matching entry/exit tangents.
        // The last tangent faces into the grip, avoiding a sideways slide into it.
        float endX=gripX+w*.035f,endY=gripY;
        float lead=clamp((float)Math.hypot(endX-releaseNoseX,endY-releaseNoseY)*.5f,w*.12f,w*.35f);
        float ax=releaseNoseX+lead*(float)Math.cos(releaseHeading),ay=releaseNoseY+lead*(float)Math.sin(releaseHeading);
        float bx=endX-lead,by=endY,q=1-approach;
        noseX=q*q*q*releaseNoseX+3*q*q*approach*ax+3*q*approach*approach*bx+approach*approach*approach*endX;
        noseY=q*q*q*releaseNoseY+3*q*q*approach*ay+3*q*approach*approach*by+approach*approach*approach*endY;
        float vx=q*q*(ax-releaseNoseX)+2*q*approach*(bx-ax)+approach*approach*(endX-bx);
        float vy=q*q*(ay-releaseNoseY)+2*q*approach*(by-ay)+approach*approach*(endY-by);
        heading=(float)Math.atan2(vy,vx);
        if(p>=.28f&&!releaseBitten){
            System.arraycopy(releaseValues,0,pose,0,9);System.arraycopy(pose,0,previous,0,9);
            // Nip beside the grip, preserving the actual piece held beneath the snout.
            int grip=Math.round(gripV*Y)*(X+1)+Math.round(gripU*X);
            stamp(1,gripX-w*.070f,gripY,w*.075f,w*.060f,0,grip);biteAt=now;bites++;releaseBitten=true;
        }
        carriedPose.set(releasePose);
        if(p>.37f){
            float paperAngle=-.20f*bank,c=(float)Math.cos(paperAngle),s=(float)Math.sin(paperAngle);
            from[0]=gripX;from[1]=gripY;from[2]=gripX+1;from[3]=gripY;from[4]=gripX;from[5]=gripY+1;
            to[0]=releaseMouthX;to[1]=releaseMouthY;to[2]=releaseMouthX+c;to[3]=releaseMouthY+s;to[4]=releaseMouthX-s;to[5]=releaseMouthY+c;
            turn.setPolyToPoly(from,0,to,0,3);carriedPose.postConcat(turn);
            heading=releaseAngle;noseX=releaseMouthX+w*.035f*(float)Math.cos(heading);noseY=releaseMouthY+w*.035f*(float)Math.sin(heading);
        }
        drawPaper(canvas,carriedPose,p>=.28f?(1-ease((p-.38f)/.18f)):0);
        swimmer(canvas,noseX,noseY,heading,now,w,1);crumbs(canvas,now,w,h,1);
    }
}
