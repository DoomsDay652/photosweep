package com.dominic.photosweep;
import java.util.Arrays;
public class PhotoContactCheck {
    static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    public static void main(String[]args) {
        float[] edges=new float[1025],hit=new float[2];Arrays.fill(edges,24);
        check(PhotoContact.coverage(160,100,160,500,1,600,edges,hit)==0,"vertical movement away from teeth must not cut");
        check(PhotoContact.coverage(80,200,23,220,1,600,edges,hit)==1,"actual contact cuts without any pointer input");
        check(PhotoContact.coverage(200,200,-200,300,1,600,edges,hit)==1,"a fast crossing between frames cannot tunnel");
        check(PhotoContact.coverage(-200,100,180,500,1,600,edges,hit)==1,"reversing across the blade preserves the cut");
        check(PhotoContact.coverage(0,-40,0,-2,1,600,edges,hit)==0,"no damage above the stage");
        check(PhotoContact.coverage(0,620,0,700,1,600,edges,hit)==0,"no damage below the stage");
        check(PhotoContact.coverage(0,-40,0,640,1,600,edges,hit)==1,"swept contact across the full stage");
        Arrays.fill(edges,0);edges[513]=35;
        check(PhotoContact.coverage(30,0,30,600,1,600,edges,hit)==1,"narrow tooth tip cannot be skipped by a fast vertical swing");
        check(hit[0]>=30 && hit[1]>=0 && hit[1]<600,"fragment origin must be on the sampled collision surface");
        check(PhotoContact.coverage(80,0,80,600,1,600,edges,hit)==0,"near miss must not create damage");
        check(PhotoContact.coverage(0,0,0,0,1,0,edges,hit)==0,"unlaid-out stage is safe");
        float[] from={0,-200,250,200,0,200},to={0,200,50,-200,0,400},samples=new float[33*6];
        int count=PhotoContact.buildSweep(from,to,samples);
        // Both endpoints of this material corner miss the blade; the rotation arc crosses it.
        float smallest=Float.MAX_VALUE;
        for(int i=0;i<=count;i++)smallest=Math.min(smallest,samples[i*6]+samples[i*6+1]+samples[i*6+2]);
        check(count>1 && smallest<24,"rotating corner follows its full arc, not the endpoint chord");
        for(int i=0;i<6;i++)check(samples[i]==from[i] && samples[count*6+i]==to[i],"sweep preserves exact endpoint matrices");
        System.out.println("PASS: pointer-independent contact, reversal, fast diagonal/vertical sweeps, narrow teeth, off-stage misses.");
    }
}
