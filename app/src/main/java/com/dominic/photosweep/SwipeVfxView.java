package com.dominic.photosweep;

import android.content.Context;
import android.graphics.*;
import android.os.SystemClock;
import android.view.View;

/** Theme-specific hold loops and release bursts, above the photo without consuming gestures. */
class SwipeVfxView extends View {
    interface Sprites { Bitmap get(boolean keep); }
    float progress;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final RectF bounds = new RectF();
    private final Path path = new Path();
    private final Sprites sprites;
    private final String flag;
    private final boolean enabled;
    private final SwipeTheme.Kind kind;
    private final int intensity, speed;
    private long started, released;
    private boolean held;
    private float fingerX=-1, fingerY=-1;
    SwipeVfxView(Context context, Sprites sprites, String flag, SwipeTheme.Kind kind, boolean enabled,
                 int intensity, int speed) {
        super(context); this.sprites=sprites; this.flag=flag; this.kind=kind; this.enabled=enabled;
        this.intensity=intensity; this.speed=speed;
        setClickable(false); setFocusable(false); setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }
    void hold(float x,float y) { held=true; released=0; started=SystemClock.uptimeMillis(); progress=0; follow(x,y); }
    void follow(float x,float y) { fingerX=x; fingerY=y; invalidate(); }
    void cancel() { held=false; progress=0; released=0; started=0; invalidate(); }
    void release(boolean keep) { held=false; progress=keep?1f:-1f; released=SystemClock.uptimeMillis(); invalidate(); }
    private float dp(float value) { return value * getResources().getDisplayMetrics().density; }
    private static float loop(float value) { return value-(float)Math.floor(value); }
    @Override protected void onDraw(Canvas canvas) {
        float amount=SwipeMotion.clamp(Math.abs(progress)), w=getWidth(), h=getHeight();
        if ((!held && amount<.015f) || w<=0 || h<=0) return;
        boolean keep=progress>=0; int sign=keep?1:-1, color=SwipeTheme.color(kind);
        long now=SystemClock.uptimeMillis(); if(started==0) started=now;
        float time=(now-started)/1000f*speed/100f;
        float impact=released==0?0:SwipeMotion.clamp((now-released)/(float)SwipeMotion.duration(speed));
        float fade=released==0?1:1-SwipeMotion.ease(impact);
        float strength=(held?.32f+.68f*SwipeMotion.ease(amount):SwipeMotion.ease(amount))*fade;
        float cx=fingerX<0?w*(keep?.7f:.3f):Math.max(w*.12f,Math.min(w*.88f,fingerX));
        float cy=fingerY<0?h*.55f:Math.max(h*.18f,Math.min(h*.82f,fingerY));
        if(enabled) {
            drawStructure(canvas,w,h,cx,cy,time,impact,strength,sign,color);
            Bitmap sprite=sprites.get(keep); int count=8+intensity/6;
            for(int i=0;i<count;i++) {
                float phase=loop(time*.6f+i*.618034f), theta=i*2.399963f+time*.35f;
                float lane=((i*37)%101)/100f, x=cx, y=cy, rotation=sign*(phase*65+i*19);
                float radius=dp(13+i%4*5)*(.7f+.4f*amount);
                if(released!=0) {
                    float spread=SwipeMotion.ease(impact)*(Math.min(w,h)*.48f+dp(30));
                    x=cx+(float)Math.cos(theta)*spread+sign*impact*w*.18f;
                    y=cy+(float)Math.sin(theta)*spread;
                    radius*=1+impact*.7f;
                    if(kind==SwipeTheme.Kind.FIRE || kind==SwipeTheme.Kind.SOLAR || kind==SwipeTheme.Kind.DRAGONBALL) radius*=1.65f;
                    if(kind==SwipeTheme.Kind.CANDY || kind==SwipeTheme.Kind.EARTH || kind==SwipeTheme.Kind.TOXIC) y+=dp(90*impact*impact);
                    if(kind==SwipeTheme.Kind.BLEACH) { x=cx+sign*impact*w*.8f; y=cy+dp((i-count/2)*8); rotation=sign*60; }
                    if(kind==SwipeTheme.Kind.ONE_PIECE || kind==SwipeTheme.Kind.WATER) { x=cx+sign*impact*w*.6f; y=cy+dp((i-count/2)*10)+(float)Math.sin(theta)*dp(25); }
                } else {
                    switch(kind) {
                        case FIRE:
                            x=i%3==0?(i%2==0?dp(14):w-dp(14)):w*lane+sign*phase*dp(18)*amount;
                            y=i%3==0?h*(.2f+lane*.6f):h-phase*h*.3f;
                            radius*=1.35f; rotation=(float)Math.sin(time*6+i)*9; break;
                        case WATER:case ONE_PIECE:
                            x=loop(phase+amount*.35f)*w; if(sign<0)x=w-x;
                            y=h*(.68f+lane*.18f)+(float)Math.sin(time*3+lane*7)*dp(25); rotation=sign*20; radius*=1.3f;break;
                        case ICE:
                            x=i%2==0?dp(15):w-dp(15); y=h*(.13f+lane*.74f);
                            radius*=.7f+amount*.8f+(float)Math.sin(time+i)*.15f; rotation=i*47;break;
                        case CANDY:
                            x=loop(lane+sign*time*.08f)*w;
                            y=h*(.56f+lane*.23f)-Math.abs((float)Math.sin(time*2.5+i))*dp(45);rotation=time*sign*55+i*20;break;
                        case TOXIC:
                            x=w*lane; y=h*(.1f+phase*.8f); radius*=.55f+phase*.85f;rotation=0;break;
                        case LIGHTNING:case THUNDER:case MY_HERO:
                            x=cx+(float)Math.cos(theta)*dp(42+lane*70);
                            y=cy+(float)Math.sin(theta)*dp(42+lane*70);radius*=.7f+Math.abs((float)Math.sin(time*8+i))*.6f;rotation=i*67;break;
                        case SPACE:
                            x=cx+(float)Math.cos(theta)*dp(35+lane*100); y=cy+(float)Math.sin(theta)*dp(20+lane*65);rotation=theta*57.3f-135;break;
                        case WEB:
                            x=i%2==0?dp(20):w-dp(20);y=h*(.15f+lane*.6f)+(float)Math.sin(time+i)*dp(12);radius*=.7f;rotation=0;break;
                        case GOLD:case SOLAR:
                            x=cx+(float)Math.cos(theta)*dp(40+lane*90);y=cy+(float)Math.sin(theta)*dp(40+lane*90);radius*=.7f+.4f*(float)Math.sin(time*3+i);break;
                        case GAMMA:case DRAGONBALL:
                            x=cx+(float)Math.cos(theta)*dp(25+phase*75);y=cy+(float)Math.sin(theta)*dp(25+phase*75);radius*=1.1f;rotation=theta*57.3f;break;
                        case SHIELD:case SCARLET:case NARUTO:case YIN_YANG:
                            x=cx+(float)Math.cos(theta+time*sign)*dp(45+lane*65);y=cy+(float)Math.sin(theta+time*sign)*dp(45+lane*65);rotation=time*sign*95+i*40;break;
                        case COSMIC:case NEBULA:case SKY:case WISPS:
                            x=cx+(float)Math.cos(theta+time*.5f)*w*.28f;y=cy+(float)Math.sin(theta*1.3f+time*.3f)*h*.26f;radius*=kind==SwipeTheme.Kind.NEBULA?1.35f:.85f;break;
                        case STEALTH:
                            x=w*(.14f+(i%4)*.24f);y=h*(.2f+(i%3)*.27f);radius*=.65f;rotation=0;break;
                        case BLEACH:
                            x=cx+sign*dp((phase-.5f)*100);y=cy+(i-count/2)*dp(9);rotation=sign*65;radius*=.95f;break;
                        case JAPAN:case KOREA:
                            x=w*lane+(float)Math.sin(time+i)*dp(16);y=h*(.15f+loop(phase*(kind==SwipeTheme.Kind.KOREA?-1:1))*.7f);rotation=(float)Math.sin(time+i)*16;break;
                        case SPAIN:
                            x=cx+(float)Math.cos(theta)*dp(80);y=cy+(float)Math.sin(theta)*dp(60);rotation=sign*(float)Math.sin(time*2+i)*60;break;
                        case USA:
                            x=cx+(float)Math.cos(theta+time)*dp(35+lane*95);y=cy+(float)Math.sin(theta+time)*dp(35+lane*95);rotation=time*45+i*18;break;
                        case MEXICO:
                            x=cx+(float)Math.cos(theta)*dp(45+phase*80);y=cy+(float)Math.sin(theta)*dp(45+phase*80);rotation=time*70+i*30;break;
                        case BRAZIL:case FRANCE:case ITALY:case EARTH:case SAKURA:
                            x=loop(lane+sign*time*.07f)*w; y=h*(.12f+phase*.76f)+(float)Math.sin(time+i)*dp(12);rotation=(float)Math.sin(time*1.3f+i)*(kind==SwipeTheme.Kind.ITALY?20:40);break;
                        default:
                            x=cx+sign*phase*w*.26f;y=h*(.12f+lane*.75f);break;
                    }
                }
                float alpha=strength*(released!=0?1f:.5f+.5f*(float)Math.sin(Math.PI*phase));
                paint.setStyle(Paint.Style.FILL);paint.setColor(Color.WHITE);paint.setAlpha((int)(220*alpha));
                canvas.save();canvas.translate(x,y);
                if(flag!=null && i%3==0) {
                    paint.setTextAlign(Paint.Align.CENTER);paint.setTextSize(radius*2);canvas.drawText(flag,0,radius*.65f,paint);
                } else if(sprite!=null && !sprite.isRecycled()) {
                    canvas.rotate(rotation);if(sign<0 && kind!=SwipeTheme.Kind.FIRE)canvas.scale(-1,1);
                    float rh=radius*sprite.getHeight()/sprite.getWidth();bounds.set(-radius,-rh,radius,rh);canvas.drawBitmap(sprite,null,bounds,paint);
                }
                canvas.restore();
            }
        }
        if(isAttachedToWindow() && (released==0 || impact<1))postInvalidateOnAnimation();
    }
    private void drawStructure(Canvas c,float w,float h,float x,float y,float time,float impact,float strength,int sign,int color) {
        paint.setShader(null);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(dp(2));paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setColor(withAlpha(color,(int)(145*strength)));
        float r=dp(38+impact*125),pulse=dp(4*(float)Math.sin(time*3));
        switch(kind) {
            case WEB:
                for(int i=0;i<9;i++) { float a=i*.698f;c.drawLine(x,y,x+(float)Math.cos(a)*w,y+(float)Math.sin(a)*h,paint); }
                for(int i=1;i<4;i++)c.drawCircle(x,y,r*i*.55f,paint);break;
            case WATER:case ONE_PIECE:
                for(int i=0;i<3;i++){float yy=y+dp(i*20);path.reset();path.moveTo(0,yy);
                    for(int j=1;j<=20;j++)path.lineTo(w*j/20,yy+(float)Math.sin(j*.7f-time*sign*3)*dp(12+impact*22));c.drawPath(path,paint);}break;
            case LIGHTNING:case THUNDER:case MY_HERO:
                for(int i=0;i<(kind==SwipeTheme.Kind.THUNDER?5:3);i++){path.reset();path.moveTo(x,y);
                    for(int j=1;j<=6;j++)path.lineTo(x+sign*j*dp(20+impact*8),y+(i-1)*dp(40)+(float)Math.sin(time*14+i*7+j*5)*dp(22));c.drawPath(path,paint);}break;
            case SHIELD:
                c.drawCircle(x,y,r,paint);c.drawCircle(x,y,r*.72f,paint);c.drawArc(x-r,y-r,x+r,y+r,time*100,120,false,paint);break;
            case SCARLET:case SKY:
                c.drawCircle(x,y,r+pulse,paint);for(int i=0;i<6;i++){float a=i*1.047f+time*.3f;
                    c.drawLine(x+(float)Math.cos(a)*r*.7f,y+(float)Math.sin(a)*r*.7f,x+(float)Math.cos(a+.7f)*r,y+(float)Math.sin(a+.7f)*r,paint);}break;
            case STEALTH:
                for(int i=0;i<6;i++)c.drawLine(w*(.14f+(i%3)*.32f),h*(.2f+(i%2)*.5f),w*(.14f+((i+1)%3)*.32f),h*(.2f+((i+1)%2)*.5f),paint);break;
            case BLEACH:
                paint.setStrokeWidth(dp(4+impact*8));c.drawLine(x-sign*w*.3f,y+dp(40),x+sign*w*.4f,y-dp(40),paint);break;
            case NARUTO:case COSMIC:case YIN_YANG:
                path.reset();for(int i=0;i<65;i++){float a=i*.2f+time*sign,rr=r*i/65;
                    float xx=x+(float)Math.cos(a)*rr,yy=y+(float)Math.sin(a)*rr;if(i==0)path.moveTo(xx,yy);else path.lineTo(xx,yy);}c.drawPath(path,paint);break;
            case SOLAR:case GOLD:case DRAGONBALL:
                for(int i=0;i<12;i++){float a=i*.524f+time*.1f;c.drawLine(x+(float)Math.cos(a)*r*.4f,y+(float)Math.sin(a)*r*.4f,x+(float)Math.cos(a)*r*1.5f,y+(float)Math.sin(a)*r*1.5f,paint);}break;
            case ICE:
                for(int i=0;i<6;i++){float a=i*1.047f;c.drawLine(x,y,x+(float)Math.cos(a)*(r+pulse),y+(float)Math.sin(a)*(r+pulse),paint);}break;
            case GAMMA:
                c.drawOval(x-r*1.2f,y-r*.7f,x+r*1.2f,y+r*.7f,paint);c.drawOval(x-r*.7f,y-r*1.2f,x+r*.7f,y+r*1.2f,paint);break;
            default: if(impact>0)c.drawCircle(x,y,r,paint);break;
        }
        paint.setStyle(Paint.Style.FILL);paint.setAlpha(255);
    }
    private static int withAlpha(int color,int alpha) { return Color.argb(Math.max(0,Math.min(255,alpha)),Color.red(color),Color.green(color),Color.blue(color)); }
}
