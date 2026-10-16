package com.dominic.photosweep;

import android.graphics.*;

/** Six photo-material render paths. Every surface, shard and droplet samples the current photo. */
final class PhotoMaterialRenderer {
    final PhotoMaterialMotion motion=new PhotoMaterialMotion();
    private Bitmap receiverArt,flameArt;
    private RadialGradient stellarGlow;private float stellarX,stellarY,stellarRadius;
    void setArtwork(Bitmap receiver,Bitmap flame){receiverArt=receiver;flameArt=flame;}
    private final Paint paint=new Paint(Paint.FILTER_BITMAP_FLAG),overlay=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path shape=new Path();
    private final Matrix texture=new Matrix();
    private final float[] src=new float[6],dst=new float[6];
    private final Rect sample=new Rect();
    private final RectF whole=new RectF(),destination=new RectF();
    private final boolean[] detached=new boolean[320];
    private final float[] anchoredChunks=new float[320*8],chunkPose=new float[8];
    private final int[] chunkIndices=new int[4];
    private final java.util.Random random=new java.util.Random();
    PhotoMaterialRenderer snapshot() {
        PhotoMaterialRenderer copy=new PhotoMaterialRenderer();copy.setArtwork(receiverArt,flameArt);
        System.arraycopy(motion.base,0,copy.motion.base,0,motion.base.length);
        copy.motion.candyVariant=motion.candyVariant;return copy;
    }
    void reset(){motion.reset();motion.candyVariant=random.nextInt(4);java.util.Arrays.fill(detached,false);}
    void draw(Canvas canvas,Bitmap photo,SwipeTheme.Kind kind,float seconds,float density,float width,float height,
              boolean keeping,float keepPhase,float close,float recovery) {
        if(photo==null || photo.isRecycled())return;
        whole.set(0,0,photo.getWidth(),photo.getHeight());paint.setAlpha(255);paint.setShader(null);
        if(keeping) {
            motion.keep(kind,keepPhase,seconds,density,width,height,close);
            canvas.drawBitmapMesh(photo,PhotoMaterialMotion.X,PhotoMaterialMotion.Y,motion.vertices,0,motion.colors,0,paint);
            if(kind==SwipeTheme.Kind.FIRE)drawKeepFire(canvas,keepPhase,close,seconds,density);
            else drawKeepFinish(canvas,kind,keepPhase,seconds,density,width,height,close);
        } else {
            motion.trash(kind,seconds,density,width,height,recovery);
            if(kind==SwipeTheme.Kind.SPACE)drawSpaceSlices(canvas,photo,seconds,density,width,height,recovery);
            else if(kind==SwipeTheme.Kind.ICE || kind==SwipeTheme.Kind.CANDY)
                drawBrittle(canvas,photo,kind,seconds,density,height,recovery);
            else {
                canvas.drawBitmapMesh(photo,PhotoMaterialMotion.X,PhotoMaterialMotion.Y,motion.vertices,0,motion.colors,0,paint);
                if(kind!=SwipeTheme.Kind.FIRE)drawShedMaterial(canvas,photo,kind,seconds,density,height,recovery);
                if(kind==SwipeTheme.Kind.FIRE)drawBurnFront(canvas,seconds,density,recovery);
            }
        }
    }
    private void drawSpaceSlices(Canvas canvas,Bitmap photo,float seconds,float density,float width,float height,float recovery) {
        // Render independent strips. No connecting triangles bridge the ripped seams.
        for(int row=0;row<PhotoMaterialMotion.Y;row+=5)for(int col=0;col<PhotoMaterialMotion.X;col++) {
            int bottom=Math.min(PhotoMaterialMotion.Y,row+5);
            int a=row*(PhotoMaterialMotion.X+1)+col,b=a+1,c=bottom*(PhotoMaterialMotion.X+1)+col,d=c+1;
            chunkIndices[0]=a;chunkIndices[1]=b;chunkIndices[2]=c;chunkIndices[3]=d;
            for(int i=0;i<4;i++) {
                int n=chunkIndices[i];float x=motion.base[n*2],y=motion.base[n*2+1];
                float reach=Math.min(width*.70f,350*density),force=PhotoMaterialMotion.clamp(1-x/reach)*recovery;
                float tidal=force*force,angle=tidal*(2.4f+.18f*(row/5));
                float dx=x-density*8,dy=y-height*.5f;
                chunkPose[i*2]=density*8+(dx*(float)Math.cos(angle)-dy*(float)Math.sin(angle))*(1-.94f*tidal)-reach*.30f*tidal;
                chunkPose[i*2+1]=height*.5f+(dx*(float)Math.sin(angle)+dy*(float)Math.cos(angle))*(1-.94f*tidal)+density*12*force*(float)Math.sin(row/5*2.7);
            }
            int alpha=(motion.colors[a]>>>24);paint.setAlpha(alpha);
            drawTriangle(canvas,photo,a,b,d,0,0,0,0,0,1,1);drawTriangle(canvas,photo,a,d,c,0,0,0,0,0,1,1);
        }
        paint.setAlpha(255);
    }
    private void crystal(Canvas canvas,float cx,float cy,float r,float alpha,float density) {
        // Small faceted ice growth, with translucent faces rather than image-edge outlines.
        for(int i=0;i<6;i++) {
            float angle=i*(float)Math.PI/3;
            shape.reset();shape.moveTo(cx,cy);shape.lineTo(cx+r*(float)Math.cos(angle-.19f),cy+r*(float)Math.sin(angle-.19f));shape.lineTo(cx+r*1.65f*(float)Math.cos(angle),cy+r*1.65f*(float)Math.sin(angle));shape.lineTo(cx+r*(float)Math.cos(angle+.19f),cy+r*(float)Math.sin(angle+.19f));shape.close();
            overlay.setColor(i%2==0?0xFFE5FAFF:0xFF72BDE5);overlay.setAlpha((int)(180*alpha));canvas.drawPath(shape,overlay);
        }
    }
    private void drawBrittle(Canvas canvas,Bitmap photo,SwipeTheme.Kind kind,float seconds,float density,float height,float recovery) {
        boolean ice=kind==SwipeTheme.Kind.ICE;
        int cols=ice?8:16,rows=ice?10:20;
        for(int row=0;row<rows;row++)for(int col=0;col<cols;col++) {
            int c0=col*PhotoMaterialMotion.X/cols,c1=(col+1)*PhotoMaterialMotion.X/cols;
            int r0=row*PhotoMaterialMotion.Y/rows,r1=(row+1)*PhotoMaterialMotion.Y/rows;
            int i0=r0*(PhotoMaterialMotion.X+1)+c0,i1=r0*(PhotoMaterialMotion.X+1)+c1;
            int i2=r1*(PhotoMaterialMotion.X+1)+c0,i3=r1*(PhotoMaterialMotion.X+1)+c1;
            float touch=motion.firstTouch(c0,r0,c1,r1),age=touch<0?-1:(seconds-touch)*recovery;
            int index=row*cols+col;float seed=PhotoMaterialMotion.noise(index+83);
            float cx=(motion.base[i0*2]+motion.base[i3*2])*.5f,cy=(motion.base[i0*2+1]+motion.base[i3*2+1])*.5f;
            float detach=age<0?0:Math.max(0,age-(ice?.22f:.10f));
            chunkIndices[0]=i0;chunkIndices[1]=i1;chunkIndices[2]=i2;chunkIndices[3]=i3;
            if(detach>0 && !detached[index]) {
                for(int i=0;i<4;i++){anchoredChunks[index*8+i*2]=motion.base[chunkIndices[i]*2];anchoredChunks[index*8+i*2+1]=motion.base[chunkIndices[i]*2+1];}
                detached[index]=true;
            }
            for(int i=0;i<4;i++) {
                chunkPose[i*2]=detached[index]?anchoredChunks[index*8+i*2]:motion.base[chunkIndices[i]*2];
                chunkPose[i*2+1]=detached[index]?anchoredChunks[index*8+i*2+1]:motion.base[chunkIndices[i]*2+1];
                if(detached[index] && recovery<1) {
                    chunkPose[i*2]=motion.base[chunkIndices[i]*2]+(chunkPose[i*2]-motion.base[chunkIndices[i]*2])*recovery;
                    chunkPose[i*2+1]=motion.base[chunkIndices[i]*2+1]+(chunkPose[i*2+1]-motion.base[chunkIndices[i]*2+1])*recovery;
                }
            }
            cx=(chunkPose[0]+chunkPose[6])*.5f;cy=(chunkPose[1]+chunkPose[7])*.5f;
            float scaleX=1,scaleY=1,angle=0,offsetX=0,offsetY=0,alpha=1;
            if(age>=0) {
                if(ice) {
                    // The contacted part first becomes solid frost, then cracks into large rigid photo shards.
                    angle=detach*(seed-.5f)*3.2f;
                    offsetX=density*(30+seed*100)*detach;
                    offsetY=density*(-24*detach+145*detach*detach);
                    alpha=1-PhotoMaterialMotion.smooth((detach-.95f)/.45f);
                } else {
                    // Compression precedes a discrete crunch; broken pieces are small, tumbling, bouncing crumbs.
                    float squash=PhotoMaterialMotion.smooth(age/.10f)*(1-PhotoMaterialMotion.smooth(detach/.08f));
                    scaleX=1-.78f*squash;scaleY=1+.30f*squash;
                    float kick=25+seed*130;offsetX=density*kick*detach;angle=detach*(seed-.5f)*16;
                    float fall=density*(-60*detach+260*detach*detach);
                    float floor=height-density*16-cy;
                    if(fall>floor) {
                        float after=(float)Math.sqrt(Math.max(0,(fall-floor)/Math.max(1,density*260)));
                        fall=floor-density*22*(float)Math.abs(Math.sin(after*13))*(float)Math.exp(-after*3.5);
                    }
                    offsetY=fall;alpha=1-PhotoMaterialMotion.smooth((detach-1)/.5f);
                }
            }
            if(alpha<=0)continue;
            paint.setAlpha((int)(255*alpha));
            if(ice) {
                drawTriangle(canvas,photo,i0,i1,i2,cx,cy,offsetX,offsetY,angle,scaleX,scaleY);
                frost(canvas,age,alpha);
                if(age>=0 && detach<.6f)crystal(canvas,cx+offsetX,cy+offsetY,density*(5+8*seed)*PhotoMaterialMotion.smooth(age/.22f),alpha*.8f,density);
                drawTriangle(canvas,photo,i1,i3,i2,cx,cy,offsetX+density*detach*8,offsetY,angle+.25f*detach,scaleX,scaleY);
                frost(canvas,age,alpha);
            } else {
                drawTriangle(canvas,photo,i0,i1,i3,cx,cy,offsetX,offsetY,angle,scaleX,scaleY);
                drawTriangle(canvas,photo,i0,i3,i2,cx,cy,offsetX,offsetY,angle,scaleX,scaleY);
                if(age>=0 && detach<.08f) {
                    overlay.setColor(0xFF6B342A);overlay.setAlpha((int)(95*PhotoMaterialMotion.smooth(age/.1f)));
                    canvas.drawPath(shape,overlay);
                }
            }
        }
        paint.setAlpha(255);
    }
    private void drawTriangle(Canvas canvas,Bitmap photo,int a,int b,int c,float cx,float cy,
                              float dx,float dy,float angle,float sx,float sy) {
        point(a,0,photo,cx,cy,dx,dy,angle,sx,sy);point(b,2,photo,cx,cy,dx,dy,angle,sx,sy);point(c,4,photo,cx,cy,dx,dy,angle,sx,sy);
        shape.reset();shape.moveTo(dst[0],dst[1]);shape.lineTo(dst[2],dst[3]);shape.lineTo(dst[4],dst[5]);shape.close();
        if(!texture.setPolyToPoly(src,0,dst,0,3))return;
        int save=canvas.save();canvas.clipPath(shape);canvas.concat(texture);canvas.drawBitmap(photo,null,whole,paint);canvas.restoreToCount(save);
    }
    private void point(int n,int at,Bitmap photo,float cx,float cy,float dx,float dy,float angle,float sx,float sy) {
        src[at]=(n%(PhotoMaterialMotion.X+1))/(float)PhotoMaterialMotion.X*photo.getWidth();
        src[at+1]=(n/(PhotoMaterialMotion.X+1))/(float)PhotoMaterialMotion.Y*photo.getHeight();
        int slot=n==chunkIndices[0]?0:n==chunkIndices[1]?1:n==chunkIndices[2]?2:3;
        float x=(chunkPose[slot*2]-cx)*sx,y=(chunkPose[slot*2+1]-cy)*sy;
        dst[at]=cx+x*(float)Math.cos(angle)-y*(float)Math.sin(angle)+dx;
        dst[at+1]=cy+x*(float)Math.sin(angle)+y*(float)Math.cos(angle)+dy;
    }
    private void frost(Canvas canvas,float age,float alpha) {
        if(age<0)return;
        overlay.setColor(0xFFD5F3FF);overlay.setAlpha((int)(185*PhotoMaterialMotion.smooth(age/.14f)*alpha));canvas.drawPath(shape,overlay);
        if(age>.10f && age<.25f) {
            overlay.setStyle(Paint.Style.STROKE);overlay.setStrokeWidth(1.3f);overlay.setColor(0xFF72BFDB);
            overlay.setAlpha((int)(210*alpha));canvas.drawPath(shape,overlay);overlay.setStyle(Paint.Style.FILL);
        }
    }
    private void drawShedMaterial(Canvas canvas,Bitmap photo,SwipeTheme.Kind kind,float seconds,float density,float height,float recovery) {
        if(kind==SwipeTheme.Kind.ICE || kind==SwipeTheme.Kind.CANDY)return;
        for(int row=1;row<PhotoMaterialMotion.Y;row+=3)for(int col=1;col<PhotoMaterialMotion.X;col+=3) {
            int n=row*(PhotoMaterialMotion.X+1)+col;float age=motion.age(n,seconds);
            if(age<0)continue;
            float seed=PhotoMaterialMotion.noise(n),x=motion.contactX[n],y=motion.contactY[n],rx=0,ry=0,alpha=0,angle=0;
            switch(kind) {
                case FIRE: {
                    float t=age-.28f;if(t<0 || t>1)continue;
                    x+=density*(10+seed*35)*t;y-=density*(25*t+20*t*t);rx=ry=density*(1.8f+seed*2.5f)*(1-t);
                    alpha=(1-t)*recovery;angle=t*2;break;
                }
                case WATER: {
                    float t=age-.18f;if(t<0 || t>1.4f)continue;
                    x+=density*(15+seed*25)*(float)Math.sin(t*3+seed*6)*t;y+=density*(70*t+160*t*t);
                    rx=density*(3+seed*5);ry=rx*(1+.9f*t);alpha=(1-PhotoMaterialMotion.smooth((t-.8f)/.6f))*recovery;break;
                }
                case TOXIC: {
                    float t=age-.30f;if(t<0 || t>2)continue;
                    x+=density*4*(float)Math.sin(seed*8+t);y+=density*(12*t+45*t*t);
                    rx=density*(4+seed*5);ry=rx*(1+2*t);alpha=(1-PhotoMaterialMotion.smooth((t-1.4f)/.6f))*recovery;break;
                }
                case SPACE: {
                    float t=age;if(t>1)continue;
                    float pull=PhotoMaterialMotion.smooth(t),cy=height*.5f;
                    x+=(density*3-x)*pull;y+=(cy-y)*pull;
                    rx=density*(15+30*t)*(1-pull);ry=density*2*(1-pull);alpha=(1-pull)*recovery;angle=(float)Math.atan2(cy-y,density*3-x);break;
                }
                default:continue;
            }
            if(rx<.2f || alpha<=0)continue;
            int u=col*photo.getWidth()/PhotoMaterialMotion.X,v=row*photo.getHeight()/PhotoMaterialMotion.Y;
            int side=Math.max(2,photo.getWidth()/PhotoMaterialMotion.X);
            sample.set(Math.max(0,u-side),Math.max(0,v-side),Math.min(photo.getWidth(),u+side),Math.min(photo.getHeight(),v+side));
            destination.set(x-rx,y-ry,x+rx,y+ry);
            int save=canvas.save();canvas.rotate((float)Math.toDegrees(angle),x,y);
            shape.reset();shape.addOval(destination,Path.Direction.CW);canvas.clipPath(shape);
            paint.setAlpha((int)(255*alpha));canvas.drawBitmap(photo,sample,destination,paint);
            overlay.setColor(kind==SwipeTheme.Kind.FIRE?0xFF21140C:kind==SwipeTheme.Kind.TOXIC?0xFF82B727:0xFF75CAED);
            overlay.setAlpha((int)((kind==SwipeTheme.Kind.FIRE?225:kind==SwipeTheme.Kind.TOXIC?110:35)*alpha));canvas.drawOval(destination.left,destination.top,destination.right,destination.bottom,overlay);
            canvas.restoreToCount(save);
        }
        paint.setAlpha(255);
    }
    private void drawBurnFront(Canvas canvas,float seconds,float density,float recovery) {
        if(flameArt==null || flameArt.isRecycled())return;
        // Only touched UV material ignites; flames originate at the fixed left collision surface.
        for(int row=0;row<=PhotoMaterialMotion.Y;row+=2) {
            int selected=-1;float best=Float.MAX_VALUE;
            for(int col=0;col<=PhotoMaterialMotion.X;col++) {
                int n=row*(PhotoMaterialMotion.X+1)+col;float age=motion.age(n,seconds);
                if(age<0 || age>.36f)continue;
                float error=Math.abs(age-.10f);if(error<best){best=error;selected=n;}
            }
            if(selected<0)continue;
            float age=motion.age(selected,seconds),heat=(1-PhotoMaterialMotion.smooth((age-.22f)/.14f))*recovery;
            float x=motion.contactX[selected],y=motion.contactY[selected];
            drawFlame(canvas,x,y,density*(22+7*PhotoMaterialMotion.noise(row)),55,seconds+row,heat);
        }
        paint.setAlpha(255);
    }
    private void drawFlame(Canvas canvas,float x,float y,float size,float angle,float seconds,float alpha) {
        float flicker=.88f+.12f*(float)Math.sin(seconds*19);
        destination.set(x-size*.65f,y-size*2.2f*flicker,x+size*.65f,y+size*.08f);
        int save=canvas.save();canvas.rotate(angle,x,y);
        paint.setAlpha((int)(245*PhotoMaterialMotion.clamp(alpha)));canvas.drawBitmap(flameArt,null,destination,paint);canvas.restoreToCount(save);
    }
    private void drawKeepFire(Canvas canvas,float phase,float flight,float seconds,float density) {
        if(flameArt==null || flameArt.isRecycled())return;
        float meshWidth=(float)Math.hypot(motion.vertices[PhotoMaterialMotion.X*2]-motion.vertices[0],motion.vertices[PhotoMaterialMotion.X*2+1]-motion.vertices[1]);
        for(int col=0;col<=PhotoMaterialMotion.X;col+=2) {
            int selected=-1;float best=Float.MAX_VALUE;
            for(int row=0;row<=PhotoMaterialMotion.Y;row++) {
                float age=PhotoMaterialMotion.fireKeepAge(row/(float)PhotoMaterialMotion.Y,phase,flight);
                if(age<0 || age>.30f)continue;
                float error=Math.abs(age-.10f);if(error<best){best=error;selected=row*(PhotoMaterialMotion.X+1)+col;}
            }
            if(selected<0)continue;
            float age=PhotoMaterialMotion.fireKeepAge((selected/(PhotoMaterialMotion.X+1))/(float)PhotoMaterialMotion.Y,phase,flight);
            float alpha=1-PhotoMaterialMotion.smooth((age-.18f)/.12f);
            drawFlame(canvas,motion.vertices[selected*2],motion.vertices[selected*2+1],Math.max(density*18,meshWidth/PhotoMaterialMotion.X*2.4f),0,seconds+col,alpha);
        }
        paint.setAlpha(255);
    }
    private void drawKeepFinish(Canvas canvas,SwipeTheme.Kind kind,float phase,float seconds,float density,float width,float height,float close) {
        float p=PhotoMaterialMotion.smooth(phase),alpha=p*(1-PhotoMaterialMotion.smooth((close-.78f)/.22f));
        if(alpha<.001f)return;
        float cx=PhotoMaterialMotion.keepX(kind,width,density,close),cy=PhotoMaterialMotion.keepY(kind,height,density,close);
        float r=PhotoMaterialMotion.keepRadius(kind,height,density,close);
        overlay.setStyle(Paint.Style.FILL);
        switch(kind) {
            case WATER:
                overlay.setColor(0xFF88DFFF);overlay.setAlpha((int)(36*alpha));canvas.drawCircle(cx,cy,r,overlay);
                overlay.setStyle(Paint.Style.STROKE);overlay.setStrokeWidth(density*2);overlay.setAlpha((int)(160*alpha));canvas.drawCircle(cx,cy,r,overlay);
                overlay.setStyle(Paint.Style.FILL);overlay.setAlpha((int)(100*alpha));canvas.drawOval(cx-r*.60f,cy-r*.78f,cx-r*.05f,cy-r*.59f,overlay);
                overlay.setColor(0xFFFFFFFF);overlay.setAlpha((int)(85*alpha));canvas.drawOval(cx+r*.43f,cy+r*.16f,cx+r*.55f,cy+r*.47f,overlay);break;
            case ICE:
                // Facets grow over the photograph; the whole crystal slides out with the photo inside.
                for(int i=0;i<8;i++) {
                    float angle=i*(float)Math.PI/4-.24f-close*.45f;
                    float next=angle+(float)Math.PI/4;
                    shape.reset();shape.moveTo(cx,cy);
                    shape.lineTo(cx+r*(float)Math.cos(angle)*.91f,cy+r*(float)Math.sin(angle)*1.12f);
                    shape.lineTo(cx+r*(float)Math.cos(next)*.91f,cy+r*(float)Math.sin(next)*1.12f);shape.close();
                    overlay.setColor(i%2==0?0xFFCFF5FF:0xFF70BCE5);overlay.setAlpha((int)((i%2==0?110:65)*alpha));canvas.drawPath(shape,overlay);
                }
                crystal(canvas,cx-r*.35f,cy-r*.3f,r*.22f,alpha,density);break;
            case TOXIC: {
                int save=canvas.save();canvas.rotate(-31.5f*PhotoMaterialMotion.smooth(close),cx,cy);
                float seal=PhotoMaterialMotion.smooth((p-.55f)/.45f);
                destination.set(cx-r*.53f,cy-r*1.05f,cx+r*.53f,cy+r*.94f);
                overlay.setColor(0xFF77CF69);overlay.setAlpha((int)(55*alpha*seal));canvas.drawRoundRect(destination.left,destination.top,destination.right,destination.bottom,r*.22f,r*.22f,overlay);
                overlay.setStyle(Paint.Style.STROKE);overlay.setStrokeWidth(Math.max(1,density*1.5f));overlay.setColor(0xFFB8FFD3);overlay.setAlpha((int)(180*alpha*seal));canvas.drawRoundRect(destination.left,destination.top,destination.right,destination.bottom,r*.22f,r*.22f,overlay);overlay.setStyle(Paint.Style.FILL);
                overlay.setColor(0xFF314F38);overlay.setAlpha((int)(255*alpha*seal));
                canvas.drawRoundRect(destination.left-r*.04f,destination.top-r*.08f,destination.right+r*.04f,destination.top+r*.15f,r*.06f,r*.06f,overlay);
                overlay.setColor(0xFFE1FFED);overlay.setAlpha((int)(140*alpha*seal));canvas.drawRoundRect(cx-r*.40f,cy-r*.63f,cx-r*.32f,cy+r*.64f,r*.04f,r*.04f,overlay);
                canvas.restoreToCount(save);break;
            }
            case CANDY: {
                float seal=PhotoMaterialMotion.smooth((p-.55f)/.45f),a=alpha*seal;
                int variant=motion.candyVariant;
                overlay.setColor(variant==0?0xFFFF89B7:variant==1?0xFFFFD471:variant==2?0xFFA9F2C6:0xFFC5A1FF);overlay.setAlpha((int)(145*a));
                if(variant==0) {
                    canvas.drawRoundRect(cx-r*.48f,cy-r*.30f,cx+r*.48f,cy+r*.30f,r*.15f,r*.15f,overlay);
                    for(int side=-1;side<=1;side+=2) {
                        shape.reset();shape.moveTo(cx+side*r*.42f,cy);shape.lineTo(cx+side*r*.78f,cy-r*.35f);shape.lineTo(cx+side*r*.78f,cy+r*.35f);shape.close();canvas.drawPath(shape,overlay);
                    }
                } else if(variant==1) {
                    canvas.drawCircle(cx,cy,r*.53f,overlay);
                    overlay.setColor(0xFFFFF1C7);overlay.setAlpha((int)(130*a));canvas.drawOval(cx-r*.32f,cy-r*.40f,cx+r*.10f,cy-r*.23f,overlay);
                } else if(variant==2) {
                    shape.reset();shape.moveTo(cx-r*.38f,cy-r*.58f);shape.lineTo(cx+r*.38f,cy-r*.58f);shape.lineTo(cx+r*.11f,cy+r*.58f);shape.lineTo(cx-r*.11f,cy+r*.58f);shape.close();canvas.drawPath(shape,overlay);
                } else {
                    overlay.setColor(0xFFFFE8FF);overlay.setAlpha((int)(155*a));
                    canvas.drawRoundRect(cx-r*.04f,cy+r*.42f,cx+r*.04f,cy+r*.95f,r*.03f,r*.03f,overlay);
                    overlay.setColor(0xFFC5A1FF);overlay.setAlpha((int)(120*a));canvas.drawCircle(cx,cy,r*.58f,overlay);
                    overlay.setStyle(Paint.Style.STROKE);overlay.setStrokeWidth(r*.06f);
                    shape.reset();for(int i=0;i<=50;i++){float angle=i*.22f,rad=r*.04f+i*r*.009f;float x=cx+rad*(float)Math.cos(angle),y=cy+rad*(float)Math.sin(angle);if(i==0)shape.moveTo(x,y);else shape.lineTo(x,y);}canvas.drawPath(shape,overlay);overlay.setStyle(Paint.Style.FILL);
                }
                break;
            }
            case SPACE: {
                float burst=PhotoMaterialMotion.smooth((p-.60f)/.4f);
                float spread=r*(.18f+close*3.2f+burst*.85f),fade=alpha*burst*(1-close);
                // Bright stellar bloom and radial rays replace the receiving black hole.
                if(stellarGlow==null || stellarX!=cx || stellarY!=cy || stellarRadius!=r) {
                    stellarX=cx;stellarY=cy;stellarRadius=r;
                    stellarGlow=new RadialGradient(cx,cy,r*4,new int[]{0xFFFFFFFF,0xFFEAF6FF,0xB075BDFF,0x405B39B8,0x00000000},new float[]{0,.05f,.24f,.58f,1},Shader.TileMode.CLAMP);
                }
                overlay.setShader(stellarGlow);overlay.setAlpha((int)(255*fade));canvas.drawCircle(cx,cy,spread*2,overlay);overlay.setShader(null);
                overlay.setColor(0xFFFFFFFF);overlay.setAlpha((int)(255*fade));canvas.drawCircle(cx,cy,r*(.06f+.15f*(1-close)),overlay);
                for(int i=0;i<12;i++) {
                    float angle=i*(float)Math.PI/6+.12f*seconds,inner=spread*.32f,outer=spread*(i%4==0?1.6f:1);
                    shape.reset();shape.moveTo(cx+inner*(float)Math.cos(angle-.07),cy+inner*(float)Math.sin(angle-.07));shape.lineTo(cx+outer*(float)Math.cos(angle),cy+outer*(float)Math.sin(angle));shape.lineTo(cx+inner*(float)Math.cos(angle+.07),cy+inner*(float)Math.sin(angle+.07));shape.close();canvas.drawPath(shape,overlay);
                }
                break;
            }
            default:break;
        }
        overlay.setStyle(Paint.Style.FILL);
    }
}
