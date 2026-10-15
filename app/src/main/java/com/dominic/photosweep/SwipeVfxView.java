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
    private final int intensity, speed;
    private final Random random = new Random();
    private final Chip[] chips = new Chip[96];
    private Bitmap bitmap;
    private long released, started;
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
        for(int i=0;i<chips.length;i++)chips[i]=new Chip();
        setClickable(false);setFocusable(false);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }
    private float dp(float value) { return value*getResources().getDisplayMetrics().density; }
    void setPhoto(Bitmap image, RectF bounds, float dx, float dy) {
        bitmap=image;photo.set(bounds);photo.offset(dx,dy);
        float travel=tracked?Math.max(0,lastLeft-photo.left):0;
        lastLeft=photo.left;tracked=true;
        if(enabled && progress<0 && photo.left<dp(10) && photo.right>0) {
            feed+=travel/dp(7)*(.4f+intensity/75f);
            int count=Math.min(10,(int)feed);feed-=count;
            for(int i=0;i<count;i++)spawnChip();
        } else feed=0;
        invalidate();
    }
    void hold(float x,float y) { clearChips(); released=0;started=SystemClock.uptimeMillis();progress=0;tracked=false; }
    void follow(float x,float y) { if(progress>=0)clearChips();invalidate(); }
    void release(boolean keep) { progress=keep?1:-1;released=SystemClock.uptimeMillis();invalidate(); }
    void cancel() { progress=0;released=0;tracked=false;bitmap=null;photo.setEmpty();clearChips();invalidate(); }
    private void clearChips() { for(Chip chip:chips){chip.born=0;chip.bitmap=null;}feed=0; }
    private void spawnChip() {
        Chip chip=chips[nextChip++%chips.length];chip.born=SystemClock.uptimeMillis();
        chip.x=dp(9);chip.y=photo.top+photo.height()*(.03f+random.nextFloat()*.94f);
        chip.size=dp(3+random.nextFloat()*6);chip.vx=dp(22+random.nextFloat()*100);
        chip.vy=dp(-50+random.nextFloat()*75);chip.angle=random.nextFloat()*180;
        chip.spin=-210+random.nextFloat()*420;chip.bitmap=bitmap;
        if(bitmap!=null && !bitmap.isRecycled()) {
            int bw=bitmap.getWidth(),bh=bitmap.getHeight();
            int sx=Math.max(0,Math.min(bw-1,(int)((dp(9)-photo.left)/Math.max(1,photo.width())*bw)));
            int sy=Math.max(0,Math.min(bh-1,(int)((chip.y-photo.top)/Math.max(1,photo.height())*bh)));
            int side=Math.max(1,(int)(chip.size/Math.max(1,photo.width())*bw));
            chip.source.set(sx,sy,Math.min(bw,sx+side),Math.min(bh,sy+side));
        }
    }
    @Override protected void onDraw(Canvas canvas) {
        if(SwipeMotion.intent(progress)==0 || getWidth()==0 || getHeight()==0)return;
        long now=SystemClock.uptimeMillis();if(started==0)started=now;
        float time=(now-started)/1000f*speed/100f;
        float phase=released==0?0:SwipeMotion.clamp((now-released)/(float)(reducedMotion?80:SwipeMotion.releaseDuration(speed,progress>0)));
        float amount=SwipeMotion.clamp(Math.abs(progress));
        float fade=released==0?1:1-SwipeMotion.ease(phase);
        if(progress>0)drawHeavenly(canvas,time,amount,fade);
        else drawGrinder(canvas,time,amount,fade,now);
        paint.setShader(null);paint.setAlpha(255);paint.setStyle(Paint.Style.FILL);
        if(isAttachedToWindow() && enabled && (!reducedMotion || released!=0))postInvalidateOnAnimation();
    }
    private void drawHeavenly(Canvas canvas,float time,float amount,float fade) {
        RectF rim=photo;
        if(rim.isEmpty()) {target.set(getWidth()*.20f,getHeight()*.25f,getWidth()*.80f,getHeight()*.75f);rim=target;}
        float strength=(.20f+.80f*amount)*fade;
        paint.setShader(null);paint.setStyle(Paint.Style.STROKE);
        for(int layer=3;layer>0;layer--) {
            paint.setStrokeWidth(dp(layer*6));paint.setColor(0xFFFFF0BD);
            paint.setAlpha((int)((layer==1?140:layer==2?50:18)*strength));
            canvas.drawRoundRect(rim,dp(8),dp(8),paint);
        }
        if(!enabled || reducedMotion)return;
        int save=canvas.save();canvas.clipRect(rim);
        float band=rim.left+((time*.75f)%1.6f-.3f)*rim.width();
        paint.setStyle(Paint.Style.FILL);
        paint.setShader(new LinearGradient(band-dp(55),rim.top,band+dp(55),rim.bottom,
                new int[]{0x00FFFFFF,0x88FFF5D6,0x00FFFFFF},null,Shader.TileMode.CLAMP));
        paint.setAlpha((int)(100*strength));canvas.drawRect(rim,paint);paint.setShader(null);
        canvas.restoreToCount(save);
        Bitmap motif=sprites.get(true);
        int count=12+intensity/5;
        for(int i=0;i<count;i++) {
            float lane=((i*43)%101)/100f, phase=(time*.40f+i*.137f)%1f;
            float x=rim.left+rim.width()*lane+(float)Math.sin(time*.8f+i)*dp(9);
            float y=rim.bottom-phase*rim.height();
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
    private void drawGrinder(Canvas canvas,float time,float amount,float fade,long now) {
        float top=photo.isEmpty()?getHeight()*.20f:Math.max(dp(5),photo.top);
        float bottom=photo.isEmpty()?getHeight()*.80f:Math.min(getHeight()-dp(5),photo.bottom);
        paint.setStyle(Paint.Style.FILL);paint.setColor(0xFFFFC995);paint.setAlpha((int)(70*amount*fade));
        canvas.drawRoundRect(0,top,dp(15),bottom,dp(7),dp(7),paint);
        paint.setColor(0xFFFFE7C5);paint.setAlpha((int)(210*amount*fade));
        float shift=enabled&&!reducedMotion?(time*dp(35))%dp(13):0;
        int save=canvas.save();canvas.clipRect(0,top,dp(15),bottom);
        for(float y=top-dp(13)+shift;y<bottom;y+=dp(13)) {
            path.reset();path.moveTo(dp(2),y);path.lineTo(dp(12),y+dp(5));path.lineTo(dp(2),y+dp(10));path.close();canvas.drawPath(path,paint);
        }
        canvas.restoreToCount(save);
        if(!enabled || reducedMotion)return;
        // Preview has no real photo. Show paper-colored chips instead of fabricating a photo.
        if(bitmap==null && random.nextFloat()<.45f) {
            photo.set(0,getHeight()*.20f,getWidth()*.6f,getHeight()*.80f);spawnChip();
        }
        for(Chip chip:chips) {
            if(chip.born==0)continue;
            float age=(now-chip.born)/1000f;
            if(age>.85f){chip.born=0;chip.bitmap=null;continue;}
            float x=chip.x+chip.vx*age,y=chip.y+chip.vy*age+dp(130)*age*age;
            float size=chip.size*(1-age*.65f);
            paint.setColor(0xFFFFE8CB);paint.setAlpha((int)(235*(1-age/.85f)*fade));
            int piece=canvas.save();canvas.rotate(chip.angle+chip.spin*age,x,y);
            target.set(x-size,y-size*.65f,x+size,y+size*.65f);
            if(chip.bitmap!=null && !chip.bitmap.isRecycled() && !chip.source.isEmpty())canvas.drawBitmap(chip.bitmap,chip.source,target,paint);
            else canvas.drawRect(target,paint);
            canvas.restoreToCount(piece);
        }
    }
}
