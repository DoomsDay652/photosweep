package com.dominic.photosweep;
import android.content.Context;import android.graphics.*;import java.awt.image.BufferedImage;import java.io.File;import javax.imageio.ImageIO;
public class SlasherPreview {
 public static void main(String[]args)throws Exception {
  File folder=new File(args[0]);folder.mkdirs();Context context=new Context();
  BufferedImage fixture=ImageIO.read(new File("app/src/main/res/drawable/tutorial_samples.png"));Bitmap photo=new Bitmap(fixture.getSubimage(0,0,fixture.getWidth()/2,fixture.getHeight()/2));
  Bitmap background=new Bitmap(ImageIO.read(new File("app/src/main/res/drawable-nodpi/theme_slasher_v1.png")));
  SlasherRenderer trash=new SlasherRenderer(context,new java.util.Random(14)),keep=new SlasherRenderer(context,new java.util.Random(8));Matrix pose=new Matrix();pose.setRectToRect(new RectF(0,0,1,1),new RectF(70,270,510,710),Matrix.ScaleToFit.FILL);trash.begin(photo,pose);
  pose.setRectToRect(new RectF(0,0,1,1),new RectF(50,270,490,710),Matrix.ScaleToFit.FILL);keep.begin(photo,pose);
  for(int frame=0;frame<312;frame++) {
   if(frame>0 && frame%104==0){pose.setRectToRect(new RectF(0,0,1,1),new RectF(70,270,510,710),Matrix.ScaleToFit.FILL);trash.begin(photo,pose);pose.setRectToRect(new RectF(0,0,1,1),new RectF(50,270,490,710),Matrix.ScaleToFit.FILL);keep.begin(photo,pose);}
   float t=(frame%104)/30f,trashPhase=Math.min(1,t/2.8f),keepPhase=Math.min(1,t/3.0f);BufferedImage sheet=new BufferedImage(1080,800,BufferedImage.TYPE_INT_RGB);java.awt.Graphics2D out=sheet.createGraphics();
   for(int panel=0;panel<2;panel++) {
    Bitmap scene=Bitmap.createBitmap(720,1000,Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(scene);Paint paint=new Paint(3);canvas.drawBitmap(background,null,new RectF(0,0,720,1080),paint);
    (panel==0?trash:keep).draw(canvas,panel==1,panel==0?trashPhase:keepPhase,720,1000,2);
    out.drawImage(scene.image,panel*540,50,540,750,null);canvas.graphics.dispose();
   }
   out.setColor(java.awt.Color.BLACK);out.fillRect(0,0,1080,50);out.setColor(java.awt.Color.WHITE);out.setFont(new java.awt.Font("SansSerif",java.awt.Font.BOLD,20));out.drawString("SLASHER / TRASH",18,23);out.drawString("SLASHER / KEEP",558,23);out.setFont(new java.awt.Font("SansSerif",0,14));out.drawString(trashPhase<.16?"Trash door opens":"The photo walks into the trash door",18,44);out.drawString(keepPhase<.27?"Keep pose stays locked":keepPhase<.35?"Keep stab leaves BLACK INK":"The hand carries the kept photo away",558,44);
   out.dispose();ImageIO.write(sheet,"png",new File(folder,String.format("frame-%03d.png",frame)));
  }
  System.out.println("Rendered 312 frames of both production Slasher release sequences.");
 }
}
