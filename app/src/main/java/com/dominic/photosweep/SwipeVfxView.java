package com.dominic.photosweep;

import android.content.Context;
import android.graphics.*;
import android.os.SystemClock;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import java.util.Random;

/** Six UV-based material reactions, with the original saw/light renderer retained for other themes. */
class SwipeVfxView extends View {
    interface Sprites { Bitmap get(boolean keep); }
    float progress;
    private final PhotoMaterialRenderer materials=new PhotoMaterialRenderer();
    private final java.util.ArrayList<KeptPhoto> keptPhotos=new java.util.ArrayList<>();
    private boolean keepDetached;
    private long materialStarted;
    boolean hasRetainedDamage(){return elemental && bitmap!=null && materials.motion.hasDamage();}
    private float materialSeconds(long now){if(materialStarted==0)materialStarted=now;return (now-materialStarted)/1000f;}
    private static final class KeptPhoto {
        final PhotoMaterialRenderer renderer;final Bitmap photo;final long born;final float phase;
        KeptPhoto(PhotoMaterialRenderer renderer,Bitmap photo,long born,float phase) {
            this.renderer=renderer;this.photo=photo;this.born=born;this.phase=phase;
        }
    }
    boolean hasIndependentKeep(){return elemental;}
    private float keepPhase(){return SwipeMotion.clamp((photo.right-restRight)/Math.max(dp(120),getWidth()*.72f));}
    private void drawKeptPhotos(Canvas canvas,long now) {
        for(int i=keptPhotos.size()-1;i>=0;i--) {
            KeptPhoto kept=keptPhotos.get(i);
            float age=(now-kept.born)/1000f;
            float duration=kind==SwipeTheme.Kind.WATER?2.2f:kind==SwipeTheme.Kind.SPACE?1.35f:1.75f;
            if(age>=duration || kept.photo.isRecycled()){keptPhotos.remove(i);continue;}
            float phase=kept.phase+(1-kept.phase)*PhotoMaterialMotion.smooth(age/.42f);
            float flight=PhotoMaterialMotion.clamp((age-.42f)/(duration-.42f));
            kept.renderer.draw(canvas,kept.photo,kind,age,dp(1),getWidth(),getHeight(),true,phase,flight,1);
        }
    }
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final RectF photo = new RectF(), target = new RectF();
    private final Path path = new Path();
    private final boolean enabled, reducedMotion;
    private final SwipeTheme.Kind kind;
    private final boolean elemental;
    private final Paint elementPaint=new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        private final RectF bladeRect = new RectF();
    private static Bitmap bladeArt;
    private static float[][] bladeOutline;
    private final Bitmap sawBlade;
    private final RectF sleeveRect = new RectF();
    private final Path lightPath = new Path(), ribbonPath = new Path();
    private final PathMeasure measure = new PathMeasure();
    private final float[] position = new float[2];
    private final Path visiblePath = new Path(), localClip = new Path();
    private final Matrix inverse = new Matrix();
    private static Bitmap glowArt, rayArt,elementAtlas;
    private static final int ATLAS_COLUMNS=3,ATLAS_ROWS=2;
    private final Rect atlasSource=new Rect();
    private final RectF receiverBounds=new RectF(),trashBounds=new RectF();
    private final float[] materialHit=new float[2],previousTransform=new float[9];
    private final float[] freshX=new float[MASK_W*MASK_H],freshY=new float[MASK_W*MASK_H];
    private final float[] elementEdge=new float[1025],innerEdge=new float[1025];
    private static final Bitmap[] detailCache=new Bitmap[6],elementTiles=new Bitmap[6];
    private Bitmap textureArt;
    private int atlasTile=-1;
    private Bitmap detailArt;
    private float maximumContact;
    private final float[] fromPose=new float[6],toPose=new float[6],sweptPoses=new float[33*6];
    private final Paint materialPaint=new Paint(Paint.FILTER_BITMAP_FLAG);
    private final int[] edgeTintPixels=new int[MASK_W*MASK_H];
    private Bitmap edgeTint;
    private static final int MASK_W=192,MASK_H=256,MESH_X=40,MESH_Y=40;
    private final RectF unitRect=new RectF(0,0,1,1);
    private final Matrix photoToStage=new Matrix(),stageToPhoto=new Matrix(),maskToLocal=new Matrix();
    private final float[] corners=new float[8],transform=new float[9],contactPoint=new float[2];
    private final Path photoQuad=new Path(),stageBoundary=new Path();
    private final int[] damagePixels=new int[MASK_W*MASK_H],freshCells=new int[MASK_W*MASK_H];
    private final float[] freshWeights=new float[MASK_W*MASK_H],toothRows=new float[1025];
    private final Paint erasePaint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
    private Bitmap damageBitmap;
    private boolean damaged;
    private float visibleLeft,entryEnergy;
    private final float[] foldedPhoto=new float[(MESH_X+1)*(MESH_Y+1)*2];
    private final Path aperture=new Path(), backRim=new Path(), foldWindow=new Path(), innerRim=new Path();
    private final Path glitterBand=new Path();
    private final float[] bandX=new float[49],bandY=new float[49],bandWidth=new float[49];
    private float portalX,portalRadius,portalHeight;
    private long mendStarted;
    private View clippedPhoto;
    private float lightAlpha;
    private long geometryFrame;

