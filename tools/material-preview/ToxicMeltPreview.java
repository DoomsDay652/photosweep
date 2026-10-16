package com.dominic.photosweep;
import android.content.Context;import android.graphics.*;import android.os.SystemClock;import java.awt.image.BufferedImage;import java.io.File;import javax.imageio.ImageIO;
/** Production contact, deformation and strand renderer against a tilted pool surface. */
public class ToxicMeltPreview {
 public static void main(String[]args)throws Exception{
  File folder=new File(args[0]);folder.mkdirs();boolean reapproach=args.length>1&&args[1].equals("reapproach");Context context=new Context();context.getResources().getDisplayMetrics().density=2;BufferedImage fixture=ImageIO.read(new File("app/src/main/res/drawable/tutorial_samples.png"));Bitmap photo=new Bitmap(fixture.getSubimage(0,0,fixture.getWidth()/2,fixture.getHeight()/2));Bitmap backdrop=new Bitmap(ImageIO.read(new File("app/src/main/res/drawable-nodpi/theme_19.jpg")));
  SwipeVfxView fx=new SwipeVfxView(context,k->null,"",SwipeTheme.Kind.TOXIC,true,75,100,false);SystemClock.now=1000;fx.hold(360,440);fx.setToxicPool(.12f,1,1100*.84f);
  for(int frame=0;frame<144;frame++){
   float t=frame/24f,dx=t<1.4f?-220*SlasherMotion.ease(t/1.4f):t<2.4f?-220-45*(t-1.4f):t<3.3f?-265+300*SlasherMotion.ease((t-2.4f)/.9f):t<4.1f?35:35-(reapproach?295:360)*SlasherMotion.ease((t-4.1f)/1.4f);
   Bitmap scene=Bitmap.createBitmap(720,1100,Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(scene);Paint paint=new Paint(3);canvas.drawBitmap(backdrop,null,new RectF(0,0,720,1100),paint);
   Path pool=new Path();pool.moveTo(0,924);pool.lineTo(720,837.6f);pool.lineTo(720,1100);pool.lineTo(0,1100);pool.close();paint.setColor(0xCC648D1C);canvas.drawPath(pool,paint);
   SystemClock.now=1000+(long)(t*1000);fx.setPhoto(photo,new RectF(180,260,620,700),dx,0);fx.progress=dx/440;fx.onDraw(canvas);
   BufferedImage sheet=new BufferedImage(576,920,BufferedImage.TYPE_INT_RGB);java.awt.Graphics2D out=sheet.createGraphics();out.drawImage(scene.image,0,40,576,880,null);out.setColor(java.awt.Color.BLACK);out.fillRect(0,0,576,40);out.setColor(java.awt.Color.WHITE);out.setFont(new java.awt.Font("SansSerif",java.awt.Font.BOLD,17));out.drawString("TOXIC / FLOWING SLUDGE MELTING SURFACE",10,18);out.setFont(new java.awt.Font("SansSerif",0,13));out.drawString(t<2.4?"Touch: edge droops into photo-textured strands":t<4.1?"Pull away: damage remains, stretched necks detach":reapproach?"Return to the same edge: old strands do not replay":"Feed farther: fresh contact melts into the tilted pool",10,35);out.dispose();canvas.graphics.dispose();ImageIO.write(sheet,"png",new File(folder,String.format("frame-%03d.png",frame)));
  }
  System.out.println("Rendered 144 production Toxic contact / droop / strands / pull-away frames.");
 }
}
