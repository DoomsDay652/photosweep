package com.dominic.photosweep;
import android.content.Context;
import android.graphics.*;

/** Photo-mesh interactions for Halloween, using isolated painted props. */
final class SeasonalRenderer {
    static final int TRASH_MS=2700, KEEP_MS=2600;
    private static final int COLS=32, ROWS=36;
    private static Bitmap pumpkin,ghost;
    private final SwipeTheme.Kind kind;
    private Bitmap photo;
    private final float[] anchor=new float[8], mesh=new float[(COLS+1)*(ROWS+1)*2];
    final int[] colors=new int[(COLS+1)*(ROWS+1)];
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
    private final RectF rect=new RectF();private final Rect fragment=new Rect();
    private float cx,cy,pw,ph;
    SeasonalRenderer(Context context,SwipeTheme.Kind kind){
        this.kind=kind;
        if(pumpkin==null){BitmapFactory.Options opts=new BitmapFactory.Options();opts.inScaled=false;
            Bitmap sheet=BitmapFactory.decodeResource(context.getResources(),R.drawable.halloween_props_v1,opts);
            pumpkin=Bitmap.createBitmap(sheet,0,0,sheet.getWidth()/2,sheet.getHeight());
            ghost=Bitmap.createBitmap(sheet,sheet.getWidth()/2,0,sheet.getWidth()/2,sheet.getHeight());
        }
    }
    void begin(Bitmap value,Matrix pose){photo=value;float[] a={0,0,1,0,0,1,1,1};pose.mapPoints(a);System.arraycopy(a,0,anchor,0,8);cx=(a[0]+a[6])*.5f;cy=(a[1]+a[7])*.5f;pw=(float)Math.hypot(a[2]-a[0],a[3]-a[1]);ph=(float)Math.hypot(a[4]-a[0],a[5]-a[1]);}
    void clear(){photo=null;}
    boolean active(){return photo!=null&&!photo.isRecycled();}
    static float ease(float p){return SlasherMotion.ease(p);}
    static float clamp(float p){return Math.max(0,Math.min(1,p));}
    void drawSwipeCue(Canvas canvas,Matrix pose,float w,float h){
        float[] at={.5f,.5f};pose.mapPoints(at);float amount=ease(Math.abs(at[0]-w*.5f)/(w*.32f));if(amount==0)return;
        int color=0xB06C3518;
        paint.setShader(new RadialGradient(at[0]<w*.5f?0:w,at[1],w*.55f,new int[]{color,0},new float[]{0,1},Shader.TileMode.CLAMP));paint.setAlpha((int)(amount*180));canvas.drawRect(0,0,w,h,paint);paint.setShader(null);paint.setAlpha(255);
    }
    void draw(Canvas canvas,boolean keep,float phase,float w,float h,float density){
        if(!active())return;float p=clamp(phase);if(p>=1)return;
        if(keep)haunt(canvas,p,w,h);else pumpkin(canvas,p,w,h,density);
        paint.setAlpha(255);
    }
    private void build(float p,float x,float y,float scale,float angle,float curl){
        float c=(float)Math.cos(angle),s=(float)Math.sin(angle);
        for(int row=0;row<=ROWS;row++)for(int col=0;col<=COLS;col++){
            int i=row*(COLS+1)+col;float u=col/(float)COLS,v=row/(float)ROWS;
            float xx=((anchor[2]-anchor[0])*(u-.5f)+(anchor[4]-anchor[0])*(v-.5f))*scale;
            float yy=((anchor[3]-anchor[1])*(u-.5f)+(anchor[5]-anchor[1])*(v-.5f))*scale;
            yy+=(float)Math.sin(u*5.5f+p*9)*curl*(float)Math.sin(v*Math.PI);
            mesh[i*2]=x+xx*c-yy*s;mesh[i*2+1]=y+xx*s+yy*c;
            int alpha=255;
            int tone=(int)(255-Math.abs(curl)*.65f*(.5f+.5f*(float)Math.cos(u*5.5f+p*9)));
            colors[i]=(alpha<<24)|(tone<<16)|(tone<<8)|tone;
        }
    }
    private void paper(Canvas canvas){paint.setAlpha(255);canvas.drawBitmapMesh(photo,COLS,ROWS,mesh,0,colors,0,paint);}
    private void art(Canvas canvas,Bitmap bitmap,float x,float y,float width,float height,int alpha){rect.set(x-width*.5f,y-height*.5f,x+width*.5f,y+height*.5f);paint.setAlpha(alpha);canvas.drawBitmap(bitmap,null,rect,paint);paint.setAlpha(255);}
    private void haunt(Canvas canvas,float p,float w,float h){
        float fly=ease((p-.15f)/.85f),appear=ease(p/.20f);
        float x=cx+(w+pw)*fly,y=cy-(h+ph)*fly;
        float size=Math.max(pw*1.25f,w*.65f),gh=size*ghost.getHeight()/ghost.getWidth();
        art(canvas,ghost,x,y-ph*.10f,size,gh,(int)(255*appear));
        build(p,x,y+ph*.12f,1-.48f*appear,-.1f*appear,ph*.028f*appear);paper(canvas);
        int save=canvas.save();canvas.clipRect(0,y+ph*.24f,w,h);art(canvas,ghost,x,y-ph*.10f,size,gh,(int)(220*appear));canvas.restoreToCount(save);
    }
    private void pumpkin(Canvas canvas,float p,float w,float h,float density){
        float enter=ease(p/.20f),swallow=ease((p-.18f)/.60f),leave=ease((p-.80f)/.20f);
        float size=Math.max(pw*1.12f,w*.60f),height=size*pumpkin.getHeight()/pumpkin.getWidth();
        float x=-size*.65f+(cx+size*.65f)*enter-(w+size)*leave,y=cy+ph*.08f;
        art(canvas,pumpkin,x,y,size,height,255);
        float fold=1-.98f*swallow;
        build(p,cx+(x-cx)*swallow,cy+(y+height*.15f-cy)*swallow,fold,-.24f*swallow,ph*.06f*swallow*(1-swallow));
        if(swallow<.995f)paper(canvas);
        // The physical lower pumpkin teeth occlude the folding print as it enters the mouth.
        int save=canvas.save();canvas.clipRect(x-size*.5f,y+height*.17f,x+size*.5f,h*2);art(canvas,pumpkin,x,y,size,height,255);canvas.restoreToCount(save);
    }
}
