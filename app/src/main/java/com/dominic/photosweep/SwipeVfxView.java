package com.dominic.photosweep;

import android.content.Context;
import android.graphics.*;
import android.os.SystemClock;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import java.util.Random;

/** Wordless photo feedback: feed actual photo chips left; bathe kept photos in light. */
class SwipeVfxView extends View {
    interface Sprites { Bitmap get(boolean keep); }
    float progress;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final RectF photo = new RectF(), target = new RectF();
    private final Path path = new Path();
    private final boolean enabled, reducedMotion;
    private final RectF bladeRect = new RectF();
    private static Bitmap bladeArt;
    private final Bitmap sawBlade;
    private final RectF sleeveRect = new RectF(), fitPhoto = new RectF();
    private final Path lightPath = new Path(), ribbonPath = new Path();
    private final PathMeasure measure = new PathMeasure();
    private final float[] position = new float[2];
    private final Path visiblePath = new Path(), localClip = new Path();
    private final Matrix inverse = new Matrix();
    private static Bitmap glowArt, rayArt;
    private final Path biteMask=new Path(), biteEdge=new Path();
    private final float[] biteNoise=new float[64];
    private float deepestBite;
    private long mendStarted;
    private View clippedPhoto;
    private float lightAlpha;
    private long geometryFrame;

