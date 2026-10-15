package com.dominic.photosweep;

public class TokenReelCheck {
    static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    public static void main(String[] args) {
        check(TokenReelMotion.duration(0)<TokenReelMotion.duration(1) && TokenReelMotion.duration(1)<TokenReelMotion.duration(2),"reels stop in sequence");
        for(int token=0;token<ThemeProgression.REWARD_TYPES;token++)for(int reel=0;reel<3;reel++)for(int prior=0;prior<ThemeProgression.REWARD_TYPES;prior++) {
            int target=TokenReelMotion.target(prior,token,reel);
            check(target>prior+15,"reel travels several complete turns");
            check(TokenReelMotion.symbol(target)==token,"all reels settle on the banked token");
            check(TokenReelMotion.position(prior,target,0,reel)==prior,"reel starts without a jump");
            check(TokenReelMotion.position(prior,target,10000,reel)==target,"reel ends at exact target");
            float previous=prior;
            for(int time=0;time<3000;time+=16) {
                float value=TokenReelMotion.position(prior,target,time,reel);
                check(value>=previous && value<=target,"reel never reverses or overshoots");
                previous=value;
            }
            float d=TokenReelMotion.duration(reel);
            float early=TokenReelMotion.position(prior,target,100,reel)-prior;
            float late=target-TokenReelMotion.position(prior,target,d-100,reel);
            check(early>late,"reel slows gently before stopping");
        }
        check(TokenReelMotion.symbol(-1)==ThemeProgression.REWARD_TYPES-1,"offscreen symbols wrap safely");
        System.out.println("PASS: staggered reel timing, exact reward alignment, smooth forward travel and deceleration.");
    }
}

