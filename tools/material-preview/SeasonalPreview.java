package com.dominic.photosweep;
import android.content.Context;import android.graphics.*;import java.awt.image.BufferedImage;import java.io.File;import javax.imageio.ImageIO;
/** Runs the app's seasonal renderer on the host; no emulator or simulated Android GPU. */
public class SeasonalPreview {
 public static void main(String[]args)throws Exception {
  File folder=new File(args[0]);folder.mkdirs();SwipeTheme.Kind kind=SwipeTheme.Kind.HALLOWEEN;
  Context context=new Context();BufferedImage fixture=ImageIO.read(new File("app/src/main/res/drawable/tutorial_samples.png"));Bitmap photo=new Bitmap(fixture.getSubimage(0,0,fixture.getWidth()/2,fixture.getHeight()/2));Bitmap background=new Bitmap(ImageIO.read(new File("app/src/main/res/drawable-nodpi/"+"holiday_halloween"+".png")));
  SeasonalRenderer[] effects={new SeasonalRenderer(context,kind),new SeasonalRenderer(context,kind)};Matrix[] poses={new Matrix(),new Matrix()};
  for(int frame=0;frame<104;frame++) {
   float t=frame/30f;BufferedImage sheet=new BufferedImage(864,650,BufferedImage.TYPE_INT_RGB);java.awt.Graphics2D out=sheet.createGraphics();
   for(int panel=0;panel<2;panel++) {
    Bitmap scene=Bitmap.createBitmap(600,840,Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(scene);Paint paint=new Paint(3);canvas.drawBitmap(background,null,new RectF(0,0,600,900),paint);
    Matrix pose=poses[panel];float held=Math.min(1,t/.5f),x=300+(panel==0?-60:65)*SlasherMotion.ease(held);
    pose.setRectToRect(new RectF(0,0,1,1),new RectF(x-165,245,x+165,575),Matrix.ScaleToFit.FILL);
    if(frame<15){float[] vertices={0,0,1,0,0,1,1,1};pose.mapPoints(vertices);canvas.drawBitmapMesh(photo,1,1,vertices,0,null,0,paint);effects[panel].drawSwipeCue(canvas,pose,600,840);}
    else {if(frame==15)effects[panel].begin(photo,pose);effects[panel].draw(canvas,panel==1,Math.min(1,(t-.5f)/(panel==0?2.7f:2.6f)),600,840,1.6f);}
    out.drawImage(scene.image,panel*432,45,432,605,null);canvas.graphics.dispose();
   }
   out.setColor(java.awt.Color.BLACK);out.fillRect(0,0,864,45);out.setColor(java.awt.Color.WHITE);out.setFont(new java.awt.Font("SansSerif",java.awt.Font.BOLD,18));out.drawString("HALLOWEEN"+" / TRASH",14,20);out.drawString("HALLOWEEN"+" / KEEP",446,20);out.setFont(new java.awt.Font("SansSerif",0,13));out.drawString(frame<15?"Drag left":"Jack-o-lantern folds and swallows the photo",14,38);out.drawString(frame<15?"Drag right":"A ghost carries the photo into the night",446,38);out.dispose();ImageIO.write(sheet,"png",new File(folder,String.format("frame-%03d.png",frame)));
  }
  System.out.println("Rendered 104 production "+kind+" Trash / Keep frames.");
 }
}
