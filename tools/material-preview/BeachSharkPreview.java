package com.dominic.photosweep;
import android.content.Context;import android.graphics.*;import java.awt.image.BufferedImage;import java.io.File;import javax.imageio.ImageIO;
/** Production overhead swimmer: held circling, retained damage, then final clamp and haul. */
public final class BeachSharkPreview {
 public static void main(String[]args)throws Exception{
  int first=args.length>1?Integer.parseInt(args[1]):0;
  if(args.length>2){java.lang.reflect.Field seed=BeachSharkRenderer.class.getDeclaredField("nextSpecies");seed.setAccessible(true);seed.setInt(null,Integer.parseInt(args[2]));}
  File folder=new File(args[0]);folder.mkdirs();Context context=new Context();BufferedImage fixture=ImageIO.read(new File("app/src/main/res/drawable/tutorial_samples.png"));
  Bitmap photo=new Bitmap(fixture.getSubimage(0,0,fixture.getWidth()/2,fixture.getHeight()/2));Bitmap bg=new Bitmap(ImageIO.read(new File("app/src/main/res/drawable-nodpi/theme_summer_coast_v1.png")));
  BeachSharkRenderer[] sharks={new BeachSharkRenderer(context),new BeachSharkRenderer(context),new BeachSharkRenderer(context)};Matrix[] poses={new Matrix(),new Matrix(),new Matrix()};
  for(int n=0;n<3;n++){poses[n].setRectToRect(new RectF(0,0,1,1),new RectF(145,280,455,590),Matrix.ScaleToFit.FILL);sharks[n].bind(photo,poses[n]);}
  for(int frame=0;frame<318;frame++){
   float t=frame/30f;BufferedImage sheet=new BufferedImage(1080,600,BufferedImage.TYPE_INT_RGB);java.awt.Graphics2D out=sheet.createGraphics();
   for(int n=0;n<3;n++){
    Bitmap image=Bitmap.createBitmap(frame>=first?600:1,frame>=first?840:1,Bitmap.Config.ARGB_8888);Canvas c=new Canvas(image);Paint paint=new Paint(3);if(frame>=first)c.drawBitmap(bg,null,new RectF(0,0,600,900),paint);
    float dx=-105*SlasherMotion.ease((t-.2f)/.65f),dy=0,angle=-8;
    if(t>=5.3f&&t<6.5f)dx=-105+215*SlasherMotion.ease((t-5.3f)/.5f);
    if(t>=6.5f)dx=110-220*SlasherMotion.ease((t-6.5f)/.5f);
    Matrix m=poses[n];m.setRectToRect(new RectF(0,0,1,1),new RectF(145+dx,280+dy,455+dx,590+dy),Matrix.ScaleToFit.FILL);
    Matrix spin=new Matrix();spin.transform.rotate(Math.toRadians(angle),300+dx,435+dy);m.postConcat(spin);
    long now=1000+frame*1000/30;
    if(t<7.7f){sharks[n].bind(photo,m);sharks[n].contact(now,600,840);if(frame>=first)sharks[n].paper(c,m);sharks[n].draw(c,now,600,840,1);}
    else{if(!sharks[n].releasing())sharks[n].beginRelease(photo,m,8700,600,840);sharks[n].drawRelease(c,Math.min(1,(t-7.7f)/2.7f),600,840);}
    out.drawImage(image.image,n*360,66,360,504,null);c.graphics.dispose();
    out.setColor(new java.awt.Color(8,29,40));out.fillRect(n*360,0,360,66);out.setColor(java.awt.Color.WHITE);out.setFont(new java.awt.Font("SansSerif",java.awt.Font.BOLD,20));out.drawString(BeachSharkRenderer.SPECIES[sharks[n].species()],n*360+14,26);
    out.setFont(new java.awt.Font("SansSerif",0,14));out.drawString(t<.85f?"Bring the photo left":t<5.3f?"Hold → shark circles and bites":t<6.5f?"Pull away → damage stays":t<7.7f?"Return → bites touch fresh stock":t<8.7f?"Release → final bite and clamp":"Shark drags the remaining photo away",n*360+14,50);
   }
   out.setColor(new java.awt.Color(8,29,40));out.fillRect(0,570,1080,30);out.setColor(java.awt.Color.WHITE);out.setFont(new java.awt.Font("SansSerif",0,14));out.drawString("BEACH DAY DRAFT • Full top-down • Production renderer, host preview • Not a device recording",16,591);out.dispose();if(frame>=first)ImageIO.write(sheet,"png",new File(folder,String.format("frame-%03d.png",frame)));
  }
  System.out.println("Rendered "+(318-first)+" overhead circling/contact/clamp/drag frames for all species.");
 }
}
