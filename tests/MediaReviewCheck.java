package com.dominic.photosweep;
import java.util.*;

/** Video review must never overwrite a photo decision or resolve to its URI. */
public final class MediaReviewCheck {
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    public static void main(String[] args){
        long[] ids={0,1,42,2147483648L,(1L<<62)-1};
        Set<Long> identities=new HashSet<>();
        for(long id:ids)for(boolean video:new boolean[]{false,true}){
            long key=MediaReviewPolicy.key(id,video);
            check(key>=0,"video collides with tutorial/pending-action sentinels");
            check(identities.add(key),"image and video ID collision");
            check(MediaReviewPolicy.providerId(key)==id,"provider ID not recoverable");
            check(MediaReviewPolicy.isVideo(Long.parseLong(Long.toString(key)))==video,"type lost after persistence");
            if(!video)check(key==id,"upgrade changes existing photo ownership or review state");
        }
        for(long sample:new long[]{-1,-101,-201})check(!MediaReviewPolicy.isVideo(sample),"tutorial treated as a real video");
        for(long invalid:new long[]{-1,1L<<62,Long.MAX_VALUE}){
            boolean rejected=false;
            try{MediaReviewPolicy.key(invalid,true);}catch(IllegalArgumentException expected){rejected=true;}
            check(rejected,"ambiguous provider ID accepted");
        }
        Set<String> reviewed=new HashSet<>(),kept=new HashSet<>(),trashed=new HashSet<>(),rewarded=new HashSet<>();
        long image=MediaReviewPolicy.key(42,false),video=MediaReviewPolicy.key(42,true);
        String i=Long.toString(image),v=Long.toString(video);
        reviewed.add(i);kept.add(i);reviewed.add(v);kept.add(v);rewarded.add(v);
        ReviewUndo undo=new ReviewUndo(video,"2026-10",1234,false,false,false,false);
        undo.rollback(reviewed,kept,trashed,rewarded);
        check(reviewed.contains(i)&&kept.contains(i),"Undo video modified image with same provider ID");
        check(!reviewed.contains(v)&&!kept.contains(v),"Undo did not restore video state");
        check(rewarded.contains(v),"video Undo allows reward farming");
        check(MediaReviewPolicy.duration(-99).equals("0:00"),"negative duration");
        check(MediaReviewPolicy.duration(65000).equals("1:05"),"minute duration");
        check(MediaReviewPolicy.duration(3723000).equals("1:02:03"),"hour duration");
        check(MediaReviewPolicy.count(1,true).equals("1 video")&&MediaReviewPolicy.count(2,false).equals("2 photos"),"media labels");
        System.out.println("PASS: noncolliding media IDs, persisted collection routing, photo upgrade compatibility, isolated video Undo, reward retention and duration labels.");
    }
}
