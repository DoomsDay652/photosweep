package com.dominic.photosweep;
import java.util.Arrays;
/** Five independent regression groups for the six photo-material reactions. */
public class PhotoMaterialPolishCheck {
 static final SwipeTheme.Kind[] KINDS={SwipeTheme.Kind.FIRE,SwipeTheme.Kind.WATER,SwipeTheme.Kind.ICE,SwipeTheme.Kind.TOXIC,SwipeTheme.Kind.SPACE,SwipeTheme.Kind.CANDY};
 static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
 static PhotoMaterialMotion sample(float w,float h){PhotoMaterialMotion m=new PhotoMaterialMotion();m.pose(new float[]{w,0,130,0,h,100});return m;}
 static int touched(PhotoMaterialMotion m){int total=0;for(float t:m.touched)if(t>=0)total++;return total;}
 static void contact() {
  PhotoMaterialMotion m=sample(400,500);float[] edges=new float[1025];Arrays.fill(edges,38);
  float[] previous={400,0,130,0,500,100},next=previous.clone(),sweep=new float[198];
  int steps=PhotoContact.buildSweep(previous,next,sweep);m.contact(sweep,steps,edges,1100,0,2);
  check(touched(m)==0,"separated photo must remain intact");int before=0;
  for(int i=1;i<=8;i++) {
   next[2]=130-i*40;steps=PhotoContact.buildSweep(previous,next,sweep);m.pose(next);m.contact(sweep,steps,edges,1100,i*.1f,2);
   check(touched(m)>=before,"pushing farther left lost contact state");before=touched(m);System.arraycopy(next,0,previous,0,6);
  }
  check(before>PhotoMaterialMotion.N/2,"large sweep missed most photo material");
  m.reset();check(touched(m)==0,"new card inherited old damage");
  m.touched[500]=0;m.trash(SwipeTheme.Kind.FIRE,2,1,720,1100,1);
  check(touched(m)==1 && m.colors[501]==0xFFFFFFFF,"Trash fire spread beyond contact");
  System.out.println("PASS round 1: actual photo contact, progressive feed, reset and contact-only fire.");
 }
 static void keepShapes() {
  for(SwipeTheme.Kind k:KINDS) {
   PhotoMaterialMotion m=sample(400,500);m.keep(k,0,0,1,720,1100,0);check(Arrays.equals(m.base,m.vertices),"Keep start jumps: "+k);
   for(int i=0;i<=40;i++){float t=i/40f;m.keep(k,t,t,1,720,1100,t*.5f);for(float v:m.vertices)check(Float.isFinite(v),"non-finite Keep: "+k);}
  }
  PhotoMaterialMotion bubble=sample(400,500);bubble.keep(SwipeTheme.Kind.WATER,1,0,1,720,1100,0);
  for(int n=0;n<PhotoMaterialMotion.N;n++)check(bubble.vertices[n*2]>=0 && bubble.vertices[n*2]<=720,"bubble clipped before flight");
  PhotoMaterialMotion fire=sample(400,500);fire.keep(SwipeTheme.Kind.FIRE,1,1,1,720,1100,.3f);
  check((fire.colors[0]>>>24)==255 && (fire.colors[PhotoMaterialMotion.N-1]>>>24)==0,"Keep fire lost bottom-to-top ordering");
  System.out.println("PASS round 2: continuous six-theme Keep geometry, complete bubble and correct Keep ignition.");
 }
 static void recovery() {
  for(SwipeTheme.Kind k:KINDS) {
   PhotoMaterialMotion m=sample(400,500);Arrays.fill(m.touched,0);m.trash(k,1,1,720,1100,0);
   check(Arrays.equals(m.vertices,m.base),"cancelled Trash retained distortion: "+k);
   for(int c:m.colors)check(c==0xFFFFFFFF,"cancelled Trash retained holes or tint: "+k);
   m.reset();for(int c:m.colors)check(c==0xFFFFFFFF,"new gesture retained tint: "+k);
  }
  System.out.println("PASS round 3: retreat restores geometry/opacity and reset clears tint and damage.");
 }
 static void sizes() {
  int count=0;
  for(float[] size:new float[][]{{720,1100,1},{1100,720,1},{1080,2340,3},{1440,3120,3.5f}})
   for(float[] aspect:new float[][]{{500,500},{160,900},{900,160}})
    for(SwipeTheme.Kind k:KINDS) {
     PhotoMaterialMotion m=sample(aspect[0]*size[2],aspect[1]*size[2]);
     for(int frame=0;frame<=30;frame++){float t=frame/30f;m.keep(k,t,t,size[2],size[0],size[1],t);for(float v:m.vertices)check(Float.isFinite(v),"invalid aspect/density geometry: "+k);}
     m.trash(k,.5f,size[2],size[0],size[1],1);for(float v:m.vertices)check(Float.isFinite(v),"invalid Trash geometry");count++;
    }
  System.out.println("PASS round 4: "+count+" theme/aspect/density combinations, portrait and landscape.");
 }
 static void repeat() {
  PhotoMaterialMotion m=sample(400,500);float[][] candy=new float[4][];
  for(int variant=0;variant<4;variant++){m.candyVariant=variant;m.keep(SwipeTheme.Kind.CANDY,1,0,1,720,1100,0);candy[variant]=m.vertices.clone();}
  for(int a=0;a<4;a++)for(int b=a+1;b<4;b++)check(!Arrays.equals(candy[a],candy[b]),"duplicate candy geometry");
  for(int i=0;i<100;i++){m.reset();m.pose(new float[]{400,0,130,0,500,100});m.keep(KINDS[i%6],1,i,1,720,1100,1);for(int color:m.colors)check((color>>>24)==0,"completed Keep still visible");}
  System.out.println("PASS round 5: four candy forms, 100 repeated completions and final opacity cleanup.");
 }
 public static void main(String[] args){int round=args.length==0?0:Integer.parseInt(args[0]);if(round==0||round==1)contact();if(round==0||round==2)keepShapes();if(round==0||round==3)recovery();if(round==0||round==4)sizes();if(round==0||round==5)repeat();}
}
