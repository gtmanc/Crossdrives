package com.crossdrives.ui.chart;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.graphics.Paint;

import java.util.ArrayList;
import java.util.List;

public class Pie extends View {
    private Paint mPaint;
    private List<Integer> mColorList = new ArrayList<>();
    private List<Integer> mRateList = new ArrayList<>();
    private int offset = 0;
    private float radius;
    private int centerPointRadius;
    private int xOffset;
    private int yOffset;
    private Rect textRect;
    private float showRateSize;

    public Pie(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        mColorList.add(Color.BLUE);
        mRateList.add(30);    //float[] rate = {30f, 40f, 15f, 15f};
        radius = 240; //DP2PX.dip2px(mContext, 80);
        centerPointRadius = 6;//DP2PX.dip2px(mContext, 2);
        xOffset = 60;//DP2PX.dip2px(mContext, 20);
        yOffset = 15;//DP2PX.dip2px(mContext, 5);
        showRateSize = 30; //DP2PX.dip2px(mContext, 10);
        
        textRect = new Rect();

        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setColor(Color.RED);
        mPaint.setAntiAlias(true);
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setTextSize(showRateSize);

        if (mRateList.size() > 0) {
            textRect = new Rect();
            mPaint.getTextBounds((mRateList.get(0) + "%"), 0, (mRateList.get(0) + "%").length(), textRect);
        }
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setColor(mColorList.get(0));
//            Log.e("TAG", "startAngle=" + startAngle + "--sweepAngle=" + ((int) (mRateList.get(i) * (360)) - offset));

        RectF rectF = new RectF(0 + centerPointRadius + (xOffset + yOffset + textRect.width()),
                0 + centerPointRadius + (xOffset + yOffset + textRect.height()), 2 * radius + centerPointRadius + (xOffset + yOffset + textRect.width()), 2 * radius + centerPointRadius + (xOffset + yOffset + textRect.height()));
        int startAngle = 0;

        canvas.drawArc(rectF, startAngle, (int) (mRateList.get(0) * (360)) - offset, true, mPaint);
    }
}