    /** The photo and light use the very same aperture, including during a cancelled drag. */
    static class Card extends FrameLayout {
        SwipeVfxView effect;
        Card(Context context) { super(context); }
        @Override protected void dispatchDraw(Canvas canvas) {
            int save=effect==null?canvas.save():effect.beginPhoto(canvas,this);
            super.dispatchDraw(canvas);
            if(effect!=null)effect.erasePhotoDamage(canvas,this);
            canvas.restoreToCount(save);
        }
    }
    static class PreviewPhoto extends ImageView {
        SwipeVfxView effect;
        PreviewPhoto(Context context) { super(context); }
        @Override protected void onDraw(Canvas canvas) {
            int save=effect==null?canvas.save():effect.beginPhoto(canvas,this);
            super.onDraw(canvas);
            if(effect!=null)effect.erasePhotoDamage(canvas,this);
            canvas.restoreToCount(save);
        }
    }
    private boolean holding;
    private boolean holdEffectsVisible;
    private long returning, closing, lastFrame;
    private float restRight, open, returnOpen, returnAlpha;
    private float haloX, haloY;
    private final int intensity, speed;
    private final Random random = new Random();
    private final Chip[] chips = new Chip[480];
    private Bitmap bitmap;
    private long released, started;
    private float feed;
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
        super(context); this.enabled=enabled; this.intensity=intensity; this.speed=speed;
        this.reducedMotion=reducedMotion;this.kind=kind;this.elemental=PhotoInteraction.supports(kind)&&enabled&&!reducedMotion;
        // Share immutable textures; decode once at bounded sizes, never in a draw frame.
        BitmapFactory.Options options=new BitmapFactory.Options();options.inScaled=false;options.inSampleSize=4;
        if(bladeArt==null || bladeArt.isRecycled())
            bladeArt=BitmapFactory.decodeResource(context.getResources(),R.drawable.grinder_saw_blade,options);
        sawBlade=bladeArt;
        if(elemental) {
            if(elementAtlas==null || elementAtlas.isRecycled()) {
                BitmapFactory.Options atlasOptions=new BitmapFactory.Options();atlasOptions.inScaled=false;
                elementAtlas=BitmapFactory.decodeResource(context.getResources(),R.drawable.swipe_element_atlas,atlasOptions);
            }
            switch(kind) {
                case FIRE:atlasTile=0;break;case WATER:atlasTile=1;break;case ICE:atlasTile=2;break;
                case TOXIC:atlasTile=3;break;case SPACE:atlasTile=4;break;case CANDY:atlasTile=5;break;
                default:break;
            }
            int detailId=kind==SwipeTheme.Kind.FIRE?R.drawable.fx_flame:kind==SwipeTheme.Kind.WATER?R.drawable.fx_water_drop:
                    kind==SwipeTheme.Kind.ICE?R.drawable.fx_ice:kind==SwipeTheme.Kind.TOXIC?R.drawable.fx_toxic_drop:
                    kind==SwipeTheme.Kind.SPACE?R.drawable.fx_nebula:R.drawable.fx_candy;
            BitmapFactory.Options detailOptions=new BitmapFactory.Options();detailOptions.inScaled=false;detailOptions.inSampleSize=2;
            if(detailCache[atlasTile]==null || detailCache[atlasTile].isRecycled())
                detailCache[atlasTile]=BitmapFactory.decodeResource(context.getResources(),detailId,detailOptions);
            detailArt=detailCache[atlasTile];
            if(elementAtlas!=null) {
                int tw=elementAtlas.getWidth()/ATLAS_COLUMNS,th=elementAtlas.getHeight()/ATLAS_ROWS;
                atlasSource.set(atlasTile%3*tw,atlasTile/3*th,(atlasTile%3+1)*tw,(atlasTile/3+1)*th);
                // The alpha outline is the collider: even the outer flame tongues and crystal tips cut.
                int[] pixels=new int[tw*th];elementAtlas.getPixels(pixels,0,tw,atlasSource.left,atlasSource.top,tw,th);
                // Isolate and feather the tile gutter once. Filtering can never sample an adjacent sprite or a matte edge.
                for(int y=0;y<th;y++)for(int x=0;x<tw;x++) {
                    int i=y*tw+x,gutter=Math.min(Math.min(x,tw-1-x),Math.min(y,th-1-y));
                    int alpha=Math.round((pixels[i]>>>24)*SwipeMotion.clamp((gutter-2)/12f));
                    pixels[i]=alpha==0?0:(alpha<<24)|(pixels[i]&0xFFFFFF);
                }
                if(elementTiles[atlasTile]==null || elementTiles[atlasTile].isRecycled()) {
                    elementTiles[atlasTile]=Bitmap.createBitmap(tw,th,Bitmap.Config.ARGB_8888);
                    elementTiles[atlasTile].setPixels(pixels,0,tw,0,0,tw,th);
                }
                textureArt=elementTiles[atlasTile];materials.setArtwork(textureArt,kind==SwipeTheme.Kind.FIRE?detailArt:null);
                materials.setMaterialArt(PhotoMaterialArt.load(context));
                detailArt=PhotoMaterialArt.load(context).debris[atlasTile];
                for(int row=0;row<elementEdge.length;row++) {
                    int y=Math.min(th-1,row*th/(elementEdge.length-1));float edge=0;
                    for(int x=tw-1;x>=0;x--)if((pixels[y*tw+x]>>>24)>100){edge=(x+.5f)/tw;break;}
                    elementEdge[row]=edge;
                    float inner=.5f;
                    for(int x=tw/2;x>=0;x--)if((pixels[y*tw+x]>>>24)>130){inner=(x+1f)/tw;break;}
                    innerEdge[row]=inner;
                }
                float[] smooth=innerEdge.clone();
                for(int row=0;row<innerEdge.length;row++) {
                    float total=0;int count=0;
                    for(int j=Math.max(0,row-12);j<=Math.min(innerEdge.length-1,row+12);j++){total+=smooth[j];count++;}
                    innerEdge[row]=total/count;
                }
            }
        }
        if(bladeOutline==null && sawBlade!=null)buildBladeOutline(sawBlade);
        if(glowArt==null) {
            glowArt=Bitmap.createBitmap(96,96,Bitmap.Config.ARGB_8888);
            Canvas glowCanvas=new Canvas(glowArt);Paint soft=new Paint(Paint.ANTI_ALIAS_FLAG);
            soft.setShader(new RadialGradient(48,48,48,
                    new int[]{0xFFFFF8E5,0xCCFFE4A4,0x55EAB761,0x14D58E36,0x00D58E36},
                    new float[]{0,.08f,.28f,.56f,1},Shader.TileMode.CLAMP));
            glowCanvas.drawCircle(48,48,48,soft);
        }
        if(rayArt==null) {
            // Small soft beam texture, generated once; no shaders or bitmaps allocated per frame.
            int w=160,h=96;int[] pixels=new int[w*h];
            for(int x=0;x<w;x++) {
                float t=x/(float)(w-1),spread=2+36*(float)Math.pow(t,.8);
                float lengthFade=(float)Math.pow(Math.sin(Math.PI*t),.85)*(1-t);
                for(int y=0;y<h;y++) {
                    float d=(y-h*.5f)/spread;
                    int a=(int)(210*lengthFade*Math.exp(-d*d*3.2));
                    pixels[y*w+x]=(a<<24)|(255<<16)|((int)(246-46*t)<<8)|(int)(216-85*t);
                }
            }
            rayArt=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);rayArt.setPixels(pixels,0,w,0,0,w,h);
        }
        erasePaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_OUT));
        for(int i=0;i<chips.length;i++)chips[i]=new Chip();
        resetBites();
        setClickable(false);setFocusable(false);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }
    private float dp(float value) { return value*getResources().getDisplayMetrics().density; }
    void setHoldEffectsVisible(boolean value) { holdEffectsVisible=value;invalidate(); }
    private boolean heldEffectsActive() { return holdEffectsVisible && (holding || returning!=0 || released!=0); }
    private float heldEffectsAlpha(long now) {
        if(holding)return 1;
        if(returning!=0)return 1-SwipeMotion.ease((now-returning)/280f);
        if(released!=0)return 1-SwipeMotion.ease((now-released)/(float)(reducedMotion?80:releaseDuration(progress>0)));
        return 0;
    }
    protected void onGrinderShake(float offset) { }
    float grinderEdge() { return 0; }
    private static void buildBladeOutline(Bitmap blade) {
        // Contact follows the opaque rotating teeth, not the transparent square around the art.
        int w=blade.getWidth(),h=blade.getHeight();int[] pixels=new int[w*h];
        blade.getPixels(pixels,0,w,0,0,w,h);bladeOutline=new float[72][137];
        for(int phase=0;phase<72;phase++) {
            double a=Math.toRadians(phase*5),cos=Math.cos(a),sin=Math.sin(a);
            for(int row=0;row<137;row++) {
                float y=row*.5f-34,edge=-34;
                for(float x=34;x>=-34;x-=.5f) {
                    int sx=(int)(((x*cos+y*sin)/68+.5)*w);
                    int sy=(int)(((-x*sin+y*cos)/68+.5)*h);
                    if(sx>=0 && sx<w && sy>=0 && sy<h && (pixels[sy*w+sx]>>>24)>96){edge=x;break;}
                }
                bladeOutline[phase][row]=edge;
            }
        }
    }
    private float grinderTop() { return dp(5); }
    private float grinderBottom() { return Math.max(grinderTop(),getHeight()-dp(5)); }
    private float toothEdge(float y,float time) {
        if(elemental && elementAtlas!=null) {
            int row=Math.max(0,Math.min(elementEdge.length-1,Math.round(y/Math.max(1,getHeight())*(elementEdge.length-1))));
            return Math.max(0,trashBounds.left+elementEdge[row]*trashBounds.width());
        }
        float top=grinderTop(),spacing=dp(49),edge=0;
        if(y<top || y>=grinderBottom())return 0;
        int nearest=Math.max(0,(int)((y-top)/spacing));
        for(int index=Math.max(0,nearest-1);index<=nearest+1;index++) {
            float offset=(y-(top+index*spacing))/dp(1);
            if(Math.abs(offset)>34)continue;
            float angle=(time*115*(index%2==0?1:-1)+index*29)%360;
            int phase=((Math.round(angle/5)%72)+72)%72;
            int row=Math.max(0,Math.min(136,Math.round((offset+34)*2)));
            float reach=bladeOutline==null?0:bladeOutline[phase][row]-16;
            edge=Math.max(edge,dp(reach));
        }
        return edge;
    }
    private void resetBites() {
        materials.reset();materialStarted=0;
        damaged=false;mendStarted=0;
        java.util.Arrays.fill(damagePixels,0);
        if(edgeTint!=null){java.util.Arrays.fill(edgeTintPixels,0);edgeTint.setPixels(edgeTintPixels,0,MASK_W,0,0,MASK_W,MASK_H);}
        if(damageBitmap!=null)damageBitmap.setPixels(damagePixels,0,MASK_W,0,0,MASK_W,MASK_H);
    }
    void setPhoto(Bitmap image,RectF bounds,float dx,float dy) {
        photoToStage.setRectToRect(unitRect,bounds,Matrix.ScaleToFit.FILL);
        photoToStage.postTranslate(dx,dy);updatePhoto(image);
    }
    void setTransformedPhoto(Bitmap image,RectF bounds,View view) {
        photoToStage.setRectToRect(unitRect,bounds,Matrix.ScaleToFit.FILL);
        photoToStage.postConcat(view.getMatrix());photoToStage.postTranslate(view.getLeft(),view.getTop());
        updatePhoto(image);
    }
    private void updatePhoto(Bitmap image) {
        bitmap=image;
        corners[0]=0;corners[1]=0;corners[2]=1;corners[3]=0;
        corners[4]=1;corners[5]=1;corners[6]=0;corners[7]=1;
        photoToStage.mapPoints(corners);photoToStage.getValues(transform);photoToStage.invert(stageToPhoto);
        photo.set(corners[0],corners[1],corners[0],corners[1]);photoQuad.reset();photoQuad.moveTo(corners[0],corners[1]);
        for(int i=2;i<8;i+=2) {
            photo.left=Math.min(photo.left,corners[i]);photo.right=Math.max(photo.right,corners[i]);
            photo.top=Math.min(photo.top,corners[i+1]);photo.bottom=Math.max(photo.bottom,corners[i+1]);
            photoQuad.lineTo(corners[i],corners[i+1]);
        }
        photoQuad.close();if(!tracked)restRight=photo.right;
        if(!tracked)System.arraycopy(transform,0,previousTransform,0,9);
        if(elemental) {
            toPose[0]=transform[0];toPose[1]=transform[1];toPose[2]=transform[2];
            toPose[3]=transform[3];toPose[4]=transform[4];toPose[5]=transform[5];materials.motion.pose(toPose);
        }
        updateDamage();System.arraycopy(transform,0,previousTransform,0,9);tracked=true;invalidate();
    }
    private boolean trashActive() { return enabled&&!reducedMotion&&(holding || (released!=0 && progress<0)); }
    private void prepareTrashBoundary(float time) {
        trashBounds.set(-dp(132),0,dp(42),getHeight());
        maximumContact=0;
        for(int i=0;i<toothRows.length;i++) {
            toothRows[i]=toothEdge(getHeight()*i/1024f,time);
            maximumContact=Math.max(maximumContact,toothRows[i]);
        }
    }
    private void updateDamage() {
        visibleLeft=photo.left;
        if(!trashActive() || !tracked || bitmap==null || bitmap.isRecycled() || getHeight()<=0)return;
        float time=(SystemClock.uptimeMillis()-started)/1000f*speed/100f;
        prepareTrashBoundary(time);
        float a=transform[Matrix.MSCALE_X],b=transform[Matrix.MSKEW_X],c=transform[Matrix.MTRANS_X];
        float d=transform[Matrix.MSKEW_Y],e=transform[Matrix.MSCALE_Y],f=transform[Matrix.MTRANS_Y];
        float pa=previousTransform[Matrix.MSCALE_X],pb=previousTransform[Matrix.MSKEW_X],pc=previousTransform[Matrix.MTRANS_X];
        float pd=previousTransform[Matrix.MSKEW_Y],pe=previousTransform[Matrix.MSCALE_Y],pf=previousTransform[Matrix.MTRANS_Y];
        fromPose[0]=pa;fromPose[1]=pb;fromPose[2]=pc;fromPose[3]=pd;fromPose[4]=pe;fromPose[5]=pf;
        toPose[0]=a;toPose[1]=b;toPose[2]=c;toPose[3]=d;toPose[4]=e;toPose[5]=f;
        int sweepSteps=PhotoContact.buildSweep(fromPose,toPose,sweptPoses);
        float cellWidth=Math.max(1,Math.abs(a)/MASK_W+Math.abs(b)/MASK_H);
        if(elemental) {
            materials.motion.contact(sweptPoses,sweepSteps,toothRows,getHeight(),materialSeconds(SystemClock.uptimeMillis()),
                    Math.max(1,(float)Math.hypot(a,d)/PhotoMaterialMotion.X*.3f));
            return;
        }
        float cellArea=Math.abs(a*e-b*d)/(MASK_W*MASK_H),fresh=0;
        int changed=0;visibleLeft=Float.MAX_VALUE;
        for(int row=0;row<MASK_H;row++)for(int col=0;col<MASK_W;col++) {
            int index=row*MASK_W+col,old=damagePixels[index]>>>24;
            if(old==255)continue;
            float u=(col+.5f)/MASK_W,v=(row+.5f)/MASK_H;
            float x=a*u+b*v+c,y=d*u+e*v+f,px=pa*u+pb*v+pc,py=pd*u+pe*v+pf;
            int coverage=old;
            float hitX=0,hitY=0;
            for(int step=1;step<=sweepSteps && coverage<255;step++) {
                int at=step*6;
                float nx=sweptPoses[at]*u+sweptPoses[at+1]*v+sweptPoses[at+2];
                float ny=sweptPoses[at+3]*u+sweptPoses[at+4]*v+sweptPoses[at+5];
                if(Math.min(nx,px)<=maximumContact+cellWidth && Math.max(ny,py)>=0 && Math.min(ny,py)<getHeight()) {
                    int next=Math.round(255*PhotoContact.coverage(px,py,nx,ny,cellWidth,getHeight(),toothRows,materialHit));
                    if(next>coverage){coverage=next;hitX=materialHit[0];hitY=materialHit[1];}
                }
                px=nx;py=ny;
            }
            if(coverage>old) {
                damagePixels[index]=(coverage<<24)|0xFFFFFF;damaged=true;
                freshCells[changed]=index;freshWeights[changed]=(coverage-old)/255f;
                freshX[changed]=hitX;freshY[changed]=hitY;
                fresh+=freshWeights[changed++];
            }
            if(coverage<240)visibleLeft=Math.min(visibleLeft,x-cellWidth*.5f);
        }
        if(changed>0) {
            if(damageBitmap==null)damageBitmap=Bitmap.createBitmap(MASK_W,MASK_H,Bitmap.Config.ARGB_8888);
            damageBitmap.setPixels(damagePixels,0,MASK_W,0,0,MASK_W,MASK_H);
            updateMaterialEdge();
        }
        if(fresh>0) {
            feed+=fresh*cellArea/(dp(10)*dp(10))*(1.1f+intensity/100f);
            int count=Math.min(90,(int)feed);feed=Math.min(90,feed-count);
            for(int n=0;n<count;n++) {
                float pick=random.nextFloat()*fresh;int at=0;
                while(at<changed-1 && pick>=freshWeights[at])pick-=freshWeights[at++];
                int index=freshCells[at];
                spawnChip(freshX[at],freshY[at],(index%MASK_W+.5f)/MASK_W,(index/MASK_W+.5f)/MASK_H);
            }
        } else feed=0;
    }
    private void updateMaterialEdge() {
        if(!elemental || (kind!=SwipeTheme.Kind.FIRE && kind!=SwipeTheme.Kind.TOXIC && kind!=SwipeTheme.Kind.ICE))return;
        java.util.Arrays.fill(edgeTintPixels,0);
        int color=kind==SwipeTheme.Kind.FIRE?0x34200F:kind==SwipeTheme.Kind.TOXIC?0x7EAE18:0xB8E7FC;
        for(int y=1;y<MASK_H-1;y++)for(int x=1;x<MASK_W-1;x++) {
            int i=y*MASK_W+x,alpha=damagePixels[i]>>>24;
            if(alpha>230)continue;
            int near=Math.max(Math.max(damagePixels[i-1]>>>24,damagePixels[i+1]>>>24),
                    Math.max(damagePixels[i-MASK_W]>>>24,damagePixels[i+MASK_W]>>>24));
            if(near>100)edgeTintPixels[i]=((kind==SwipeTheme.Kind.ICE?150:215)<<24)|color;
        }
        if(edgeTint==null)edgeTint=Bitmap.createBitmap(MASK_W,MASK_H,Bitmap.Config.ARGB_8888);
        edgeTint.setPixels(edgeTintPixels,0,MASK_W,0,0,MASK_W,MASK_H);
    }
    void hold(float x,float y) {
        keepDetached=false;clearChips();if(!hasRetainedDamage())resetBites();onGrinderShake(0);released=0;returning=0;closing=0;
        started=SystemClock.uptimeMillis();lastFrame=started;progress=0;open=0;entryEnergy=0;tracked=false;holding=true;geometryFrame=0;sleeveRect.setEmpty();invalidate();
    }
    void follow(float x,float y) { if(progress>=0)onGrinderShake(0);invalidate(); }
    int releaseDuration(boolean keep) {
        if(!elemental)return reducedMotion?80:SwipeMotion.releaseDuration(speed,keep);
        if(keep)return 240;
        int base=kind==SwipeTheme.Kind.TOXIC?1300:kind==SwipeTheme.Kind.ICE?750:kind==SwipeTheme.Kind.CANDY?800:1000;
        return Math.max(400,base*100/Math.max(50,speed));
    }
    void release(boolean keep) { onGrinderShake(0);holding=false;returning=0;progress=keep?1:-1;released=SystemClock.uptimeMillis();if(keep) {
            if(!elemental)mendStarted=released;
            if(elemental && bitmap!=null && !bitmap.isRecycled()) {
                // Own renderer and retained bitmap let this flight outlive the next card binding.
                if(keptPhotos.size()>=8)keptPhotos.remove(0);
                materials.prepareKeep(kind,materialSeconds(released),dp(1),getWidth(),getHeight());
                keptPhotos.add(new KeptPhoto(materials.snapshot(),bitmap,released,keepPhase()));keepDetached=true;
            }
        }invalidate(); }
    void retreat() {
        onGrinderShake(0);mendStarted=elemental?0:SystemClock.uptimeMillis();returnOpen=open;
        returnAlpha=SwipeMotion.ease((SystemClock.uptimeMillis()-started)/140f)*(1-SwipeMotion.clamp(-progress*4));
        holding=false;released=0;closing=0;returning=SystemClock.uptimeMillis();progress=0;invalidate();
    }
    void beginKeepClose() { returnOpen=open;closing=SystemClock.uptimeMillis();invalidate(); }
    void cancel() {
        onGrinderShake(0);holding=false;returning=0;closing=0;open=0;
        keepDetached=false;progress=0;released=0;tracked=false;resetBites();bitmap=null;photo.setEmpty();sleeveRect.setEmpty();visiblePath.reset();geometryFrame=0;clearChips();
        if(clippedPhoto!=null){clippedPhoto.invalidate();clippedPhoto=null;}invalidate();
    }
    @Override protected void onDetachedFromWindow() { keptPhotos.clear();cancel();super.onDetachedFromWindow(); }
    private void clearChips() { for(Chip chip:chips){chip.born=0;chip.bitmap=null;}feed=0; }
    private void spawnChip(float x,float y,float u,float v) {
        Chip chip=chips[nextChip++%chips.length];chip.born=SystemClock.uptimeMillis();
        chip.x=x;chip.y=y;
        chip.size=dp(1.8f+random.nextFloat()*8.2f);chip.vx=dp(75+random.nextFloat()*245);
        chip.vy=dp(-135+random.nextFloat()*215);chip.angle=random.nextFloat()*180;
        chip.spin=-540+random.nextFloat()*1080;chip.bitmap=bitmap;
        if(elemental) {
            if(kind==SwipeTheme.Kind.ICE){chip.size*=1.8f;chip.vx*=.65f;}
            if(kind==SwipeTheme.Kind.TOXIC){chip.vx*=.17f;chip.vy=dp(25+random.nextFloat()*50);chip.spin*=.13f;}
            if(kind==SwipeTheme.Kind.FIRE){chip.vx*=.5f;chip.vy=-dp(30+random.nextFloat()*95);}
            if(kind==SwipeTheme.Kind.CANDY){chip.size*=.7f;chip.vy=dp(45+random.nextFloat()*90);}
        }
        if(bitmap!=null && !bitmap.isRecycled()) {
            int bw=bitmap.getWidth(),bh=bitmap.getHeight();
            int sx=Math.max(0,Math.min(bw-1,(int)(u*bw)));
            int sy=Math.max(0,Math.min(bh-1,(int)(v*bh)));
            int side=Math.max(1,(int)(chip.size/Math.max(1,photo.width())*bw));
            chip.source.set(sx,sy,Math.min(bw,sx+side),Math.min(bh,sy+side));
        }
    }
    @Override protected void onDraw(Canvas canvas) {
        if(getWidth()==0 || getHeight()==0)return;
        long now=SystemClock.uptimeMillis();if(started==0)started=now;
        float time=(now-started)/1000f*speed/100f;
        float dt=lastFrame==0?16:Math.min(50,now-lastFrame);lastFrame=now;
        float phase=released==0?0:SwipeMotion.clamp((now-released)/(float)(reducedMotion?80:releaseDuration(progress>0)));
        float amount=SwipeMotion.clamp(Math.abs(progress));
        float fade=released==0?1:1-SwipeMotion.ease(phase);
        if(released==0) {
            float proximity=trashActive()&&!photo.isEmpty()?SwipeMotion.grinderProximity(visibleLeft,dp(14),dp(110)):0;
            onGrinderShake(enabled&&!reducedMotion?dp(SwipeMotion.trashShake((now-started)/1000f,proximity,!elemental)):0);
        }
        boolean steady=heldEffectsActive();
        boolean lightVisible=steady || holding || returning!=0 || (released!=0 && progress>0) || progress>0;
        int frame=canvas.save();canvas.clipRect(0,0,getWidth(),getHeight());
        if(elemental) {
            boolean active=bitmap!=null && !bitmap.isRecycled() && (holding || released!=0 || returning!=0 || hasRetainedDamage());
            float contactAlpha=photo.isEmpty()?0:SwipeMotion.grinderProximity(photo.left,Math.max(dp(22),maximumContact),dp(100));
            boolean keeping=released!=0?progress>0:
                    photo.right>Math.max(restRight+dp(12),getWidth()-dp(150)) && photo.left>maximumContact+dp(24);
            float keepPhase=SwipeMotion.clamp((photo.right-restRight)/Math.max(dp(120),getWidth()*.72f));
            float close=closing==0?0:SwipeMotion.clamp((now-closing)/220f);
            if(active && !keepDetached)materials.draw(canvas,bitmap,kind,materialSeconds(now),dp(1),getWidth(),getHeight(),
                    keeping,keepPhase,close,1);
            if(steady || contactAlpha>0)drawElementTrash(canvas,time,steady?heldEffectsAlpha(now):contactAlpha);
            drawKeptPhotos(canvas,now);
        } else {
            if(lightVisible)drawHeavenly(canvas,time,dt,now);
            float contactAlpha=photo.isEmpty()?0:SwipeMotion.grinderProximity(photo.left,Math.max(dp(22),maximumContact),dp(100));
            if(steady || contactAlpha>0) {
                float alpha=steady?heldEffectsAlpha(now):contactAlpha*(returning!=0?1-SwipeMotion.ease((now-returning)/280f):1);
                drawGrinder(canvas,time,1,alpha,now);
            }
            if(enabled && !reducedMotion)drawChips(canvas,fade,now);
        }
        canvas.restoreToCount(frame);
        paint.setShader(null);paint.setAlpha(255);paint.setStyle(Paint.Style.FILL);
        boolean tail=returning!=0 && now-returning<280;
        if(returning!=0 && !tail){
            returning=0;open=0;
            if(!hasRetainedDamage()){resetBites();photo.setEmpty();bitmap=null;}
        }
        if(isAttachedToWindow() && (holding || tail || (hasRetainedDamage() && materials.motion.animating(materialSeconds(now))) || !keptPhotos.isEmpty() || hasChips(now) || (released!=0 && (progress>0 || phase<1)) || progress>0)) {
            if(clippedPhoto!=null)clippedPhoto.postInvalidateOnAnimation();
            postInvalidateOnAnimation();
        }
    }
    float keepMouthX() { return getWidth()-dp(28); }
    float keepClipEdge() { preparePocket(SystemClock.uptimeMillis());return sleeveRect.left; }
    private void clipPhoto(Canvas canvas,View view) {
        clippedPhoto=view;
        if(elemental && bitmap!=null && !bitmap.isRecycled() && (holding || released!=0 || returning!=0 || hasRetainedDamage())) {
            canvas.clipRect(0,0,0,0);return;
        }
        if(holding || released!=0 || returning!=0 || progress!=0) {
            stageBoundary.reset();stageBoundary.moveTo(0,0);stageBoundary.lineTo(getWidth(),0);
            stageBoundary.lineTo(getWidth(),getHeight());stageBoundary.lineTo(0,getHeight());stageBoundary.close();
            clipStagePath(canvas,view,stageBoundary);
        }
        if(hasPhotoDamage())clipStagePath(canvas,view,photoQuad);
        if(!(holding || released!=0 || returning!=0 || closing!=0))return;
        preparePocket(SystemClock.uptimeMillis());
        if(open<.001f || visiblePath.isEmpty())return;
        clipStagePath(canvas,view,visiblePath);
    }
    private void clipStagePath(Canvas canvas,View view,Path stagePath) {
        localClip.set(stagePath);localClip.offset(-view.getLeft(),-view.getTop());
        if(view.getMatrix().invert(inverse)) {
            localClip.transform(inverse);canvas.clipPath(localClip);
        }
    }
    private float damageRecovery() {
        return mendStarted==0?1:1-SwipeMotion.ease((SystemClock.uptimeMillis()-mendStarted)/280f);
    }
    private boolean hasPhotoDamage() { return damaged && damageBitmap!=null && damageRecovery()>0; }
    private int beginPhoto(Canvas canvas,View view) {
        int save=hasPhotoDamage()?canvas.saveLayer(0,0,view.getWidth(),view.getHeight(),null):canvas.save();
        clipPhoto(canvas,view);return save;
    }
    private void erasePhotoDamage(Canvas canvas,View view) {
        if(!hasPhotoDamage())return;
        maskToLocal.set(photoToStage);maskToLocal.postTranslate(-view.getLeft(),-view.getTop());
        if(!view.getMatrix().invert(inverse))return;
        maskToLocal.postConcat(inverse);
        int save=canvas.save();canvas.concat(maskToLocal);
        if(edgeTint!=null) { materialPaint.setAlpha((int)(255*damageRecovery()));canvas.drawBitmap(edgeTint,null,unitRect,materialPaint); }
        erasePaint.setAlpha((int)(255*damageRecovery()));canvas.drawBitmap(damageBitmap,null,unitRect,erasePaint);
        canvas.restoreToCount(save);
    }
    private boolean hasChips(long now) {
        if(!enabled || reducedMotion)return false;
        for(Chip chip:chips)if(chip.born!=0 && now-chip.born<1050)return true;
        return false;
    }
    private void preparePocket(long now) {
        // Touch, clipping and foreground drawing share one geometry sample per frame.
        long frame=getDrawingTime();if(frame<=0)frame=now;
        if(geometryFrame==frame)return;
        float dt=geometryFrame==0?16:Math.min(50,Math.max(0,frame-geometryFrame));geometryFrame=frame;
        lightAlpha=holding?SwipeMotion.ease((now-started)/160f):1;
        float wanted=photo.isEmpty()?Math.max(0,progress):
                SwipeMotion.pocketOpening(restRight,photo.right,keepMouthX()-dp(16),dp(70));

        if(released!=0 && progress>0)wanted=Math.max(wanted,open);
        if(heldEffectsActive()) {
            open=1;lightAlpha=heldEffectsAlpha(now);
        } else if(returning!=0) {
            open=returnOpen*(1-SwipeMotion.ease((now-returning)/220f));
            lightAlpha=returnAlpha*(1-SwipeMotion.ease((now-returning)/280f));
        } else if(closing!=0) {
            open=returnOpen*(1-SwipeMotion.ease((now-closing)/220f));
            lightAlpha=1-SwipeMotion.ease(Math.max(0,now-closing-100)/180f);
        } else open+= (wanted-open)*SwipeMotion.followFactor(dt,32);

        float energy=photo.isEmpty()?0:SwipeMotion.clamp((photo.right-(getWidth()-dp(65)))/dp(100));
        if(returning!=0 || closing!=0)energy=0;
        entryEnergy+=(energy-entryEnergy)*SwipeMotion.followFactor(dt,70);
        // The receiving portal belongs to the screen; only its opening reacts to approach.
        haloX=getWidth();haloY=getHeight()*.5f;
        float o=SwipeMotion.clamp(open);
        // A side-on oval opens in two dimensions. Its near lip is also the photo's cut boundary.
        portalX=getWidth();
        portalRadius=dp(42)*(float)Math.pow(o,.78);
        portalHeight=Math.max(dp(2),(getHeight()*.5f-dp(12))*SwipeMotion.ease(o));
        receiverBounds.set(portalX-dp(116)*o,haloY-portalHeight*1.18f,
                portalX+dp(116)*o,haloY+portalHeight*1.18f);
        float top=haloY-portalHeight,bottom=haloY+portalHeight;
        sleeveRect.set(portalX-portalRadius,top,portalX+portalRadius,bottom);
        lightPath.reset();lightPath.moveTo(portalX,top);
        float minimumFront=portalX;
        for(int i=1;i<=64;i++) {
            float y=top+(bottom-top)*i/64;
            float front=frontAt(y);minimumFront=Math.min(minimumFront,front);
            lightPath.lineTo(front,y);
        }
        if(elemental)sleeveRect.left=minimumFront;
        backRim.reset();backRim.moveTo(portalX,bottom);
        for(int i=1;i<=64;i++) {
            double a=Math.PI/2-Math.PI*i/64;
            backRim.lineTo(portalX+portalRadius*(float)Math.cos(a),haloY+portalHeight*(float)Math.sin(a));
        }
        aperture.set(lightPath);
        for(int i=1;i<=64;i++) {
            double a=Math.PI/2-Math.PI*i/64;
            aperture.lineTo(portalX+portalRadius*(float)Math.cos(a),haloY+portalHeight*(float)Math.sin(a));
        }
        aperture.close();
        visiblePath.set(lightPath);
        visiblePath.lineTo(getWidth(),getHeight()*4f);visiblePath.lineTo(-getWidth()*4f,getHeight()*4f);
        visiblePath.lineTo(-getWidth()*4f,-getHeight()*4f);visiblePath.lineTo(getWidth(),-getHeight()*4f);
        visiblePath.lineTo(portalX,top);visiblePath.close();
        foldWindow.set(lightPath);
        for(int i=64;i>=0;i--) {
            float y=top+(bottom-top)*i/64,front=frontAt(y);
            foldWindow.lineTo(front+throatDepth(front)*.94f,y);
        }
        foldWindow.close();
    }
    private float frontAt(float y) {
        if(elemental && !receiverBounds.isEmpty()) {
            float v=SwipeMotion.clamp((y-receiverBounds.top)/receiverBounds.height());
            int row=Math.max(0,Math.min(innerEdge.length-1,Math.round(v*(innerEdge.length-1))));
            return receiverBounds.left+receiverBounds.width()*innerEdge[row];
        }
        return SwipeMotion.portalFront(portalX,portalRadius,haloY,portalHeight,y);
    }
    private float throatDepth(float front) { return Math.max(1,Math.min(dp(23)*open,getWidth()+dp(6)-front)); }
    private void drawEnteringPhoto(Canvas canvas) {
        if(bitmap==null || bitmap.isRecycled() || photo.isEmpty() || photo.right<sleeveRect.left || open<.02f)return;
        int at=0;
        for(int row=0;row<=MESH_Y;row++)for(int col=0;col<=MESH_X;col++) {
            foldedPhoto[at++]=col/(float)MESH_X;foldedPhoto[at++]=row/(float)MESH_Y;
        }
        photoToStage.mapPoints(foldedPhoto);
        for(int i=0;i<foldedPhoto.length;i+=2) {
            float x=foldedPhoto[i],y=foldedPhoto[i+1];
            float front=frontAt(y);
            float throat=throatDepth(front);
            float depth=SwipeMotion.portalDepth(x-front,throat);
            foldedPhoto[i]=x<=front?x:front+throat*depth;
            foldedPhoto[i+1]=haloY+(y-haloY)*(1-.06f*depth);
        }
        int save=canvas.save();canvas.clipPath(aperture);canvas.clipPath(foldWindow);
        int layer=hasPhotoDamage()?canvas.saveLayer(sleeveRect.left,sleeveRect.top,sleeveRect.right,sleeveRect.bottom,null):canvas.save();
        paint.setShader(null);paint.setStyle(Paint.Style.FILL);paint.setColor(Color.WHITE);paint.setAlpha(255);
        materialPaint.setColor(Color.WHITE);materialPaint.setAlpha(255);
        canvas.drawBitmapMesh(bitmap,MESH_X,MESH_Y,foldedPhoto,0,null,0,materialPaint);
        if(hasPhotoDamage()) {
            erasePaint.setAlpha((int)(255*damageRecovery()));
            canvas.drawBitmapMesh(damageBitmap,MESH_X,MESH_Y,foldedPhoto,0,null,0,erasePaint);
        }
        canvas.restoreToCount(layer);canvas.restoreToCount(save);
    }
    private void drawPortalWell(Canvas canvas,float time,float opening,float alpha) {
        int save=canvas.save();canvas.clipPath(aperture);
        paint.setShader(null);paint.setStyle(Paint.Style.FILL);paint.setColor(0xFF171C2A);paint.setAlpha((int)(235*alpha));
        canvas.drawPath(aperture,paint);
        glow(canvas,getWidth()+dp(4)*opening,haloY,dp(48)*opening,portalHeight*.85f,alpha*.65f);
        // Receding inner rings give the opening a visible throat before the photo arrives.
        for(int ring=0;ring<3;ring++) {
            float drift=enabled&&!reducedMotion?(time*.14f+ring/3f)%1:ring/3f;
            float rx=portalRadius*(.55f+.32f*drift),ry=portalHeight*(.64f+.28f*drift);
            innerRim.reset();
            for(int i=0;i<=72;i++) {
                double a=Math.PI*2*i/72;float x=portalX+dp(9)*opening+rx*(float)Math.cos(a);
                float y=haloY+ry*(float)Math.sin(a);
                if(i==0)innerRim.moveTo(x,y);else innerRim.lineTo(x,y);
            }
            glowStroke(canvas,innerRim,alpha*opening*.22f*(float)Math.sin(Math.PI*drift),.65f);
        }
        canvas.restoreToCount(save);glowStroke(canvas,backRim,alpha*.45f,.8f);
    }
    private void drawHeavenly(Canvas canvas,float time,float dt,long now) {
        preparePocket(now);
        boolean animate=enabled&&!reducedMotion;
        float o=SwipeMotion.clamp(open),alpha=lightAlpha;
        paint.setShader(null);paint.setStyle(Paint.Style.FILL);
        if(o>.006f && alpha>.001f) {
            float top=sleeveRect.top,bottom=sleeveRect.bottom,h=bottom-top,mouth=sleeveRect.left;
            drawPortalWell(canvas,time,o,alpha);
            if(holding || (released!=0 && progress>0))drawEnteringPhoto(canvas);
            glow(canvas,mouth+dp(3)*o,haloY,dp(30+12*entryEnergy)*o,h*.52f,alpha*(.33f+.18f*entryEnergy));
            if(animate) {
                drawPortalRays(canvas,time,o,alpha);
                drawGlitterBands(canvas,time,o,alpha);
            }
            // The near lip is drawn last, crossing in front of the curled photo strip.
            glowStroke(canvas,lightPath,alpha,1.55f);
            if(animate) {
                pathParticles(canvas,lightPath,time,.16f,36+intensity/8,alpha*o);
                drawRibbon(canvas,-1,o,alpha,time);
                drawRibbon(canvas,1,o,alpha,time);
                // Staggered curved motes drift into the seam; no fence of identical straight rays.
                for(int i=0;i<18;i++) {
                    float u=(time*(.20f+(i%5)*.027f)+i*.618034f)%1;
                    float v=.07f+.86f*((i*.381966f)%1);
                    float reach=dp(12+(i*17)%35)*o;
                    float x=mouth-reach*(1-u)*(1-u);
                    float y=top+h*v+dp(5)*o*(float)Math.sin(u*Math.PI+i);
                    float a=alpha*o*(float)Math.pow(Math.sin(Math.PI*u),2)*.60f;
                    glow(canvas,x,y,dp(3.5f),dp(3.5f),a*.55f);
                    if(i%5==0)sparkle(canvas,x,y,dp(.9f),a);
                    else { paint.setColor(i%3==0?0xFFFFD18C:0xFFFFF7DC);paint.setAlpha((int)(210*a));canvas.drawCircle(x,y,dp(.5f+(i%3)*.2f),paint); }
                }
            }
        }
        float dot=alpha*(1-SwipeMotion.ease(Math.min(1,o*4)));
        glow(canvas,haloX-dp(1.5f),haloY,dp(19),dp(22),dot*.7f);
        paint.setColor(0xFFFFF8E8);paint.setAlpha((int)(245*dot));canvas.drawCircle(haloX-dp(1),haloY,dp(1.6f),paint);
        if(animate)for(int i=0;i<4;i++) {
            float t=(time*.32f+i*.25f)%1,angle=i*2.4f+time*.18f;
            float reach=dp(4+13*t),x=haloX+(float)Math.cos(angle)*reach,y=haloY+(float)Math.sin(angle)*reach;
            sparkle(canvas,x,y,dp(.9f),dot*(float)Math.sin(Math.PI*t));
        }
    }
    private void drawPortalRays(Canvas canvas,float time,float opening,float alpha) {
        measure.setPath(lightPath,false);float length=measure.getLength();
        if(length<1)return;
        float strength=alpha*opening*Math.min(1.25f,.65f+intensity/200f)*(1+.62f*entryEnergy);
        for(int i=0;i<12;i++) {
            float v=.08f+.84f*((i*.618034f+.13f)%1);
            if(!measure.getPosTan(length*v,position,null))continue;
            float pulse=.40f+.60f*(float)Math.pow(Math.sin(time*(.43f+i*.043f)+i*2.1f),2);
            float angle=180-(v-.5f)*48+4*(float)Math.sin(time*.37f+i);
            float reach=Math.min(getWidth()*.48f,dp(96+(i*29)%85))*opening*(1+.20f*entryEnergy);
            float width=dp(22+(i*13)%36)*opening;
            int save=canvas.save();canvas.rotate(angle,position[0],position[1]);
            target.set(position[0],position[1]-width*.5f,position[0]+reach,position[1]+width*.5f);
            paint.setShader(null);paint.setStyle(Paint.Style.FILL);paint.setColor(Color.WHITE);paint.setAlpha(Math.min(255,(int)(195*strength*pulse)));
            canvas.drawBitmap(rayArt,null,target,paint);canvas.restoreToCount(save);
        }
        // Each glint has its own lifetime and direction, streaming out towards the photo.
        for(int i=0;i<18;i++) {
            float u=(time*(.28f+(i%5)*.041f)+i*.618034f)%1;
            float v=.04f+.92f*((i*.381966f+.21f)%1);
            if(!measure.getPosTan(length*v,position,null))continue;
            float reach=dp(30+(i*23)%83)*opening;
            float x=position[0]-reach*u,y=position[1]+(v-.5f)*dp(54)*u*opening;
            float a=strength*(float)Math.pow(Math.sin(Math.PI*u),1.4)*(.45f+(i%3)*.13f);
            glow(canvas,x,y,dp(3+(i%3)),dp(3+(i%3)),a*.8f);
            if(i%3==0)sparkle(canvas,x,y,dp(1.1f+(i%4)*.25f),a);
            else {
                paint.setShader(null);paint.setStyle(Paint.Style.STROKE);paint.setStrokeCap(Paint.Cap.ROUND);
                paint.setColor(0xFFFFE5AE);paint.setAlpha((int)(145*a));paint.setStrokeWidth(dp(.7f));
                canvas.drawLine(x,y,x+dp(3+u*6)*opening,y-(v-.5f)*dp(3)*opening,paint);paint.setStyle(Paint.Style.FILL);
            }
        }
    }
    private void drawRibbon(Canvas canvas,int side,float opening,float alpha,float time) {
        float anchorY=haloY+side*portalHeight*.82f;
        float anchorX=frontAt(anchorY);
        for(int strand=0;strand<3;strand++) {
            float reach=Math.min(dp(105+strand*20),getWidth()*.38f)*opening;
            float phase=time*(.85f+strand*.13f)+side*1.7f+strand*2.2f;
            float lift=dp(12+strand*6)*opening;
            ribbonPath.reset();
            // A travelling wave runs from the rim to the tapered free end.
            for(int i=0;i<=48;i++) {
                float u=i/48f;
                float x=anchorX-reach*u;
                float y=anchorY+side*lift*(float)Math.sin(u*Math.PI*.85)
                        +dp(9+strand*3)*opening*u*(float)Math.sin(u*7-phase);
                if(i==0)ribbonPath.moveTo(x,y);else ribbonPath.lineTo(x,y);
            }
            measure.setPath(ribbonPath,false);float length=measure.getLength();
            float px=0,py=0;
            for(int i=0;i<=48;i++) {
                float u=i/48f;if(!measure.getPosTan(length*u,position,null))continue;
                float taper=(float)Math.pow(1-u,1.3),pulse=.72f+.28f*(float)Math.sin(time*1.5f-u*9+strand);
                float a=alpha*opening*taper*pulse*(strand==0?.82f:.44f);
                if(i>0) {
                    paint.setShader(null);paint.setStyle(Paint.Style.STROKE);paint.setStrokeCap(Paint.Cap.ROUND);
                    paint.setColor(strand==0?0xFFFFF8E8:0xFFFFD490);paint.setAlpha((int)(185*a));
                    paint.setStrokeWidth(dp(.5f+1.4f*taper));canvas.drawLine(px,py,position[0],position[1],paint);
                }
                glow(canvas,position[0],position[1],dp(3.8f),dp(3.8f),a*.24f);
                px=position[0];py=position[1];
            }
            pathParticles(canvas,ribbonPath,-time+10+strand*.4f,.24f,9,alpha*opening*.8f);
        }
    }
    private void drawGlitterBands(Canvas canvas,float time,float opening,float alpha) {
        float strength=alpha*opening*Math.min(1.2f,.65f+intensity/200f)*(1+.25f*entryEnergy);
        // Broad bloom stays beside the mouth, leaving the centre of the photo readable.
        float breath=.82f+.18f*(float)Math.sin(time*.72f);
        glow(canvas,sleeveRect.left+dp(6)*opening,haloY,dp(56)*opening,
                portalHeight*.76f,strength*.18f*breath);
        for(int band=0;band<2;band++) {
            float side=band==0?-1:1;
            float anchorY=haloY+side*portalHeight*(.48f+.055f*(float)Math.sin(time*.36f+band));
            float anchorX=frontAt(anchorY);
            float reach=Math.min(getWidth()*.34f,dp(94+band*17))*opening;
            float phase=time*(.82f+band*.11f)+band*2.7f;
            for(int i=0;i<=48;i++) {
                float u=i/48f;
                bandX[i]=anchorX-reach*u;
                bandY[i]=anchorY+side*dp(22)*opening*(float)Math.sin(u*Math.PI)
                        +dp(10)*opening*u*(float)Math.sin(u*7-phase);
                bandWidth[i]=dp(1+4*(float)Math.sin(Math.PI*u))*opening*(1-u);
            }
            glitterBand.reset();glitterBand.moveTo(bandX[0],bandY[0]-bandWidth[0]);
            for(int i=1;i<=48;i++)glitterBand.lineTo(bandX[i],bandY[i]-bandWidth[i]);
            for(int i=48;i>=0;i--)glitterBand.lineTo(bandX[i],bandY[i]+bandWidth[i]);
            glitterBand.close();
            paint.setShader(null);paint.setStyle(Paint.Style.FILL);paint.setColor(0xFFFFDEA0);
            paint.setAlpha((int)(28*strength));canvas.drawPath(glitterBand,paint);
            for(int i=1;i<=48;i++) {
                float u=i/48f,taper=(float)Math.pow(1-u,1.5);
                float shimmer=.68f+.32f*(float)Math.sin(u*8-time*1.4f+band);
                float a=strength*taper*shimmer;
                // The luminous core, wide haze and glitter all follow one flowing band.
                paint.setShader(null);paint.setStyle(Paint.Style.STROKE);paint.setStrokeCap(Paint.Cap.ROUND);
                paint.setColor(0xFFFFF3D5);paint.setAlpha((int)(115*a));paint.setStrokeWidth(dp(.6f+1.1f*taper));
                canvas.drawLine(bandX[i-1],bandY[i-1],bandX[i],bandY[i],paint);
                if(i%3==0)glow(canvas,bandX[i],bandY[i],dp(9)*opening,dp(7)*opening,a*.28f);
            }
            for(int i=0;i<22;i++) {
                float u=(time*(.19f+band*.027f)+i*.618034f)%1;
                float index=u*48;int at=Math.min(47,(int)index);float f=index-at;
                float x=bandX[at]+(bandX[at+1]-bandX[at])*f;
                float width=bandWidth[at]+(bandWidth[at+1]-bandWidth[at])*f;
                float y=bandY[at]+(bandY[at+1]-bandY[at])*f+width*(float)Math.sin(i*2.4f+time*.65f);
                float a=strength*(float)Math.pow(Math.sin(Math.PI*u),.8)
                        *(.35f+.65f*(float)Math.pow(Math.sin(time*(1.1f+(i%4)*.13f)+i*1.9f),2));
                if(i%6==0) {
                    glow(canvas,x,y,dp(6)*opening,dp(6)*opening,a*.65f);
                    paint.setStyle(Paint.Style.FILL);sparkle(canvas,x,y,dp(1.1f)*opening,a*.85f);
                } else {
                    paint.setShader(null);paint.setStyle(Paint.Style.FILL);paint.setColor(i%3==0?0xFFFFCE7E:0xFFFFFAEB);
                    paint.setAlpha(Math.min(255,(int)(205*a)));canvas.drawCircle(x,y,dp(.45f+(i%3)*.2f)*opening,paint);
                }
            }
            glow(canvas,anchorX,anchorY,dp(18)*opening,dp(24)*opening,strength*.42f*breath);
        }
        paint.setStyle(Paint.Style.FILL);
    }
    private void glow(Canvas canvas,float x,float y,float rx,float ry,float alpha) {
        if(rx<=0 || ry<=0 || alpha<=0)return;
        paint.setShader(null);paint.setStyle(Paint.Style.FILL);paint.setColor(Color.WHITE);
        paint.setAlpha((int)(255*SwipeMotion.clamp(alpha)));target.set(x-rx,y-ry,x+rx,y+ry);
        canvas.drawBitmap(glowArt,null,target,paint);
    }
    private void glowStroke(Canvas canvas,Path line,float alpha,float width) {
        paint.setShader(null);paint.setStyle(Paint.Style.STROKE);paint.setStrokeCap(Paint.Cap.ROUND);paint.setStrokeJoin(Paint.Join.ROUND);
        for(int layer=7;layer>=0;layer--) {
            paint.setColor(layer==0?0xFFFFF9E9:0xFFFFD48C);
            paint.setStrokeWidth(dp(width+layer*layer*.27f));
            paint.setAlpha((int)((layer==0?230:layer==1?66:layer==2?30:10)*SwipeMotion.clamp(alpha)));
            canvas.drawPath(line,paint);
        }
        paint.setStyle(Paint.Style.FILL);
    }
    private void pathParticles(Canvas canvas,Path line,float time,float rate,int count,float alpha) {
        paint.setStyle(Paint.Style.FILL);
        measure.setPath(line,false);float length=measure.getLength();if(length<1)return;
        for(int i=0;i<count;i++) {
            float u=((time*rate+i*.618034f)%1+1)%1;
            if(!measure.getPosTan(length*u,position,null))continue;
            float strength=alpha*(float)Math.sin(Math.PI*u)*(.4f+.6f*(float)Math.pow(Math.sin(time*1.2f+i*2.3f),2));
            if(i%7==0)sparkle(canvas,position[0],position[1],dp(1.15f),strength);
            else {paint.setColor(i%3==0?0xFFFFDDA1:0xFFFFFCEF);paint.setAlpha((int)(195*SwipeMotion.clamp(strength)));canvas.drawCircle(position[0],position[1],dp(.5f+i%3*.15f),paint);}
        }
    }
    private void sparkle(Canvas canvas,float x,float y,float size,float alpha) {
        paint.setColor(0xFFFFF6DF);paint.setAlpha((int)(220*SwipeMotion.clamp(alpha)));
        path.reset();path.moveTo(x,y-size*2);path.lineTo(x+size*.35f,y-size*.35f);
        path.lineTo(x+size*2,y);path.lineTo(x+size*.35f,y+size*.35f);
        path.lineTo(x,y+size*2);path.lineTo(x-size*.35f,y+size*.35f);
        path.lineTo(x-size*2,y);path.lineTo(x-size*.35f,y-size*.35f);path.close();canvas.drawPath(path,paint);
    }
    private void drawElementTrash(Canvas canvas,float time,float alpha) {
        if(elementAtlas==null || elementAtlas.isRecycled())return;
        prepareTrashBoundary(time);
        // Filled alpha artwork and collider use the identical screen-anchored rectangle.
        materialPaint.setAlpha((int)(255*SwipeMotion.clamp(alpha)));
        canvas.drawBitmap(textureArt,null,trashBounds,materialPaint);
        // Animate material details in place, without moving its collision surface.
        if(holding || released!=0)drawMaterialMotes(canvas,time,alpha,false);
    }
    private void drawMaterialMotes(Canvas canvas,float time,float alpha,boolean keep) {
        // Broad, filled sprites provide the moving material; there are no white outline paths.
        Bitmap detail=materialDetail();if(detail==null || detail.isRecycled())return;
        if(kind==SwipeTheme.Kind.SPACE) {
            float radius=Math.min(getHeight()*.24f,dp(130));
            target.set(-radius,getHeight()*.5f-radius,radius,getHeight()*.5f+radius);
            materialPaint.setAlpha((int)(255*alpha));canvas.drawBitmap(detail,null,target,materialPaint);return;
        }
        int count=kind==SwipeTheme.Kind.TOXIC?7:kind==SwipeTheme.Kind.ICE?9:14;
        for(int i=0;i<count;i++) {
            float u=(time*(kind==SwipeTheme.Kind.TOXIC?.09f:.30f)+i*.618034f)%1;
            float y=getHeight()*((i*.381966f)%1),x=keep?frontAt(y):toothEdge(y,time);
            float life=(float)Math.sin(Math.PI*u),size=dp(kind==SwipeTheme.Kind.FIRE?25:kind==SwipeTheme.Kind.WATER?22:kind==SwipeTheme.Kind.ICE?20:18)*(.65f+.35f*life);
            float move=dp(kind==SwipeTheme.Kind.TOXIC?25:38)*u;
            y+=kind==SwipeTheme.Kind.FIRE?-move:move;
            x+=(keep?-1:1)*dp(8)*life;
            target.set(x-size,y-size,x+size,y+size);
            materialPaint.setAlpha((int)(180*alpha*life));
            if(kind==SwipeTheme.Kind.FIRE && !keep) {
                float anchor=toothEdge(y,time);target.set(anchor-size*.65f,y-size*2.2f,anchor+size*.65f,y+size*.08f);
                int flame=canvas.save();canvas.rotate(55,anchor,y);canvas.drawBitmap(detail,null,target,materialPaint);canvas.restoreToCount(flame);
            } else canvas.drawBitmap(detail,null,target,materialPaint);
        }
    }
    private Bitmap materialDetail() { return detailArt; }
    private void drawGrinder(Canvas canvas,float time,float amount,float fade,long now) {
        float top=grinderTop(),bottom=grinderBottom();
        // Only transparent teeth are drawn here: no rectangular rail behind the saws.
        paint.setShader(null);paint.setStyle(Paint.Style.FILL);paint.setColor(Color.WHITE);paint.setAlpha((int)(235*SwipeMotion.ease(amount)*fade));
        int save=canvas.save();canvas.clipRect(0,top,dp(22),bottom);
        int index=0;
        for(float y=top;y<bottom+dp(34);y+=dp(49)) {
            float radius=dp(34),cx=-dp(16);
            int blade=canvas.save();
            canvas.rotate(enabled&&!reducedMotion?(time*115*(index%2==0?1:-1)+index*29)%360:0,cx,y);
            bladeRect.set(cx-radius,y-radius,cx+radius,y+radius);
            if(sawBlade!=null && !sawBlade.isRecycled())canvas.drawBitmap(sawBlade,null,bladeRect,paint);
            canvas.restoreToCount(blade);index++;
        }
        canvas.restoreToCount(save);
    }
    private void drawChips(Canvas canvas,float fade,long now) {
        for(Chip chip:chips) {
            if(chip.born==0)continue;
            float age=(now-chip.born)/1000f;
            if(age>1.05f){chip.born=0;chip.bitmap=null;continue;}
            float x=chip.x+chip.vx*age,y=chip.y+chip.vy*age+dp(180)*age*age;
            if(elemental && (kind==SwipeTheme.Kind.WATER || kind==SwipeTheme.Kind.SPACE)) {
                float pull=SwipeMotion.ease(age/1.05f),cx=dp(9),cy=getHeight()*.5f;
                float rx=x-cx,ry=y-cy,angle=age*(kind==SwipeTheme.Kind.SPACE?6:4);
                x=cx+(1-pull)*(rx*(float)Math.cos(angle)-ry*(float)Math.sin(angle)*.16f);
                y=cy+(1-pull)*(ry*(float)Math.cos(angle)+rx*(float)Math.sin(angle));
            }
            float size=chip.size*(1-age*.55f);
            paint.setColor(0xFFFFE8CB);paint.setAlpha((int)(245*(1-age/1.05f)*fade));
            int piece=canvas.save();canvas.rotate(chip.angle+chip.spin*age,x,y);
            target.set(x-size,y-size*.65f,x+size,y+size*.65f);
            path.reset();path.moveTo(x-size,y-size*.25f);path.lineTo(x-size*.35f,y-size*.65f);
            path.lineTo(x+size,y-size*.4f);path.lineTo(x+size*.55f,y+size*.65f);path.lineTo(x-size*.7f,y+size*.4f);path.close();
            if(elemental && kind==SwipeTheme.Kind.ICE) {
                path.reset();path.moveTo(x-size,y-size);path.lineTo(x+size,y-size*.4f);path.lineTo(x-size*.3f,y+size);path.close();
            } else if(elemental && (kind==SwipeTheme.Kind.TOXIC || kind==SwipeTheme.Kind.WATER)) {
                path.reset();path.addOval(target,Path.Direction.CW);
            }
            canvas.clipPath(path);
            if(chip.bitmap!=null && !chip.bitmap.isRecycled() && !chip.source.isEmpty())canvas.drawBitmap(chip.bitmap,chip.source,target,paint);
            else canvas.drawRect(target,paint);
            if(elemental && (kind==SwipeTheme.Kind.FIRE || kind==SwipeTheme.Kind.TOXIC)) {
                paint.setColor(kind==SwipeTheme.Kind.FIRE?0xFF6B240D:0xFF79BF1C);
                paint.setAlpha((int)(150*age));canvas.drawRect(target,paint);
            }
            canvas.restoreToCount(piece);
        }
    }
}





