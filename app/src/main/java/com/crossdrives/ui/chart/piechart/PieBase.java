package com.crossdrives.ui.chart.piechart;

import android.content.Context;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Pair;
import android.view.View;

import androidx.annotation.Nullable;

public abstract class PieBase extends View {
    Context mContext;
    DisplayMetrics displayMetrics;

    public PieBase(Context context,  @Nullable AttributeSet attrs) {
        super(context, attrs);
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
