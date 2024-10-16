package com.crossdrives.ui.chart.piechart;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.crossdrives.R;

public class Pie extends View {
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

    protected Paint getPaint(){
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.RED);
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.FILL);
        int[] attrs = {android.R.attr.textColor};
        //https://stackoverflow.com/questions/13719103/how-to-retrieve-style-attributes-programmatically-from-styles-xml

        TypedArray array = mContext.obtainStyledAttributes(R.style.ActionModeTitleTextStyle, attrs);
        paint.setTextSize(array.getColor(0, Color.BLACK));

        return paint;
    }
}
