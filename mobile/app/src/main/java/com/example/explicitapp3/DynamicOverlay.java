package com.example.explicitapp3;

import static androidx.appcompat.content.res.AppCompatResources.getDrawable;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;

public class DynamicOverlay {


    public DynamicOverlay (LayoutInflater lf, int widthR, int heightR, Context mcontext, int x, int y) {
        Drawable drawable = getDrawable(mcontext, R.drawable.window_background);
        WindowManager windowManager = mcontext.getSystemService(WindowManager.class);
        View floating_view = new View(mcontext);
        floating_view.setBackgroundColor(Color.RED);

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                widthR,
                heightR,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.START;
        params.x = x;
        params.y = y;

        windowManager.addView(floating_view, params);


    }
}
