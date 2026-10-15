package com.dominic.photosweep;

import android.graphics.*;
import android.graphics.drawable.Drawable;
import java.util.Locale;

/** One scalable, rounded icon family used throughout navigation, settings and currency. */
final class StudioIcon extends Drawable {
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final String key;
    private int color, alpha = 255;
    StudioIcon(String key, int color) { this.key = key.toLowerCase(Locale.ROOT); this.color = color; }
    private void line(Canvas c, float x, float y, float xx, float yy) { c.drawLine(x,y,xx,yy,p); }
    private void star(Canvas c, float x, float y, float r) {
        path.reset();
        for (int i=0;i<8;i++) {
            double a=-Math.PI/2+i*Math.PI/4; float rr=i%2==0?r:r*.32f;
            float xx=x+(float)Math.cos(a)*rr, yy=y+(float)Math.sin(a)*rr;
            if(i==0)path.moveTo(xx,yy);else path.lineTo(xx,yy);
        }
        path.close(); c.drawPath(path,p);
    }
    @Override public void draw(Canvas c) {
        int save=c.save(); Rect b=getBounds(); c.translate(b.left,b.top);c.scale(b.width()/24f,b.height()/24f);
        p.setColor(color);p.setAlpha(alpha);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1.65f);
        p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeJoin(Paint.Join.ROUND);
        if(key.contains("diamond")) {
            path.reset();path.moveTo(3,8);path.lineTo(7,3);path.lineTo(17,3);path.lineTo(21,8);path.lineTo(12,22);path.close();c.drawPath(path,p);
            line(c,3,8,21,8);line(c,8,8,12,22);line(c,16,8,12,22);line(c,7,3,8,8);line(c,17,3,16,8);
        } else if(key.contains("gold") || key.contains("silver") || key.contains("shop") || key.contains("token")) {
            p.setShader(new LinearGradient(3,2,20,22,new int[]{Color.WHITE,color},null,Shader.TileMode.CLAMP));
            c.drawCircle(12,12,9,p);c.drawCircle(12,12,7,p);p.setShader(null);p.setStyle(Paint.Style.FILL);star(c,12,12,4.6f);
        } else if(key.contains("trash")) {
            line(c,4,6,20,6);line(c,9,3,15,3);path.reset();path.moveTo(6,6);path.lineTo(7,21);path.lineTo(17,21);path.lineTo(18,6);c.drawPath(path,p);
            line(c,10,10,10,17);line(c,14,10,14,17);
        } else if(key.contains("setting") || key.contains("admin") || key.contains("testing")) {
            c.drawCircle(12,12,6.5f,p);c.drawCircle(12,12,2.6f,p);
            for(int i=0;i<8;i++){double a=i*Math.PI/4;line(c,12+(float)Math.cos(a)*8,12+(float)Math.sin(a)*8,12+(float)Math.cos(a)*10,12+(float)Math.sin(a)*10);}
        } else if(key.contains("theme") || key.contains("feature")) {
            p.setStyle(Paint.Style.FILL);star(c,11,12,9);star(c,20,4,2.6f);
        } else if(key.contains("report") || key.contains("feedback") || key.contains("problem")) {
            line(c,6,22,6,3);path.reset();path.moveTo(6,3);path.cubicTo(12,0,14,7,21,3);path.lineTo(21,13);path.cubicTo(14,17,12,9,6,13);c.drawPath(path,p);
        } else if(key.contains("audio") || key.contains("music") || key.contains("radio")) {
            line(c,9,17,9,4);line(c,9,4,20,2);line(c,20,2,20,15);p.setStyle(Paint.Style.FILL);c.drawOval(3,15,9,20,p);c.drawOval(14,13,20,18,p);
        } else if(key.contains("account")) {
            c.drawCircle(12,7,4,p);c.drawArc(4,13,20,28,185,170,false,p);
        } else if(key.contains("privacy")) {
            path.reset();path.moveTo(12,2);path.lineTo(21,6);path.lineTo(19,16);path.quadTo(16,20,12,22);path.quadTo(8,20,5,16);path.lineTo(3,6);path.close();c.drawPath(path,p);
            line(c,8,11,11,14);line(c,11,14,17,8);
        } else if(key.contains("display")) {
            c.drawRoundRect(3,3,21,21,4,4,p);line(c,9,3,9,21);line(c,14,8,18,8);line(c,14,12,18,12);
        } else if(key.contains("replay") || key.contains("tutorial") || key.contains("help")) {
            c.drawCircle(12,12,9,p);path.reset();path.moveTo(10,8);path.lineTo(17,12);path.lineTo(10,16);path.close();c.drawPath(path,p);
        } else if(key.contains("month") || key.contains("year")) {
            c.drawRoundRect(3,5,21,21,3,3,p);line(c,3,10,21,10);line(c,8,2,8,7);line(c,16,2,16,7);
        } else { p.setStyle(Paint.Style.FILL);star(c,12,12,8); }
        p.setStyle(Paint.Style.FILL);c.restoreToCount(save);
    }
    @Override public void setAlpha(int value){alpha=value;invalidateSelf();}
    @Override public void setColorFilter(ColorFilter filter){p.setColorFilter(filter);invalidateSelf();}
    @Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
}
