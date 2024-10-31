package com.crossdrives.ui.chart.piechart;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.DisplayMetrics;
import android.util.Pair;
import android.view.View;

import androidx.annotation.NonNull;

public abstract class IPie {
    Context mContext;
    DisplayMetrics displayMetrics;

    abstract void onDraw(@NonNull Canvas canvas);

    abstract void onMeasure(int widthMeasureSpec, int heightMeasureSpec, View v);

    abstract Pair<Integer,Integer> getMeasuredDimention(int widthMode, int heightMode, int widthSize, int heightSize);

    public IPie(Context context) {
        mContext = context;
        displayMetrics = mContext.getResources().getDisplayMetrics();
    }

    protected Paint getPaint(){
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.FILL);

//        paint.setColor(Color.RED);
        return paint;
    }

    float getDensity(){return displayMetrics.density;}

    int getDisplayHeight() {return displayMetrics.heightPixels;    }
    int getDisplayWidth() {return displayMetrics.widthPixels;    }
}
