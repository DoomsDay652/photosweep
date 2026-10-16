package com.dominic.photosweep;
import android.content.Context;import android.graphics.*;import android.os.SystemClock;import java.lang.reflect.*;
public class SlasherCheck {
 static void check(boolean v,String message){if(!v)throw new AssertionError(message);}
 static Object field(Object value,String name)throws Exception{Field f=value.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(value);}
 public static void main(String[]args)throws Exception {
  SlasherMotion m=new SlasherMotion();
  for(int i=0;i<=26;i++){m.sample(false,i/100f,720,1100,210,500);check(m.dx==0 && m.dy==0 && m.angle==0 && m.yaw==0 && !m.punctured,"release pose moved before stab");}
  m.sample(false,.30f,720,1100,210,500);check(m.punctured && m.approach==1 && m.drag==0 && m.penetration>0,"knife must enter before dragging");
  float previous=-Float.MAX_VALUE;for(int i=36;i<=100;i++){m.sample(false,i/100f,720,1100,210,500);check(m.dx>=previous,"drag reversed");previous=m.dx;check(Float.isFinite(m.dx+m.dy+m.scale),"invalid pose");}
  check(m.dx>1000 && m.alpha==0,"stab does not finish off stage");
  m.sample(true,.16f,720,1100,550,500);check(m.doorOpen>.9f && m.scale==1,"door must open before photo enters");
  m.sample(true,.88f,720,1100,550,500);check(m.alpha==0,"door closes on the photo before it is inside");
  m.sample(true,.60f,720,1100,550,500);check(m.backFacing && m.alpha>0,"photo must reveal its reverse before disappearing");
  m.sample(true,1,720,1100,550,500);check(m.doorOpen==0,"trash door never closes");
  Context context=new Context();Bitmap photo=Bitmap.createBitmap(400,400,Bitmap.Config.ARGB_8888);Canvas fixture=new Canvas(photo);Paint paint=new Paint(1);paint.setColor(0xFFDAEBF4);fixture.drawRect(0,0,400,400,paint);
  SwipeVfxView fx=new SwipeVfxView(context,k->null,"",SwipeTheme.Kind.SLASHER,true,70,100,false);SystemClock.now=1000;fx.hold(300,500);fx.setPhoto(photo,new RectF(120,220,560,660),-190,0);
  Bitmap before=Bitmap.createBitmap(720,1100,Bitmap.Config.ARGB_8888);fx.onDraw(new Canvas(before));check(!(Boolean)field(fx,"damaged"),"saw damages Slasher photo before release");
  check(fx.hasStagedRelease() && fx.hasIndependentKeep(),"Slasher release dispatched to legacy animation");
  check(fx.releaseDuration(false)==2800 && fx.releaseDuration(true)==3000,"staged release timing changed");
  fx.release(true);SlasherRenderer renderer=(SlasherRenderer)field(fx,"slasherRenderer");check(renderer.active(),"release did not capture photo");
  Method begin=SwipeVfxView.class.getDeclaredMethod("beginPhoto",Canvas.class,android.view.View.class);begin.setAccessible(true);Canvas canvas=new Canvas(Bitmap.createBitmap(720,1100,Bitmap.Config.ARGB_8888));int save=(Integer)begin.invoke(fx,canvas,new android.view.View(context));check(canvas.graphics.getClip().getBounds2D().isEmpty(),"original photo drawn over staged release");canvas.restoreToCount(save);
  Bitmap first=Bitmap.createBitmap(720,1100,Bitmap.Config.ARGB_8888),second=Bitmap.createBitmap(720,1100,Bitmap.Config.ARGB_8888);renderer.draw(new Canvas(first),true,.1f,720,1100,1);
  fx.setPhoto(photo,new RectF(120,220,560,660),-700,300);renderer.draw(new Canvas(second),true,.1f,720,1100,1);
  check(java.util.Arrays.equals(first.image.getRGB(0,0,720,1100,null,0,720),second.image.getRGB(0,0,720,1100,null,0,720)),"release follows the generic offscreen card animation");
  renderer.draw(new Canvas(second),true,.56f,720,1100,1);int holes=0;for(int color:renderer.meshColors)if((color>>>24)==0)holes++;check(holes>0,"knife has no actual hole through the photograph");
  SlasherRenderer seeded=new SlasherRenderer(context,new java.util.Random(14));Matrix pose=new Matrix();pose.setRectToRect(new RectF(0,0,1,1),new RectF(70,270,510,710),Matrix.ScaleToFit.FILL);
  java.util.Set<String> spots=new java.util.HashSet<>();String last="";boolean entered=false;
  for(int action=0;action<20;action++){
   seeded.begin(photo,pose);String spot=seeded.hitU+":"+seeded.hitV;check(!spot.equals(last),"immediately repeated stab point");last=spot;spots.add(spot);
   for(int frame=28;frame<90;frame+=3){seeded.draw(new Canvas(second),true,frame/100f,720,1000,1);check(seeded.armRootX>720,"sleeve disconnects from right edge");check(spot.equals(seeded.hitU+":"+seeded.hitV),"stab point changes mid-action");}
  }
  check(spots.size()>=6,"stab targets have insufficient variation");
  seeded.begin(photo,pose);for(int frame=62;frame<84;frame++){seeded.draw(new Canvas(second),false,frame/100f,720,1000,1);if(seeded.insideDoor && seeded.motion.alpha>0)entered=true;}
  check(entered,"photo never clears the open door leaf");
  Field meshField=SlasherRenderer.class.getDeclaredField("mesh");meshField.setAccessible(true);
  for(float degrees:new float[]{-38,27,155,180}) {
   double radians=Math.toRadians(degrees);float c=(float)Math.cos(radians),sn=(float)Math.sin(radians);
   float[] unit={0,0,1,0,0,1},quad={300-180*c+220*sn,500-180*sn-220*c,300+180*c+220*sn,500+180*sn-220*c,300-180*c-220*sn,500-180*sn+220*c};
   pose.setPolyToPoly(unit,0,quad,0,3);seeded.begin(photo,pose,720,1000);
   for(float phase:new float[]{0,.26f,.31f,.6f}) {
    seeded.draw(new Canvas(second),true,phase,720,1000,1);float[] vertices=(float[])meshField.get(seeded);
    float vx=vertices[56]-vertices[0],vy=vertices[57]-vertices[1];
    check(vx*c+vy*sn>0 && Math.abs(vx*sn-vy*c)<.02f,"release rotation was not preserved");
   }
  }
  fx.cancel();check(!renderer.active(),"finished or cancelled release retains photo");
  SwipeVfxView reduced=new SwipeVfxView(context,k->null,"",SwipeTheme.Kind.SLASHER,true,70,100,true);check(!reduced.hasStagedRelease() && reduced.releaseDuration(false)==80,"reduced motion ignored");
  System.out.println("PASS: Slasher held pose, stationary release, blade penetration and UV hole, back-facing walk, release-only impact with locked orientation, random stable targets, connected sleeve, clear doorway, monotonic departure, door timing, frozen release pose, no saw damage, photo clipping, cancellation and reduced motion.");
 }
}
