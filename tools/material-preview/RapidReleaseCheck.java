package com.dominic.photosweep;
import android.content.Context;
import android.graphics.*;
import android.os.SystemClock;
import java.lang.reflect.Field;

/** Real outgoing views survive reparenting while fresh cards are already held. */
public final class RapidReleaseCheck {
 static Object field(Object o,String name)throws Exception{Field f=SwipeVfxView.class.getDeclaredField(name);f.setAccessible(true);return f.get(o);}
 static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
 public static void main(String[]args)throws Exception{
  SwipeTheme.Kind[] kinds={SwipeTheme.Kind.SUMMER,SwipeTheme.Kind.HALLOWEEN,SwipeTheme.Kind.SLASHER,
   SwipeTheme.Kind.FIRE,SwipeTheme.Kind.WATER,SwipeTheme.Kind.ICE,SwipeTheme.Kind.TOXIC,SwipeTheme.Kind.SPACE,SwipeTheme.Kind.CANDY};
  Context context=new Context();Canvas canvas=new Canvas(Bitmap.createBitmap(72,110,Bitmap.Config.ARGB_8888));
  for(SwipeTheme.Kind kind:kinds)for(boolean keep:new boolean[]{false,true}){
   Bitmap first=Bitmap.createBitmap(32,32,Bitmap.Config.ARGB_8888);
   SwipeVfxView old=new SwipeVfxView(context,k->null,"",kind,true,70,100,false);
   old.width=72;old.height=110;
   SystemClock.now=1000;old.hold(35,55);old.setPhoto(first,new RectF(13,33,57,77),0,0);
   SystemClock.now=1016;old.setPhoto(first,new RectF(13,33,57,77),keep?19:-19,0);old.onDraw(canvas);
   check(old.advancesOnRelease(),"new theme still requires animation gate: "+kind);
   old.release(keep);old.continueIndependently();
   old.transferRelease(()->old.onDetachedFromWindow());
   check(field(old,"bitmap")==first&&(Boolean)field(old,"independentRelease"),"transfer discarded released photo");
   if(keep&&PhotoInteraction.supports(kind))check(old.independentReleaseDuration(true)>=2200,"Keep flight cut short");
   // Three subsequent photos arrive before this outgoing animation completes.
   for(int n=0;n<3;n++){
    SwipeVfxView next=new SwipeVfxView(context,k->null,"",kind,true,70,100,false);
    next.width=72;next.height=110;
    Bitmap fresh=Bitmap.createBitmap(32,32,Bitmap.Config.ARGB_8888);
    SystemClock.now=1032+n*80;next.hold(35,55);next.setPhoto(fresh,new RectF(13,33,57,77),0,0);
    check(!(Boolean)field(next,"independentRelease")&&(Long)field(next,"released")==0,"fresh card inherits release gate");
    check(!next.hasRetainedDamage(),"fresh photo inherits damage");
    next.onDraw(canvas);old.onDraw(canvas);
    check(field(old,"bitmap")==first&&field(next,"bitmap")==fresh,"next card overwrote outgoing photo");
    next.cancel();check(field(old,"bitmap")==first,"cancelling next card killed outgoing animation");
   }
   SystemClock.now=1016+old.releaseDuration(keep)*3/4;old.onDraw(canvas);
   if(old.hasStagedRelease()){
    Object renderer=field(old,kind==SwipeTheme.Kind.SLASHER?"slasherRenderer":"seasonalRenderer");
    check(renderer instanceof SlasherRenderer?((SlasherRenderer)renderer).active():((SeasonalRenderer)renderer).active(),"captured stage lost its photo");
   }else if(!keep){
    Matrix pose=(Matrix)field(old,"photoToStage");float[] p={1,.5f};pose.mapPoints(p);
    check(p[0]<0,"independent trash never travels left");
   }
   old.onDetachedFromWindow();check(field(old,"bitmap")==null,"completed outgoing view retains bitmap");
  }
  SwipeVfxView reduced=new SwipeVfxView(context,k->null,"",SwipeTheme.Kind.SUMMER,true,70,100,true);
  check(!reduced.advancesOnRelease()&&reduced.releaseDuration(false)==80,"reduced-motion fallback changed");
  System.out.println("PASS: 18 independent Keep/Trash releases, 54 immediately available fresh cards, preserved outgoing ownership, transfer, material travel, full Keep lifetime and cleanup.");
 }
}
