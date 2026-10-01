package com.dominic.photosweep;
import java.util.*;

public class ReviewLogicCheck {
    static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
    public static void main(String[] args) {
        check(SwipeMotion.travel(.7f,true)>0 && SwipeMotion.travel(.7f,false)<0,"directional VFX follow gesture");
        check(Math.abs(SwipeMotion.travel(.7f,true)+SwipeMotion.travel(.7f,false))<.0001,"mirrored trajectories");
        check(SwipeMotion.ease(-1)==0 && SwipeMotion.ease(2)==1,"clamped drag progression");
        check(SwipeMotion.falling(.8f)>SwipeMotion.falling(.2f),"candy and petals fall down");
        check(SwipeMotion.duration(200)<SwipeMotion.duration(50),"speed changes completion time");
        int[] animated={18,19,20,21,22,23,24,25,27,30,31,32,33,34,35,36,37,38,39,40,41,42,43,50,51,52,53,54,55,56,57,58,59,60};
        Set<SwipeTheme.Kind> kinds=new HashSet<>();
        for(int theme:animated){SwipeTheme.Kind kind=SwipeTheme.forTheme(theme);check(kind!=SwipeTheme.Kind.SPARKLE,"every Tier 3 theme has a matched profile");kinds.add(kind);}
        check(kinds.size()==animated.length,"all Tier 3 profiles are distinct");
        check(SwipeTheme.forTheme(21)==SwipeTheme.Kind.FIRE,"Fire burns and bursts");
        check(SwipeMotion.shouldCommit(100,20,85)&&SwipeMotion.shouldCommit(-100,20,85),"both review directions commit");
        check(!SwipeMotion.shouldCommit(60,0,85)&&!SwipeMotion.shouldCommit(100,100,85),"tap and vertical pan do not commit");
        for(int[] size:new int[][]{{4000,3000},{3000,4000},{12000,2000},{12000,12000},{800,600}}){
            float scale=PhotoFit.scale(size[0],size[1]);
            check(scale>0&&scale<=1,"never upscale original photos");
            check(size[0]*scale<=3072.01&&size[1]*scale<=3072.01,"display decode edge bound");
            check((double)size[0]*size[1]*scale*scale<=4194305,"display decode memory bound");
            check(Math.abs((size[0]*scale)/(size[1]*scale)-(float)size[0]/size[1])<.0001,"whole photo aspect ratio retained");
        }
        for(String key:new String[]{"trash_entries","trash_evictions","photo_access","sound_enabled","music_enabled","purchase_remove_ads"})
            check(!ResetPolicy.clears(key),"reset preserves photos, cleanup timers, permissions and entitlements");
        for(String key:new String[]{"xp","reviewed","rewarded","theme","admin_mode","kept_ids","swipe_style_21"})
            check(ResetPolicy.clears(key),"reset restarts early progression");
        List<String> months = Arrays.asList("2026-10", "2025-12", "2026-01", "2026-04");
        check("2025-12".equals(ReviewNavigation.adjacent(months,"2026-01",false)), "previous year");
        check("2026-01".equals(ReviewNavigation.adjacent(months,"2025-12",true)), "next year");
        check("2026-04".equals(ReviewNavigation.adjacent(months,"2026-01",true)), "skip empty months");
        check(ReviewNavigation.adjacent(months,"2026-10",true)==null, "latest endpoint");
        check(ReviewNavigation.adjacent(months,"2025-12",false)==null, "earliest endpoint");
        check("2026-04".equals(ReviewNavigation.adjacent(months,"2026-09",false)), "all photos trashed in current month");
        check("1 photo".equals(ReviewNavigation.photoCount(1))&&"0 photos".equals(ReviewNavigation.photoCount(0)), "photo count grammar");
        check("Wiped in 1 day".equals(ReviewNavigation.expiry(1)), "singular");
        check("Wiped in 2 days".equals(ReviewNavigation.expiry(86400001)), "plural");
        check("Waiting for cleanup".equals(ReviewNavigation.expiry(0)), "expired wording");
        for (boolean trash : new boolean[]{false,true}) {
            Set<String> reviewed=new HashSet<>(), kept=new HashSet<>(), trashed=new HashSet<>(), rewarded=new HashSet<>();
            ReviewUndo undo=new ReviewUndo(7,"2026-10",1,trash,false,false,false);undo.earnedXp=trash?15:10;
            reviewed.add("7");kept.add("7");trashed.add("7");rewarded.add("7");
            undo.rollback(reviewed,kept,trashed,rewarded);
            check(reviewed.isEmpty()&&kept.isEmpty()&&trashed.isEmpty()&&rewarded.isEmpty(),"undo both action types");
            check(undo.restoredXp(100+undo.earnedXp)==100,"undo XP refund");
        }
        ReviewUndo prior=new ReviewUndo(7,"2026-10",1,false,true,true,true);
        Set<String> marks=new HashSet<>(Collections.singleton("7"));
        prior.rollback(marks,marks,marks,marks);check(marks.contains("7")&&prior.restoredXp(100)==100,"previous rewards retained");
        AppServices offline=AppServices.offline();
        check(!offline.account.signedIn()&&!offline.purchases.available()&&!offline.canShowAds(false,false),"offline release");
        for(boolean consent:new boolean[]{false,true})for(boolean owns:new boolean[]{false,true}) {
            AppServices services=new AppServices(offline.account,new AppServices.Purchases(){
                public boolean available(){return true;}public boolean ownsRemoveAds(){return owns;}
                public String localizedPrice(AppServices.Product p){return "localized price";}
            },new AppServices.Ads(){public boolean configured(){return true;}public boolean consentAllowsAds(){return consent;}});
            check(services.canShowAds(false,false)==(consent&&!owns),"consent and entitlement gates");
            check(!services.canShowAds(true,false)&&!services.canShowAds(false,true),"no ads over review or Trash");
        }
        System.out.println("PASS: 34 distinct theme profiles, swipe intent, aspect/memory bounds, reset safety, month boundaries, empty months, undo state/XP, expiry grammar, offline accounts and ad consent/ownership gates.");
    }
}
