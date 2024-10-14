package com.crossdrives.ui.chart.piechart;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import android.graphics.Paint;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class PieTextOutside extends View {
    private final String TAG = "CD.Pie";
    private Paint mPaint;
    private List<Integer> mColorList = new ArrayList<>();
    private List<Float> mRateList = new ArrayList<>();
    private int offset = 0;
    private float radius;
    private int centerPointRadius;
    private int xOffset;
    private int yOffset;
    private Rect textRect;
    private float showRateSize;
    private int startAngle;
    private Point lastPoint;
    Context mContext;


    public PieTextOutside(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        mColorList.add(Color.BLUE);mColorList.add(Color.RED);
        mRateList.add(50f/100);mRateList.add(50f/100);//float[] rate = {30f, 40f, 15f, 15f};

        mContext = context;
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        float density = displayMetrics.density;

        radius = 80 * density; //DP2PX.dip2px(mContext, 80);
        centerPointRadius = Math.round(2 * density) ;//DP2PX.dip2px(mContext, 2);
        xOffset = Math.round(20 * density);//DP2PX.dip2px(mContext, 20);
        yOffset = Math.round(5 * density);//DP2PX.dip2px(mContext, 5);
        showRateSize = Math.round(10 * density); //DP2PX.dip2px(mContext, 10);
        startAngle = 270;

        textRect = new Rect();

        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setColor(Color.RED);
        mPaint.setAntiAlias(true);
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setTextSize(showRateSize);

        Log.d(TAG,"default display size: " + displayMetrics.heightPixels + "," + displayMetrics.widthPixels + " density: " + displayMetrics.density);

        if (mRateList.size() > 0) {
            textRect = new Rect();
            mPaint.getTextBounds((mRateList.get(0) + "%"), 0, (mRateList.get(0) + "%").length(), textRect);
        }
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        List<Point> points = new ArrayList<>();

        //            Log.e("TAG", "startAngle=" + startAngle + "--sweepAngle=" + ((int) (mRateList.get(i) * (360)) - offset));
        Log.d(TAG, "text Rect w and h: " + textRect.width() + "," + textRect.height());
        for(int i = 0; i < mRateList.size(); i++){
            mPaint.setStyle(Paint.Style.FILL);
            mPaint.setColor(mColorList.get(i));
            RectF rectF = new RectF(0 + centerPointRadius + (xOffset + yOffset + textRect.width()),
                    0 + centerPointRadius + (xOffset + yOffset + textRect.height()),
                    2 * radius + centerPointRadius + (xOffset + yOffset + textRect.width()),
                    2 * radius + centerPointRadius + (xOffset + yOffset + textRect.height()));
            // To get size on each of a view's life cycle:
            // https://stackoverflow.com/questions/21926644/get-height-and-width-of-a-layout-programmatically
            Log.d(TAG, "RectF w and h: " + Float.toString(rectF.width()) + "," + Float.toString(rectF.height()));
            canvas.drawArc(rectF, startAngle, (int) (mRateList.get(i) * (360)) - offset, true, mPaint);
            startAngle = startAngle + (int) (mRateList.get(i) * 360);
            Point point = calcPoint(rectF, startAngle, (mRateList.get(i) * 360 - offset) / 2);
            points.add(point);
            dealRateText(canvas, point, i, points);
        }
    }

    private Point calcPoint(RectF rectF, float startAngle, float endAngle) {
        Path path = new Path();
        //通过Path类画一个90度（180—270）的内切圆弧路径
        path.addArc(rectF, startAngle, endAngle);

        PathMeasure measure = new PathMeasure(path, false);
//        Log.e("路径的测量长度:", "" + measure.getLength());

        float[] coords = new float[]{0f, 0f};
        //利用PathMeasure分别测量出各个点的坐标值coords
        int divisor = 1;
        measure.getPosTan(measure.getLength() / divisor, coords, null);
//        Log.e("coords:", "x轴:" + coords[0] + " -- y轴:" + coords[1]);
        float x = coords[0];
        float y = coords[1];
        Point point = new Point(Math.round(x), Math.round(y));
        return point;
    }

    private void dealRateText(Canvas canvas, Point point, int position, List<Point> pointList) {
        if (position == 0) {
            lastPoint = pointList.get(0);
        } else {
            lastPoint = pointList.get(position - 1);
        }
        float[] floats = new float[8];
        floats[0] = point.x;
        floats[1] = point.y;
        //右半圆
        if (point.x >= radius + centerPointRadius + (xOffset + yOffset + textRect.width())) {
            mPaint.setTextAlign(Paint.Align.LEFT);
            floats[6] = point.x + xOffset;
            //防止相邻的圆饼绘制的文字重叠显示
//            if (lastPoint != null) {
//                int absX = Math.abs(point.x - lastPoint.x);
//                int absY = Math.abs(point.y - lastPoint.y);
//                if (absX > 0 && absX < 20 && absY > 0 && absY < 20) {
//                    floats[6] = point.x + xOffset - textRect.width() / 2;
//                    Log.e("TAG", "右半圆");
//                } else {
//                    floats[6] = point.x + xOffset;
//                }
//            } else {
//                floats[6] = point.x + xOffset;
//            }
            if (point.y <= radius + centerPointRadius + (xOffset + yOffset + textRect.height())) {
                //右上角
                floats[2] = point.x + yOffset;
                floats[3] = point.y - yOffset;
                floats[4] = point.x + yOffset;
                floats[5] = point.y - yOffset;
                floats[7] = point.y - yOffset;
            } else {
                //右下角
                floats[2] = point.x + yOffset;
                floats[3] = point.y + yOffset;
                floats[4] = point.x + yOffset;
                floats[5] = point.y + yOffset;
                floats[7] = point.y + yOffset;
            }
            //左半圆
        } else {
            mPaint.setTextAlign(Paint.Align.RIGHT);
            floats[6] = point.x - xOffset;
            //防止相邻的圆饼绘制的文字重叠显示
//            if (lastPoint != null) {
//                int absX = Math.abs(point.x - lastPoint.x);
//                int absY = Math.abs(point.y - lastPoint.y);
//                if (absX > 0 && absX < 20 && absY > 0 && absY < 20) {
//                    floats[6] = point.x - xOffset - textRect.width() / 2;
//                    Log.e("TAG", "左半圆");
//                } else {
//                    floats[6] = point.x - xOffset;
//                }
//            } else {
//                floats[6] = point.x - xOffset;
//            }
            if (point.y <= radius + centerPointRadius) {
                //左上角
                floats[2] = point.x - yOffset;
                floats[3] = point.y - yOffset;
                floats[4] = point.x - yOffset;
                floats[5] = point.y - yOffset;
                floats[7] = point.y - yOffset;
            } else {
                //左下角
                floats[2] = point.x - yOffset;
                floats[3] = point.y + yOffset;
                floats[4] = point.x - yOffset;
                floats[5] = point.y + yOffset;
                floats[7] = point.y + yOffset;
            }
        }
        //根据每块的颜色，绘制对应颜色的折线
//        mPaint.setColor(mRes.getColor(colorList.get(position)));
        mPaint.setColor(Color.GRAY);    //mPaint.setColor(ContextCompat.getColor(mContext, R.color.color_081638));
        //画圆饼图每块边上的折线
        canvas.drawLines(floats, mPaint);

        mPaint.setStyle(Paint.Style.STROKE);
        //绘制显示的文字,需要根据类型显示不同的文字
        if (mRateList.size() > 0) {
            //Y轴：+ textRect.height() / 2 ,相对沿线居中显示
            canvas.drawText(getFormatPercentRate(mRateList.get(position) * 100) + "%", floats[6], floats[7] + textRect.height() / 2, mPaint);
        }
    }

    /**
     * 获取格式化的保留两位数的数
     */
    private String getFormatPercentRate(float dataValue) {
        DecimalFormat decimalFormat = new DecimalFormat(".00");//构造方法的字符格式这里如果小数不足2位,会以0补足.
        return decimalFormat.format(dataValue);
    }

    //A good article for the method: https://stackoverflow.com/questions/12266899/onmeasure-custom-view-explanation
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        Log.d(TAG, "spec size: " + widthSize + "," + heightSize);
        Log.d(TAG, "paddings: " + getPaddingLeft() + "," + getPaddingRight() + ";"
        + getPaddingTop() + "," + getPaddingBottom());
        if (heightMode == MeasureSpec.AT_MOST) {
            //边沿线和文字所占的长度：(xOffset + yOffset + textRect.width())
            heightSize = (int) (radius * 2) + 2 * centerPointRadius + getPaddingLeft() + getPaddingRight() + (xOffset + yOffset + textRect.height()) * 2;
        }
        if (widthMode == MeasureSpec.AT_MOST) {
            widthSize = (int) (radius * 2) + 2 * centerPointRadius + getPaddingLeft() + getPaddingRight() + (xOffset + yOffset + textRect.width()) * 2;
        }

        Log.d(TAG, "Calculated w and h: " + widthSize + "," + heightSize);
        //保存测量结果
        setMeasuredDimension(widthSize, heightSize);
    }
}
