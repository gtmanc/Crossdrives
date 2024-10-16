package com.crossdrives.ui.chart.piechart;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class PieTextInCenter extends IPie{
    private final String TAG = "CD.PieTextInCenter";
    Rect rectArc;
    float radiusOuter;
    float radiusInner;
    int startAngle;
    private float textSize;

    public PieTextInCenter(Context context, @Nullable AttributeSet attrs) {
        super(context);

        float density = getDensity();

        //Radius outer and inner
        radiusOuter = 160 * density;
        radiusInner = 120 * density;

        startAngle = 270;
        textSize = Math.round(10 * density);
    }

    @Override
    public void onDraw(@NonNull Canvas canvas) {

        drawOuter(canvas);



        mPaint.setColor(ContextCompat.getColor(mContext, R.color.color_081638));
        mPaint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(radius + centerPointRadius + (xOffset + yOffset + textRect.width()), radius + centerPointRadius + (xOffset + yOffset + textRect.height()), radius / 1.5f, mPaint);


    }

    private void drawOuter(Canvas canvas){
        Paint paint = new Paint();
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.RED);
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.FILL);
        paint.setTextSize(textSize);
        RectF rectF = new RectF(getDisplayWidth()/2- radiusOuter,0,
                getDisplayWidth()/2+ radiusOuter, radiusOuter *2);

        canvas.drawArc(rectF, startAngle, (int) (1 * (360)), true, paint);
    }

    private void drawInner(Canvas canvas){
        mPaint.setColor(ContextCompat.getColor(mContext, R.color.color_081638));
        mPaint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(radius + centerPointRadius + (xOffset + yOffset + textRect.width()), radius + centerPointRadius + (xOffset + yOffset + textRect.height()), radius / 1.5f, mPaint);

    }
    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

    }
}
