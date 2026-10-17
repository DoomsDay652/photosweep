package com.dominic.photosweep;

import android.content.Context;
import android.graphics.*;

/** Overhead shore: one moving waterline drives both the foam and floating props. */
final class BeachBackgroundRenderer {
    private static final int COLS=12,ROWS=20,COUNT=12;
    private final Bitmap sand,surf,props;
    private final Paint paint=new Paint(3);
    private final float[] mesh=new float[(COLS+1)*(ROWS+1)*2];
    private final int[] tint=new int[(COLS+1)*(ROWS+1)];
    final float[] x=new float[COUNT],y=new float[COUNT],vx=new float[COUNT],vy=new float[COUNT],spin=new float[COUNT];
    private final Rect source=new Rect();
    private final RectF dest=new RectF();
    private long last=-1;
    private float elapsed;
    BeachBackgroundRenderer(Context context){
        BitmapFactory.Options opts=new BitmapFactory.Options();opts.inScaled=false;
        sand=BitmapFactory.decodeResource(context.getResources(),R.drawable.theme_beach_shore_v8,opts);
        surf=BitmapFactory.decodeResource(context.getResources(),R.drawable.beach_keep_surf_v3,opts);
        props=BitmapFactory.decodeResource(context.getResources(),R.drawable.beach_props_v8,opts);
        for(int i=0;i<COUNT;i++){
            x[i]=.08f+((i*37)%85)/100f;y[i]=.08f+((i*23)%86)/100f;spin[i]=i*43;
        }
    }
    // Water arrives from below. The same function is sampled for buoyancy,
    // flow velocity and the foam mesh; objects cannot react to an invisible wave.
    static float shoreline(float t,float x){
        return .735f-.135f*(float)Math.sin(t*.83f)+.014f*(float)Math.sin(x*11+t*.7f)
                +.007f*(float)Math.sin(x*23-t*.9f);
    }
    void draw(Canvas canvas,long now,float w,float h,boolean still){
        if(w<=0||h<=0)return;
        float dt=last<0?0:Math.max(0,Math.min(.05f,(now-last)/1000f));last=now;
        if(!still)elapsed+=dt;
        float t=still?0:elapsed;
        dest.set(0,0,w,h);paint.setAlpha(255);canvas.drawBitmap(sand,null,dest,paint);
        for(int row=0;row<=ROWS;row++)for(int col=0;col<=COLS;col++){
            int i=row*(COLS+1)+col;float u=col/(float)COLS,v=row/(float)ROWS;
            float sx=1.1f-v*1.2f;
            mesh[i*2]=sx*w;
            mesh[i*2+1]=(shoreline(t,sx)+(u-.28f)*.82f)*h;
            int alpha=(int)(255*Math.max(0,Math.min(1,(u-.10f)/.17f)));
            tint[i]=alpha<<24|0xFFFFFF;
        }
        canvas.drawBitmapMesh(surf,COLS,ROWS,mesh,0,tint,0,paint);
        for(int i=0;i<COUNT;i++){
            if(!still)step(i,t,dt);
            float radius=w*(i%6==2?.055f:.046f);
            float bob=y[i]>shoreline(t,x[i])?h*.002f*(float)Math.sin(t*2.6f+i):0;
            float px=x[i]*w,py=y[i]*h+bob;
            int tw=props.getWidth()/3,th=props.getHeight()/2,n=i%6;
            source.set(n%3*tw,n/3*th,(n%3+1)*tw,(n/3+1)*th);
            int save=canvas.save();canvas.rotate(spin[i],px,py);
            dest.set(px-radius,py-radius,px+radius,py+radius);
            canvas.drawBitmap(props,source,dest,paint);canvas.restoreToCount(save);
        }
    }
    private void step(int i,float t,float dt){
        float front=shoreline(t,x[i]);
        if(y[i]>front-.018f){
            float flow=(shoreline(t+.02f,x[i])-front)/.02f;
            // Drag toward the moving swash, with a little along-shore drift.
            float target=flow*.86f+(front+.045f-y[i])*1.6f;
            vy[i]+=(target-vy[i])*(1-(float)Math.exp(-dt*5));
            vx[i]+=((float)Math.sin(t*.65f+i)*.034f-vx[i])*(1-(float)Math.exp(-dt*2));
            spin[i]+=dt*(flow*190+(float)Math.sin(t+i)*12);
        }else{
            // Slow tumbling fall, like the Candy scene, until the surf catches it.
            vy[i]=Math.min(.065f,vy[i]+dt*.035f);
            vx[i]*=(float)Math.exp(-dt*1.2f);spin[i]+=dt*(5+i%4*2);
        }
        x[i]+=vx[i]*dt;y[i]+=vy[i]*dt;
        if(x[i]<.045f){x[i]=.045f;vx[i]=Math.abs(vx[i])*.5f;}
        if(x[i]>.955f){x[i]=.955f;vx[i]=-Math.abs(vx[i])*.5f;}
        if(y[i]>1.08f){y[i]=-.06f;x[i]=.08f+((i*37+(int)(t*7))%85)/100f;vy[i]=.025f;}
    }
}
