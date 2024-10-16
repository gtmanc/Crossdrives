package com.crossdrives.ui.chart.piechart;

import android.content.Context;
import android.graphics.Canvas;
import android.util.DisplayMetrics;

import androidx.annotation.NonNull;

public abstract class IPie {
    Context mContext;
    DisplayMetrics displayMetrics;

    abstract void onDraw(@NonNull Canvas canvas);

    abstract void onMeasure(int widthMeasureSpec, int heightMeasureSpec);

    public IPie(Context context) {
        mContext = context;
        displayMetrics = mContext.getResources().getDisplayMetrics();
    }

    float getDensity(){return displayMetrics.density;}

    int getDisplayHeight() {return displayMetrics.heightPixels;    }
    int getDisplayWidth() {return displayMetrics.widthPixels;    }
}
