package com.dominic.photosweep;
import java.util.*;

public class ReviewLogicCheck {
    static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
    public static void main(String[] args) {
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
        System.out.println("PASS: month boundaries, empty months, undo state/XP, expiry grammar, offline accounts and ad consent/ownership gates.");
    }
}
