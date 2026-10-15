package com.dominic.photosweep;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.*;
import android.view.View;
import android.view.animation.LinearInterpolator;

/** Three decorative reels reveal one already-banked token; never computes or grants a reward. */
final class TokenSlotView extends View {
    interface Listener { void onReelStop(); void onComplete(boolean animated); }
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF box=new RectF(),well=new RectF();
    private final Path shape=new Path(),clip=new Path();
    private final float[] positions=new float[3],starts=new float[3];
    private final int[] targets=new int[3];
    private final boolean[] stopped=new boolean[3];
    private final int background,panel,accent,ink;
    private final boolean reduced;
    private ValueAnimator animator;
    private Listener listener;
    private boolean running;
    private int token;
    private float reveal=1;
    private LinearGradient reelShade;

    TokenSlotView(Context context,int background,int panel,int accent,int ink,boolean reduced) {
        super(context);this.background=background;this.panel=panel;this.accent=accent;this.ink=ink;this.reduced=reduced;
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        setFocusable(false);setClickable(false);
    }
    private float dp(float v){return v*getResources().getDisplayMetrics().density;}
    private float sp(float v){return v*getResources().getDisplayMetrics().scaledDensity;}
    private static int mix(int a,int b,float t) {
        return Color.rgb((int)(Color.red(a)*(1-t)+Color.red(b)*t),
                (int)(Color.green(a)*(1-t)+Color.green(b)*t),(int)(Color.blue(a)*(1-t)+Color.blue(b)*t));
    }
    void showResult(int result) {
        token=Math.max(0,Math.min(2,result));
        for(int i=0;i<3;i++){positions[i]=token;stopped[i]=true;}
        reveal=1;invalidate();
    }
    void spin(int result,Listener callback) {
        if(running)return;
        token=Math.max(0,Math.min(2,result));listener=callback;
        running=true;reveal=0;
        for(int i=0;i<3;i++) {
            starts[i]=TokenReelMotion.symbol(Math.round(positions[i]));
            positions[i]=starts[i];targets[i]=TokenReelMotion.target(starts[i],token,i);stopped[i]=false;
        }
        if(reduced || !ValueAnimator.areAnimatorsEnabled()){finishImmediately();return;}
        animator=ValueAnimator.ofFloat(0,TokenReelMotion.duration(2)+320);
        animator.setDuration(TokenReelMotion.duration(2)+320);
        animator.setInterpolator(new LinearInterpolator());
        animator.addUpdateListener(frame -> {
            float time=(float)frame.getAnimatedValue();
            for(int i=0;i<3;i++) {
                positions[i]=TokenReelMotion.position(starts[i],targets[i],time,i);
                if(!stopped[i] && time>=TokenReelMotion.duration(i)) {
                    stopped[i]=true;if(listener!=null)listener.onReelStop();
                }
            }
            reveal=Math.max(0,Math.min(1,(time-TokenReelMotion.duration(2))/320));
            invalidate();
        });
        animator.addListener(new AnimatorListenerAdapter() {
            @Override public void onAnimationEnd(Animator animation) { if(running)complete(true); }
        });
        animator.start();
    }
    void finishImmediately() {
        if(!running)return;
        running=false;
        if(animator!=null){animator.cancel();animator=null;}
        complete(false);
    }
    private void complete(boolean animated) {
        running=false;animator=null;showResult(token);
        Listener callback=listener;listener=null;
        if(callback!=null)callback.onComplete(animated);
    }
    void dispose() {
        running=false;listener=null;
        if(animator!=null){animator.cancel();animator=null;}
    }
    @Override protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        if(visibility!=VISIBLE)finishImmediately();
    }
    @Override protected void onDetachedFromWindow(){dispose();super.onDetachedFromWindow();}
    @Override protected void onSizeChanged(int w,int h,int oldw,int oldh) {
        super.onSizeChanged(w,h,oldw,oldh);
        reelShade=new LinearGradient(0,0,0,h,new int[]{0xE6000000,0x00000000,0x00000000,0xE6000000},
                new float[]{0,.32f,.68f,1},Shader.TileMode.CLAMP);
    }
    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float w=getWidth(),h=getHeight();if(w<=0 || h<=0)return;
        paint.setShader(null);paint.setStyle(Paint.Style.FILL);paint.setColor(panel);paint.setAlpha(255);
        box.set(0,0,w,h);canvas.drawRoundRect(box,dp(19),dp(19),paint);
        paint.setColor(accent);paint.setAlpha(60);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(dp(1));
        box.inset(dp(.8f),dp(.8f));canvas.drawRoundRect(box,dp(19),dp(19),paint);paint.setStyle(Paint.Style.FILL);
        float gap=dp(7),padding=dp(11),rw=(w-padding*2-gap*2)/3,cell=dp(79),center=h/2;
        for(int reel=0;reel<3;reel++) {
            float left=padding+reel*(rw+gap);
            well.set(left,dp(10),left+rw,h-dp(10));
            paint.setColor(mix(background,Color.BLACK,.22f));paint.setAlpha(255);
            canvas.drawRoundRect(well,dp(12),dp(12),paint);
            int save=canvas.save();clip.reset();clip.addRoundRect(well,dp(12),dp(12),Path.Direction.CW);canvas.clipPath(clip);
            int base=(int)Math.floor(positions[reel]);
            for(int offset=-2;offset<=2;offset++) {
                int index=base+offset;
                float y=center+(index-positions[reel])*cell;
                drawToken(canvas,left+rw/2,y-dp(7),Math.min(dp(22),rw*.29f),TokenReelMotion.symbol(index));
            }
            paint.setShader(reelShade);paint.setAlpha(255);canvas.drawRect(well,paint);paint.setShader(null);
            canvas.restoreToCount(save);
            paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(dp(stopped[reel]?1.4f:.8f));
            paint.setColor(stopped[reel]?tokenColor(token):ink);paint.setAlpha(stopped[reel]?120:25);
            canvas.drawRoundRect(well,dp(12),dp(12),paint);paint.setStyle(Paint.Style.FILL);
        }
        box.set(dp(5),center-dp(42),w-dp(5),center+dp(42));
        paint.setColor(accent);paint.setAlpha(60);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(dp(1));
        canvas.drawRoundRect(box,dp(12),dp(12),paint);paint.setStyle(Paint.Style.FILL);
        if(reveal>0) {
            paint.setColor(tokenColor(token));paint.setAlpha((int)(70*reveal));
            for(int side=0;side<2;side++) {
                float x=side==0?dp(5):w-dp(5);
                for(int i=0;i<3;i++)canvas.drawCircle(x,center+dp((i-1)*8),dp(i==1?2:1),paint);
            }
        }
        paint.setAlpha(255);
    }
    private int tokenColor(int value){return value==0?0xFFD9E4F0:value==1?0xFFFFDA8B:0xFF9DEFFF;}
    private void drawToken(Canvas canvas,float x,float y,float r,int kind) {
        paint.setAlpha(255);paint.setStyle(Paint.Style.FILL);
        if(kind==2) {
            shape.reset();shape.moveTo(x-r*.58f,y-r*.7f);shape.lineTo(x+r*.58f,y-r*.7f);
            shape.lineTo(x+r,y-r*.15f);shape.lineTo(x,y+r);shape.lineTo(x-r,y-r*.15f);shape.close();
            paint.setColor(0xFF6DBBD9);canvas.drawPath(shape,paint);
            shape.reset();shape.moveTo(x-r*.58f,y-r*.7f);shape.lineTo(x,y-r*.7f);shape.lineTo(x-r*.33f,y-r*.15f);shape.lineTo(x-r,y-r*.15f);shape.close();
            paint.setColor(0xFFE5FBFF);canvas.drawPath(shape,paint);
            shape.reset();shape.moveTo(x,y-r*.7f);shape.lineTo(x+r*.58f,y-r*.7f);shape.lineTo(x+r*.33f,y-r*.15f);shape.lineTo(x-r*.33f,y-r*.15f);shape.close();
            paint.setColor(0xFFB6F4FF);canvas.drawPath(shape,paint);
            shape.reset();shape.moveTo(x-r*.33f,y-r*.15f);shape.lineTo(x+r*.33f,y-r*.15f);shape.lineTo(x,y+r);shape.close();
            paint.setColor(0xFFCFFAFF);canvas.drawPath(shape,paint);
        } else {
            paint.setColor(kind==0?0xFF70869D:0xFFA8702D);canvas.drawCircle(x,y+dp(2),r,paint);
            paint.setColor(tokenColor(kind));canvas.drawCircle(x,y,r,paint);
            paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(dp(1.3f));paint.setColor(kind==0?0xFF8DA0B5:0xFFD49A40);
            canvas.drawCircle(x,y,r*.78f,paint);paint.setStyle(Paint.Style.FILL);
            // A single four-point light mark also keeps the icons distinct at small sizes.
            shape.reset();shape.moveTo(x,y-r*.5f);shape.lineTo(x+r*.16f,y-r*.12f);
            shape.lineTo(x+r*.48f,y);shape.lineTo(x+r*.16f,y+r*.12f);shape.lineTo(x,y+r*.5f);
            shape.lineTo(x-r*.16f,y+r*.12f);shape.lineTo(x-r*.48f,y);shape.lineTo(x-r*.16f,y-r*.12f);shape.close();
            paint.setColor(kind==0?0xFF748CA6:0xFFAC742F);canvas.drawPath(shape,paint);
            paint.setColor(Color.WHITE);paint.setAlpha(165);canvas.drawCircle(x-r*.36f,y-r*.44f,dp(1.8f),paint);
        }
        paint.setAlpha(255);paint.setColor(ink);paint.setTextAlign(Paint.Align.CENTER);paint.setTextSize(sp(10));
        paint.setTypeface(Typeface.DEFAULT_BOLD);
        canvas.drawText(kind==0?"SILVER":kind==1?"GOLD":"DIAMOND",x,y+r+dp(18),paint);
    }
}
