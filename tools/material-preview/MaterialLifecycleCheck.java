package com.dominic.photosweep;
import android.content.Context;import android.graphics.*;import android.os.SystemClock;import android.view.View;import java.lang.reflect.*;import java.util.List;
/** Runs the actual SwipeVfxView lifecycle with host-only Android UI adapters. */
public class MaterialLifecycleCheck {
 static Object field(Object target,String name)throws Exception{Field f=target.getClass()==SwipeVfxView.class?SwipeVfxView.class.getDeclaredField(name):target.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(target);}
 static void check(boolean value,String why){if(!value)throw new AssertionError(why);}
 static int holes(PhotoMaterialMotion motion){int n=0;for(float a:motion.remaining)if(a<.01f)n++;return n;}
 public static void main(String[]args)throws Exception {
  Context context=new Context();Bitmap photo=Bitmap.createBitmap(400,400,Bitmap.Config.ARGB_8888);RectF bounds=new RectF(130,330,570,770);Canvas canvas=new Canvas(Bitmap.createBitmap(720,1100,Bitmap.Config.ARGB_8888));
  for(SwipeTheme.Kind kind:PhotoMaterialPolishCheck.KINDS){
   SwipeVfxView fx=new SwipeVfxView(context,k->null,"",kind,true,70,100,false);
   SystemClock.now=1000;fx.hold(350,500);fx.setPhoto(photo,bounds,0,0);
   SystemClock.now=1033;fx.setPhoto(photo,bounds,-220,0);fx.progress=.8f;fx.onDraw(canvas);
   PhotoMaterialRenderer renderer=(PhotoMaterialRenderer)field(fx,"materials");check(renderer.motion.hasDamage(),"real contact missed with opposite finger intent: "+kind);
   SystemClock.now=2400;fx.setPhoto(photo,bounds,160,0);fx.onDraw(canvas);int lost=holes(renderer.motion);check(lost>0,"pull-away did not retain loss: "+kind);
   fx.retreat();SystemClock.now=2800;fx.setPhoto(photo,bounds,0,0);fx.onDraw(canvas);
   check(fx.hasRetainedDamage() && field(fx,"bitmap")==photo,"return animation cleared the current photo damage: "+kind);
   check(holes(renderer.motion)>=lost,"settling repaired the photograph: "+kind);
   Method begin=SwipeVfxView.class.getDeclaredMethod("beginPhoto",Canvas.class,View.class);begin.setAccessible(true);int clip=(Integer)begin.invoke(fx,canvas,new View(context));check(canvas.graphics.getClip().getBounds2D().isEmpty(),"intact card drawn over retained damage: "+kind);canvas.restoreToCount(clip);
   fx.hold(350,500);fx.setPhoto(photo,bounds,0,0);check(holes(renderer.motion)>=lost,"re-grab healed damage: "+kind);
   fx.release(true);List<?> flights=(List<?>)field(fx,"keptPhotos");check(flights.size()==1,"Keep snapshot missing");PhotoMaterialRenderer old=(PhotoMaterialRenderer)field(flights.get(0),"renderer");
   fx.cancel();fx.hold(350,500);fx.setPhoto(photo,bounds,0,0);check(!renderer.motion.hasDamage(),"next photo inherited old damage");check(holes(old.motion)>=lost,"next card repaired old Keep snapshot");
   for(int i=0;i<20;i++){SystemClock.now=3000+i*80;fx.cancel();fx.hold(350,500);fx.setPhoto(photo,bounds,120,0);fx.release(true);check(flights.size()<=8,"unbounded rapid-Keep retention");}
   SystemClock.now=9000;fx.cancel();fx.onDraw(canvas);check(flights.isEmpty(),"expired Keep photos retained");fx.onDetachedFromWindow();
   for(float angle:new float[]{-150,37,179}) {
    SwipeVfxView swing=new SwipeVfxView(context,k->null,"",kind,true,70,100,false);View card=new View(context);
    SystemClock.now=1000;swing.hold(350,500);swing.setTransformedPhoto(photo,bounds,card);
    card.tx=-240;card.rotation=angle;SystemClock.now=1033;swing.progress=.9f;swing.setTransformedPhoto(photo,bounds,card);
    check(swing.hasRetainedDamage(),"fast rotating photo missed contact: "+kind+" / "+angle);swing.onDetachedFromWindow();
   }
   SwipeVfxView reduced=new SwipeVfxView(context,k->null,"",kind,true,70,100,true);check(!(Boolean)field(reduced,"elemental"),"reduced motion ignored");
  }
  System.out.println("PASS: actual View contact, reversal, settled display clipping, re-grab, next-card isolation, damaged Keep snapshots, 120 rapid Keeps, expiry and reduced motion.");
 }
}
