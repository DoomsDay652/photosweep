package com.dominic.photosweep;
import android.content.Context;
import android.graphics.*;
/** Exercise real surf/prop coupling, paused mode and a long frame gap. */
public final class BeachBackgroundCheck {
 public static void main(String[] args){
  BeachBackgroundRenderer beach=new BeachBackgroundRenderer(new Context());
  Canvas canvas=new Canvas(Bitmap.createBitmap(60,84,Bitmap.Config.ARGB_8888));
  float top=1,bottom=0;int pushedUp=0,pulledDown=0;
  for(int f=0;f<600;f++){
   beach.draw(canvas,1000+f*33,60,84,false);
   float front=BeachBackgroundRenderer.shoreline(f*.033f,.5f);
   top=Math.min(top,front);bottom=Math.max(bottom,front);
   for(int i=0;i<beach.y.length;i++){
    if(!Float.isFinite(beach.x[i]+beach.y[i]+beach.vy[i]))throw new AssertionError("nonfinite beach prop");
    if(beach.x[i]<.04f||beach.x[i]>.96f)throw new AssertionError("prop escapes side");
    if(beach.vy[i]<-.035f)pushedUp++;
    if(beach.y[i]>.65f&&beach.vy[i]>.05f)pulledDown++;
   }
  }
  if(top<.5f||bottom>.92f||bottom-top<.24f)throw new AssertionError("surf must advance and recede in lower half");
  if(pushedUp<60||pulledDown<60)throw new AssertionError("surf fails to transport props both ways");
  float x=beach.x[0],y=beach.y[0];
  beach.draw(canvas,100000,60,84,true);beach.draw(canvas,200000,60,84,true);
  if(x!=beach.x[0]||y!=beach.y[0])throw new AssertionError("reduced motion moves props");
  beach.draw(canvas,300000,60,84,false);
  if(Math.abs(beach.y[0]-y)>.02f)throw new AssertionError("resume teleports prop");
  System.out.println("PASS: lower-half shoreline, incoming/outgoing transport, bounded props, reduced motion and resume.");
 }
}
