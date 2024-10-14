package com.crossdrives.ui.chart.piechart;

import android.content.Context;
import android.graphics.Canvas;
import android.util.DisplayMetrics;

import androidx.annotation.NonNull;

public abstract class IPie {
    Context mContext;

    abstract void onDraw(@NonNull Canvas canvas);

    abstract void onMeasure(int widthMeasureSpec, int heightMeasureSpec);

    public IPie(Context context) {
        mContext = context;
    }

    float getDesity(){
        DisplayMetrics displayMetrics = mContext.getResources().getDisplayMetrics();
        return displayMetrics.density;
    }
}
