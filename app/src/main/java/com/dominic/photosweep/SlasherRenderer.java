package com.dominic.photosweep;
import android.content.Context;
import android.graphics.*;

/** Photo-attached knife and hinged door artwork; no grinder or pre-release damage. */
final class SlasherRenderer {
    private static Bitmap handArt,doorArt,backArt,inkArt;
    private static final int COLS=28,ROWS=30;
    private final float[] mesh=new float[(COLS+1)*(ROWS+1)*2];
    final int[] meshColors=new int[(COLS+1)*(ROWS+1)];
    private float pinX,pinY,knifeX,knifeY,knifeSize,bladeX,bladeY;
    private final Path knifeClip=new Path();
    private Bitmap photo,inkPhoto;
    private final java.util.Random random;
    private int lastSpot=-1;
    float hitU,hitV,armRootX;
    boolean insideDoor;
    float clearLeft,clearRight,doorTop,doorBottom;
    private static final float[] HIT_US={.35f,.59f,.81f},HIT_VS={.40f,.58f,.75f};
    private final float[] anchor=new float[8],vertices=new float[8],doorVertices=new float[8];
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
    private final Paint trim=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF target=new RectF();private final Path slit=new Path();
    final SlasherMotion motion=new SlasherMotion();
    private float cx,cy;
    SlasherRenderer(Context context){this(context,new java.util.Random());}
    SlasherRenderer(Context context,java.util.Random random) {
        this.random=random;
        if(handArt==null) {
            BitmapFactory.Options options=new BitmapFactory.Options();options.inScaled=false;
            handArt=BitmapFactory.decodeResource(context.getResources(),R.drawable.slasher_hand_overhand_v4,options);
            inkArt=BitmapFactory.decodeResource(context.getResources(),R.drawable.slasher_ink_v3,options);
            Bitmap sheet=BitmapFactory.decodeResource(context.getResources(),R.drawable.slasher_door_v1,options);
            // Isolate the straight door leaf from the transparent lighting margins.
            doorArt=Bitmap.createBitmap(sheet,215,103,597,1332);
            // Matte print stock on the reverse, with subtle physical paper grain.
            backArt=Bitmap.createBitmap(96,96,Bitmap.Config.ARGB_8888);int[] stock=new int[96*96];
            for(int i=0;i<stock.length;i++){int hash=i*1664525+1013904223;hash^=hash>>>16;hash*=0x7FEB352D;hash^=hash>>>15;int tone=224+(hash&3);stock[i]=0xFF000000|(tone<<16)|((tone-2)<<8)|(tone-9);}
            backArt.setPixels(stock,0,96,0,0,96,96);
        }
    }
    void begin(Bitmap value,Matrix pose) {begin(value,pose,0,0);}
    void begin(Bitmap value,Matrix pose,float width,float height) {
        photo=value;float[] uv=anchor;uv[0]=0;uv[1]=0;uv[2]=1;uv[3]=0;uv[4]=0;uv[5]=1;uv[6]=1;uv[7]=1;
        pose.mapPoints(uv);cx=(uv[0]+uv[6])*.5f;cy=(uv[1]+uv[7])*.5f;
        int[] candidates=new int[9];int count=0;
        for(int candidate=0;candidate<9;candidate++) {
            float u=HIT_US[candidate%3],v=HIT_VS[candidate/3];
            float x=uv[0]+(uv[2]-uv[0])*u+(uv[4]-uv[0])*v;
            float y=uv[1]+(uv[3]-uv[1])*u+(uv[5]-uv[1])*v;
            if(candidate!=lastSpot && (width<=0 || (x>width*.06f && x<width*.92f && y>height*.12f && y<height*.90f)))candidates[count++]=candidate;
        }
        int spot=count>0?candidates[random.nextInt(count)]:(lastSpot+1+random.nextInt(8))%9;lastSpot=spot;
        hitU=Math.round(HIT_US[spot%3]*COLS)/(float)COLS;hitV=Math.round(HIT_VS[spot/3]*ROWS)/(float)ROWS;
        // Stamp real black ink in UV space once; the stain follows the same moving paper and hole.
        inkPhoto=Bitmap.createBitmap(256,256,Bitmap.Config.ARGB_8888);
        float pw=(float)Math.hypot(uv[2]-uv[0],uv[3]-uv[1]),ph=(float)Math.hypot(uv[4]-uv[0],uv[5]-uv[1]);
        float iw=256*.32f,ih=iw*pw/Math.max(1,ph);
        target.set(hitU*256-iw*.5f,hitV*256-ih*.5f,hitU*256+iw*.5f,hitV*256+ih*.5f);
        paint.setAlpha(255);new Canvas(inkPhoto).drawBitmap(inkArt,null,target,paint);
    }
    void drawSwipeCue(Canvas canvas,Matrix pose,float width,float height) {
        float[] center={.5f,.5f};pose.mapPoints(center);
        float side=(center[0]-width*.5f)/(width*.32f);
        float amount=SlasherMotion.ease(Math.abs(side));if(amount<=0)return;
        boolean keep=side>0;float x=keep?width:0;
        int color=keep?0xD0091018:0x80597B87;
        paint.setShader(new RadialGradient(x,center[1],width*.66f,new int[]{color,0x00000000},new float[]{0,1},Shader.TileMode.CLAMP));
        paint.setAlpha(Math.round(255*amount));canvas.drawRect(0,0,width,height,paint);
        paint.setShader(null);paint.setAlpha(255);
    }
    void clear(){photo=null;inkPhoto=null;}
    boolean active(){return photo!=null && !photo.isRecycled();}
    void draw(Canvas canvas,boolean keep,float phase,float width,float height,float density) {
        if(!active())return;
        boolean door=!keep;
        motion.sample(door,phase,width,height,cx,cy);
        if(door) {
            float halfHeight=(float)Math.hypot(anchor[4]-anchor[0],anchor[5]-anchor[1])*.5f;
            float footStart=cy+halfHeight,threshold=height*.50f+doorHeight(width,height)*.5f-density*5;
            float inside=SlasherMotion.ease((phase-.64f)/.19f);
            float foot=footStart+(threshold-footStart)*motion.walkTravel-doorHeight(width,height)*.24f*inside;
            motion.dy=foot-halfHeight*motion.scale-cy-motion.stepLift;
        }
        buildPhotoMesh(door);
        if(!door)prepareKnife(width,height,density);
        insideDoor=false;
        if(door){
            drawRoom(canvas,width,height,density);drawDoor(canvas,width,height,density);
            float dw=doorWidth(width,height),dh=doorHeight(width,height),left=width*.28f-dw*.5f;
            clearLeft=left+density*3;
            clearRight=left+dw-dw*(float)Math.cos(motion.doorOpen*1.40f)-density*3;doorTop=height*.50f-dh*.5f+density*3;doorBottom=height*.50f+dh*.5f-density*3;
            insideDoor=phase>=.62f && fitsDoorOpening();
        }
        else if(motion.punctured)drawKnifeLayer(canvas,false,width,height);
        paint.setAlpha(Math.round(255*motion.alpha));
        int save=canvas.save();
        if(door && insideDoor)canvas.clipRect(clearLeft,doorTop,clearRight,doorBottom);
        if(motion.backFacing) {
            int bottom=ROWS*(COLS+1)*2;
            vertices[0]=mesh[0];vertices[1]=mesh[1];vertices[2]=mesh[COLS*2];vertices[3]=mesh[COLS*2+1];
            vertices[4]=mesh[bottom];vertices[5]=mesh[bottom+1];vertices[6]=mesh[bottom+COLS*2];vertices[7]=mesh[bottom+COLS*2+1];
            canvas.drawBitmapMesh(backArt,1,1,vertices,0,null,0,paint);
        } else canvas.drawBitmapMesh(photo,COLS,ROWS,mesh,0,meshColors,0,paint);
        if(!door && motion.punctured && inkPhoto!=null) {
            paint.setAlpha(Math.round(255*motion.alpha*motion.penetration));
            canvas.drawBitmapMesh(inkPhoto,COLS,ROWS,mesh,0,meshColors,0,paint);
        }
        canvas.restoreToCount(save);
        if(door){if(insideDoor){drawDoor(canvas,width,height,density);drawDoorJamb(canvas,width,height,density);}}
        else {
            if(motion.punctured)drawPaperTear(canvas,density);
            drawKnifeLayer(canvas,true,width,height);
        }
        paint.setAlpha(255);
    }
    static float doorWidth(float w,float h){return Math.min(w*.38f,h*.25f);}
    static float doorHeight(float w,float h){return doorWidth(w,h)*2.23f;}
    private void drawRoom(Canvas canvas,float width,float height,float density) {
        float w=doorWidth(width,height),h=doorHeight(width,height),x=width*.28f,y=height*.50f;
        trim.setColor(0xFF111A20);canvas.drawRoundRect(x-w*.5f-density*7,y-h*.5f-density*7,x+w*.5f+density*7,y+h*.5f+density*7,density*3,density*3,trim);
        trim.setColor(0xFF02070B);canvas.drawRect(x-w*.5f,y-h*.5f,x+w*.5f,y+h*.5f,trim);
        // A recessed threshold and inner jamb establish depth behind the leaf.
        trim.setColor(0xFF26444E);canvas.drawRect(x-w*.5f+density*4,y+h*.5f-density*7,x+w*.5f-density*4,y+h*.5f,trim);
    }
    private void drawDoor(Canvas canvas,float width,float height,float density) {
        float w=doorWidth(width,height),h=doorHeight(width,height),left=width*.28f-w*.5f,top=height*.50f-h*.5f;
        float open=motion.doorOpen,right=left+w,far=right-w*(float)Math.cos(open*1.40f),inset=w*.22f*open;
        doorVertices[0]=right;doorVertices[1]=top;doorVertices[2]=far;doorVertices[3]=top+inset;
        doorVertices[4]=right;doorVertices[5]=top+h;doorVertices[6]=far;doorVertices[7]=top+h-inset;
        paint.setAlpha(255);canvas.drawBitmapMesh(doorArt,1,1,doorVertices,0,null,0,paint);
    }
    private void buildPhotoMesh(boolean door) {
        float cos=(float)Math.cos(motion.angle),sin=(float)Math.sin(motion.angle);
        float yawCos=(float)Math.cos(motion.yaw),yawSin=(float)Math.sin(motion.yaw);
        for(int row=0;row<=ROWS;row++)for(int col=0;col<=COLS;col++) {
            int n=row*(COLS+1)+col;float u=col/(float)COLS,v=row/(float)ROWS;
            float perspective=1/(1+(u-.5f)*yawSin*.40f);
            float pu=(u-.5f)*yawCos*perspective,pv=(v-.5f)*perspective;
            float x=((anchor[2]-anchor[0])*pu+(anchor[4]-anchor[0])*pv)*motion.scale;
            float y=((anchor[3]-anchor[1])*pu+(anchor[5]-anchor[1])*pv)*motion.scale;
            float du=(u-hitU),dv=(v-hitV);
            float bend=(float)Math.exp(-(du*du+dv*dv)*45)*motion.penetration;
            // The punctured stock buckles inward around the steel, rather than staying a rigid overlay.
            y+=bend*16;
            mesh[n*2]=cx+motion.dx+x*cos-y*sin;mesh[n*2+1]=cy+motion.dy+x*sin+y*cos;
            float hole=du*du/(.038f*.038f)+dv*dv/(.033f*.033f);
            int alpha=!door && motion.punctured && hole<motion.penetration?0:255;
            float shade=1-.16f*Math.abs(yawSin)-bend*.13f;
            int tone=Math.round(255*shade);meshColors[n]=(alpha<<24)|(tone<<16)|(tone<<8)|tone;
        }
        // Actual UV contact point shared by the hole, paper folds and blade split plane.
        int col=Math.round(hitU*COLS),row=Math.round(hitV*ROWS),n=row*(COLS+1)+col;
        pinX=mesh[n*2];pinY=mesh[n*2+1];
    }
    private boolean fitsDoorOpening() {
        for(int i=0;i<mesh.length;i+=2)if(mesh[i]<clearLeft || mesh[i]>clearRight || mesh[i+1]<doorTop || mesh[i+1]>doorBottom)return false;
        return true;
    }
    private void drawDoorJamb(Canvas canvas,float width,float height,float density) {
        float w=doorWidth(width,height),h=doorHeight(width,height),l=width*.28f-w*.5f,t=height*.50f-h*.5f;
        trim.setColor(0xFF18242A);
        canvas.drawRect(l-density*6,t-density*6,l,t+h+density*6,trim);
        canvas.drawRect(l+w,t-density*6,l+w+density*6,t+h+density*6,trim);
        canvas.drawRect(l-density*6,t-density*6,l+w+density*6,t,trim);
    }
    private void prepareKnife(float width,float height,float density) {
        float photoWidth=(float)Math.hypot(anchor[2]-anchor[0],anchor[3]-anchor[1]);
        float photoHeight=(float)Math.hypot(anchor[4]-anchor[0],anchor[5]-anchor[1]);
        // Fixed overscan keeps the sleeve's physical root beyond the RIGHT edge throughout the Keep strike.
        float maxPinX=width-cx+width*.18f+photoWidth*.5f;
        knifeSize=Math.max(photoWidth*1.90f,(maxPinX+density*55)/.66f);
        float knifeHeight=knifeSize*handArt.getHeight()/(float)handArt.getWidth();
        float angle=1.50f+motion.angle;bladeX=-(float)Math.cos(angle);bladeY=(float)Math.sin(angle);
        float reach=(1-motion.approach)*Math.min(width*.24f,height*.15f);
        float depth=Math.min(knifeHeight*.54f,Math.max(knifeHeight*.46f,(1-hitV)*photoHeight+density*20))*motion.penetration;
        knifeX=pinX+bladeX*(depth-reach);knifeY=pinY+bladeY*(depth-reach);
        float cos=(float)Math.cos(motion.angle),sin=(float)Math.sin(motion.angle);
        armRootX=knifeX+knifeSize*.747f*cos-knifeHeight*.670f*sin;
    }
    private void drawKnifeLayer(Canvas canvas,boolean front,float width,float height) {
        if(!motion.handVisible)return;
        int save=canvas.save();
        if(front && motion.punctured) {
            // Only the hand, handle and blade BEFORE entry remain in front of the paper.
            // The tip is drawn behind it, and visibly emerges beyond the far edge.
            float span=Math.max(width,height)*4,px=-bladeY,py=bladeX;
            knifeClip.reset();knifeClip.moveTo(pinX+px*span,pinY+py*span);
            knifeClip.lineTo(pinX-px*span,pinY-py*span);
            knifeClip.lineTo(pinX-px*span-bladeX*span,pinY-py*span-bladeY*span);
            knifeClip.lineTo(pinX+px*span-bladeX*span,pinY+py*span-bladeY*span);knifeClip.close();canvas.clipPath(knifeClip);
        }
        float knifeHeight=knifeSize*handArt.getHeight()/(float)handArt.getWidth();
        target.set(knifeX-knifeSize*.747f,knifeY-knifeHeight*.970f,knifeX+knifeSize*.253f,knifeY+knifeHeight*.030f);
        canvas.rotate((float)Math.toDegrees(motion.angle),knifeX,knifeY);
        canvas.scale(-1,1,knifeX,knifeY);
        paint.setAlpha(Math.round(255*motion.approach*motion.alpha));canvas.drawBitmap(handArt,null,target,paint);canvas.restoreToCount(save);
    }
    private void drawPaperTear(Canvas canvas,float density) {
        float spread=density*(2+5*motion.penetration),px=-bladeY,py=bladeX;
        trim.setStyle(Paint.Style.FILL);trim.setColor(0xFF24272A);trim.setAlpha(Math.round(255*motion.alpha));
        slit.reset();slit.moveTo(pinX+px*spread,pinY+py*spread);
        slit.lineTo(pinX+bladeX*spread*2.4f,pinY+bladeY*spread*2.4f);
        slit.lineTo(pinX-px*spread,pinY-py*spread);slit.close();canvas.drawPath(slit,trim);
        // Two lifted lips expose the matte reverse at the puncture.
        for(int side=-1;side<=1;side+=2) {
            trim.setColor(side<0?0xFF979D96:0xFFCBC9BD);trim.setAlpha(Math.round(255*motion.alpha));
            slit.reset();slit.moveTo(pinX+side*px*spread,pinY+side*py*spread);
            slit.lineTo(pinX+side*px*spread*1.55f+bladeX*spread,pinY+side*py*spread*1.55f+bladeY*spread);
            slit.lineTo(pinX+bladeX*spread*2.4f,pinY+bladeY*spread*2.4f);slit.close();canvas.drawPath(slit,trim);
        }
    }
}
