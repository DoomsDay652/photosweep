package com.dominic.photosweep;
import android.content.Context;
import android.graphics.*;

/** Photo-mesh interactions for Halloween and Summer, using isolated painted props. */
final class SeasonalRenderer {
    static final int TRASH_MS=2700, KEEP_MS=2600;
    private static final int COLS=32, ROWS=36;
    private static final int WCOLS=14,WROWS=32;
    private static Bitmap pumpkin,ghost,wave;
    private final SwipeTheme.Kind kind;
    final BeachSharkRenderer beach;
    private final Matrix beachPose=new Matrix();
    private long beachRelease;
    private Bitmap photo;
    private final float[] anchor=new float[8], mesh=new float[(COLS+1)*(ROWS+1)*2];
    final int[] colors=new int[(COLS+1)*(ROWS+1)];
    private final float[] waterMesh=new float[(WCOLS+1)*(WROWS+1)*2];
    private final int[] waterColors=new int[(WCOLS+1)*(WROWS+1)];
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
    private final RectF rect=new RectF();private final Rect fragment=new Rect();
    private float cx,cy,pw,ph;
    SeasonalRenderer(Context context,SwipeTheme.Kind kind){
        this.kind=kind;beach=kind==SwipeTheme.Kind.SUMMER?new BeachSharkRenderer(context):null;
        if(pumpkin==null){BitmapFactory.Options opts=new BitmapFactory.Options();opts.inScaled=false;
            Bitmap sheet=BitmapFactory.decodeResource(context.getResources(),R.drawable.halloween_props_v1,opts);
            pumpkin=Bitmap.createBitmap(sheet,0,0,sheet.getWidth()/2,sheet.getHeight());
            ghost=Bitmap.createBitmap(sheet,sheet.getWidth()/2,0,sheet.getWidth()/2,sheet.getHeight());
            wave=BitmapFactory.decodeResource(context.getResources(),R.drawable.beach_keep_surf_v3,opts);
        }
    }
    void begin(Bitmap value,Matrix pose){photo=value;if(beach!=null){beach.bind(value,pose);beachPose.set(pose);beachRelease=android.os.SystemClock.uptimeMillis();}float[] a={0,0,1,0,0,1,1,1};pose.mapPoints(a);System.arraycopy(a,0,anchor,0,8);cx=(a[0]+a[6])*.5f;cy=(a[1]+a[7])*.5f;pw=(float)Math.hypot(a[2]-a[0],a[3]-a[1]);ph=(float)Math.hypot(a[4]-a[0],a[5]-a[1]);}
    void clear(){photo=null;if(beach!=null)beach.clear();}
    boolean active(){return photo!=null&&!photo.isRecycled();}
    static float ease(float p){return SlasherMotion.ease(p);}
    static float clamp(float p){return Math.max(0,Math.min(1,p));}
    void drawSwipeCue(Canvas canvas,Matrix pose,float w,float h){
        float[] at={.5f,.5f};pose.mapPoints(at);float amount=ease(Math.abs(at[0]-w*.5f)/(w*.32f));if(amount==0)return;
        if(kind==SwipeTheme.Kind.SUMMER&&at[0]<=w*.5f)return;
        if(kind==SwipeTheme.Kind.SUMMER&&at[0]>w*.5f){
            float t=(android.os.SystemClock.uptimeMillis()%12000)/1000f;
            water(canvas,w+w*.20f-w*.25f*amount,h*.5f,w*1.6f,h*1.45f,t,amount,false);
            return;
        }
        int color=kind==SwipeTheme.Kind.SUMMER?0x9052D8D9:0xB06C3518;
        paint.setShader(new RadialGradient(at[0]<w*.5f?0:w,at[1],w*.55f,new int[]{color,0},new float[]{0,1},Shader.TileMode.CLAMP));paint.setAlpha((int)(amount*180));canvas.drawRect(0,0,w,h,paint);paint.setShader(null);paint.setAlpha(255);
    }
    void draw(Canvas canvas,boolean keep,float phase,float w,float h,float density){
        if(!active())return;float p=clamp(phase);if(p>=1)return;
        if(kind==SwipeTheme.Kind.SUMMER){if(keep)surf(canvas,p,w,h);else beachTrash(canvas,p,w,h);}
        else {if(keep)haunt(canvas,p,w,h);else pumpkin(canvas,p,w,h,density);}
        paint.setAlpha(255);
    }
    private void build(float p,float x,float y,float scale,float angle,boolean bites,float curl){
        float c=(float)Math.cos(angle),s=(float)Math.sin(angle);
        for(int row=0;row<=ROWS;row++)for(int col=0;col<=COLS;col++){
            int i=row*(COLS+1)+col;float u=col/(float)COLS,v=row/(float)ROWS;
            float xx=((anchor[2]-anchor[0])*(u-.5f)+(anchor[4]-anchor[0])*(v-.5f))*scale;
            float yy=((anchor[3]-anchor[1])*(u-.5f)+(anchor[5]-anchor[1])*(v-.5f))*scale;
            yy+=(float)Math.sin(u*5.5f+p*9)*curl*(float)Math.sin(v*Math.PI);
            mesh[i*2]=x+xx*c-yy*s;mesh[i*2+1]=y+xx*s+yy*c;
            int alpha=255;
            if(beach!=null&&beach.missing(Math.round(u*BeachSharkRenderer.X),Math.round(v*BeachSharkRenderer.Y)))alpha=0;
            int tone=(int)(255-Math.abs(curl)*.65f*(.5f+.5f*(float)Math.cos(u*5.5f+p*9)));
            colors[i]=(alpha<<24)|(tone<<16)|(tone<<8)|tone;
        }
    }
    private void paper(Canvas canvas){paint.setAlpha(255);canvas.drawBitmapMesh(photo,COLS,ROWS,mesh,0,colors,0,paint);}
    private void art(Canvas canvas,Bitmap bitmap,float x,float y,float width,float height,int alpha){rect.set(x-width*.5f,y-height*.5f,x+width*.5f,y+height*.5f);paint.setAlpha(alpha);canvas.drawBitmap(bitmap,null,rect,paint);paint.setAlpha(255);}
    private void uv(float u,float v,float[] at){at[0]=anchor[0]+(anchor[2]-anchor[0])*u+(anchor[4]-anchor[0])*v;at[1]=anchor[1]+(anchor[3]-anchor[1])*u+(anchor[5]-anchor[1])*v;}
    private final float[] contact=new float[2];
    private void beachTrash(Canvas canvas,float p,float w,float h){
        if(!beach.releasing())beach.beginRelease(photo,beachPose,beachRelease,w,h);
        beach.drawRelease(canvas,p,w,h);
    }
    private void surf(Canvas canvas,float p,float w,float h){
        float pickup=ease((p-.12f)/.24f),haul=(float)Math.pow(clamp((p-.34f)/.66f),1.75);
        float width=Math.max(w*1.6f,pw*1.55f),height=Math.max(h*1.45f,ph*2.7f);
        float left=Math.min(Math.min(anchor[0],anchor[2]),Math.min(anchor[4],anchor[6]));
        float heldAmount=ease((cx-w*.5f)/(w*.32f));
        float front=lerp(w+w*.20f-w*.25f*heldAmount,left-pw*.10f,ease(p/.34f));
        float carry=Math.max(w*.4f,w-left+width*.40f+pw*.12f)*haul;
        float lift=h*.030f*pickup*(1-.4f*haul),drift=-h*.045f*haul;
        float opacity=lerp(heldAmount,1,ease(p/.10f));
        float seconds=(beachRelease%12000)/1000f+p*2.6f;
        // Water is behind the print; only the frothy leading lip returns above it.
        water(canvas,front+carry,h*.5f+drift,width,height,seconds,opacity,false);
        float angle=-.055f*pickup+.10f*haul,c=(float)Math.cos(angle),s=(float)Math.sin(angle);
        for(int row=0;row<=ROWS;row++)for(int col=0;col<=COLS;col++){
            int i=row*(COLS+1)+col;float u=col/(float)COLS,v=row/(float)ROWS;
            float ox=anchor[0]+(anchor[2]-anchor[0])*u+(anchor[4]-anchor[0])*v;
            float oy=anchor[1]+(anchor[3]-anchor[1])*u+(anchor[5]-anchor[1])*v;
            float wet=ease((ox-front+pw*.08f)/Math.max(1,pw*.20f));
            float distance=(ox-front)/Math.max(1,pw*.24f);
            float ridge=(float)Math.exp(-distance*distance);
            float ripple=(float)Math.sin(ox/Math.max(1,pw)*7.5f+v*2.0f-p*13f);
            float dx=(ox-cx)*(1-.055f*haul),dy=(oy-cy)*(1-.055f*haul);
            mesh[i*2]=cx+carry+dx*c-dy*s-w*.016f*pickup*(1-haul);
            mesh[i*2+1]=cy+drift+dx*s+dy*c-lift*(.35f+ridge)*wet+ph*.014f*ripple*wet*pickup;
            int alpha=beach!=null&&beach.missing(Math.round(u*BeachSharkRenderer.X),Math.round(v*BeachSharkRenderer.Y))?0:255;
            float shade=1-.065f*wet*(.55f+.45f*ridge);
            int red=(int)(255*shade),green=(int)(255*(1-.025f*wet)),blue=255;
            colors[i]=alpha<<24|red<<16|green<<8|blue;
        }
        paper(canvas);
        water(canvas,front+carry,h*.5f+drift,width,height,seconds,opacity,true);
    }
    private static float lerp(float a,float b,float p){return a+(b-a)*p;}
    private void water(Canvas canvas,float front,float centerY,float width,float height,float seconds,float opacity,boolean foreground){
        for(int row=0;row<=WROWS;row++)for(int col=0;col<=WCOLS;col++){
            int i=row*(WCOLS+1)+col;float u=col/(float)WCOLS,v=row/(float)WROWS;
            float envelope=(float)Math.sin(v*Math.PI);
            float roll=(float)Math.sin(v*12-seconds*2.5f)*width*.018f*envelope;
            float flow=(float)Math.sin(v*7+seconds*2.1f+u*3)*height*.009f*envelope;
            waterMesh[i*2]=front+(u-.27f)*width+roll;
            waterMesh[i*2+1]=centerY+(v-.5f)*height+flow;
            // Feather by UV alpha, avoiding a straight clipping seam over the photo.
            float mask=(foreground?clamp((.44f-u)/.14f):1)*ease(u/.17f);
            waterColors[i]=((int)(255*opacity*mask)<<24)|0x00FFFFFF;
        }
        paint.setAlpha(255);canvas.drawBitmapMesh(wave,WCOLS,WROWS,waterMesh,0,waterColors,0,paint);
    }
    private void haunt(Canvas canvas,float p,float w,float h){
        float fly=ease((p-.15f)/.85f),appear=ease(p/.20f);
        float x=cx+(w+pw)*fly,y=cy-(h+ph)*fly;
        float size=Math.max(pw*1.25f,w*.65f),gh=size*ghost.getHeight()/ghost.getWidth();
        art(canvas,ghost,x,y-ph*.10f,size,gh,(int)(255*appear));
        build(p,x,y+ph*.12f,1-.48f*appear,-.1f*appear,false,ph*.028f*appear);paper(canvas);
        int save=canvas.save();canvas.clipRect(0,y+ph*.24f,w,h);art(canvas,ghost,x,y-ph*.10f,size,gh,(int)(220*appear));canvas.restoreToCount(save);
    }
    private void pumpkin(Canvas canvas,float p,float w,float h,float density){
        float enter=ease(p/.20f),swallow=ease((p-.18f)/.60f),leave=ease((p-.80f)/.20f);
        float size=Math.max(pw*1.12f,w*.60f),height=size*pumpkin.getHeight()/pumpkin.getWidth();
        float x=-size*.65f+(cx+size*.65f)*enter-(w+size)*leave,y=cy+ph*.08f;
        art(canvas,pumpkin,x,y,size,height,255);
        float fold=1-.98f*swallow;
        build(p,cx+(x-cx)*swallow,cy+(y+height*.15f-cy)*swallow,fold,-.24f*swallow,false,ph*.06f*swallow*(1-swallow));
        if(swallow<.995f)paper(canvas);
        // The physical lower pumpkin teeth occlude the folding print as it enters the mouth.
        int save=canvas.save();canvas.clipRect(x-size*.5f,y+height*.17f,x+size*.5f,h*2);art(canvas,pumpkin,x,y,size,height,255);canvas.restoreToCount(save);
    }
}
