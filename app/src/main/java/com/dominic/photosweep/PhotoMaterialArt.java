package com.dominic.photosweep;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
/** Unscaled, transparent material photography. Shared by all live and departing cards. */
final class PhotoMaterialArt {
    private static PhotoMaterialArt cached;
    final Bitmap[] finishes,debris,candy;
    private PhotoMaterialArt(Context context) {
        finishes=tiles(context,R.drawable.swipe_material_finish_v2,3,2);
        debris=tiles(context,R.drawable.swipe_material_debris_v2,3,2);
        // The wide wrapper and tall lollipop cross the nominal grid gutters.
        // Explicit subject bounds prevent neighbouring sweets leaking into a finish.
        BitmapFactory.Options options=new BitmapFactory.Options();options.inScaled=false;
        Bitmap sheet=BitmapFactory.decodeResource(context.getResources(),R.drawable.swipe_candy_finish_v2,options);
        candy=new Bitmap[]{Bitmap.createBitmap(sheet,0,100,680,350),Bitmap.createBitmap(sheet,680,40,535,450),
                Bitmap.createBitmap(sheet,100,500,530,650),Bitmap.createBitmap(sheet,680,480,520,750)};
        finishes[3]=Bitmap.createBitmap(finishes[3],128,0,256,512);
    }
    static synchronized PhotoMaterialArt load(Context context) {
        if(cached==null)cached=new PhotoMaterialArt(context);return cached;
    }
    private static Bitmap[] tiles(Context context,int id,int cols,int rows) {
        BitmapFactory.Options options=new BitmapFactory.Options();options.inScaled=false;
        Bitmap sheet=BitmapFactory.decodeResource(context.getResources(),id,options);
        Bitmap[] result=new Bitmap[cols*rows];int w=sheet.getWidth()/cols,h=sheet.getHeight()/rows;
        for(int i=0;i<result.length;i++)result[i]=Bitmap.createBitmap(sheet,i%cols*w,i/cols*h,w,h);
        return result;
    }
    static int index(SwipeTheme.Kind kind) {
        switch(kind){case FIRE:return 0;case WATER:return 1;case ICE:return 2;case TOXIC:return 3;case SPACE:return 4;case CANDY:return 5;default:return -1;}
    }
}
