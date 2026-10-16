package com.dominic.photosweep;
import android.content.Context;import android.graphics.*;import android.os.SystemClock;
public class ToxicMeltCheck {
 static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
 static class Fx extends SwipeVfxView {float shake;Fx(){super(new Context(),k->null,"",SwipeTheme.Kind.TOXIC,true,75,100,false);}@Override protected void onGrinderShake(float offset){shake=offset;}}
 public static void main(String[]args)throws Exception{
  Bitmap photo=Bitmap.createBitmap(300,300,Bitmap.Config.ARGB_8888);Paint p=new Paint(1);p.setColor(0xFFECD489);new Canvas(photo).drawRect(0,0,300,300,p);
  PhotoMaterialRenderer renderer=new PhotoMaterialRenderer();renderer.motion.pose(new float[]{360,0,-20,0,400,220});renderer.setToxicPool(.12f,1,900);
  float[] edges=new float[1025];java.util.Arrays.fill(edges,38);float[] sweep={360,0,-20,0,400,220,360,0,-20,0,400,220};renderer.motion.contact(sweep,1,edges,1100,0,2);
  Bitmap scene=Bitmap.createBitmap(720,1100,Bitmap.Config.ARGB_8888);renderer.draw(new Canvas(scene),photo,SwipeTheme.Kind.TOXIC,.08f,1,720,1100,false,0,0,1);renderer.draw(new Canvas(scene),photo,SwipeTheme.Kind.TOXIC,.65f,1,720,1100,false,0,0,1);
  int droop=0;for(int n=0;n<PhotoMaterialMotion.N;n++){if(renderer.motion.age(n,.65f)>=0 && renderer.motion.remaining[n]>.1f && renderer.motion.vertices[n*2+1]>renderer.motion.base[n*2+1]+5)droop++;if(renderer.motion.touched[n]<0)check(renderer.motion.vertices[n*2+1]==renderer.motion.base[n*2+1],"untouched stock drooped");}check(droop>0,"touched edge does not sag");
  scene=Bitmap.createBitmap(720,1100,Bitmap.Config.ARGB_8888);renderer.draw(new Canvas(scene),photo,SwipeTheme.Kind.TOXIC,1.6f,1,720,1100,false,0,0,1);int streamPixels=0;for(int y=720;y<900;y++)for(int x=0;x<75;x++)if((scene.image.getRGB(x,y)>>>24)>5)streamPixels++;check(streamPixels>40,"photo-textured strands do not reach pool");
  PhotoMaterialRenderer untouched=new PhotoMaterialRenderer();untouched.motion.pose(new float[]{360,0,150,0,400,220});scene=Bitmap.createBitmap(720,1100,Bitmap.Config.ARGB_8888);untouched.draw(new Canvas(scene),photo,SwipeTheme.Kind.TOXIC,1.6f,1,720,1100,false,0,0,1);for(int y=700;y<900;y+=10)for(int x=0;x<75;x+=5)check((scene.image.getRGB(x,y)>>>24)==0,"uncontacted photo emits strands");
  Fx fx=new Fx();SystemClock.now=1000;fx.hold(350,400);fx.setPhoto(photo,new RectF(180,220,540,620),0,0);for(int frame=0;frame<30;frame++){SystemClock.now=1050+frame*25;fx.setPhoto(photo,new RectF(180,220,540,620),-200,0);fx.onDraw(new Canvas(Bitmap.createBitmap(720,1100,Bitmap.Config.ARGB_8888)));check(fx.shake==0,"Toxic retains saw shake");}fx.cancel();
  // An outgoing thread keeps draining from its original contact; approaching again cannot reattach/replay it.
  PhotoMaterialRenderer returned=new PhotoMaterialRenderer(),away=new PhotoMaterialRenderer();
  for(PhotoMaterialRenderer r:new PhotoMaterialRenderer[]{returned,away}){
   r.motion.pose(new float[]{360,0,-20,0,400,220});r.setToxicPool(.12f,1,900);r.motion.contact(sweep,1,edges,1100,0,2);
   r.draw(new Canvas(Bitmap.createBitmap(720,1100,Bitmap.Config.ARGB_8888)),photo,SwipeTheme.Kind.TOXIC,.08f,1,720,1100,false,0,0,1);
   r.draw(new Canvas(Bitmap.createBitmap(720,1100,Bitmap.Config.ARGB_8888)),photo,SwipeTheme.Kind.TOXIC,.5f,1,720,1100,false,0,0,1);
   r.motion.pose(new float[]{360,0,220,0,400,220});r.draw(new Canvas(Bitmap.createBitmap(720,1100,Bitmap.Config.ARGB_8888)),photo,SwipeTheme.Kind.TOXIC,.6f,1,720,1100,false,0,0,1);
  }
  returned.motion.pose(new float[]{360,0,-20,0,400,220});
  Bitmap nearImage=Bitmap.createBitmap(720,1100,Bitmap.Config.ARGB_8888),awayImage=Bitmap.createBitmap(720,1100,Bitmap.Config.ARGB_8888);
  returned.draw(new Canvas(nearImage),photo,SwipeTheme.Kind.TOXIC,.8f,1,720,1100,false,0,0,1);away.draw(new Canvas(awayImage),photo,SwipeTheme.Kind.TOXIC,.8f,1,720,1100,false,0,0,1);
  for(int y=720;y<900;y++)for(int x=0;x<100;x++)check(nearImage.image.getRGB(x,y)==awayImage.image.getRGB(x,y),"returning close reattaches or replays an old strand");
  nearImage=Bitmap.createBitmap(720,1100,Bitmap.Config.ARGB_8888);returned.draw(new Canvas(nearImage),photo,SwipeTheme.Kind.TOXIC,3f,1,720,1100,false,0,0,1);
  for(int y=720;y<900;y+=3)for(int x=0;x<100;x++)check((nearImage.image.getRGB(x,y)>>>24)==0,"expired contact spawns a second stream");
  returned.reset();returned.motion.pose(new float[]{360,0,-20,0,400,220});returned.motion.contact(sweep,1,edges,1100,4,2);
  returned.draw(new Canvas(Bitmap.createBitmap(720,1100,Bitmap.Config.ARGB_8888)),photo,SwipeTheme.Kind.TOXIC,4.08f,1,720,1100,false,0,0,1);
  nearImage=Bitmap.createBitmap(720,1100,Bitmap.Config.ARGB_8888);returned.draw(new Canvas(nearImage),photo,SwipeTheme.Kind.TOXIC,5.6f,1,720,1100,false,0,0,1);
  int fresh=0;for(int y=720;y<900;y++)for(int x=0;x<100;x++)if((nearImage.image.getRGB(x,y)>>>24)>5)fresh++;check(fresh>40,"new photograph cannot emit fresh strands after reset");
  // The liquid visibly flows while its contact silhouette stays fixed and ends at the pool.
  Fx curtain=new Fx();curtain.setToxicPool(.12f,1,900);
  java.lang.reflect.Method draw=SwipeVfxView.class.getDeclaredMethod("drawElementTrash",Canvas.class,float.class,float.class);draw.setAccessible(true);
  java.lang.reflect.Field edgesField=SwipeVfxView.class.getDeclaredField("toothRows");edgesField.setAccessible(true);
  Bitmap flowA=Bitmap.createBitmap(720,1100,Bitmap.Config.ARGB_8888),flowB=Bitmap.createBitmap(720,1100,Bitmap.Config.ARGB_8888);
  draw.invoke(curtain,new Canvas(flowA),0f,1f);float[] fixed=((float[])edgesField.get(curtain)).clone();
  draw.invoke(curtain,new Canvas(flowB),1f,1f);check(java.util.Arrays.equals(fixed,(float[])edgesField.get(curtain)),"flow moves the melting boundary");
  int moving=0,filled=0;for(int y=80;y<780;y+=3)for(int x=1;x<35;x++){int a=flowA.image.getRGB(x,y),b=flowB.image.getRGB(x,y);if((a>>>24)>120)filled++;if(a!=b)moving++;}
  check(filled>500 && moving>500,"sludge curtain is invisible or static");
  for(int x=0;x<70;x++)for(int y=(int)(900-.12f*x)+12;y<1050;y+=5)check((flowB.image.getRGB(x,y)>>>24)==0,"sludge extends below pool merge");
  curtain.cancel();
  System.out.println("PASS: contact-only droop, photo-textured strands reaching a tilted pool, no untouched emitters, zero Toxic saw shake, flowing curtain, fixed contact silhouette, pool merge, no replay on reapproach, and clean next-photo emitters.");
 }
}
