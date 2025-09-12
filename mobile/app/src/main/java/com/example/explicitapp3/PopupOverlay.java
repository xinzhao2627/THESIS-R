package com.example.explicitapp3;

import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;

public class PopupOverlay {

    public PopupOverlay(LayoutInflater lf, String TAG, TextModel textModel, Context mcontext, WindowManager wm) {
        View view = lf.inflate(R.layout.activity_overlay, null);
        Button b = view.findViewById(R.id.tohomescreenButton);
        if (textModel != null) {
            textModel.setView(view);
        }
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.w(TAG, "onClick: Im clicked...");
                view.setVisibility(View.GONE);
                Intent homeintent = new Intent(Intent.ACTION_MAIN);
                homeintent.addCategory(Intent.CATEGORY_HOME);
                homeintent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                mcontext.startActivity(homeintent);

            }
        });

        DisplayMetrics displayMetrics = new DisplayMetrics();
        wm.getDefaultDisplay().getRealMetrics(displayMetrics);
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                displayMetrics.widthPixels,
                displayMetrics.heightPixels,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        |  // WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE |
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                        | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
                        | WindowManager.LayoutParams.FLAG_BLUR_BEHIND
                ,
                PixelFormat.TRANSLUCENT
        );
        params.setBlurBehindRadius(50);

        // init the imageview
        wm.addView(view, params);
    }
}