    /** The photo and light use the very same aperture, including during a cancelled drag. */
    static class Card extends FrameLayout {
        SwipeVfxView effect;
        Card(Context context) { super(context); }
        @Override protected void dispatchDraw(Canvas canvas) {
            int save=canvas.save();
            if(effect!=null)effect.clipPhoto(canvas,this);
            super.dispatchDraw(canvas);canvas.restoreToCount(save);
        }
    }
    static class PreviewPhoto extends ImageView {
        SwipeVfxView effect;
        PreviewPhoto(Context context) { super(context); }
        @Override protected void onDraw(Canvas canvas) {
            int save=canvas.save();
            if(effect!=null)effect.clipPhoto(canvas,this);
            super.onDraw(canvas);canvas.restoreToCount(save);
        }
    }
    private boolean holding;
    private long returning, closing, lastFrame;
    private float restRight, open, returnOpen, returnAlpha;
    private float haloX, haloY;
    private final int intensity, speed;
    private final Random random = new Random();
    private final Chip[] chips = new Chip[320];
    private Bitmap bitmap;
    private long released, started, lastPreviewChip;
    private float lastLeft, feed;
    private boolean tracked;
    private int nextChip;
    private static class Chip {
        Bitmap bitmap;
        final Rect source = new Rect();
        float x,y,vx,vy,size,angle,spin;
        long born;
    }
    SwipeVfxView(Context context, Sprites sprites, String flag, SwipeTheme.Kind kind, boolean enabled,
                 int intensity, int speed, boolean reducedMotion) {
        super(context); this.enabled=enabled; this.intensity=intensity; this.speed=speed;
        this.reducedMotion=reducedMotion;
        // Share immutable textures; decode once at bounded sizes, never in a draw frame.
        BitmapFactory.Options options=new BitmapFactory.Options();options.inScaled=false;options.inSampleSize=4;
        if(bladeArt==null || bladeArt.isRecycled())
            bladeArt=BitmapFactory.decodeResource(context.getResources(),R.drawable.grinder_saw_blade,options);
        sawBlade=bladeArt;
        if(glowArt==null) {
            glowArt=Bitmap.createBitmap(96,96,Bitmap.Config.ARGB_8888);
            Canvas glowCanvas=new Canvas(glowArt);Paint soft=new Paint(Paint.ANTI_ALIAS_FLAG);
            soft.setShader(new RadialGradient(48,48,48,
                    new int[]{0xFFFFF8E5,0xCCFFE4A4,0x55EAB761,0x14D58E36,0x00D58E36},
                    new float[]{0,.08f,.28f,.56f,1},Shader.TileMode.CLAMP));
            glowCanvas.drawCircle(48,48,48,soft);
        }
        if(rayArt==null) {
            // Small soft beam texture, generated once; no shaders or bitmaps allocated per frame.
            int w=160,h=96;int[] pixels=new int[w*h];
            for(int x=0;x<w;x++) {
                float t=x/(float)(w-1),spread=2+36*(float)Math.pow(t,.8);
                float lengthFade=(float)Math.pow(Math.sin(Math.PI*t),.85)*(1-t);
                for(int y=0;y<h;y++) {
                    float d=(y-h*.5f)/spread;
                    int a=(int)(210*lengthFade*Math.exp(-d*d*3.2));
                    pixels[y*w+x]=(a<<24)|(255<<16)|((int)(246-46*t)<<8)|(int)(216-85*t);
                }
            }
            rayArt=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);rayArt.setPixels(pixels,0,w,0,0,w,h);
        }
        for(int i=0;i<chips.length;i++)chips[i]=new Chip();
        resetBites();
        setClickable(false);setFocusable(false);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }
    private float dp(float value) { return value*getResources().getDisplayMetrics().density; }
    protected void onGrinderShake(float offset) { }
    float grinderEdge() { return dp(14); }
    private void resetBites() {
        deepestBite=0;mendStarted=0;
        for(int i=0;i<biteNoise.length;i++)biteNoise[i]=random.nextFloat();
    }
    void setPhoto(Bitmap image, RectF bounds, float dx, float dy) {
        bitmap=image;photo.set(bounds);photo.offset(dx,dy);
        if(!tracked)restRight=bounds.right;
        float edge=grinderEdge(),width=Math.max(1,photo.width());
        float oldCut=Math.max(0,Math.min(width,edge-lastLeft));
        float newCut=Math.max(0,Math.min(width,edge-photo.left));
        float travel=tracked?Math.max(0,newCut-oldCut):0;
        lastLeft=photo.left;tracked=true;
        if(enabled && !reducedMotion && progress<0)deepestBite=SwipeMotion.deepestBite(deepestBite,newCut,width);
        if(enabled && !reducedMotion && progress<0 && travel>0) {
            feed+=travel/dp(1.65f)*(1.1f+intensity/100f);
            int count=Math.min(48,(int)feed);feed=Math.min(48,feed-count);
            for(int i=0;i<count;i++)spawnChip();
        } else feed=0;
        invalidate();
    }
    void hold(float x,float y) {
        clearChips();resetBites();onGrinderShake(0);released=0;returning=0;closing=0;
        started=SystemClock.uptimeMillis();lastFrame=started;progress=0;open=0;tracked=false;holding=true;geometryFrame=0;sleeveRect.setEmpty();invalidate();
    }
    void follow(float x,float y) { if(progress>=0)onGrinderShake(0);invalidate(); }
    void release(boolean keep) { onGrinderShake(0);holding=false;returning=0;progress=keep?1:-1;released=SystemClock.uptimeMillis();if(keep)mendStarted=released;invalidate(); }
    void retreat() {
        onGrinderShake(0);mendStarted=SystemClock.uptimeMillis();returnOpen=open;
        returnAlpha=SwipeMotion.ease((SystemClock.uptimeMillis()-started)/140f)*(1-SwipeMotion.clamp(-progress*4));
        holding=false;released=0;closing=0;returning=SystemClock.uptimeMillis();progress=0;invalidate();
    }
    void beginKeepClose() { returnOpen=open;closing=SystemClock.uptimeMillis();invalidate(); }
    void cancel() {
        onGrinderShake(0);holding=false;returning=0;closing=0;open=0;
        progress=0;released=0;tracked=false;deepestBite=0;mendStarted=0;bitmap=null;photo.setEmpty();sleeveRect.setEmpty();visiblePath.reset();geometryFrame=0;clearChips();
        if(clippedPhoto!=null){clippedPhoto.invalidate();clippedPhoto=null;}invalidate();
    }
    @Override protected void onDetachedFromWindow() { cancel();super.onDetachedFromWindow(); }
    private void clearChips() { for(Chip chip:chips){chip.born=0;chip.bitmap=null;}feed=0; }
    private void spawnChip() {
        Chip chip=chips[nextChip++%chips.length];chip.born=SystemClock.uptimeMillis();
        chip.x=grinderEdge();chip.y=photo.top+photo.height()*(.03f+random.nextFloat()*.94f);
        chip.size=dp(1.4f+random.nextFloat()*6.6f);chip.vx=dp(65+random.nextFloat()*205);
        chip.vy=dp(-115+random.nextFloat()*175);chip.angle=random.nextFloat()*180;
        chip.spin=-540+random.nextFloat()*1080;chip.bitmap=bitmap;
        if(bitmap!=null && !bitmap.isRecycled()) {
            int bw=bitmap.getWidth(),bh=bitmap.getHeight();
            int sx=Math.max(0,Math.min(bw-1,(int)((grinderEdge()-photo.left)/Math.max(1,photo.width())*bw)));
            int sy=Math.max(0,Math.min(bh-1,(int)((chip.y-photo.top)/Math.max(1,photo.height())*bh)));
            int side=Math.max(1,(int)(chip.size/Math.max(1,photo.width())*bw));
            chip.source.set(sx,sy,Math.min(bw,sx+side),Math.min(bh,sy+side));
        }
    }
    @Override protected void onDraw(Canvas canvas) {
        if(getWidth()==0 || getHeight()==0)return;
        long now=SystemClock.uptimeMillis();if(started==0)started=now;
        float time=(now-started)/1000f*speed/100f;
        float dt=lastFrame==0?16:Math.min(50,now-lastFrame);lastFrame=now;
        float phase=released==0?0:SwipeMotion.clamp((now-released)/(float)(reducedMotion?80:SwipeMotion.releaseDuration(speed,progress>0)));
        float amount=SwipeMotion.clamp(Math.abs(progress));
        float fade=released==0?1:1-SwipeMotion.ease(phase);
        if(released==0) {
            float proximity=progress<0&&!photo.isEmpty()?SwipeMotion.grinderProximity(photo.left,grinderEdge(),dp(110)):0;
            onGrinderShake(enabled&&!reducedMotion?dp(2.6f)*SwipeMotion.grinderShake((now-started)/1000f,proximity):0);
        }
        boolean lightVisible=holding || returning!=0 || (released!=0 && progress>0) || progress>0;
        int frame=canvas.save();canvas.clipRect(0,0,getWidth(),getHeight());
        if(lightVisible)drawHeavenly(canvas,time,dt,now);
        drawBiteEdge(canvas,now);
        if(SwipeMotion.intent(progress)<0)drawGrinder(canvas,time,amount,fade,now);
        if(enabled && !reducedMotion)drawChips(canvas,fade,now);
        canvas.restoreToCount(frame);
        paint.setShader(null);paint.setAlpha(255);paint.setStyle(Paint.Style.FILL);
        boolean tail=returning!=0 && now-returning<280;
        if(returning!=0 && !tail){returning=0;open=0;deepestBite=0;mendStarted=0;photo.setEmpty();bitmap=null;}
        if(isAttachedToWindow() && (holding || tail || hasChips(now) || (released!=0 && (progress>0 || phase<1)) || progress>0)) {
            if(clippedPhoto!=null)clippedPhoto.postInvalidateOnAnimation();
            postInvalidateOnAnimation();
        }
    }
    float keepMouthX() { return getWidth()-dp(28); }
    float keepClipEdge() { preparePocket(SystemClock.uptimeMillis());return sleeveRect.left; }
    private void clipPhoto(Canvas canvas,View view) {
        clippedPhoto=view;
        if(buildBiteMask(SystemClock.uptimeMillis()))clipStagePath(canvas,view,biteMask);
        if(!(progress>0 || returning!=0 || closing!=0))return;
        preparePocket(SystemClock.uptimeMillis());
        if(open<.001f || visiblePath.isEmpty())return;
        clipStagePath(canvas,view,visiblePath);
    }
    private void clipStagePath(Canvas canvas,View view,Path stagePath) {
        localClip.set(stagePath);localClip.offset(-view.getLeft(),-view.getTop());
        if(view.getMatrix().invert(inverse)) {
            localClip.transform(inverse);canvas.clipPath(localClip);
        }
    }
    private boolean buildBiteMask(long now) {
        if(photo.isEmpty() || deepestBite<=0 || reducedMotion || !enabled)return false;
        long frame=getDrawingTime();if(frame>0)now=frame;
        float recovery=mendStarted==0?1:1-SwipeMotion.ease((now-mendStarted)/280f);
        if(recovery<=0)return false;
        float cut=deepestBite*photo.width()*recovery;
        float roughness=dp(8)*Math.min(1,cut/dp(12))*recovery;
        int segments=Math.max(8,Math.min(62,(int)(photo.height()/dp(8))));
        biteEdge.reset();biteMask.reset();
        float x=photo.left+cut;
        biteMask.moveTo(x,-getHeight()*4f);biteMask.lineTo(x,photo.top);
        biteEdge.moveTo(x,photo.top);
        for(int i=1;i<segments;i++) {
            float y=photo.top+photo.height()*i/segments;
            float tooth=(i%3==0?.08f:.35f+.65f*biteNoise[i]);
            x=Math.min(photo.right,photo.left+cut+roughness*tooth);
            biteMask.lineTo(x,y);biteEdge.lineTo(x,y);
        }
        x=photo.left+cut;biteMask.lineTo(x,photo.bottom);biteEdge.lineTo(x,photo.bottom);
        biteMask.lineTo(x,getHeight()*4f);biteMask.lineTo(getWidth()*4f,getHeight()*4f);
        biteMask.lineTo(getWidth()*4f,-getHeight()*4f);biteMask.close();
        return true;
    }
    private void drawBiteEdge(Canvas canvas,long now) {
        if(!buildBiteMask(now))return;
        int save=canvas.save();
        if(progress>0 && open>.001f)canvas.clipPath(visiblePath);
        paint.setShader(null);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(dp(.65f));
        paint.setColor(0xFFEEE0C7);paint.setAlpha((int)(85*(mendStarted==0?1:1-SwipeMotion.ease((now-mendStarted)/280f))));
        canvas.drawPath(biteEdge,paint);paint.setStyle(Paint.Style.FILL);canvas.restoreToCount(save);
    }
    private boolean hasChips(long now) {
        if(!enabled || reducedMotion)return false;
        for(Chip chip:chips)if(chip.born!=0 && now-chip.born<1050)return true;
        return false;
    }
    private void preparePocket(long now) {
        // Touch, clipping and foreground drawing share one geometry sample per frame.
        long frame=getDrawingTime();if(frame<=0)frame=now;
        if(geometryFrame==frame)return;
        float dt=geometryFrame==0?16:Math.min(50,Math.max(0,frame-geometryFrame));geometryFrame=frame;
        lightAlpha=holding?SwipeMotion.ease((now-started)/160f):1;
        float wanted=photo.isEmpty()?Math.max(0,progress):
                SwipeMotion.pocketOpening(restRight,photo.right,keepMouthX()-dp(16),dp(70));
        if(progress<0)wanted=0;
        if(released!=0 && progress>0)wanted=Math.max(wanted,open);
        if(returning!=0) {
            open=returnOpen*(1-SwipeMotion.ease((now-returning)/220f));
            lightAlpha=returnAlpha*(1-SwipeMotion.ease((now-returning)/280f));
        } else if(closing!=0) {
            open=returnOpen*(1-SwipeMotion.ease((now-closing)/220f));
            lightAlpha=1-SwipeMotion.ease(Math.max(0,now-closing-100)/180f);
        } else open+= (wanted-open)*SwipeMotion.followFactor(dt,32);
        lightAlpha*=1-SwipeMotion.ease(SwipeMotion.clamp(-progress*4));
        haloX=getWidth();haloY=photo.isEmpty()?getHeight()*.5f:photo.centerY();
        haloY=Math.max(dp(12),Math.min(getHeight()-dp(12),haloY));
        if(photo.isEmpty())fitPhoto.set(getWidth()*.18f,getHeight()*.25f,getWidth()*.82f,getHeight()*.75f);
        else fitPhoto.set(photo);
        float o=SwipeMotion.clamp(open),mouth=getWidth()-dp(28)*o;
        float top=haloY+(fitPhoto.top-haloY-dp(9))*o;
        float bottom=haloY+(fitPhoto.bottom-haloY+dp(9))*o;
        sleeveRect.set(mouth,top,getWidth()+dp(8)*o,bottom);
        float h=Math.max(1,bottom-top),right=sleeveRect.right;
        lightPath.reset();lightPath.moveTo(right,top);
        lightPath.cubicTo(mouth+dp(8)*o,top,mouth,top+h*.12f,mouth,top+h*.32f);
        lightPath.lineTo(mouth,bottom-h*.32f);
        lightPath.cubicTo(mouth,bottom-h*.12f,mouth+dp(8)*o,bottom,right,bottom);
        visiblePath.set(lightPath);
        visiblePath.lineTo(getWidth(),getHeight()*4f);visiblePath.lineTo(-getWidth()*4f,getHeight()*4f);
        visiblePath.lineTo(-getWidth()*4f,-getHeight()*4f);visiblePath.lineTo(getWidth(),-getHeight()*4f);
        visiblePath.lineTo(right,top);visiblePath.close();
    }
    private void drawHeavenly(Canvas canvas,float time,float dt,long now) {
        preparePocket(now);
        boolean animate=enabled&&!reducedMotion;
        float o=SwipeMotion.clamp(open),alpha=lightAlpha;
        paint.setShader(null);paint.setStyle(Paint.Style.FILL);
        if(o>.006f && alpha>.001f) {
            float top=sleeveRect.top,bottom=sleeveRect.bottom,h=bottom-top,mouth=sleeveRect.left;
            // Diffuse light is concentrated on the receiving seam, not a panel over the image.
            glow(canvas,mouth+dp(7)*o,haloY,dp(25)*o,h*.60f,alpha*.40f);
            if(animate)drawPortalRays(canvas,time,o,alpha);
            glowStroke(canvas,lightPath,alpha,1.15f);
            if(animate) {
                pathParticles(canvas,lightPath,time,.16f,36+intensity/8,alpha*o);
                drawRibbon(canvas,-1,o,alpha,time);
                drawRibbon(canvas,1,o,alpha,time);
                // Staggered curved motes drift into the seam; no fence of identical straight rays.
                for(int i=0;i<18;i++) {
                    float u=(time*(.20f+(i%5)*.027f)+i*.618034f)%1;
                    float v=.07f+.86f*((i*.381966f)%1);
                    float reach=dp(12+(i*17)%35)*o;
                    float x=mouth-reach*(1-u)*(1-u);
                    float y=top+h*v+dp(5)*o*(float)Math.sin(u*Math.PI+i);
                    float a=alpha*o*(float)Math.pow(Math.sin(Math.PI*u),2)*.60f;
                    glow(canvas,x,y,dp(3.5f),dp(3.5f),a*.55f);
                    if(i%5==0)sparkle(canvas,x,y,dp(.9f),a);
                    else { paint.setColor(i%3==0?0xFFFFD18C:0xFFFFF7DC);paint.setAlpha((int)(210*a));canvas.drawCircle(x,y,dp(.5f+(i%3)*.2f),paint); }
                }
            }
        }
        float dot=alpha*(1-SwipeMotion.ease(Math.min(1,o*4)));
        glow(canvas,haloX-dp(1.5f),haloY,dp(19),dp(22),dot*.7f);
        paint.setColor(0xFFFFF8E8);paint.setAlpha((int)(245*dot));canvas.drawCircle(haloX-dp(1),haloY,dp(1.6f),paint);
        if(animate)for(int i=0;i<4;i++) {
            float t=(time*.32f+i*.25f)%1,angle=i*2.4f+time*.18f;
            float reach=dp(4+13*t),x=haloX+(float)Math.cos(angle)*reach,y=haloY+(float)Math.sin(angle)*reach;
            sparkle(canvas,x,y,dp(.9f),dot*(float)Math.sin(Math.PI*t));
        }
    }
    private void drawPortalRays(Canvas canvas,float time,float opening,float alpha) {
        measure.setPath(lightPath,false);float length=measure.getLength();
        if(length<1)return;
        float strength=alpha*opening*Math.min(1.25f,.65f+intensity/200f);
        for(int i=0;i<9;i++) {
            float v=.08f+.84f*((i*.618034f+.13f)%1);
            if(!measure.getPosTan(length*v,position,null))continue;
            float pulse=.40f+.60f*(float)Math.pow(Math.sin(time*(.43f+i*.043f)+i*2.1f),2);
            float angle=180-(v-.5f)*48+4*(float)Math.sin(time*.37f+i);
            float reach=Math.min(getWidth()*.48f,dp(84+(i*29)%65))*opening;
            float width=dp(18+(i*13)%26)*opening;
            int save=canvas.save();canvas.rotate(angle,position[0],position[1]);
            target.set(position[0],position[1]-width*.5f,position[0]+reach,position[1]+width*.5f);
            paint.setShader(null);paint.setStyle(Paint.Style.FILL);paint.setColor(Color.WHITE);paint.setAlpha((int)(195*strength*pulse));
            canvas.drawBitmap(rayArt,null,target,paint);canvas.restoreToCount(save);
        }
        // Each glint has its own lifetime and direction, streaming out towards the photo.
        for(int i=0;i<18;i++) {
            float u=(time*(.28f+(i%5)*.041f)+i*.618034f)%1;
            float v=.04f+.92f*((i*.381966f+.21f)%1);
            if(!measure.getPosTan(length*v,position,null))continue;
            float reach=dp(30+(i*23)%83)*opening;
            float x=position[0]-reach*u,y=position[1]+(v-.5f)*dp(54)*u*opening;
            float a=strength*(float)Math.pow(Math.sin(Math.PI*u),1.4)*(.45f+(i%3)*.13f);
            glow(canvas,x,y,dp(3+(i%3)),dp(3+(i%3)),a*.8f);
            if(i%3==0)sparkle(canvas,x,y,dp(1.1f+(i%4)*.25f),a);
            else {
                paint.setShader(null);paint.setStyle(Paint.Style.STROKE);paint.setStrokeCap(Paint.Cap.ROUND);
                paint.setColor(0xFFFFE5AE);paint.setAlpha((int)(145*a));paint.setStrokeWidth(dp(.7f));
                canvas.drawLine(x,y,x+dp(3+u*6)*opening,y-(v-.5f)*dp(3)*opening,paint);paint.setStyle(Paint.Style.FILL);
            }
        }
    }
    private void drawRibbon(Canvas canvas,int side,float opening,float alpha,float time) {
        float edgeY=side<0?sleeveRect.top:sleeveRect.bottom,mouth=sleeveRect.left;
        float reach=Math.min(dp(54),fitPhoto.width()*.20f)*opening;
        float wave=dp(3)*(float)Math.sin(time*1.1f+side)*opening;
        ribbonPath.reset();ribbonPath.moveTo(mouth-reach,edgeY+side*(dp(5)*opening+wave));
        ribbonPath.cubicTo(mouth-reach*.45f,edgeY+side*dp(9)*opening,
                mouth-dp(2)*opening,edgeY-side*dp(5)*opening,getWidth()+dp(5),edgeY+side*dp(3)*opening);
        // The strand tapers into motes at its free end instead of ending in a hard horn.
        measure.setPath(ribbonPath,false);float length=measure.getLength();
        for(int i=0;i<34;i++) {
            float u=i/33f;if(!measure.getPosTan(length*u,position,null))continue;
            float pulse=.72f+.28f*(float)Math.sin(time*1.4f-u*7+side);
            glow(canvas,position[0],position[1],dp(2.2f),dp(2.2f),alpha*opening*u*pulse*.34f);
        }
        pathParticles(canvas,ribbonPath,time+(side<0?0:.7f),.30f,12,alpha*opening*.65f);
    }
    private void glow(Canvas canvas,float x,float y,float rx,float ry,float alpha) {
        if(rx<=0 || ry<=0 || alpha<=0)return;
        paint.setShader(null);paint.setStyle(Paint.Style.FILL);paint.setColor(Color.WHITE);
        paint.setAlpha((int)(255*SwipeMotion.clamp(alpha)));target.set(x-rx,y-ry,x+rx,y+ry);
        canvas.drawBitmap(glowArt,null,target,paint);
    }
    private void glowStroke(Canvas canvas,Path line,float alpha,float width) {
        paint.setShader(null);paint.setStyle(Paint.Style.STROKE);paint.setStrokeCap(Paint.Cap.ROUND);paint.setStrokeJoin(Paint.Join.ROUND);
        for(int layer=7;layer>=0;layer--) {
            paint.setColor(layer==0?0xFFFFF9E9:0xFFFFD48C);
            paint.setStrokeWidth(dp(width+layer*layer*.27f));
            paint.setAlpha((int)((layer==0?230:layer==1?66:layer==2?30:10)*SwipeMotion.clamp(alpha)));
            canvas.drawPath(line,paint);
        }
        paint.setStyle(Paint.Style.FILL);
    }
    private void pathParticles(Canvas canvas,Path line,float time,float rate,int count,float alpha) {
        measure.setPath(line,false);float length=measure.getLength();if(length<1)return;
        for(int i=0;i<count;i++) {
            float u=(time*rate+i*.618034f)%1;
            if(!measure.getPosTan(length*u,position,null))continue;
            float strength=alpha*(float)Math.sin(Math.PI*u)*(.4f+.6f*(float)Math.pow(Math.sin(time*1.2f+i*2.3f),2));
            if(i%7==0)sparkle(canvas,position[0],position[1],dp(1.15f),strength);
            else {paint.setColor(i%3==0?0xFFFFDDA1:0xFFFFFCEF);paint.setAlpha((int)(195*SwipeMotion.clamp(strength)));canvas.drawCircle(position[0],position[1],dp(.5f+i%3*.15f),paint);}
        }
    }
    private void sparkle(Canvas canvas,float x,float y,float size,float alpha) {
        paint.setColor(0xFFFFF6DF);paint.setAlpha((int)(220*SwipeMotion.clamp(alpha)));
        path.reset();path.moveTo(x,y-size*2);path.lineTo(x+size*.35f,y-size*.35f);
        path.lineTo(x+size*2,y);path.lineTo(x+size*.35f,y+size*.35f);
        path.lineTo(x,y+size*2);path.lineTo(x-size*.35f,y+size*.35f);
        path.lineTo(x-size*2,y);path.lineTo(x-size*.35f,y-size*.35f);path.close();canvas.drawPath(path,paint);
    }
    private void drawGrinder(Canvas canvas,float time,float amount,float fade,long now) {
        float top=photo.isEmpty()?getHeight()*.20f:Math.max(dp(5),photo.top);
        float bottom=photo.isEmpty()?getHeight()*.80f:Math.min(getHeight()-dp(5),photo.bottom);
        // Only transparent teeth are drawn here: no rectangular rail behind the saws.
        paint.setShader(null);paint.setStyle(Paint.Style.FILL);paint.setColor(Color.WHITE);paint.setAlpha((int)(235*SwipeMotion.ease(amount)*fade));
        int save=canvas.save();canvas.clipRect(0,top,dp(22),bottom);
        int index=0;
        for(float y=top;y<bottom+dp(34);y+=dp(49)) {
            float radius=dp(34),cx=-dp(16);
            int blade=canvas.save();
            canvas.rotate(enabled&&!reducedMotion?(time*115*(index%2==0?1:-1)+index*29)%360:0,cx,y);
            bladeRect.set(cx-radius,y-radius,cx+radius,y+radius);
            if(sawBlade!=null && !sawBlade.isRecycled())canvas.drawBitmap(sawBlade,null,bladeRect,paint);
            canvas.restoreToCount(blade);index++;
        }
        canvas.restoreToCount(save);
        if(!enabled || reducedMotion)return;
        // Preview has no real photo. Show paper-colored chips instead of fabricating a photo.
        if(bitmap==null && now-lastPreviewChip>=35) {
            lastPreviewChip=now;photo.set(0,getHeight()*.20f,getWidth()*.6f,getHeight()*.80f);
            for(int i=0;i<4;i++)spawnChip();
        }
    }
    private void drawChips(Canvas canvas,float fade,long now) {
        for(Chip chip:chips) {
            if(chip.born==0)continue;
            float age=(now-chip.born)/1000f;
            if(age>1.05f){chip.born=0;chip.bitmap=null;continue;}
            float x=chip.x+chip.vx*age,y=chip.y+chip.vy*age+dp(180)*age*age;
            float size=chip.size*(1-age*.55f);
            paint.setColor(0xFFFFE8CB);paint.setAlpha((int)(245*(1-age/1.05f)*fade));
            int piece=canvas.save();canvas.rotate(chip.angle+chip.spin*age,x,y);
            target.set(x-size,y-size*.65f,x+size,y+size*.65f);
            path.reset();path.moveTo(x-size,y-size*.25f);path.lineTo(x-size*.35f,y-size*.65f);
            path.lineTo(x+size,y-size*.4f);path.lineTo(x+size*.55f,y+size*.65f);path.lineTo(x-size*.7f,y+size*.4f);path.close();
            canvas.clipPath(path);
            if(chip.bitmap!=null && !chip.bitmap.isRecycled() && !chip.source.isEmpty())canvas.drawBitmap(chip.bitmap,chip.source,target,paint);
            else canvas.drawRect(target,paint);
            canvas.restoreToCount(piece);
        }
    }
}





