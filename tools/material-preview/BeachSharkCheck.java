package com.dominic.photosweep;
import android.content.Context;import android.graphics.*;
public final class BeachSharkCheck{
 static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
 static Matrix pose(float x,float y,float angle){Matrix m=new Matrix();m.setRectToRect(new RectF(0,0,1,1),new RectF(x,y,x+310,y+310),Matrix.ScaleToFit.FILL);Matrix r=new Matrix();r.transform.rotate(Math.toRadians(angle),x+155,y+155);m.postConcat(r);return m;}
 static int missing(BeachSharkRenderer s){int n=0;for(int y=0;y<=64;y++)for(int x=0;x<=64;x++)if(s.missing(x,y))n++;return n;}
 public static void main(String[]args){
  Context context=new Context();Bitmap photo=Bitmap.createBitmap(400,400,Bitmap.Config.ARGB_8888);BeachSharkRenderer s=new BeachSharkRenderer(context);
  for(int frame=0;frame<180;frame++){s.bind(photo,pose(145,280,0));s.contact(1000+frame*16,600,840);}check(!s.hasDamage(),"resting photo bitten");int species=s.species();
  float minX=1000,maxX=-1000,minY=1000,maxY=-1000;int prior=0,lastBites=0;long lastBite=-10000;
  for(int frame=0;frame<420;frame++){
   s.bind(photo,pose(35,280,-8));s.contact(4000+frame*16,600,840);
   int gone=missing(s);check(gone>=prior,"holes healed while circling");prior=gone;
   if(s.biteCount()>lastBites){long time=4000+frame*16;check(time-lastBite>=900,"held nips happen too rapidly");lastBite=time;lastBites=s.biteCount();}
   check(!s.releasing()&&!s.clamped(),"held photo is clamped or hauled before release");
   minX=Math.min(minX,s.mouthX());maxX=Math.max(maxX,s.mouthX());minY=Math.min(minY,s.mouthY());maxY=Math.max(maxY,s.mouthY());
  }
  check(s.hasDamage()&&s.biteCount()>=3,"stationary photo does not receive repeated circling bites");check(maxX-minX>100&&maxY-minY>120,"shark never circles");
  s.bind(photo,pose(260,280,-8));s.contact(11000,600,840);
  int gone=missing(s),bites=s.biteCount();
  for(int frame=1;frame<160;frame++){s.bind(photo,pose(260,280,-8));s.contact(11000+frame*16,600,840);}
  check(missing(s)==gone&&s.biteCount()==bites,"distant photo is bitten");check(s.species()==species,"species changes during same photo");
  // Both endpoints can miss the coast: the swept photo still crosses a visible mouth.
  BeachSharkRenderer fast=new BeachSharkRenderer(context);
  fast.bind(photo,pose(145,280,0));
  for(int frame=0;frame<26;frame++){fast.bind(photo,pose(35,280,0));fast.contact(20000+frame*16,600,840);}
  float fy=fast.mouthY()-155;
  fast.bind(photo,pose(350,fy,0));fast.contact(20430,600,840);
  // Recover from the warm-up bite while both photo endpoints stay clear of the mouth.
  fast.bind(photo,pose(350,fy,0));fast.contact(21430,600,840);fy=fast.mouthY()-155;
  fast.bind(photo,pose(350,fy,0));fast.contact(21446,600,840);int beforeFast=fast.biteCount();
  fast.bind(photo,pose(-450,fy,0));fast.contact(21462,600,840);
  check(fast.biteCount()>beforeFast,"fast crossing of visible mouth is missed");
  Matrix captured=pose(35,280,147);s.bind(photo,captured);s.beginRelease(photo,captured,15000,600,840);Canvas c=new Canvas(Bitmap.createBitmap(600,840,Bitmap.Config.ARGB_8888));
  Matrix carried=new Matrix();float[] original={.1f,.2f,.9f,.8f};captured.mapPoints(original);
  s.drawRelease(c,.15f,600,840);s.carriedMatrix(carried);float[] before={.1f,.2f,.9f,.8f};carried.mapPoints(before);for(int i=0;i<4;i++)check(Math.abs(before[i]-original[i])<.01,"release pose moves before clamp");
  s.drawRelease(c,.32f,600,840);check(s.clamped(),"release never clamps");int clampedCount=s.biteCount();
  for(int frame=38;frame<=100;frame++){
   s.drawRelease(c,frame/100f,600,840);s.carriedMatrix(carried);float[] grip={s.gripU(),s.gripV()};carried.mapPoints(grip);
   check(Math.hypot(grip[0]-s.mouthX(),grip[1]-s.mouthY())<.15,"photo slips from mouth during turn/haul");
   check(s.biteCount()==clampedCount,"final bite repeats every frame");
  }
  float[] end={0,0,1,0,0,1,1,1};carried.mapPoints(end);for(int i=0;i<8;i+=2)check(end[i]<0,"remaining paper not fully dragged offscreen");
  check(missing(s)>=gone,"release heals old holes");s.clear();check(!s.hasDamage()&&!s.releasing(),"cancel leaks shark release");
  s.bind(Bitmap.createBitmap(400,400,Bitmap.Config.ARGB_8888),pose(145,280,0));check(s.species()!=species&&!s.hasDamage(),"next photo reset/species failed");
  SwipeVfxView view=new SwipeVfxView(context,k->null,"",SwipeTheme.Kind.SUMMER,true,70,100,false);
  view.hold(-500,400);
  for(int frame=0;frame<12;frame++){android.os.SystemClock.now=30000+frame*100;view.setPhoto(photo,new RectF(145,280,455,590),0,0);view.onDraw(c);}
  check(!view.hasRetainedDamage(),"nearby finger bites distant card in View");
  for(int frame=0;frame<35;frame++){android.os.SystemClock.now=32000+frame*100;view.setPhoto(photo,new RectF(145,280,455,590),-110,0);view.onDraw(c);}
  check(view.hasRetainedDamage(),"View does not dispatch held bites");
  view.retreat();android.os.SystemClock.now=36000;view.setPhoto(photo,new RectF(145,280,455,590),0,0);view.onDraw(c);check(view.hasRetainedDamage(),"retreat heals View photo");
  view.hold(1000,400);check(view.hasRetainedDamage(),"re-grab resets bites");
  view.setPhoto(Bitmap.createBitmap(400,400,Bitmap.Config.ARGB_8888),new RectF(145,280,455,590),0,0);check(!view.hasRetainedDamage(),"new View photo inherits bites");view.cancel();
  System.out.println("PASS: resting safety; repeated live-stock bites while stationary; orbit movement; retained damage; distant-photo safety; captured rotation; one final bite; locked grip throughout turn/haul; complete offscreen exit; reset and species cycling.");
 }
}
