package com.dominic.photosweep;
import java.util.*;

public class ReviewLogicCheck {
    static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
    public static void main(String[] args) {
        check(ThemeProgression.level(0)==1 && ThemeProgression.level(199)==1 && ThemeProgression.level(200)==2,"200 XP level boundary");
        check(ThemeProgression.addLevels(245,5)==1245,"admin adds exactly five levels preserving progress");
        check(ThemeProgression.level(ThemeProgression.migratedXp(2750))==6,"migration preserves earned level");
        check(ThemeProgression.reviewXp(9)==5 && ThemeProgression.reviewXp(10)==15 && ThemeProgression.reviewXp(20)==15,"unique review milestone bonus");
        int[] odds=new int[ThemeProgression.REWARD_TYPES],families=new int[3],units=new int[3];
        for(int i=0;i<100;i++) {
            int reward=ThemeProgression.roll(i);odds[reward]++;
            families[ThemeProgression.tokenKind(reward)]++;
            units[ThemeProgression.tokenKind(reward)]+=ThemeProgression.tokenCount(reward);
        }
        check(Arrays.equals(odds,new int[]{50,20,20,5,5}),"five reward odds sum to 100 percent");
        check(Arrays.equals(families,new int[]{70,25,5}),"base token-family odds remain 70/25/5 without guarantee");
        check(Arrays.equals(units,new int[]{110,30,5}),"bundle payouts award correct amounts");
        check(ThemeProgression.roll(94)==3 && ThemeProgression.roll(95)==4,"Diamond starts at the flat five-percent boundary");
        check(ThemeProgression.tokenCount(1)==3 && ThemeProgression.tokenKind(1)==0,"3 Silver bundle");
        check(ThemeProgression.tokenCount(3)==2 && ThemeProgression.tokenKind(3)==1,"2 Gold bundle");
        check(ThemeProgression.legacyReward(0)==0 && ThemeProgression.legacyReward(1)==2 && ThemeProgression.legacyReward(2)==4 && ThemeProgression.legacyReward(-1)==-1,"last-result migration preserves token kind");
        check("3 Silver tokens".equals(ThemeProgression.rewardLabel(1)) && "2 Gold tokens".equals(ThemeProgression.rewardLabel(3)) && "1 Diamond token".equals(ThemeProgression.rewardLabel(4)),"reward descriptions match amounts");
        float response60=0,response120=0;
        for(int i=0;i<60;i++)response60+=(1-response60)*SwipeMotion.followFactor(1000f/60,32);
        for(int i=0;i<120;i++)response120+=(1-response120)*SwipeMotion.followFactor(1000f/120,32);
        check(Math.abs(response60-response120)<.00001f,"pocket response is independent of refresh rate");
        check(SwipeMotion.followFactor(-1,32)==0 && SwipeMotion.followFactor(16,32)>0 && SwipeMotion.followFactor(50,32)<1,"pocket easing cannot overshoot");
        check(SwipeMotion.keepClipRight(100,400,368,1)==268,"photo passes behind pocket mouth");
        check(SwipeMotion.keepClipRight(900,400,368,1)==0,"fully received photo is occluded");
        check(SwipeMotion.keepClipRight(-100,400,368,1)==400,"reverse movement restores photo");
        check(SwipeMotion.keepClipRight(100,400,368,1.025f)==266,"occlusion accounts for release scale");
        check(ResetPolicy.clears("progress_spins") && ResetPolicy.clears("progress_owned"),"reset includes tokens, spins and ownership");
        check(SwipeMotion.travel(.7f,true)>0 && SwipeMotion.travel(.7f,false)<0,"directional VFX follow gesture");
        check(SwipeMotion.intent(0)==0 && SwipeMotion.intent(.03f)==0 && SwipeMotion.intent(-.03f)==0,"touch and center crossing have no action");
        check(SwipeMotion.intent(SwipeMotion.dragProgress(20,100,400))==0,"vertical gestures show no review action");
        check(SwipeMotion.dragProgress(150,10,400)>0 && SwipeMotion.dragProgress(-150,10,400)<0,"horizontal action follows current drag");
        check(SwipeMotion.dragProgress(0,0,0)==0,"zero-size layout stays neutral");
        check(SwipeMotion.intent(.1f)==1 && SwipeMotion.intent(-.1f)==-1,"drag direction selects correct action");
        check(!SwipeMotion.actionLabel(true).equals(SwipeMotion.actionLabel(false)),"keep and trash use different labels");
        check(SwipeMotion.actionColor(true)!=SwipeMotion.actionColor(false),"theme-independent semantic colors differ");
        for (boolean keep : new boolean[]{false,true}) {
            check(SwipeMotion.cardScale(0,keep)==1 && SwipeMotion.cardAlpha(0,keep)==1 && SwipeMotion.cardVertical(0,keep)==0,"new photo starts intact");
            for (float phase : new float[]{-.5f,0,.25f,.5f,.75f,1,1.5f}) {
                check(SwipeMotion.cardScale(phase,keep)>0,"release scale remains valid");
                check(SwipeMotion.cardAlpha(phase,keep)>=0 && SwipeMotion.cardAlpha(phase,keep)<=1,"release alpha remains valid");
                if (keep) check(SwipeMotion.cardAlpha(phase,true)==1,"kept photos never dissolve");
            }
        }
        check(SwipeMotion.cardScale(1,false)==1 && SwipeMotion.cardAlpha(1,false)==1,"trash feeds intact into the edge before being clipped");
        check(SwipeMotion.cardScale(1,true)>=1 && SwipeMotion.cardVertical(1,true)<0,"keep lifts intact");
        check(SwipeMotion.cardVertical(1,false)==0 && SwipeMotion.cardRotation(false)==0,"trash feeds straight into grinder");
        check(SwipeMotion.grinderClipLeft(-100,400,9)==109,"grinder clipping stays fixed at screen edge");
        check(SwipeMotion.grinderClipLeft(-900,400,9)==400,"fully consumed photo is fully clipped");
        check(SwipeMotion.grinderClipLeft(20,400,9)==0,"reversed drag restores the whole card");
        check(SwipeMotion.grinderProximity(150,9,100)==0,"distant photos do not shake");
        check(SwipeMotion.pocketOpening(300,300,400,70)==0,"holding starts at a dot");
        check(SwipeMotion.keepSleeveLeft(100,200,300,0)==300,"closed sleeve stays a narrow seam");
        check(SwipeMotion.keepSleeveLeft(100,200,300,1)==204,"open sleeve wraps the entering portion of the photo");
        check(SwipeMotion.keepSleeveLeft(280,200,300,1)==280,"light cannot extend beyond photo left edge");
        check(SwipeMotion.keepSleeveLeft(350,200,300,1)==300,"offscreen photos never create inverted light bounds");
        check(SwipeMotion.pocketOpening(300,400,400,70)==1,"approaching edge opens pocket");
        check(SwipeMotion.pocketOpening(300,330,400,70)<SwipeMotion.pocketOpening(300,370,400,70),"pocket follows approach and retreat");
        check(SwipeMotion.pocketOpening(380,390,400,70)<.1f,"edge-adjacent photos do not jump fully open");
        check(SwipeMotion.pocketOpening(300,250,400,70)==0,"left swipe leaves pocket closed");
        check(SwipeMotion.grinderProximity(9,9,100)==1,"contact reaches full grinder strength");
        check(SwipeMotion.grinderProximity(60,9,100)<SwipeMotion.grinderProximity(30,9,100),"shake increases near teeth");
        check(SwipeMotion.spellHalfHeight(300,800,400,10)>SwipeMotion.spellHalfHeight(150,800,400,10),"spell height follows portrait and landscape photos");
        check(SwipeMotion.spellHalfWidth(200,500,10)<SwipeMotion.spellHalfWidth(400,500,10),"spell width follows photo width");
        check(SwipeMotion.spellHalfHeight(900,300,60,10)<=50,"spell stays inside short layouts");
        for(int frame=0;frame<180;frame++) {
            check(SwipeMotion.grinderShake(frame/60f,0)==0,"no vibration away from grinder");
            check(Math.abs(SwipeMotion.grinderShake(frame/60f,1))<=1.001,"vibration remains bounded");
        }
        for (int speed : new int[]{0,50,100,200,500}) {
            check(SwipeMotion.releaseDuration(speed,false)>SwipeMotion.releaseDuration(speed,true),"grinder feeding has its own timing");
            check(SwipeMotion.releaseDuration(speed,true)>=220 && SwipeMotion.releaseDuration(speed,false)<=710,"release timing stays bounded");
        }
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
        for (int i : new int[]{3,4,5,6,7,8,9,10,34,35,36,37,38,39,40,41,56,57,58,59,60}) check(!PlayPolicy.themeAllowed(i), "franchise theme excluded");
        for (int i : new int[]{0,18,21,22,30,32,42,43,55,61}) check(PlayPolicy.themeAllowed(i), "original theme available");
        check(!PlayPolicy.accountReady("", "", "", "", ""), "unconfigured account requests blocked");
        check(!PlayPolicy.accountReady("key","app","project","https://example.com/privacy","https://localhost/delete"), "placeholder endpoints blocked");
        check(PlayPolicy.accountReady("key","app","project","https://photosweep.test/privacy","https://photosweep.test/delete"), "configured account gate");
        check(!PlayPolicy.validUrl("http://photosweep.test/delete"), "cleartext endpoints blocked");
        List<String> months = Arrays.asList("2026-10", "2025-12", "2026-01", "2026-04");
        check("2025-12".equals(ReviewNavigation.adjacent(months,"2026-01",false)), "previous year");
        check("2026-01".equals(ReviewNavigation.adjacent(months,"2025-12",true)), "next year");
        check("2026-04".equals(ReviewNavigation.adjacent(months,"2026-01",true)), "skip empty months");
        check(ReviewNavigation.adjacent(months,"2026-10",true)==null, "latest endpoint");
        check(ReviewNavigation.adjacent(months,"2025-12",false)==null, "earliest endpoint");
        check("2026-04".equals(ReviewNavigation.adjacent(months,"2026-09",false)), "all photos trashed in current month");
        check("1 photo".equals(ReviewNavigation.photoCount(1))&&"0 photos".equals(ReviewNavigation.photoCount(0)), "photo count grammar");
        check("Recovery window: 1 day".equals(ReviewNavigation.expiry(1)), "singular");
        check("Recovery window: 2 days".equals(ReviewNavigation.expiry(86400001)), "plural");
        check("Waiting for cleanup".equals(ReviewNavigation.expiry(0)), "expired wording");
        for (boolean trash : new boolean[]{false,true}) {
            Set<String> reviewed=new HashSet<>(), kept=new HashSet<>(), trashed=new HashSet<>(), rewarded=new HashSet<>();
            ReviewUndo undo=new ReviewUndo(7,"2026-10",1,trash,false,false,false);undo.earnedXp=trash?15:10;
            reviewed.add("7");kept.add("7");trashed.add("7");rewarded.add("7");
            undo.rollback(reviewed,kept,trashed,rewarded);
            check(reviewed.isEmpty()&&kept.isEmpty()&&trashed.isEmpty()&&rewarded.contains("7"),"undo restores photo state and retains earned credit");
            check(undo.restoredXp(100+undo.earnedXp)==100+undo.earnedXp,"undo keeps earned XP and cannot farm levels");
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
        System.out.println("PASS: release theme exclusions, account configuration gates, 34 distinct theme profiles, swipe intent, aspect/memory bounds, reset safety, month boundaries, empty months, undo state/XP, expiry grammar, offline accounts and ad consent/ownership gates.");
    }
}







