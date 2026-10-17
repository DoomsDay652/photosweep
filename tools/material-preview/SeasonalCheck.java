package com.dominic.photosweep;
import android.content.Context;import android.graphics.*;import android.os.SystemClock;import java.lang.reflect.Field;
public class SeasonalCheck {
 static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
 static Object field(Object o,String name)throws Exception{Field f=o.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(o);}
 public static void main(String[]args)throws Exception{
  Context context=new Context();Bitmap photo=Bitmap.createBitmap(400,400,Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(Bitmap.createBitmap(600,840,Bitmap.Config.ARGB_8888));Matrix pose=new Matrix();pose.setRectToRect(new RectF(0,0,1,1),new RectF(75,245,405,575),Matrix.ScaleToFit.FILL);
  for(SwipeTheme.Kind kind:new SwipeTheme.Kind[]{SwipeTheme.Kind.HALLOWEEN,SwipeTheme.Kind.SUMMER}){
   SwipeVfxView fx=new SwipeVfxView(context,k->null,"",kind,true,70,100,false);SystemClock.now=1000;fx.hold(230,400);fx.setPhoto(photo,new RectF(135,245,465,575),-60,0);
   fx.onDraw(canvas);check(!(Boolean)field(fx,"damaged"),"seasonal hold uses saw damage");SeasonalRenderer renderer=(SeasonalRenderer)field(fx,"seasonalRenderer");check(!renderer.active(),"held card starts a release");check(fx.hasStagedRelease()&&fx.hasIndependentKeep(),"seasonal swipe uses legacy effects");
   fx.release(false);check(renderer.active()&&fx.releaseDuration(false)==2700,"trash release not captured");renderer.draw(canvas,false,.5f,600,840,1);
   fx.cancel();check(!renderer.active(),"cancel retains captured photo");
   fx.hold(360,400);fx.setPhoto(photo,new RectF(135,245,465,575),65,0);fx.release(true);check(fx.releaseDuration(true)==2600,"keep timing");renderer.draw(canvas,true,.5f,600,840,1);
   renderer.draw(canvas,true,1,600,840,1);fx.cancel();check(!renderer.active(),"keep release never clears");
   SwipeVfxView reduced=new SwipeVfxView(context,k->null,"",kind,true,70,100,true);check(!reduced.hasStagedRelease()&&reduced.releaseDuration(false)==80,"reduced motion bypass ignored");
  }
  System.out.println("PASS: contact-driven Beach Day ownership, hold/release dispatch, no saw damage, cancellation, Keep ownership, and reduced motion for both seasonal profiles.");
 }
}
