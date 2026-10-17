package com.dominic.photosweep;
import android.content.Context;import android.graphics.*;import android.os.SystemClock;
import java.awt.image.BufferedImage;import java.io.File;import javax.imageio.ImageIO;
/** Portrait preview of the production Keep surf, including a previously bitten print. */
public final class BeachKeepPreview {
 public static void main(String[]args)throws Exception{
  File folder=new File(args[0]);folder.mkdirs();Context context=new Context();
  BufferedImage fixture=ImageIO.read(new File("app/src/main/res/drawable/tutorial_samples.png"));
  Bitmap photo=new Bitmap(fixture.getSubimage(0,0,fixture.getWidth()/2,fixture.getHeight()/2));
  Bitmap bg=new Bitmap(ImageIO.read(new File("app/src/main/res/drawable-nodpi/theme_summer_coast_v1.png")));
  SeasonalRenderer[] surf={new SeasonalRenderer(context,SwipeTheme.Kind.SUMMER),new SeasonalRenderer(context,SwipeTheme.Kind.SUMMER)};
  for(int n=0;n<2;n++)surf[n].beach.bind(photo,BeachSharkCheck.pose(145,280,0));
  for(int i=0;i<350;i++){surf[1].beach.bind(photo,BeachSharkCheck.pose(35,280,0));surf[1].beach.contact(1000+i*16,600,840);}
  for(int frame=0;frame<222;frame++){
   int which=frame<111?0:1,local=frame%111;float t=local/30f;
   Bitmap scene=Bitmap.createBitmap(600,840,Bitmap.Config.ARGB_8888);Canvas c=new Canvas(scene);Paint paint=new Paint(3);c.drawBitmap(bg,null,new RectF(0,0,600,900),paint);
   float dx=90*SlasherMotion.ease(t/.65f);Matrix pose=BeachSharkCheck.pose(145+dx,280,which==0?-12:24);
   SystemClock.now=10000+local*1000/30;
   if(local<20){surf[which].beach.bind(photo,pose);surf[which].beach.paper(c,pose);surf[which].drawSwipeCue(c,pose,600,840);}
   else {if(local==20)surf[which].begin(photo,pose);surf[which].draw(c,true,Math.min(1,(t-20/30f)/2.6f),600,840,1);}
   BufferedImage sheet=new BufferedImage(600,930,BufferedImage.TYPE_INT_RGB);java.awt.Graphics2D out=sheet.createGraphics();
   out.setColor(new java.awt.Color(8,29,40));out.fillRect(0,0,600,930);out.drawImage(scene.image,0,64,null);
   out.setColor(java.awt.Color.WHITE);out.setFont(new java.awt.Font("SansSerif",java.awt.Font.BOLD,21));out.drawString("BEACH DAY · KEEP",18,26);
   out.setFont(new java.awt.Font("SansSerif",0,16));out.drawString(local<20?"Drag right · surf gathers at the edge":t<1.55f?"Wave rolls around and lifts the photo":"Backwash carries the photo safely away",18,51);
   out.setFont(new java.awt.Font("SansSerif",0,14));out.drawString(which==0?"Production renderer · host preview · no emulator":"Second pass · previous shark bite damage stays",18,922);
   out.dispose();c.graphics.dispose();ImageIO.write(sheet,"png",new File(folder,String.format("frame-%03d.png",frame)));
  }
  System.out.println("Rendered 222 portrait Keep surf frames: angled clean and bitten prints.");
 }
}
