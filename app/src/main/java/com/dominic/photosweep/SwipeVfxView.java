package com.dominic.photosweep;

import android.content.Context;
import android.graphics.*;
import android.os.SystemClock;
import android.view.View;

/** Reusable non-interactive VFX layer. Runs only while a swipe/preview is active. */
class SwipeVfxView extends View {
    interface Sprites { Bitmap get(boolean keep); }
    float progress;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final RectF bounds = new RectF();
    private final Path trail = new Path();
    private final Sprites sprites;
    private final String flag;
    private final boolean falling, enabled;
    private final int keepColor, trashColor, intensity, speed;
    private long started, released;
    private int direction;
    SwipeVfxView(Context context, Sprites sprites, String flag, boolean falling, boolean enabled,
                 int keepColor, int trashColor, int intensity, int speed) {
        super(context); this.sprites=sprites; this.flag=flag; this.falling=falling; this.enabled=enabled;
        this.keepColor=keepColor; this.trashColor=trashColor; this.intensity=intensity; this.speed=speed;
        setClickable(false); setFocusable(false); setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }
    void release(boolean keep) { progress=keep?1f:-1f; released=SystemClock.uptimeMillis(); invalidate(); }
    private float dp(float value) { return value * getResources().getDisplayMetrics().density; }
    @Override protected void onDraw(Canvas canvas) {
        float amount=SwipeMotion.clamp(Math.abs(progress)), w=getWidth(), h=getHeight();
        if (amount<.015f || w<=0 || h<=0) { started=0; released=0; return; }
        boolean keep=progress>0; int sign=keep?1:-1, color=keep?keepColor:trashColor;
        long now=SystemClock.uptimeMillis();
        if (started==0 || direction!=sign) { started=now; direction=sign; }
        float time=(now-started)/1000f*speed/100f;
        float impact=released==0?0:SwipeMotion.clamp((now-released)/(float)SwipeMotion.duration(speed));
        float fade=released==0?1:1-SwipeMotion.ease(impact);
        float strength=SwipeMotion.ease(amount)*fade;
        // Subtle edge tint keeps the photo's center visible.
        paint.setStyle(Paint.Style.FILL);
        paint.setShader(new LinearGradient(0,0,w,0,
                keep?new int[]{Color.TRANSPARENT,Color.TRANSPARENT,withAlpha(color,(int)(60*strength))}
                    :new int[]{withAlpha(color,(int)(60*strength)),Color.TRANSPARENT,Color.TRANSPARENT},
                new float[]{0,.5f,1},Shader.TileMode.CLAMP));
        canvas.drawRect(0,0,w,h,paint); paint.setShader(null);
        float origin=w*(keep?.73f:.27f), center=h*.55f;
        if (enabled) {
            // Curved ribbons sweep in the same direction as the gesture.
            for(int lane=0;lane<3;lane++) {
                float y=center+dp((lane-1)*46), length=w*(.15f+.4f*amount);
                trail.reset(); trail.moveTo(origin-sign*length,y+dp(22));
                trail.cubicTo(origin-sign*length*.6f,y-dp(55),origin+sign*length*.25f,y+dp(55),origin+sign*length*.5f,y-dp(22));
                paint.setColor(withAlpha(color,(int)((lane==1?105:55)*strength)));
                paint.setStyle(Paint.Style.STROKE); paint.setStrokeCap(Paint.Cap.ROUND);
                paint.setStrokeWidth(dp(lane==1?5:2)); canvas.drawPath(trail,paint);
            }
            paint.setStyle(Paint.Style.FILL);
            Bitmap sprite=sprites.get(keep);
            int count=8+intensity/7;
            for(int i=0;i<count;i++) {
                float phase=(time*.65f+i*.618034f)%1f;
                float lane=((i*37)%101)/100f;
                float x=origin+SwipeMotion.travel(phase,keep)*w*(.12f+.22f*amount)+sign*dp(impact*72);
                float y=h*(.15f+lane*.7f)+(float)Math.sin(phase*6.283f+i)*dp(15)
                        +(falling?dp(SwipeMotion.falling(phase)):0);
                float alpha=strength*(.35f+.65f*(float)Math.sin(Math.PI*phase));
                float radius=dp(12+i%4*5)*(.65f+.45f*amount)*(1+impact*.6f);
                paint.setColor(Color.WHITE); paint.setAlpha((int)(210*alpha));
                canvas.save(); canvas.translate(x,y);
                if(flag!=null && i%3==0) {
                    paint.setTextAlign(Paint.Align.CENTER); paint.setTextSize(radius*2);
                    canvas.drawText(flag,0,radius*.65f,paint);
                } else if(sprite!=null && !sprite.isRecycled()) {
                    canvas.rotate(sign*(phase*65+i*19));
                    // Mirror directional energy so both sides have a purposeful flow.
                    if(!keep) canvas.scale(-1,1);
                    float rh=radius*sprite.getHeight()/sprite.getWidth();
                    bounds.set(-radius,-rh,radius,rh); canvas.drawBitmap(sprite,null,bounds,paint);
                }
                canvas.restore();
            }
            if(released!=0) {
                paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(dp(3)*(1-impact));
                paint.setColor(withAlpha(color,(int)(160*strength)));
                canvas.drawCircle(origin,center,dp(28+impact*110),paint);
                paint.setStyle(Paint.Style.FILL);
            }
        }
        // Words communicate the action independently of the theme's colors.
        paint.setAlpha(255); paint.setColor(withAlpha(color,(int)(220*strength)));
        float labelWidth=dp(132), labelHeight=dp(42), labelX=keep?w-labelWidth-dp(12):dp(12);
        bounds.set(labelX,h*.12f,labelX+labelWidth,h*.12f+labelHeight);
        canvas.drawRoundRect(bounds,dp(16),dp(16),paint);
        paint.setColor(withAlpha(Color.WHITE,(int)(255*strength))); paint.setTextSize(dp(17));
        paint.setTypeface(Typeface.create(Typeface.DEFAULT,Typeface.BOLD)); paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(keep?"KEEP →":"← TRASH",bounds.centerX(),bounds.centerY()-(paint.ascent()+paint.descent())/2,paint);
        paint.setTypeface(Typeface.DEFAULT); paint.setAlpha(255);
        if(isAttachedToWindow() && (released==0 || impact<1)) postInvalidateOnAnimation();
    }
    private static int withAlpha(int color,int alpha) { return Color.argb(Math.max(0,Math.min(255,alpha)),Color.red(color),Color.green(color),Color.blue(color)); }
}
