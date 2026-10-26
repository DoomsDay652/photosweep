package com.dominic.photosweep;
import android.content.Context;
import android.graphics.*;
import java.lang.reflect.*;
/** Geometry checks against the production mesh, including moving turns. */
public final class BeachSwimCheck {
 static void check(boolean b,String message){if(!b)throw new AssertionError(message);}
 public static void main(String[]args)throws Exception{
  BeachSharkRenderer shark=new BeachSharkRenderer(new Context());
  Bitmap photo=Bitmap.createBitmap(8,8,Bitmap.Config.ARGB_8888);shark.bind(photo,BeachSharkCheck.pose(145,280,0));
  Method draw=BeachSharkRenderer.class.getDeclaredMethod("swimmer",Canvas.class,float.class,float.class,float.class,long.class,float.class,float.class);draw.setAccessible(true);
  Field field=BeachSharkRenderer.class.getDeclaredField("swimMesh");field.setAccessible(true);
  Canvas canvas=new Canvas(Bitmap.createBitmap(600,840,Bitmap.Config.ARGB_8888));
  float spine=new float[]{.506f,.455f,.400f}[shark.species()];
  float low=Float.MAX_VALUE,high=-Float.MAX_VALUE;
  for(int frame=0;frame<100;frame++){
   float heading=frame<50?0:(frame-50)*.035f;
   draw.invoke(shark,canvas,400f,400f,heading,1000L+frame*25,600f,1f);
   float[] mesh=(float[])field.get(shark);float[] center=new float[82];
   int row=(int)(spine*12);float blend=spine*12-row;
   for(int col=0;col<=40;col++)for(int axis=0;axis<2;axis++){
    int i=(row*41+col)*2+axis;center[col*2+axis]=mesh[i]+(mesh[i+82]-mesh[i])*blend;
   }
   float arc=0;
   for(int col=1;col<=40;col++)arc+=Math.hypot(center[col*2]-center[(col-1)*2],center[col*2+1]-center[(col-1)*2+1]);
   check(Math.abs(arc-384)<1.5,"body stretches or collapses during swimming");
   float headX=center[76]+(center[78]-center[76])*.6f,headY=center[77]+(center[79]-center[77])*.6f;
   check(Math.hypot(headX-400,headY-400)<.12,"snout drifts off its contact anchor");
   for(int col=2;col<38;col++){
    int i=(row*41+col)*2;float dx=center[(col+1)*2]-center[(col-1)*2],dy=center[(col+1)*2+1]-center[(col-1)*2+1];
    float nx=mesh[i+82]-mesh[i],ny=mesh[i+83]-mesh[i+1];
    check(dx*ny-dy*nx>0,"skin section folds inside out");
   }
   if(frame<50){low=Math.min(low,center[1]);high=Math.max(high,center[1]);}
  }
  check(high-low>40,"tail has insufficient propulsion movement");
  // A smooth mesh alone cannot hide a shark sliding sideways. Check the
  // production controller's facing against its actual path while pursuing stock.
  shark.clear();shark.bind(photo,BeachSharkCheck.pose(145,280,0));
  float lastX=0,lastY=0,alignment=0;int samples=0;
  for(int frame=0;frame<260;frame++){
   shark.bind(photo,BeachSharkCheck.pose(35,280,-8));shark.contact(10000+frame*16,600,840);
   float a=shark.heading(),x=shark.mouthX()+21*(float)Math.cos(a),y=shark.mouthY()+21*(float)Math.sin(a);
   float dx=x-lastX,dy=y-lastY,distance=(float)Math.hypot(dx,dy);
   if(frame>20&&distance>.2f){alignment+=(dx*(float)Math.cos(a)+dy*(float)Math.sin(a))/distance;samples++;}
   lastX=x;lastY=y;
  }
  check(samples>150&&alignment/samples>.85f,"head slides sideways instead of tracking swim direction");
  Field routes=BeachSharkRenderer.class.getDeclaredField("routes");routes.setAccessible(true);
  Field recovery=BeachSharkRenderer.class.getDeclaredField("lastRecovery");recovery.setAccessible(true);
  for(int seed=0;seed<16;seed++){
   BeachSharkRenderer run=new BeachSharkRenderer(new Context());((java.util.Random)routes.get(run)).setSeed(seed);
   run.bind(photo,BeachSharkCheck.pose(145,280,0));int last=-1,changes=0;float px=0,py=0;
   for(int frame=0;frame<420;frame++){
    run.bind(photo,BeachSharkCheck.pose(35,280,-8));run.contact(20000+frame*16,600,840);
    int route=recovery.getInt(run);if(route>=0&&route!=last){changes++;last=route;}
    float x=run.mouthX(),y=run.mouthY();
    check(Float.isFinite(x+y+run.heading()),"invalid steering");
    if(frame>0)check(Math.hypot(x-px,y-py)<9,"visible swim teleports");
    px=x;py=y;
   }
   check(changes>=3&&run.biteCount()>=3,"seeded swim repeats one route or misses contact bites");
  }
  System.out.println("PASS: constant body length, snout/contact registration, no inverted spine sections, strong traveling tail stroke through turns and 16 varied seeded swim routes.");
 }
}
