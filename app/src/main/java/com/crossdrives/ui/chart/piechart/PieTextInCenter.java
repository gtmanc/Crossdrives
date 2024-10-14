package com.crossdrives.ui.chart.piechart;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class PieTextInCenter extends IPie{
    Rect rectArc;
    float radius;

    public PieTextInCenter(Context context, @Nullable AttributeSet attrs) {
        super(context);

        float density = getDesity();
        radius = 80 * density;
    }

    @Override
    public void onDraw(@NonNull Canvas canvas) {

    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

    }
}
