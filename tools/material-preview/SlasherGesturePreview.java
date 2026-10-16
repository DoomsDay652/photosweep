package com.dominic.photosweep;
import android.content.Context;import android.graphics.*;import java.awt.image.BufferedImage;import java.io.File;import javax.imageio.ImageIO;
/** Host render of production drag cues plus the captured release, including rotated poses. */
public class SlasherGesturePreview {
 static void pose(Matrix pose,float cx,float cy,float degrees){float c=(float)Math.cos(Math.toRadians(degrees)),s=(float)Math.sin(Math.toRadians(degrees));float[] unit={0,0,1,0,0,1};float[] dst={cx-190*c+210*s,cy-190*s-210*c,cx+190*c+210*s,cy+190*s-210*c,cx-190*c-210*s,cy-190*s+210*c};pose.setPolyToPoly(unit,0,dst,0,3);}
 static float drag(float t){if(t<.45f)return SlasherMotion.ease(t/.45f)*.72f;if(t<.85f)return .72f*(1-SlasherMotion.ease((t-.45f)/.40f));return SlasherMotion.ease((t-.85f)/.65f);}
 public static void main(String[] args)throws Exception{
  File folder=new File(args[0]);folder.mkdirs();Context context=new Context();BufferedImage fixture=ImageIO.read(new File("app/src/main/res/drawable/tutorial_samples.png"));Bitmap photo=new Bitmap(fixture.getSubimage(0,0,fixture.getWidth()/2,fixture.getHeight()/2));Bitmap background=new Bitmap(ImageIO.read(new File("app/src/main/res/drawable-nodpi/theme_slasher_v1.png")));
  SlasherRenderer[] renderers={new SlasherRenderer(context,new java.util.Random(14)),new SlasherRenderer(context,new java.util.Random(8))};Matrix[] poses={new Matrix(),new Matrix()};float[] angles={-28,155,32};
  for(int frame=0;frame<(args.length>1?Integer.parseInt(args[1]):450);frame++){
   int local=frame%150,action=frame/150;float t=local/30f;boolean held=local<45;float amount=drag(t);BufferedImage sheet=new BufferedImage(1080,800,BufferedImage.TYPE_INT_RGB);java.awt.Graphics2D out=sheet.createGraphics();
   for(int panel=0;panel<2;panel++){
    Bitmap scene=Bitmap.createBitmap(720,1000,Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(scene);Paint paint=new Paint(3);canvas.drawBitmap(background,null,new RectF(0,0,720,1080),paint);
    Matrix pose=poses[panel];SlasherRenderer renderer=renderers[panel];
    if(held){pose(pose,360+(panel==0?-170:120)*amount,490+20*amount,(panel==1?angles[action]:-14)*amount);float[] quad={0,0,1,0,0,1,1,1};pose.mapPoints(quad);canvas.drawBitmapMesh(photo,1,1,quad,0,null,0,paint);renderer.drawSwipeCue(canvas,pose,720,1000);}
    else {if(local==45){pose(pose,panel==0?190:480,510,panel==1?angles[action]:-14);renderer.begin(photo,pose,720,1000);}renderer.draw(canvas,panel==1,Math.min(1,(t-1.5f)/(panel==0?2.8f:3f)),720,1000,2);}
    out.drawImage(scene.image,panel*540,50,540,750,null);canvas.graphics.dispose();
   }
   out.setColor(java.awt.Color.BLACK);out.fillRect(0,0,1080,50);out.setColor(java.awt.Color.WHITE);out.setFont(new java.awt.Font("SansSerif",java.awt.Font.BOLD,20));out.drawString("SLASHER / TRASH",18,23);out.drawString("SLASHER / KEEP",558,23);out.setFont(new java.awt.Font("SansSerif",0,14));out.drawString(held?"HELD: tease, return, drag left. No stab.":"RELEASED: walk, turn, enter the trash door",18,44);out.drawString(held?"HELD: drag right. Knife waits for release.":"RELEASED: Keep stab at "+(int)angles[action]+" degrees",558,44);out.dispose();ImageIO.write(sheet,"png",new File(folder,String.format("frame-%03d.png",frame)));
  }
  System.out.println("Rendered production held gestures, release-only Keep stabs, and Trash door actions.");
 }
}
