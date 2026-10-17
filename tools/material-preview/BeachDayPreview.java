package com.dominic.photosweep;
import android.content.Context;import android.graphics.*;import android.os.SystemClock;
import java.awt.image.BufferedImage;import java.io.File;import javax.imageio.ImageIO;
/** One seasonal controller, followed through held Trash, release, next photo and Keep. */
public final class BeachDayPreview {
 public static void main(String[]args)throws Exception{
  File folder=new File(args[0]);folder.mkdirs();Context context=new Context();
  java.lang.reflect.Field seed=BeachSharkRenderer.class.getDeclaredField("nextSpecies");seed.setAccessible(true);seed.setInt(null,0);
  BufferedImage fixture=ImageIO.read(new File("app/src/main/res/drawable/tutorial_samples.png"));
  Bitmap first=new Bitmap(fixture.getSubimage(0,0,fixture.getWidth()/2,fixture.getHeight()/2));
  Bitmap second=new Bitmap(fixture.getSubimage(0,0,fixture.getWidth()/2,fixture.getHeight()/2));
  BeachBackgroundRenderer background=new BeachBackgroundRenderer(context);
  SeasonalRenderer effect=new SeasonalRenderer(context,SwipeTheme.Kind.SUMMER);
  effect.beach.bind(first,BeachSharkCheck.pose(145,280,0));
  for(int frame=0;frame<360;frame++){
   float t=frame/30f;SystemClock.now=1000+frame*1000/30;
   Bitmap image=Bitmap.createBitmap(600,840,Bitmap.Config.ARGB_8888);Canvas c=new Canvas(image);Paint paint=new Paint(3);background.draw(c,SystemClock.now,600,840,false);
   String title="BEACH DAY · TRASH",caption;
   if(frame<174){
    Matrix pose=BeachSharkCheck.pose(145-105*SlasherMotion.ease(t/.65f),280,-8);
    effect.beach.bind(first,pose);effect.beach.contact(SystemClock.now,600,840);effect.beach.paper(c,pose);effect.beach.draw(c,SystemClock.now,600,840,1);
    caption=t<.65f?"Drag left":"Still holding · circles and occasional nips ("+effect.beach.biteCount()+")";
   }else if(frame<255){
    if(frame==174)effect.begin(first,BeachSharkCheck.pose(40,280,-8));
    effect.draw(c,false,(t-5.8f)/2.7f,600,840,1);caption="Released left · final bite, clamp and drag away";
   }else{
    title="BEACH DAY · KEEP";float keepTime=t-8.5f;
    if(frame==255){effect.clear();effect.beach.bind(second,BeachSharkCheck.pose(145,280,0));}
    Matrix pose=BeachSharkCheck.pose(145+90*SlasherMotion.ease(keepTime/.6f),280,-12);
    if(frame<273){effect.beach.bind(second,pose);effect.beach.paper(c,pose);effect.drawSwipeCue(c,pose,600,840);caption="Next photo · drag right";}
    else {if(frame==273)effect.begin(second,pose);effect.draw(c,true,(t-9.1f)/2.6f,600,840,1);caption="Released right · full-width surf carries it away";}
   }
   BufferedImage sheet=new BufferedImage(600,930,BufferedImage.TYPE_INT_RGB);java.awt.Graphics2D out=sheet.createGraphics();out.setColor(new java.awt.Color(8,29,40));out.fillRect(0,0,600,930);out.drawImage(image.image,0,64,null);
   out.setColor(java.awt.Color.WHITE);out.setFont(new java.awt.Font("SansSerif",java.awt.Font.BOLD,21));out.drawString(title,18,26);out.setFont(new java.awt.Font("SansSerif",0,15));out.drawString(caption,18,51);
   out.setFont(new java.awt.Font("SansSerif",0,14));out.drawString("Anatomy + living shoreline draft · production renderer · host preview",18,922);out.dispose();c.graphics.dispose();ImageIO.write(sheet,"png",new File(folder,String.format("frame-%03d.png",frame)));
  }
  System.out.println("Rendered 360 combined Trash / Keep frames through the same controller.");
 }
}
