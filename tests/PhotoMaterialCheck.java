package com.dominic.photosweep;
import java.util.Arrays;
public class PhotoMaterialCheck {
 static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
 static PhotoMaterialMotion sample(){PhotoMaterialMotion m=new PhotoMaterialMotion();m.pose(new float[]{400,0,55,0,500,100});return m;}
 public static void main(String[]args) {
  int center=20*(PhotoMaterialMotion.X+1)+8;
  PhotoMaterialMotion fire=sample();fire.touched[center]=0;fire.contactX[center]=22;fire.contactY[center]=350;
  fire.trash(SwipeTheme.Kind.FIRE,.7f,1,720,1100,1);
  check((fire.colors[center]>>>24)==0,"contacted material burns away");
  check(fire.touched[center+1]<0 && fire.colors[center+1]==0xFFFFFFFF,"Trash fire never spreads into untouched material");
  check(fire.touched[8]<0 && fire.colors[8]==0xFFFFFFFF,"side contact does not ignite the whole photograph");
  PhotoMaterialMotion deed=sample();deed.keep(SwipeTheme.Kind.FIRE,1,0,1,720,1100,.3f);
  int bottom=PhotoMaterialMotion.Y*(PhotoMaterialMotion.X+1)+8;
  check((deed.colors[bottom]>>>24)==0 && (deed.colors[8]>>>24)==255,"Keep burns bottom before top");
  deed.keep(SwipeTheme.Kind.FIRE,1,2,1,720,1100,1);
  check((deed.colors[8]>>>24)==0,"Keep consumes the top without leaving a phoenix graphic");
  PhotoMaterialMotion water=sample(),toxic=sample();
  for(PhotoMaterialMotion m:new PhotoMaterialMotion[]{water,toxic}){m.touched[center]=0;m.contactX[center]=10;m.contactY[center]=350;}
  water.trash(SwipeTheme.Kind.WATER,.7f,1,720,1100,1);toxic.trash(SwipeTheme.Kind.TOXIC,.7f,1,720,1100,1);
  check(water.vertices[center*2]<water.base[center*2],"liquefying image flows toward the contacted surface");
  check(toxic.vertices[center*2+1]>toxic.base[center*2+1],"toxic image sags downward before dissolving");
  check((toxic.colors[center]>>>24)>(water.colors[center]>>>24),"viscous toxic melt is slower than water liquefaction");
  PhotoMaterialMotion space=sample();space.trash(SwipeTheme.Kind.SPACE,0,1,720,1100,1);
  check(Math.hypot(space.vertices[0]-8,space.vertices[1]-550)<Math.hypot(space.base[0]-8,space.base[1]-550),"black hole draws photo material into its orbit before collision");
  check(Math.abs(space.vertices[1]-550)<Math.abs(space.base[1]-550),"tidal stretch compresses transversely toward black hole");
  for(SwipeTheme.Kind kind:new SwipeTheme.Kind[]{SwipeTheme.Kind.FIRE,SwipeTheme.Kind.WATER,SwipeTheme.Kind.ICE,SwipeTheme.Kind.TOXIC,SwipeTheme.Kind.SPACE,SwipeTheme.Kind.CANDY}) {
   PhotoMaterialMotion m=sample();m.keep(kind,0,.5f,1,720,1100,0);
   check(Arrays.equals(m.base,m.vertices),"Keep starts with the original photograph: "+kind);
   m.keep(kind,.85f,.5f,1,720,1100,0);
   for(float value:m.vertices)check(Float.isFinite(value),"finite material geometry: "+kind);
   for(int color:m.colors)check((color>>>24)>=0,"valid vertex alpha");
   m.reset();for(float t:m.touched)check(t<0,"new gesture clears material state");
  }
  PhotoMaterialMotion a=sample(),b=sample();a.keep(SwipeTheme.Kind.WATER,.8f,1,1,720,1100,0);b.keep(SwipeTheme.Kind.CANDY,.8f,1,1,720,1100,0);
  check(!Arrays.equals(a.vertices,b.vertices),"bubble and candy Keep have different photo geometry");
  PhotoMaterialMotion bubble=sample();bubble.keep(SwipeTheme.Kind.WATER,1,1,1,720,1100,0);
  float beforeX=bubble.vertices[center*2],beforeY=bubble.vertices[center*2+1];
  bubble.keep(SwipeTheme.Kind.WATER,1,2,1,720,1100,.6f);
  check(bubble.vertices[center*2]>beforeX && bubble.vertices[center*2+1]<beforeY,"completed bubble floats up and to the right");
  PhotoMaterialMotion candy=sample();float[][] variants=new float[4][];
  for(int variant=0;variant<4;variant++){candy.candyVariant=variant;candy.keep(SwipeTheme.Kind.CANDY,1,1,1,720,1100,0);variants[variant]=candy.vertices.clone();}
  for(int i=0;i<4;i++)for(int j=i+1;j<4;j++)check(!Arrays.equals(variants[i],variants[j]),"each candy fold has distinct photo geometry");
  check(PhotoMaterialMotion.keepRadius(SwipeTheme.Kind.TOXIC,1100,1,.8f)<PhotoMaterialMotion.keepRadius(SwipeTheme.Kind.TOXIC,1100,1,0)*.3f,"sealed vial recedes into depth");
  System.out.println("PASS: contact-only Trash fire, bottom-to-top Keep fire, water flow, viscous melt, pre-contact tidal stretch and six Keep transformations.");
 }
}
