package com.dominic.photosweep;
public class PhotoDamageRetentionCheck {
 static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
 public static void main(String[]args){
  int n=20*(PhotoMaterialMotion.X+1)+2;
  for(SwipeTheme.Kind kind:PhotoMaterialPolishCheck.KINDS){
   PhotoMaterialMotion m=PhotoMaterialPolishCheck.sample(400,500);m.touched[n]=0;m.contactX[n]=20;m.contactY[n]=350;
   m.trash(kind,2,1,720,1100,1);float left=m.remaining[n];check(left==0,"contacted material did not disappear: "+kind);
   m.pose(new float[]{400,0,220,0,500,100});m.trash(kind,3,1,720,1100,0);
   check(m.remaining[n]==0 && (m.colors[n]>>>24)==0,"pulled-away photo repaired itself: "+kind);
   for(int frame=0;frame<60;frame++){m.keep(kind,frame/59f,3+frame/60f,1,720,1100,0);check((m.colors[n]>>>24)==0,"Keep re-created lost photo material: "+kind);}
   PhotoMaterialMotion tail=PhotoMaterialPolishCheck.sample(400,500);m.copyDamageTo(tail);m.reset();
   tail.keep(kind,.8f,4,1,720,1100,0);check((tail.colors[n]>>>24)==0,"next card reset repaired old Keep flight: "+kind);
   check(!m.hasDamage(),"new photo inherited damage: "+kind);
   PhotoMaterialMotion wet=PhotoMaterialPolishCheck.sample(400,500);wet.touched[n]=0;
   for(int frame=0;frame<600;frame++)wet.trash(kind,frame/60f,1,720,1100,1);
   if(kind==SwipeTheme.Kind.WATER || kind==SwipeTheme.Kind.TOXIC){check(wet.touched[n+4]<0,"liquid ate untouched photo after pull-away: "+kind);}
  }
  PhotoMaterialMotion neck=PhotoMaterialPolishCheck.sample(400,500);neck.touched[n]=0;neck.contactX[n]=20;neck.contactY[n]=350;
  neck.pose(new float[]{400,0,300,0,500,100});neck.trash(SwipeTheme.Kind.TOXIC,.8f,1,720,1100,1);
  check(neck.remaining[n]==0,"pulled toxic neck stretched into a long ribbon instead of detaching");
  System.out.println("PASS: six-theme persistent UV damage, pull-away, Keep, independent snapshots, clean next card and bounded liquid spread.");
 }
}
