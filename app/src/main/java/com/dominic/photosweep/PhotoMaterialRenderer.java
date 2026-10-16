package com.dominic.photosweep;

import android.graphics.*;

/** Six photo-material render paths. Every surface, shard and droplet samples the current photo. */
final class PhotoMaterialRenderer {
    final PhotoMaterialMotion motion=new PhotoMaterialMotion();
    private final boolean[] toxicDetached=new boolean[PhotoMaterialMotion.N];
    private final int[] toxicEmitters=new int[PhotoMaterialMotion.Y+1];
    private final float[] toxicEmittedAt=new float[PhotoMaterialMotion.Y+1];
    PhotoMaterialRenderer(){resetToxicEmitters();}
    private void resetToxicEmitters(){java.util.Arrays.fill(toxicDetached,false);java.util.Arrays.fill(toxicEmitters,-1);java.util.Arrays.fill(toxicEmittedAt,-1);}
    private Bitmap receiverArt,flameArt;
    private float toxicNX=0,toxicNY=1,toxicLevel=Float.NaN;
    private final float[] meltX=new float[17],meltY=new float[17],meltWidth=new float[17];
    void setToxicPool(float nx,float ny,float level){toxicNX=nx;toxicNY=ny;toxicLevel=level;}
    private PhotoMaterialArt art;
    private final int[] lightColors=new int[PhotoMaterialMotion.N];
    void setMaterialArt(PhotoMaterialArt value){art=value;if(value!=null)flameArt=value.debris[0];}
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
        PhotoMaterialRenderer copy=new PhotoMaterialRenderer();copy.setArtwork(receiverArt,flameArt);copy.setMaterialArt(art);
        System.arraycopy(motion.base,0,copy.motion.base,0,motion.base.length);
        motion.copyDamageTo(copy.motion);copy.motion.candyVariant=motion.candyVariant;return copy;
    }
    void reset(){motion.reset();resetToxicEmitters();motion.candyVariant=random.nextInt(4);java.util.Arrays.fill(detached,false);}
    void draw(Canvas canvas,Bitmap photo,SwipeTheme.Kind kind,float seconds,float density,float width,float height,
              boolean keeping,float keepPhase,float close,float recovery) {
        if(photo==null || photo.isRecycled())return;
        whole.set(0,0,photo.getWidth(),photo.getHeight());paint.setAlpha(255);paint.setShader(null);
        if(keeping) {
            prepareKeep(kind,seconds,density,width,height);
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
                if(kind==SwipeTheme.Kind.TOXIC)drawToxicMelt(canvas,photo,seconds,density,height,recovery);
                else if(kind!=SwipeTheme.Kind.FIRE)drawShedMaterial(canvas,photo,kind,seconds,density,height,recovery);
                if(kind==SwipeTheme.Kind.FIRE)drawBurnFront(canvas,seconds,density,recovery);
            }
        }
    }
    void prepareKeep(SwipeTheme.Kind kind,float seconds,float density,float width,float height) {
        if(motion.hasDamage())motion.trash(kind,seconds,density,width,height,1);
    }
    private void drawSpaceSlices(Canvas canvas,Bitmap photo,float seconds,float density,float width,float height,float recovery) {
        // Render independent strips. No connecting triangles bridge the ripped seams.
        for(int row=0;row<PhotoMaterialMotion.Y;row+=5)for(int col=0;col<PhotoMaterialMotion.X;col++) {
            int bottom=Math.min(PhotoMaterialMotion.Y,row+5);
            int a=row*(PhotoMaterialMotion.X+1)+col,b=a+1,c=bottom*(PhotoMaterialMotion.X+1)+col,d=c+1;
            chunkIndices[0]=a;chunkIndices[1]=b;chunkIndices[2]=c;chunkIndices[3]=d;
            for(int i=0;i<4;i++) {
                int n=chunkIndices[i];float x=motion.base[n*2],y=motion.base[n*2+1];
                float reach=PhotoMaterialMotion.spaceReach(width,density),force=PhotoMaterialMotion.clamp(1-x/reach)*recovery;
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
        if(art!=null)sprite(canvas,art.debris[2],cx,cy,r*3,r*3,0,alpha);
    }

    private void drawBrittle(Canvas canvas,Bitmap photo,SwipeTheme.Kind kind,float seconds,float density,float height,float recovery) {
        boolean ice=kind==SwipeTheme.Kind.ICE;
        int cols=ice?8:16,rows=ice?10:20;
        // Draw the attached photograph as one mesh, avoiding a grid of clipped intact triangles.
        for(int row=0;row<rows;row++)for(int col=0;col<cols;col++) {
            int c0=col*PhotoMaterialMotion.X/cols,c1=(col+1)*PhotoMaterialMotion.X/cols;
            int r0=row*PhotoMaterialMotion.Y/rows,r1=(row+1)*PhotoMaterialMotion.Y/rows;
            float touch=motion.firstTouch(c0,r0,c1,r1);
            if(touch>=0 && seconds-touch>(ice?.22f:.10f))motion.removePatch(c0,r0,c1,r1);
        }
        canvas.drawBitmapMesh(photo,PhotoMaterialMotion.X,PhotoMaterialMotion.Y,motion.base,0,motion.colors,0,paint);
        for(int row=0;row<rows;row++)for(int col=0;col<cols;col++) {
            int c0=col*PhotoMaterialMotion.X/cols,c1=(col+1)*PhotoMaterialMotion.X/cols;
            int r0=row*PhotoMaterialMotion.Y/rows,r1=(row+1)*PhotoMaterialMotion.Y/rows;
            int i0=r0*(PhotoMaterialMotion.X+1)+c0,i1=r0*(PhotoMaterialMotion.X+1)+c1;
            int i2=r1*(PhotoMaterialMotion.X+1)+c0,i3=r1*(PhotoMaterialMotion.X+1)+c1;
            float touch=motion.firstTouch(c0,r0,c1,r1),age=touch<0?-1:(seconds-touch)*recovery;
            if(age<0)continue;
            int index=row*cols+col;float seed=PhotoMaterialMotion.noise(index+83);
            float cx=(motion.base[i0*2]+motion.base[i3*2])*.5f,cy=(motion.base[i0*2+1]+motion.base[i3*2+1])*.5f;
            float detach=age<0?0:Math.max(0,age-(ice?.22f:.10f));
            chunkIndices[0]=i0;chunkIndices[1]=i1;chunkIndices[2]=i2;chunkIndices[3]=i3;
            if(detach>0 && !detached[index]) {
                for(int i=0;i<4;i++){anchoredChunks[index*8+i*2]=motion.base[chunkIndices[i]*2];anchoredChunks[index*8+i*2+1]=motion.base[chunkIndices[i]*2+1];}
                detached[index]=true;motion.removePatch(c0,r0,c1,r1);
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
                frost(canvas,age,alpha,seconds,index);
                if(age>=0 && detach<.6f)crystal(canvas,cx+offsetX,cy+offsetY,density*(5+8*seed)*PhotoMaterialMotion.smooth(age/.22f),alpha*.8f,density);
                drawTriangle(canvas,photo,i1,i3,i2,cx,cy,offsetX+density*detach*8,offsetY,angle+.25f*detach,scaleX,scaleY);
                frost(canvas,age,alpha,seconds,index);
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
    private void frost(Canvas canvas,float age,float alpha,float seconds,int index) {
        if(age<0)return;
        float freeze=PhotoMaterialMotion.smooth(age/.14f);
        overlay.setColor(0xFFB0E7FF);overlay.setAlpha((int)(75*freeze*alpha));canvas.drawPath(shape,overlay);
        if(art==null)return;
        // Apply optical fractures to the SAME transformed photo triangle; no seam outlines.
        int save=canvas.save();canvas.clipPath(shape);
        float l=Math.min(dst[0],Math.min(dst[2],dst[4])),r=Math.max(dst[0],Math.max(dst[2],dst[4]));
        float t=Math.min(dst[1],Math.min(dst[3],dst[5])),b=Math.max(dst[1],Math.max(dst[3],dst[5]));
        sprite(canvas,art.finishes[2],(l+r)*.5f,(t+b)*.5f,(r-l)*1.65f,(b-t)*1.65f,0,alpha*freeze*.85f);
        float pulse=(float)Math.pow(Math.max(0,Math.sin(seconds*4.6f+index*2.4f)),12);
        sprite(canvas,art.finishes[4],(l+r)*.5f,(t+b)*.5f,Math.max(9,(r-l)*.60f),Math.max(9,(r-l)*.60f),0,alpha*pulse*.85f);
        canvas.restoreToCount(save);
    }
    /** Viscous strips sample the contacted photograph; intact stock is never an emitter. */
    private void drawToxicMelt(Canvas canvas,Bitmap photo,float seconds,float density,float height,float recovery) {
        float level=Float.isNaN(toxicLevel)?height*.86f:toxicLevel;
        float cell=(float)Math.hypot(motion.base[2]-motion.base[0],motion.base[3]-motion.base[1]);
        float releaseGap=Math.max(density*3,cell*.5f);
        // Separation is permanent for this UV event. Returning close to the wall cannot reattach it.
        for(int n=0;n<PhotoMaterialMotion.N;n++)if(motion.touched[n]>=0 &&
                (motion.base[n*2]>motion.contactX[n]+releaseGap || motion.remaining[n]<=.03f || motion.age(n,seconds)>=1.35f))toxicDetached[n]=true;
        for(int row=2;row<PhotoMaterialMotion.Y;row+=4) {
            int selected=toxicEmitters[row];
            if(selected>=0 && motion.age(selected,seconds)>2.3f)selected=-1;
            if(selected<0) {
                float best=Float.MAX_VALUE;
                for(int col=0;col<=PhotoMaterialMotion.X;col++) {
                    int n=row*(PhotoMaterialMotion.X+1)+col;float age=motion.age(n,seconds);
                    // Only new direct contact can start a stream; old corrosion and missing stock cannot.
                    if(age<.035f || age>.45f || motion.spread[n]!=0 || toxicDetached[n] ||
                            motion.remaining[n]<=.03f || motion.touched[n]<=toxicEmittedAt[row])continue;
                    float distance=Math.abs(motion.base[n*2]-motion.contactX[n]);
                    if(distance<best){selected=n;best=distance;}
                }
                toxicEmitters[row]=selected;
                if(selected>=0)toxicEmittedAt[row]=motion.touched[selected];
            }
            if(selected<0)continue;
            int n=selected;float age=motion.age(n,seconds),seed=PhotoMaterialMotion.noise(row+71);
            boolean attached=!toxicDetached[n];
            float startX=attached?motion.vertices[n*2]:motion.contactX[n];
            float startY=attached?motion.vertices[n*2+1]:motion.contactY[n]+density*55*Math.max(0,age-.65f)*Math.max(0,age-.65f);
            float endX=Math.max(density*5,Math.min(density*60,motion.contactX[n]+density*(6+20*seed)));
            float poolY=toxicNY>.15f?(level-toxicNX*endX)/toxicNY:height;
            poolY=Math.min(height+density*8,poolY);
            if(startY>=poolY-density*3)continue;
            float grow=PhotoMaterialMotion.smooth((age-.035f)/(1.12f+.25f*seed));
            float endY=startY+(poolY-startY)*grow;
            float fade=(1-PhotoMaterialMotion.smooth((age-1.65f)/.65f))*recovery;
            if(fade<=0 || endY-startY<density*2)continue;
            float width=density*(2.8f+seed*3.2f)*(attached?1:.72f);
            for(int step=0;step<=16;step++) {
                float t=step/16f,flow=PhotoMaterialMotion.smooth(t/.32f);
                meltX[step]=startX+(endX-startX)*flow+density*2*(float)Math.sin(t*5+seed*7+seconds*.65f)*(float)Math.sin(t*Math.PI);
                meltY[step]=startY+(endY-startY)*t;
                meltWidth[step]=width*(1-.64f*(float)Math.sin(t*Math.PI))*(1-.18f*grow);
            }
            shape.reset();for(int step=0;step<=16;step++){if(step==0)shape.moveTo(meltX[step]-meltWidth[step],meltY[step]);else shape.lineTo(meltX[step]-meltWidth[step],meltY[step]);}
            for(int step=16;step>=0;step--)shape.lineTo(meltX[step]+meltWidth[step],meltY[step]);shape.close();
            int col=n%(PhotoMaterialMotion.X+1),sy=row*photo.getHeight()/PhotoMaterialMotion.Y,sx=col*photo.getWidth()/PhotoMaterialMotion.X;
            int sw=Math.max(3,photo.getWidth()/18),sh=Math.max(3,photo.getHeight()/12);
            sample.set(Math.max(0,sx-sw),Math.max(0,sy-sh),Math.min(photo.getWidth(),sx+sw),Math.min(photo.getHeight(),sy+sh));
            float left=Math.min(startX,endX)-width-density*3,right=Math.max(startX,endX)+width+density*3;
            destination.set(left,startY,right,endY+density*2);
            int save=canvas.save();canvas.clipPath(shape);paint.setAlpha((int)(245*fade));canvas.drawBitmap(photo,sample,destination,paint);
            overlay.setColor(0xFF82B727);overlay.setAlpha((int)(55*fade));canvas.drawPath(shape,overlay);canvas.restoreToCount(save);
            // A heavy photo-textured bead hangs from the thread, then spreads into the pool.
            float rx=width*(grow>.97f?2.2f:1.25f),ry=width*(grow>.97f?.65f:1.75f);
            destination.set(endX-rx,endY-ry,endX+rx,endY+ry);shape.reset();shape.addOval(destination,Path.Direction.CW);
            save=canvas.save();canvas.clipPath(shape);paint.setAlpha((int)(230*fade));canvas.drawBitmap(photo,sample,destination,paint);overlay.setAlpha((int)(65*fade));canvas.drawOval(destination.left,destination.top,destination.right,destination.bottom,overlay);canvas.restoreToCount(save);
        }
        paint.setAlpha(255);
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
            if(art!=null && (kind==SwipeTheme.Kind.WATER || kind==SwipeTheme.Kind.TOXIC))
                sprite(canvas,art.debris[PhotoMaterialArt.index(kind)],x,y,rx*4,ry*3,(float)Math.toDegrees(angle),alpha*.85f);
        }
        paint.setAlpha(255);
    }
    private void drawBurnFront(Canvas canvas,float seconds,float density,float recovery) {
        if(flameArt==null || flameArt.isRecycled())return;
        // Only touched UV material ignites; flames originate at the fixed left collision surface.
        for(int row=0;row<=PhotoMaterialMotion.Y;row+=3) {
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
        destination.set(x-size*.82f,y-size*2.6f*flicker,x+size*.82f,y+size*.08f);
        int save=canvas.save();canvas.rotate(angle,x,y);
        paint.setAlpha((int)(245*PhotoMaterialMotion.clamp(alpha)));canvas.drawBitmap(flameArt,null,destination,paint);canvas.restoreToCount(save);
    }
    private void drawKeepFire(Canvas canvas,float phase,float flight,float seconds,float density) {
        if(flameArt==null || flameArt.isRecycled())return;
        float meshWidth=(float)Math.hypot(motion.vertices[PhotoMaterialMotion.X*2]-motion.vertices[0],motion.vertices[PhotoMaterialMotion.X*2+1]-motion.vertices[1]);
        for(int col=0;col<=PhotoMaterialMotion.X;col+=4) {
            int selected=-1;float best=Float.MAX_VALUE;
            for(int row=0;row<=PhotoMaterialMotion.Y;row++) {
                float age=PhotoMaterialMotion.fireKeepAge(row/(float)PhotoMaterialMotion.Y,phase,flight);
                if(age<0 || age>.30f || motion.remaining[row*(PhotoMaterialMotion.X+1)+col]<=0)continue;
                float error=Math.abs(age-.10f);if(error<best){best=error;selected=row*(PhotoMaterialMotion.X+1)+col;}
            }
            if(selected<0)continue;
            float age=PhotoMaterialMotion.fireKeepAge((selected/(PhotoMaterialMotion.X+1))/(float)PhotoMaterialMotion.Y,phase,flight);
            float alpha=1-PhotoMaterialMotion.smooth((age-.18f)/.12f);
            drawFlame(canvas,motion.vertices[selected*2],motion.vertices[selected*2+1],Math.max(density*18,meshWidth/PhotoMaterialMotion.X*3.8f),(PhotoMaterialMotion.noise(col)-.5f)*14,seconds+col*.37f,alpha);
        }
        paint.setAlpha(255);
    }
    private void sprite(Canvas canvas,Bitmap bitmap,float cx,float cy,float w,float h,float angle,float alpha) {
        if(bitmap==null || alpha<=.001f || w<=0 || h<=0)return;
        destination.set(cx-w*.5f,cy-h*.5f,cx+w*.5f,cy+h*.5f);
        int save=canvas.save();canvas.rotate(angle,cx,cy);
        paint.setAlpha(Math.round(255*PhotoMaterialMotion.clamp(alpha)));
        canvas.drawBitmap(bitmap,null,destination,paint);canvas.restoreToCount(save);paint.setAlpha(255);
    }
    /** Clip finishing optics to surviving material, so a shell can never conceal a torn-out patch. */
    private int clipSurvivors(Canvas canvas) {
        int save=canvas.save();shape.reset();
        for(int row=0;row<PhotoMaterialMotion.Y;row++)for(int col=0;col<PhotoMaterialMotion.X;col++) {
            int a=row*(PhotoMaterialMotion.X+1)+col,b=a+1,c=a+PhotoMaterialMotion.X+1,d=c+1;
            survivorTriangle(a,b,d);survivorTriangle(a,d,c);
        }
        canvas.clipPath(shape);return save;
    }
    private void survivorTriangle(int a,int b,int c) {
        if(motion.remaining[a]<.98f || motion.remaining[b]<.98f || motion.remaining[c]<.98f)return;
        shape.moveTo(motion.vertices[a*2],motion.vertices[a*2+1]);shape.lineTo(motion.vertices[b*2],motion.vertices[b*2+1]);shape.lineTo(motion.vertices[c*2],motion.vertices[c*2+1]);shape.close();
    }
    private void iceLight(Canvas canvas,float seconds,float alpha) {
        if(art==null)return;
        // Travelling specular band follows UV material rather than the finger or screen.
        for(int n=0;n<PhotoMaterialMotion.N;n++) {
            float u=(n%(PhotoMaterialMotion.X+1))/(float)PhotoMaterialMotion.X;
            float v=(n/(PhotoMaterialMotion.X+1))/(float)PhotoMaterialMotion.Y;
            float wave=(seconds*.48f)%1.8f-.4f,delta=u+v*.34f-wave;
            float shine=(float)Math.exp(-delta*delta/ .007f);
            int a=Math.round(190*shine*alpha*motion.remaining[n]);lightColors[n]=(a<<24)|0xFFFFFF;
        }
        canvas.drawBitmapMesh(art.finishes[2],PhotoMaterialMotion.X,PhotoMaterialMotion.Y,motion.vertices,0,lightColors,0,paint);
    }
    private void drawKeepFinish(Canvas canvas,SwipeTheme.Kind kind,float phase,float seconds,float density,float width,float height,float close) {
        if(art==null)return;
        float p=PhotoMaterialMotion.smooth(phase),alpha=p*(1-PhotoMaterialMotion.smooth((close-.78f)/.22f));
        if(alpha<.001f)return;
        float cx=PhotoMaterialMotion.keepX(kind,width,density,close),cy=PhotoMaterialMotion.keepY(kind,height,density,close);
        float r=PhotoMaterialMotion.keepRadius(kind,height,density,close);
        int save=motion.hasDamage()?clipSurvivors(canvas):canvas.save();
        switch(kind) {
            case WATER:
                sprite(canvas,art.finishes[1],cx,cy,r*2.42f,r*2.42f,3*(float)Math.sin(seconds*1.6),alpha);break;
            case ICE:
                sprite(canvas,art.finishes[2],cx,cy,r*1.84f,r*2.20f,-13.75f*p-close*25.78f,alpha*.95f);
                iceLight(canvas,seconds,alpha);
                // Distinct pinpoint flashes sweep the refractive edges rather than outlining the card.
                for(int i=0;i<5;i++) {
                    float pulse=(float)Math.pow(Math.max(0,Math.sin(seconds*5.4f+i*1.9f)),18);
                    float angle=i*1.2566f-.24f-close*.45f;
                    sprite(canvas,art.finishes[4],cx+r*.63f*(float)Math.cos(angle),cy+r*.79f*(float)Math.sin(angle),r*.48f,r*.48f,seconds*9,alpha*pulse);
                }
                break;
            case TOXIC:
                sprite(canvas,art.finishes[3],cx,cy-r*.05f,r*1.22f,r*2.44f,-31.5f*PhotoMaterialMotion.smooth(close),alpha*PhotoMaterialMotion.smooth((p-.45f)/.55f));break;
            case CANDY: {
                float seal=PhotoMaterialMotion.smooth((p-.40f)/.60f);
                int variant=motion.candyVariant;
                float scale=variant==0?2.0f:variant==1?1.30f:variant==2?1.15f:1.25f;
                sprite(canvas,art.candy[variant],cx,cy,r*scale,r*scale*art.candy[variant].getHeight()/art.candy[variant].getWidth(),variant==0?6*(float)Math.sin(seconds*2):0,alpha*seal);break;
            }
            case SPACE: {
                // Emissive stellar photography replaces polygonal rays and flat circles.
                float burst=PhotoMaterialMotion.smooth((p-.55f)/.45f);
                float spread=r*(1.1f+close*4.8f);
                sprite(canvas,art.finishes[4],cx,cy,spread*2,spread*2,seconds*4,alpha*burst);
                sprite(canvas,art.finishes[4],cx,cy,spread,spread,45-seconds*7,alpha*burst*.65f);break;
            }
            default:break;
        }
        canvas.restoreToCount(save);
    }
}
