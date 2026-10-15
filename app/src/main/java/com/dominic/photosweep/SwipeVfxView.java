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
    private final Sprites sprites;
    private final RectF glyph = new RectF();
    private final RectF unit = new RectF(-1,-1,1,1);
    private final RectF bladeRect = new RectF();
    private final Paint whiteLight = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private static Bitmap spellArt, bladeArt;
    private final Bitmap spellRing, sawBlade;
    private float haloX, haloY, haloRadius, haloHeight;
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
        super(context); this.sprites=sprites; this.enabled=enabled; this.intensity=intensity; this.speed=speed;
        this.reducedMotion=reducedMotion;
        // Share immutable textures; decode once at bounded sizes, never in a draw frame.
        BitmapFactory.Options options=new BitmapFactory.Options();options.inScaled=false;options.inSampleSize=2;
        if(spellArt==null || spellArt.isRecycled())
            spellArt=BitmapFactory.decodeResource(context.getResources(),R.drawable.keep_spell_ring,options);
        options.inSampleSize=4;
        if(bladeArt==null || bladeArt.isRecycled())
            bladeArt=BitmapFactory.decodeResource(context.getResources(),R.drawable.grinder_saw_blade,options);
        spellRing=spellArt;sawBlade=bladeArt;
        whiteLight.setColorFilter(new PorterDuffColorFilter(0xFFFFFBEE,PorterDuff.Mode.SRC_IN));
        for(int i=0;i<chips.length;i++)chips[i]=new Chip();
        setClickable(false);setFocusable(false);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }
    private float dp(float value) { return value*getResources().getDisplayMetrics().density; }
    protected void onGrinderShake(float offset) { }
    void setPhoto(Bitmap image, RectF bounds, float dx, float dy) {
        bitmap=image;photo.set(bounds);photo.offset(dx,dy);
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
    void hold(float x,float y) { clearChips();onGrinderShake(0);released=0;started=SystemClock.uptimeMillis();progress=0;tracked=false; }
    void follow(float x,float y) { if(progress>=0){clearChips();onGrinderShake(0);}invalidate(); }
    void release(boolean keep) { onGrinderShake(0);progress=keep?1:-1;released=SystemClock.uptimeMillis();invalidate(); }
    void cancel() { onGrinderShake(0);progress=0;released=0;tracked=false;bitmap=null;photo.setEmpty();clearChips();invalidate(); }
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
        if(SwipeMotion.intent(progress)==0 || getWidth()==0 || getHeight()==0){onGrinderShake(0);return;}
        long now=SystemClock.uptimeMillis();if(started==0)started=now;
        float time=(now-started)/1000f*speed/100f;
        float phase=released==0?0:SwipeMotion.clamp((now-released)/(float)(reducedMotion?80:SwipeMotion.releaseDuration(speed,progress>0)));
        float amount=SwipeMotion.clamp(Math.abs(progress));
        float fade=released==0?1:1-SwipeMotion.ease(phase);
        if(released==0) {
            float proximity=progress<0&&!photo.isEmpty()?SwipeMotion.grinderProximity(photo.left,dp(9),dp(110)):0;
            onGrinderShake(enabled&&!reducedMotion?dp(2.6f)*SwipeMotion.grinderShake((now-started)/1000f,proximity):0);
        }
        int frame=canvas.save();canvas.clipRect(0,0,getWidth(),getHeight());
        if(progress>0)drawHeavenly(canvas,time,amount,fade);
        else drawGrinder(canvas,time,amount,fade,now);
        canvas.restoreToCount(frame);
        paint.setShader(null);paint.setAlpha(255);paint.setStyle(Paint.Style.FILL);
        if(isAttachedToWindow() && enabled && (!reducedMotion || released!=0))postInvalidateOnAnimation();
    }
    private void drawHeavenly(Canvas canvas,float time,float amount,float fade) {
        boolean animate=enabled&&!reducedMotion;
        float strength=SwipeMotion.ease(amount)*fade;
        float breathe=animate?.92f+.08f*(float)Math.sin(time*1.5f):1;
        // The image matrix supplies actual portrait/landscape bounds, including letterboxing.
        haloX=getWidth();haloY=photo.isEmpty()?getHeight()*.5f:photo.centerY();
        haloY=Math.max(dp(20),Math.min(getHeight()-dp(20),haloY));
        haloRadius=SwipeMotion.spellHalfWidth(photo.isEmpty()?getWidth()*.6f:photo.width(),getWidth(),dp(10));
        haloHeight=SwipeMotion.spellHalfHeight(photo.isEmpty()?getHeight()*.6f:photo.height(),getHeight(),haloY,dp(10));
        paint.setShader(null);paint.setStyle(Paint.Style.FILL);paint.setColor(Color.WHITE);
        float breathScale=animate?.99f+.01f*(float)Math.sin(time*1.2f):1;
        paint.setAlpha((int)(205*strength*breathe));
        drawSpellRing(canvas,breathScale,animate?time*11:0,paint);
        whiteLight.setAlpha((int)(150*strength*breathe));
        drawSpellRing(canvas,.74f*breathScale,animate?-time*17:23,whiteLight);
        if(!animate)return;
        Bitmap motif=sprites.get(true);
        int count=10+intensity/7;
        for(int i=0;i<count;i++) {
            float phase=(time*.25f+i*.137f)%1f;
            double angle=Math.PI*(.56+.88*((i*43)%101)/100f);
            float distance=haloRadius*(.64f+phase*.30f);
            float x=haloX+(float)Math.cos(angle)*distance;
            float y=haloY+(float)Math.sin(angle)*distance*(haloHeight/haloRadius)-phase*dp(14);
            float size=dp(2.5f+i%4)*(.35f+.65f*(float)Math.sin(Math.PI*phase));
            paint.setColor(i%3==0?0xFFFFE8A4:Color.WHITE);paint.setAlpha((int)(220*strength*Math.sin(Math.PI*phase)));
            path.reset();path.moveTo(x,y-size*1.6f);path.lineTo(x+size*.35f,y-size*.25f);
            path.lineTo(x+size,y);path.lineTo(x+size*.25f,y+size*.3f);path.lineTo(x,y+size*1.6f);
            path.lineTo(x-size*.3f,y+size*.25f);path.lineTo(x-size,y);path.lineTo(x-size*.25f,y-size*.3f);path.close();
            canvas.drawPath(path,paint);
            if(i%7==0 && motif!=null && !motif.isRecycled()) {
                float half=dp(7),hh=half*motif.getHeight()/motif.getWidth();
                glyph.set(x-half,y-hh,x+half,y+hh);paint.setColor(Color.WHITE);
                paint.setAlpha((int)(100*strength*Math.sin(Math.PI*phase)));
                canvas.drawBitmap(motif,null,glyph,paint);
            }
        }
    }
    private void drawSpellRing(Canvas canvas,float scale,float angle,Paint ink) {
        if(spellRing==null || spellRing.isRecycled())return;
        int save=canvas.save();canvas.translate(haloX,haloY);
        // Rotate in texture space BEFORE oval scaling so the oval never wobbles out of shape.
        canvas.scale(haloRadius*scale,haloHeight*scale);canvas.rotate(angle%360);
        canvas.drawBitmap(spellRing,null,unit,ink);canvas.restoreToCount(save);
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


