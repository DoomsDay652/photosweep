package com.dominic.photosweep;
import android.graphics.*;
import java.awt.image.BufferedImage;import java.io.File;import javax.imageio.ImageIO;import java.util.Arrays;
/** Reproducible visual inspection of the production renderer using host Canvas adapters. */
public class MaterialPreview {
 public static void main(String[] args)throws Exception {
  File outputDir=new File(args[0]);outputDir.mkdirs();
  BufferedImage fixture=ImageIO.read(new File("app/src/main/res/drawable/tutorial_samples.png"));
  Bitmap photo=new Bitmap(fixture.getSubimage(0,0,fixture.getWidth()/2,fixture.getHeight()/2));
  Bitmap flame=new Bitmap(ImageIO.read(new File("app/src/main/res/drawable-nodpi/fx_flame.png")));
  SwipeTheme.Kind[] kinds={SwipeTheme.Kind.FIRE,SwipeTheme.Kind.WATER,SwipeTheme.Kind.ICE,SwipeTheme.Kind.TOXIC,SwipeTheme.Kind.SPACE,SwipeTheme.Kind.CANDY};
  PhotoMaterialRenderer[] renderers=new PhotoMaterialRenderer[6];float[][] previous=new float[6][6],edges=new float[6][1025];
  BufferedImage atlas=ImageIO.read(new File("app/src/main/res/drawable-nodpi/swipe_element_atlas.png"));
  for(int i=0;i<6;i++)for(int row=0;row<=1024;row++) {
    int y=Math.min(511,row*512/1024);float edge=0;
    for(int x=511;x>=0;x--){int gutter=Math.min(Math.min(x,511-x),Math.min(y,511-y));float alpha=(atlas.getRGB(i%3*512+x,i/3*512+y)>>>24)*PhotoMaterialMotion.clamp((gutter-2)/12f);if(alpha>100){edge=(x+.5f)/512;break;}}
    edges[i][row]=Math.max(0,-264+348*edge);
  }
  for(int i=0;i<6;i++){renderers[i]=new PhotoMaterialRenderer();renderers[i].setArtwork(null,flame);renderers[i].setMaterialArt(PhotoMaterialArt.load(new android.content.Context()));previous[i]=new float[]{440,0,130,0,440,220};}
  int frames=args.length>1?Integer.parseInt(args[1]):120;
  for(int frame=args.length>2?Integer.parseInt(args[2]):0;frame<frames;frame++){
   int stage=frame/30;float t=(frame%30)/29f,seconds=frame/12f;
   BufferedImage sheet=new BufferedImage(1080,1000,BufferedImage.TYPE_INT_RGB);java.awt.Graphics2D g=sheet.createGraphics();g.setColor(new java.awt.Color(12,20,30));g.fillRect(0,0,1080,1000);
   for(int i=0;i<6;i++) {
    PhotoMaterialRenderer renderer=renderers[i];if(frame==90){renderer.reset();renderer.motion.candyVariant=i%4;}
    float dx=stage==0?-250*PhotoMaterialMotion.smooth(t):stage==1?-250*(1-PhotoMaterialMotion.smooth(t)):0;
    float angle=stage==0?.28f*(float)Math.sin(t*Math.PI):stage==1?-.15f*(float)Math.sin(t*Math.PI):0;
    float cos=(float)Math.cos(angle),sin=(float)Math.sin(angle);
    float[] pose={440*cos,-440*sin,350+dx-220*cos+220*sin,440*sin,440*cos,440-220*sin-220*cos};renderer.motion.pose(pose);
    if(stage<2){float[] sweep=new float[198];int steps=PhotoContact.buildSweep(previous[i],pose,sweep);renderer.motion.contact(sweep,steps,edges[i],900,seconds,4);System.arraycopy(pose,0,previous[i],0,6);}
    Bitmap scene=new Bitmap(new BufferedImage(720,900,BufferedImage.TYPE_INT_ARGB));Canvas canvas=new Canvas(scene);
    canvas.graphics.setColor(new java.awt.Color(12,20,30));canvas.graphics.fillRect(0,0,720,900);
    float age=(frame%30)/12f,duration=i==1?2.2f:i==4?1.35f:1.75f;
    renderer.draw(canvas,photo,kinds[i],seconds,2,720,900,stage>=2,Math.min(1,age/.42f),stage>=2?PhotoMaterialMotion.clamp((age-.42f)/(duration-.42f)):0,1);
    int x=i%3*360,y=i/3*500;g.drawImage(scene.image,x,y+45,360,450,null);
    g.setColor(java.awt.Color.WHITE);g.setFont(new java.awt.Font("SansSerif",java.awt.Font.BOLD,16));g.drawString(kinds[i]+" / "+(stage==0?"CONTACT":stage==1?"PULL AWAY":stage==2?"KEEP DAMAGE":"FRESH KEEP"),x+12,y+20);
    g.setColor(new java.awt.Color(155,190,210));g.setFont(new java.awt.Font("SansSerif",java.awt.Font.PLAIN,13));g.drawString(stage==0?"Photo material reacts at the edge":stage==1?"Damage stays on the photograph":stage==2?"Missing material stays missing":"Next photo starts clean",x+12,y+38);
    canvas.graphics.dispose();
   }
   g.dispose();ImageIO.write(sheet,"png",new File(outputDir,String.format("frame-%03d.png",frame)));
  }
  System.out.println("Rendered "+frames*6+" production material scenes: contact, pull-away, damaged Keep, fresh Keep.");
 }
}
