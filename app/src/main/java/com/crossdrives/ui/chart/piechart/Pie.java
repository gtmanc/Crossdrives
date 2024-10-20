package com.crossdrives.ui.chart.piechart;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class Pie extends View {
    final String TAG = "CD.Pie";
    IPie chart;
    Context mContext;

    public Pie(Context context, @Nullable AttributeSet attrs){
        super(context, attrs);

        mContext = context;
        //which pie chart we like to use?
        chart = new PieTextInCenter(context, attrs);// PieTextOutside(context, attrs)

    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        chart.onDraw(canvas);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        chart.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }


}
