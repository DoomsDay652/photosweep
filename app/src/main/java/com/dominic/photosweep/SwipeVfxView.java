package com.dominic.photosweep;

import android.content.Context;
import android.graphics.*;
import android.os.SystemClock;
import android.view.View;
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
    private final Matrix veilMatrix = new Matrix();
    private final LinearGradient veil = new LinearGradient(0,0,1,0,
            new int[]{0x00FFF4D5,0x14FFF5DF,0x28FFFBEF},null,Shader.TileMode.CLAMP);
    private boolean holding;
    private long returning, closing, lastFrame;
    private float restRight, open, returnOpen, returnAlpha;
    private float haloX, haloY;
    private final int intensity, speed;
    private final Random random = new Random();
    private final Chip[] chips = new Chip[224];
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
        BitmapFactory.Options options=new BitmapFactory.Options();options.inScaled=false;options.inSampleSize=2;
        options.inSampleSize=4;
        if(bladeArt==null || bladeArt.isRecycled())
            bladeArt=BitmapFactory.decodeResource(context.getResources(),R.drawable.grinder_saw_blade,options);
        sawBlade=bladeArt;
        for(int i=0;i<chips.length;i++)chips[i]=new Chip();
        setClickable(false);setFocusable(false);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }
    private float dp(float value) { return value*getResources().getDisplayMetrics().density; }
    protected void onGrinderShake(float offset) { }
    void setPhoto(Bitmap image, RectF bounds, float dx, float dy) {
        bitmap=image;photo.set(bounds);photo.offset(dx,dy);
        if(!tracked)restRight=photo.right;
        float edge=dp(9),width=Math.max(1,photo.width());
        float oldCut=Math.max(0,Math.min(width,edge-lastLeft));
        float newCut=Math.max(0,Math.min(width,edge-photo.left));
        float travel=tracked?Math.max(0,newCut-oldCut):0;
        lastLeft=photo.left;tracked=true;
        if(enabled && !reducedMotion && progress<0 && travel>0) {
            feed+=travel/dp(2.3f)*(1.1f+intensity/100f);
            int count=Math.min(36,(int)feed);feed=Math.min(36,feed-count);
            for(int i=0;i<count;i++)spawnChip();
        } else feed=0;
        invalidate();
    }
    void hold(float x,float y) {
        clearChips();onGrinderShake(0);released=0;returning=0;closing=0;
        started=SystemClock.uptimeMillis();lastFrame=started;progress=0;open=0;tracked=false;holding=true;sleeveRect.setEmpty();invalidate();
    }
    void follow(float x,float y) { if(progress>=0){clearChips();onGrinderShake(0);}invalidate(); }
    void release(boolean keep) { onGrinderShake(0);holding=false;returning=0;progress=keep?1:-1;released=SystemClock.uptimeMillis();invalidate(); }
    void retreat() {
        onGrinderShake(0);clearChips();returnOpen=open;
        returnAlpha=SwipeMotion.ease((SystemClock.uptimeMillis()-started)/140f)*(1-SwipeMotion.clamp(-progress*4));
        holding=false;released=0;closing=0;returning=SystemClock.uptimeMillis();progress=0;invalidate();
    }
    void beginKeepClose() { closing=SystemClock.uptimeMillis();invalidate(); }
    void cancel() {
        onGrinderShake(0);holding=false;returning=0;closing=0;open=0;
        progress=0;released=0;tracked=false;bitmap=null;photo.setEmpty();sleeveRect.setEmpty();clearChips();invalidate();
    }
    @Override protected void onDetachedFromWindow() { cancel();super.onDetachedFromWindow(); }
    private void clearChips() { for(Chip chip:chips){chip.born=0;chip.bitmap=null;}feed=0; }
    private void spawnChip() {
        Chip chip=chips[nextChip++%chips.length];chip.born=SystemClock.uptimeMillis();
        chip.x=dp(9);chip.y=photo.top+photo.height()*(.03f+random.nextFloat()*.94f);
        chip.size=dp(2+random.nextFloat()*6);chip.vx=dp(60+random.nextFloat()*185);
        chip.vy=dp(-115+random.nextFloat()*175);chip.angle=random.nextFloat()*180;
        chip.spin=-540+random.nextFloat()*1080;chip.bitmap=bitmap;
        if(bitmap!=null && !bitmap.isRecycled()) {
            int bw=bitmap.getWidth(),bh=bitmap.getHeight();
            int sx=Math.max(0,Math.min(bw-1,(int)((dp(9)-photo.left)/Math.max(1,photo.width())*bw)));
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
            float proximity=progress<0&&!photo.isEmpty()?SwipeMotion.grinderProximity(photo.left,dp(9),dp(110)):0;
            onGrinderShake(enabled&&!reducedMotion?dp(2.6f)*SwipeMotion.grinderShake((now-started)/1000f,proximity):0);
        }
        boolean lightVisible=holding || returning!=0 || (released!=0 && progress>0) || progress>0;
        int frame=canvas.save();canvas.clipRect(0,0,getWidth(),getHeight());
        if(lightVisible)drawHeavenly(canvas,time,dt,now);
        if(SwipeMotion.intent(progress)<0)drawGrinder(canvas,time,amount,fade,now);
        canvas.restoreToCount(frame);
        paint.setShader(null);paint.setAlpha(255);paint.setStyle(Paint.Style.FILL);
        boolean tail=returning!=0 && now-returning<280;
        if(returning!=0 && !tail){returning=0;open=0;photo.setEmpty();bitmap=null;}
        if(isAttachedToWindow() && (holding || tail || (released!=0 && (progress>0 || phase<1)) || progress>0))
            postInvalidateOnAnimation();
    }
    private void drawHeavenly(Canvas canvas,float time,float dt,long now) {
        boolean animate=enabled&&!reducedMotion;
        float alpha=holding?SwipeMotion.ease((now-started)/140f):1;
        float wanted=photo.isEmpty()?Math.max(0,progress):
                SwipeMotion.pocketOpening(restRight,photo.right,getWidth()-dp(7),dp(70));
        if(progress<0)wanted=0;
        if(released!=0 && progress>0)wanted=Math.max(wanted,open);
        if(returning!=0) {
            wanted=returnOpen*(1-SwipeMotion.ease((now-returning)/180f));
            alpha=returnAlpha*(1-SwipeMotion.ease(Math.max(0,now-returning-180)/100f));open=wanted;
        } else if(closing!=0) {
            open=1-SwipeMotion.ease((now-closing)/180f);
            alpha=1-SwipeMotion.ease(Math.max(0,now-closing-180)/100f);
        } else open+= (wanted-open)*(reducedMotion?1:1-(float)Math.exp(-dt/38f));
        alpha*=1-SwipeMotion.clamp(-progress*4);
        haloX=getWidth()-dp(7);haloY=photo.isEmpty()?getHeight()*.5f:photo.centerY();
        haloY=Math.max(dp(20),Math.min(getHeight()-dp(20),haloY));
        if(photo.isEmpty()) {
            float shift=Math.max(0,progress)*getWidth()*.36f;
            fitPhoto.set(getWidth()*.16f+shift,getHeight()*.32f,getWidth()*.78f+shift,getHeight()*.68f);
        } else fitPhoto.set(photo);
        float o=SwipeMotion.clamp(open);
        paint.setShader(null);paint.setStyle(Paint.Style.FILL);
        if(o>.006f) {
            float right=Math.min(haloX,fitPhoto.right);
            if(fitPhoto.left<right) {
                float left=SwipeMotion.keepSleeveLeft(fitPhoto.left,fitPhoto.width(),right,o);
                sleeveRect.set(left,haloY+(fitPhoto.top-haloY)*o,right,haloY+(fitPhoto.bottom-haloY)*o);
            } else if(!sleeveRect.isEmpty() && closing!=0) {
                sleeveRect.top=haloY+(fitPhoto.top-haloY)*o;
                sleeveRect.bottom=haloY+(fitPhoto.bottom-haloY)*o;
            }
            if(!sleeveRect.isEmpty()) {
                float corner=Math.min(dp(9),Math.min(sleeveRect.width(),sleeveRect.height())*.22f);
                veilMatrix.setScale(Math.max(1,sleeveRect.width()),1);veilMatrix.postTranslate(sleeveRect.left,0);
                veil.setLocalMatrix(veilMatrix);paint.setShader(veil);paint.setAlpha((int)(255*alpha));
                canvas.drawRoundRect(sleeveRect,corner,corner,paint);paint.setShader(null);
                lightPath.reset();lightPath.addRoundRect(sleeveRect,corner,corner,Path.Direction.CW);
                glowStroke(canvas,lightPath,alpha,.85f);
                if(animate)pathParticles(canvas,lightPath,time,.14f,12+intensity/10,alpha);
                drawRibbon(canvas,-1,o,alpha,time,animate);
                drawRibbon(canvas,1,o,alpha,time,animate);
            }
        }
        // The hold dot is the seed of the opening; its opacity is independent of proximity.
        float dot=alpha*(1-SwipeMotion.ease(Math.min(1,o*3)));
        paint.setColor(0xFFFFD9A0);paint.setAlpha((int)(22*dot));canvas.drawCircle(haloX,haloY,dp(13),paint);
        paint.setAlpha((int)(65*dot));canvas.drawCircle(haloX,haloY,dp(6),paint);
        paint.setColor(0xFFFFF9E9);paint.setAlpha((int)(245*dot));canvas.drawCircle(haloX,haloY,dp(2.4f),paint);
        if(animate)for(int i=0;i<4;i++) {
            float t=(time*.32f+i*.25f)%1;
            float angle=i*2.4f+time*.18f;
            float reach=dp(5+16*t),x=haloX+(float)Math.cos(angle)*reach,y=haloY+(float)Math.sin(angle)*reach;
            sparkle(canvas,x,y,dp(1.3f),dot*(float)Math.sin(Math.PI*t));
        }
    }
    private void drawRibbon(Canvas canvas,int side,float opening,float alpha,float time,boolean animate) {
        float startX=Math.min(sleeveRect.left-dp(28),fitPhoto.left-fitPhoto.width()*.16f);
        float edgeY=side<0?sleeveRect.top:sleeveRect.bottom;
        float wave=animate?dp(5)*(float)Math.sin(time*1.2f+side):0;
        float startY=edgeY+side*(dp(10)+wave)*opening;
        for(int strand=0;strand<3;strand++) {
            float offset=side*dp(strand*1.5f)*opening;
            ribbonPath.reset();ribbonPath.moveTo(startX,startY+offset);
            ribbonPath.cubicTo(startX+(sleeveRect.left-startX)*.45f,startY+side*dp(13)*opening+offset,
                    sleeveRect.left-dp(12),edgeY+offset,sleeveRect.left+dp(9),edgeY+offset);
            ribbonPath.cubicTo(sleeveRect.left+sleeveRect.width()*.45f,edgeY+offset,
                    sleeveRect.right-dp(12),edgeY+offset,sleeveRect.right,edgeY+side*dp(3));
            glowStroke(canvas,ribbonPath,alpha*opening*(strand==0?.65f:.22f),strand==0?.62f:.34f);
            if(animate && strand==0)pathParticles(canvas,ribbonPath,time+(side<0?0:.8f),.26f,14+intensity/12,alpha*opening);
        }
    }
    private void glowStroke(Canvas canvas,Path line,float alpha,float width) {
        paint.setShader(null);paint.setStyle(Paint.Style.STROKE);paint.setStrokeCap(Paint.Cap.ROUND);
        for(int layer=3;layer>=0;layer--) {
            paint.setColor(layer==0?0xFFFFFCEF:0xFFFFD996);
            paint.setStrokeWidth(dp(width+(layer==3?7:layer==2?3.5f:layer==1?1.5f:0)));
            paint.setAlpha((int)((layer==3?12:layer==2?25:layer==1?85:205)*SwipeMotion.clamp(alpha)));
            canvas.drawPath(line,paint);
        }
        paint.setStyle(Paint.Style.FILL);
    }
    private void pathParticles(Canvas canvas,Path line,float time,float rate,int count,float alpha) {
        measure.setPath(line,false);float length=measure.getLength();
        if(length<1)return;
        for(int i=0;i<count;i++) {
            float u=(time*rate+i/(float)count)%1;
            if(!measure.getPosTan(length*u,position,null))continue;
            float shimmer=(.55f+.45f*(float)Math.sin(time*1.8f+i*2.3f));
            float strength=alpha*(float)Math.sin(Math.PI*u)*shimmer;
            if(i%5==0)sparkle(canvas,position[0],position[1],dp(1.1f+i%2*.3f),strength);
            else {
                paint.setColor(i%3==0?0xFFFFDDA1:0xFFFFFCEF);
                paint.setAlpha((int)(185*SwipeMotion.clamp(strength)));
                canvas.drawCircle(position[0],position[1],dp(.65f+i%3*.18f),paint);
            }
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
        paint.setStyle(Paint.Style.FILL);paint.setColor(0xFFFFC995);paint.setAlpha((int)(70*amount*fade));
        canvas.drawRoundRect(0,top,dp(15),bottom,dp(7),dp(7),paint);
        paint.setColor(Color.WHITE);paint.setAlpha((int)(235*SwipeMotion.ease(amount)*fade));
        int save=canvas.save();canvas.clipRect(0,top,dp(15),bottom);
        int index=0;
        for(float y=top;y<bottom+dp(27);y+=dp(40)) {
            float radius=dp(27),cx=-dp(14);
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
            for(int i=0;i<3;i++)spawnChip();
        }
        for(Chip chip:chips) {
            if(chip.born==0)continue;
            float age=(now-chip.born)/1000f;
            if(age>1.05f){chip.born=0;chip.bitmap=null;continue;}
            float x=chip.x+chip.vx*age,y=chip.y+chip.vy*age+dp(180)*age*age;
            float size=chip.size*(1-age*.55f);
            paint.setColor(0xFFFFE8CB);paint.setAlpha((int)(245*(1-age/1.05f)*fade));
            int piece=canvas.save();canvas.rotate(chip.angle+chip.spin*age,x,y);
            target.set(x-size,y-size*.65f,x+size,y+size*.65f);
            if(chip.bitmap!=null && !chip.bitmap.isRecycled() && !chip.source.isEmpty())canvas.drawBitmap(chip.bitmap,chip.source,target,paint);
            else canvas.drawRect(target,paint);
            canvas.restoreToCount(piece);
        }
    }
}




