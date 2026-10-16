package com.dominic.photosweep;

import java.util.Arrays;

/** Material state belongs to the photograph's UV coordinates, not the finger or a decorative overlay. */
final class PhotoMaterialMotion {
    static final int X=32,Y=40,N=(X+1)*(Y+1);
    final float[] touched=new float[N],contactX=new float[N],contactY=new float[N];
    final float[] base=new float[N*2],vertices=new float[N*2];
    final int[] colors=new int[N];
    private final float[] hit=new float[2];
    int candyVariant;
    PhotoMaterialMotion(){reset();}
    void reset(){Arrays.fill(touched,-1);Arrays.fill(colors,0xFFFFFFFF);}
    static float clamp(float value){return Math.max(0,Math.min(1,value));}
    static float smooth(float value){float t=clamp(value);return t*t*(3-2*t);}
    static float noise(int index){int h=index*1664525+1013904223;h^=h>>>16;return (h&65535)/65535f;}
    void pose(float[] m) {
        for(int row=0;row<=Y;row++)for(int col=0;col<=X;col++) {
            int n=row*(X+1)+col;float u=col/(float)X,v=row/(float)Y;
            base[n*2]=m[0]*u+m[1]*v+m[2];base[n*2+1]=m[3]*u+m[4]*v+m[5];
        }
    }
    void contact(float[] sweep,int steps,float[] edges,float height,float seconds,float footprint) {
        float maximum=0;for(float edge:edges)maximum=Math.max(maximum,edge);
        for(int row=0;row<=Y;row++)for(int col=0;col<=X;col++) {
            int n=row*(X+1)+col;if(touched[n]>=0)continue;
            float u=col/(float)X,v=row/(float)Y;
            float px=sweep[0]*u+sweep[1]*v+sweep[2],py=sweep[3]*u+sweep[4]*v+sweep[5];
            for(int step=1;step<=steps;step++) {
                int at=step*6;float nx=sweep[at]*u+sweep[at+1]*v+sweep[at+2],ny=sweep[at+3]*u+sweep[at+4]*v+sweep[at+5];
                if(Math.min(px,nx)<=maximum+footprint && PhotoContact.coverage(px,py,nx,ny,footprint,height,edges,hit)>.02f) {
                    touched[n]=seconds;contactX[n]=hit[0];contactY[n]=hit[1];break;
                }
                px=nx;py=ny;
            }
        }
    }
    void evolve(SwipeTheme.Kind kind,float seconds) {
        if(kind!=SwipeTheme.Kind.WATER && kind!=SwipeTheme.Kind.TOXIC)return;
        float delay=kind==SwipeTheme.Kind.WATER?.075f:.25f;
        // Saturation and corrosion spread. Fire, ice and candy stay contact-local.
        for(int n=0;n<N;n++)if(touched[n]>=0 && seconds-touched[n]>delay) {
            if(n%(X+1)>0)ignite(n-1,n,seconds,delay);
            if(n%(X+1)<X)ignite(n+1,n,seconds,delay);
            if(n>X)ignite(n-X-1,n,seconds,delay);
            if(n<N-X-1)ignite(n+X+1,n,seconds,delay);
        }
    }
    private void ignite(int next,int from,float seconds,float delay) {
        float ignition=touched[from]+delay+.045f*noise(next);
        if(touched[next]<0 && ignition<=seconds){touched[next]=ignition;contactX[next]=base[next*2];contactY[next]=base[next*2+1];}
    }
    float age(int n,float seconds){return touched[n]<0?-1:Math.max(0,seconds-touched[n]);}
    float firstTouch(int col0,int row0,int col1,int row1) {
        float first=Float.MAX_VALUE;
        for(int row=row0;row<=row1;row++)for(int col=col0;col<=col1;col++) {
            float t=touched[Math.min(Y,row)*(X+1)+Math.min(X,col)];if(t>=0)first=Math.min(first,t);
        }
        return first==Float.MAX_VALUE?-1:first;
    }
    void trash(SwipeTheme.Kind kind,float seconds,float density,float width,float height,float recovery) {
        evolve(kind,seconds);
        for(int n=0;n<N;n++) {
            float x=base[n*2],y=base[n*2+1],age=age(n,seconds),seed=noise(n),a=1;
            int r=255,g=255,b=255;
            if(kind==SwipeTheme.Kind.SPACE) {
                // Each horizontal photo slice has its own orbit and inward pull.
                float reach=Math.min(width*.70f,350*density),force=clamp(1-x/reach)*recovery;
                int band=(n/(X+1))/5;
                float tidal=force*force,angle=tidal*(2.4f+.18f*band);
                float dx=x-density*8,dy=y-height*.5f;
                x=density*8+(dx*(float)Math.cos(angle)-dy*(float)Math.sin(angle))*(1-.94f*tidal)-reach*.30f*tidal;
                y=height*.5f+(dx*(float)Math.sin(angle)+dy*(float)Math.cos(angle))*(1-.94f*tidal);
                y+=density*12*force*(float)Math.sin(band*2.7);
                if(age>=0)a*=1-smooth(age/.85f);
            } else if(age>=0) {
                float t=age*recovery;
                switch(kind) {
                    case FIRE: {
                        float heat=smooth(t/.13f);
                        r=255;g=(int)(255-120*heat);b=(int)(255-225*heat);
                        a=1-smooth((t-(.18f+seed*.06f))/.18f);
                        y-=density*8*heat;
                        break;
                    }
                    case WATER: {
                        float flow=smooth(t/.55f),px=contactX[n]+density*12*(float)Math.sin(n*.71+t*5);
                        float py=contactY[n]+density*(28*t+180*t*t);
                        x+=(px-x)*flow;y+=(py-y)*flow;
                        x+=density*9*flow*(float)Math.sin(y/(density*20)-seconds*7);
                        a=1-smooth((t-.4f)/.65f);r=190;g=235;b=255;break;
                    }
                    case TOXIC: {
                        float melt=smooth(t/.8f),lane=(n%(X+1))/2;
                        float drop=density*(18*t+65*t*t)*( .35f+.65f*noise((int)lane+37));
                        x+=(contactX[n]-x)*melt*.82f;y+=drop;
                        x+=density*3*melt*(float)Math.sin(lane*2.1+seconds*.8);
                        a=1-smooth((t-(.65f+seed*.4f))/.6f);r=145;g=220;b=80;break;
                    }
                    default:break;
                }
            }
            vertices[n*2]=base[n*2]+(x-base[n*2])*recovery;
            vertices[n*2+1]=base[n*2+1]+(y-base[n*2+1])*recovery;
            int alpha=Math.round(255*(1-(1-clamp(a))*recovery));
            r=Math.round(255+(r-255)*recovery);g=Math.round(255+(g-255)*recovery);b=Math.round(255+(b-255)*recovery);
            colors[n]=(alpha<<24)|(Math.max(0,r)<<16)|(Math.max(0,g)<<8)|Math.max(0,b);
        }
    }
    void keep(SwipeTheme.Kind kind,float phase,float seconds,float density,float width,float height,float close) {
        float p=smooth(phase),cx=keepX(kind,width,density,close),cy=keepY(kind,height,density,close);
        float radius=keepRadius(kind,height,density,close),last=1-smooth((close-.78f)/.22f);
        for(int row=0;row<=Y;row++)for(int col=0;col<=X;col++) {
            int n=row*(X+1)+col;float u=col/(float)X,v=row/(float)Y,du=u-.5f,dv=v-.5f;
            float x=base[n*2],y=base[n*2+1],tx=x,ty=y,alpha=1;int r=255,g=255,b=255;
            switch(kind) {
                case FIRE: {
                    // The accepted deed stays recognizable as flames consume it from bottom to top.
                    float centerX=(base[0]+base[(N-1)*2])*.5f,centerY=(base[1]+base[(N-1)*2+1])*.5f;
                    tx=width*.64f+(x-centerX)*.72f;
                    ty=height*.5f+(y-centerY)*.72f-density*20*close;
                    float age=fireKeepAge(v,phase,close);
                    if(age>=0) {
                        float heat=smooth(age/.10f);
                        r=255;g=(int)(255-120*heat);b=(int)(255-225*heat);
                        alpha=1-smooth((age-.10f)/.15f);
                    }
                    break;
                }
                case WATER: {
                    // Photo UVs refract over a sphere: the received image visibly becomes a bubble.
                    float theta=du*(float)Math.PI,phi=dv*(float)Math.PI;
                    tx=cx+radius*(float)Math.sin(theta)*(float)Math.cos(phi);
                    ty=cy+radius*(float)Math.sin(phi);
                    tx+=density*3*(float)Math.sin(v*14+seconds*4)*p;break;
                }
                case ICE: {
                    // An intact crystalline plaque crystallises and rotates into safe storage.
                    float angle=-.24f*p-close*.45f,px=du*radius*1.45f,py=dv*radius*1.8f;
                    tx=cx+px*(float)Math.cos(angle)-py*(float)Math.sin(angle);
                    ty=cy+px*(float)Math.sin(angle)+py*(float)Math.cos(angle);r=(int)(255-65*p);g=(int)(255-15*p);break;
                }
                case TOXIC: {
                    // Roll the photograph into a sealed glass specimen, without melting the kept image.
                    float theta=du*2.7f;
                    float tilt=-.55f*smooth(close),px=radius*.40f*(float)Math.sin(theta),py=dv*radius*1.6f;
                    tx=cx+px*(float)Math.cos(tilt)-py*(float)Math.sin(tilt);
                    ty=cy+px*(float)Math.sin(tilt)+py*(float)Math.cos(tilt)-radius*.18f*smooth((p-.65f)/.35f);
                    float shade=1-.24f*p*(1-(float)Math.cos(theta));
                    r=(int)((255-30*p)*shade);g=(int)(255*shade);b=(int)((255-70*p)*shade);break;
                }
                case SPACE: {
                    // A kept photograph compresses into a luminous star, then bursts outward.
                    float compress=1-smooth((p-.15f)/.80f);
                    float scale=radius*1.8f*compress;
                    tx=cx+du*scale;ty=cy+dv*scale;
                    r=255;g=255;b=255;alpha=1-smooth((p-.65f)/.35f);break;
                }
                case CANDY: {
                    float fold=(float)Math.sin(u*Math.PI*6),px=du*radius,py=dv*radius;
                    if(candyVariant==0) {px*=.95f;py*=.60f;py+=fold*radius*.08f*p;}
                    else if(candyVariant==1) {float theta=du*(float)Math.PI,phi=dv*(float)Math.PI;px=radius*.52f*(float)Math.sin(theta)*(float)Math.cos(phi);py=radius*.52f*(float)Math.sin(phi);}
                    else if(candyVariant==2) {px*=.75f*(1-.70f*v);py*=1.15f;}
                    else {float angle=v*5.6f;float rad=radius*(.14f+.44f*u);px=rad*(float)Math.cos(angle);py=rad*(float)Math.sin(angle);}
                    tx=cx+px;ty=cy+py;
                    float crease=1-.24f*(float)Math.sin(p*Math.PI)*Math.max(0,fold);
                    r=(int)(255*crease);g=(int)((255-15*p)*crease);b=(int)((255-8*p)*crease);break;
                }
                default:break;
            }
            vertices[n*2]=x+(tx-x)*p;vertices[n*2+1]=y+(ty-y)*p;
            colors[n]=(Math.round(255*clamp(alpha)*last)<<24)|(r<<16)|(g<<8)|b;
        }
    }
    static float fireKeepAge(float v,float phase,float flight) {
        float front=.18f*smooth(phase)+1.14f*smooth(flight/.8f);
        float passed=front-(1-v);
        return front<=0 || passed<0?-1:passed*.9f;
    }
    static float keepX(SwipeTheme.Kind kind,float width,float density,float flight) {
        float drift=kind==SwipeTheme.Kind.WATER?240:kind==SwipeTheme.Kind.ICE?290:kind==SwipeTheme.Kind.CANDY?220:0;
        return width-112*density+density*drift*flight*flight;
    }
    static float keepY(SwipeTheme.Kind kind,float height,float density,float flight) {
        return height*.5f-density*(kind==SwipeTheme.Kind.WATER?390:kind==SwipeTheme.Kind.ICE?65:kind==SwipeTheme.Kind.CANDY?160:0)*flight;
    }
    static float keepRadius(SwipeTheme.Kind kind,float height,float density,float flight) {
        return Math.min(95*density,height*.24f)*(kind==SwipeTheme.Kind.TOXIC?1-.88f*smooth(flight):kind==SwipeTheme.Kind.WATER?1-.18f*flight:1);
    }

}
