package com.dominic.photosweep;
import android.content.Context;import android.graphics.*;import java.lang.reflect.Field;
public final class BeachKeepCheck {
 static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
 public static void main(String[]args)throws Exception{
  Context context=new Context();Bitmap photo=Bitmap.createBitmap(400,400,Bitmap.Config.ARGB_8888);
  Canvas canvas=new Canvas(Bitmap.createBitmap(1,1,Bitmap.Config.ARGB_8888));
  Field field=SeasonalRenderer.class.getDeclaredField("mesh");field.setAccessible(true);
  for(float angle:new float[]{-12,24,90,147}){
   SeasonalRenderer surf=new SeasonalRenderer(context,SwipeTheme.Kind.SUMMER);
   surf.beach.bind(photo,BeachSharkCheck.pose(145,280,0));
   for(int i=0;i<350;i++){surf.beach.bind(photo,BeachSharkCheck.pose(35,280,0));surf.beach.contact(1000+i*16,600,840);}
   check(surf.beach.hasDamage(),"damaged Keep fixture has no bites");int bites=surf.beach.biteCount();
   Matrix pose=BeachSharkCheck.pose(235,280,angle);surf.begin(photo,pose);
   surf.draw(canvas,true,0,600,840,1);float[] mesh=(float[])field.get(surf);
   float[] corners={0,0,1,0,0,1,1,1};pose.mapPoints(corners);
   int[] indices={0,32,36*33,37*33-1};
   for(int k=0;k<4;k++)check(Math.hypot(mesh[indices[k]*2]-corners[k*2],mesh[indices[k]*2+1]-corners[k*2+1])<.01,"Keep release jumps from captured rotation");
   for(float p:new float[]{.12f,.24f,.34f,.48f,.65f,.80f,.97f}){
    surf.draw(canvas,true,p,600,840,1);mesh=(float[])field.get(surf);
    for(float value:mesh)check(Float.isFinite(value),"invalid water-driven photo mesh");
    for(int row=0;row<36;row++)for(int col=0;col<32;col++){
     int a=(row*33+col)*2,b=a+2,c=a+66;
     float cross=(mesh[b]-mesh[a])*(mesh[c+1]-mesh[a+1])-(mesh[b+1]-mesh[a+1])*(mesh[c]-mesh[a]);
     check(cross>0,"surf folds print inside out");
    }
    for(int row=0;row<=36;row++)for(int col=0;col<=32;col++)if(surf.beach.missing(col*2,Math.round(row*64f/36)))check((surf.colors[row*33+col]>>>24)==0,"Keep heals shark damage");
    if(p>.96f)for(int i=0;i<mesh.length;i+=2)check(mesh[i]>600,"surf leaves part of print onscreen");
   }
   check(surf.beach.biteCount()==bites,"Keep creates new shark bites");surf.clear();check(!surf.active(),"cancel retains surf ownership");
  }
  System.out.println("PASS: captured tilted/sideways/inverted poses, finite non-inverted surf meshes, preserved bite holes, no new damage, full right exit and cancellation.");
 }
}
