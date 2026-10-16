package com.dominic.photosweep;
import android.graphics.*;import android.content.Context;import java.awt.image.BufferedImage;import javax.imageio.ImageIO;import java.io.File;
/** Pixel-level regression coverage for generated material art and moving ice optics. */
public class MaterialArtCheck {
 static void check(boolean b,String why){if(!b)throw new AssertionError(why);}
 static Bitmap render(PhotoMaterialRenderer r,Bitmap photo,SwipeTheme.Kind kind,float time) {
  Bitmap result=Bitmap.createBitmap(720,900,Bitmap.Config.ARGB_8888);
  Canvas canvas=new Canvas(result);r.draw(canvas,photo,kind,time,1,720,900,true,1,0,1);canvas.graphics.dispose();return result;
 }
 public static void main(String[] args)throws Exception {
  PhotoMaterialArt art=PhotoMaterialArt.load(new Context());check(art==PhotoMaterialArt.load(new Context()),"art decoded per card");
  check(art.finishes.length==6 && art.debris.length==6 && art.candy.length==4,"incomplete materials");
  for(Bitmap[] group:new Bitmap[][]{art.finishes,art.debris,art.candy})for(Bitmap b:group){
   check(b.getWidth()>=250 && b.getHeight()>=300,"low resolution tile");check((b.image.getRGB(0,0)>>>24)==0,"opaque rectangular atlas background");
   int pixels=0;for(int y=0;y<b.getHeight();y+=8)for(int x=0;x<b.getWidth();x+=8)if((b.image.getRGB(x,y)>>>24)>80)pixels++;
   check(pixels>70,"empty material tile");
  }
  BufferedImage fixture=ImageIO.read(new File("app/src/main/res/drawable/tutorial_samples.png"));Bitmap photo=new Bitmap(fixture.getSubimage(0,0,fixture.getWidth()/2,fixture.getHeight()/2));
  PhotoMaterialRenderer r=new PhotoMaterialRenderer();r.setMaterialArt(art);r.motion.pose(new float[]{400,0,150,0,400,220});
  Bitmap a=render(r,photo,SwipeTheme.Kind.ICE,1),b=render(r,photo,SwipeTheme.Kind.ICE,1.32f);int changed=0;
  for(int y=330;y<570;y++)for(int x=490;x<710;x++)if(a.image.getRGB(x,y)!=b.image.getRGB(x,y))changed++;
  check(changed>150,"ice has no animated shimmer");
  for(SwipeTheme.Kind kind:new SwipeTheme.Kind[]{SwipeTheme.Kind.WATER,SwipeTheme.Kind.ICE,SwipeTheme.Kind.TOXIC,SwipeTheme.Kind.CANDY}) {
   r.reset();r.motion.pose(new float[]{400,0,150,0,400,220});
   r.motion.removePatch(0,0,PhotoMaterialMotion.X,PhotoMaterialMotion.Y);
   Bitmap removed=render(r,photo,kind,1);int opaque=0;
   for(int y=0;y<900;y++)for(int x=0;x<720;x++)if((removed.image.getRGB(x,y)>>>24)>0)opaque++;
   check(opaque==0,"finishing art heals missing material: "+kind);
  }
  r.reset();r.motion.pose(new float[]{400,0,150,0,400,220});
  PhotoMaterialRenderer snapshot=r.snapshot();Bitmap live=render(r,photo,SwipeTheme.Kind.ICE,1),tail=render(snapshot,photo,SwipeTheme.Kind.ICE,1);
  check(live.image.getRGB(610,430)==tail.image.getRGB(610,430),"Keep tail lost material art");
  System.out.println("PASS: 16 transparent material tiles, four candy forms, shared decode, animated ice shimmer, removed-material optics and art snapshots.");
 }
}
