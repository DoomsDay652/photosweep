package com.dominic.photosweep;

import android.content.Context;
import android.graphics.*;
import android.view.View;

/** Small native radio visualization. No bitmap frames and no work while hidden or paused. */
final class StudioWaveView extends View {
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path wave=new Path();
    private final int accent;
    private final boolean reduced,playing;
    StudioWaveView(Context context,int accent,boolean reduced,boolean playing){super(context);this.accent=accent;this.reduced=reduced;this.playing=playing;setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);}
    @Override protected void onDraw(Canvas c){
        float w=getWidth(),h=getHeight(),t=reduced||!playing?0:android.os.SystemClock.uptimeMillis()/2600f;
        paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(getResources().getDisplayMetrics().density);
        for(int k=0;k<7;k++){
            wave.reset();for(int i=0;i<=70;i++){float x=w*i/70f;float envelope=(float)Math.sin(Math.PI*i/70f);float y=h*.5f+(float)Math.sin(i*.13f+t+k*.3f)*envelope*h*(.12f+k*.025f);if(i==0)wave.moveTo(x,y);else wave.lineTo(x,y);}
            paint.setColor(accent);paint.setAlpha(55+k*23);c.drawPath(wave,paint);
        }
        if(!reduced&&playing&&isAttachedToWindow()&&getWindowVisibility()==VISIBLE&&isShown())postInvalidateDelayed(50);
    }
}
