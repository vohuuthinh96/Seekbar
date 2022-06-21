package com.example.myapplication;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;

public class Utils {
    public static int dpToPx(Context context, int dp) {
        return (int) (dp * context.getResources().getDisplayMetrics().density + 0.5f);
    }

    public static Bitmap getResizedBitmap(Bitmap bm, int newWidth) {
        int width = bm.getWidth();
        int height = bm.getHeight();
        float scaleWidth = ((float) newWidth) / width;
        Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth, scaleWidth);
        Bitmap resizedBitmap = Bitmap.createBitmap(
                bm, 0, 0, width, height, matrix, false);
        bm.recycle();
        if (resizedBitmap.getWidth() >= resizedBitmap.getHeight()) {

            resizedBitmap = Bitmap.createBitmap(
                    resizedBitmap,
                    resizedBitmap.getWidth() - resizedBitmap.getHeight(),
                    0,
                    resizedBitmap.getHeight(),
                    resizedBitmap.getHeight()
            );
        } else {

            resizedBitmap = Bitmap.createBitmap(
                    resizedBitmap,
                    0,
                    resizedBitmap.getHeight() - resizedBitmap.getWidth(),
                    resizedBitmap.getWidth(),
                    resizedBitmap.getWidth()
            );
        }
        return resizedBitmap;
    }

    public static boolean isBitmapExist(Bitmap bitmap) {
        return bitmap != null && !bitmap.isRecycled();
    }
}

