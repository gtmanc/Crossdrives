package com.crossdrives.ui.chart.piechart;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.crossdrives.R;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HexFormat;

public class PieTextInCenter extends IPie{
    private final String TAG = "CD.PieTextInCenter";
    private float radiusArc;
    private float radiusInnerCircle;
    private  int startAngle;
    private int swipeAngle = 360;
    private float textSize;
    private int CoorCenterX, CoorCenterY;

    public class Item{
        int percentage; //e.g. 40 = 40/100
        String title;
        String subtitle;
        String content[];
    }

    private Collection<Item> items = new ArrayList<>();

    public PieTextInCenter(Context context, @Nullable AttributeSet attrs) {
        super(context);

        float density = getDensity();

        //Radius outer and inner
        radiusArc = 120 * density;
        radiusInnerCircle = radiusArc/1.3f;

        startAngle = 270;
        textSize = Math.round(10 * density);

        CoorCenterX = getDisplayWidth()/2;
        CoorCenterY = Math.round(radiusArc);
    }

    @Override
    public void onDraw(@NonNull Canvas canvas) {

        drawArc(canvas);
        drawInnerCircle(canvas);
        writeText(canvas);

    }

    public void addItems(Collection<Item> items){
        if(items.stream().mapToInt((item)-> item.percentage).sum() != 100 ){
            throw new IllegalArgumentException();
        }
        items.addAll(items);
    }

    private void drawArc(Canvas canvas){
        Paint paint = getPaint();
        paint.setColor(Color.RED);
        RectF rectF = new RectF(CoorCenterX - radiusArc,0,
                CoorCenterX + radiusArc, CoorCenterY + radiusArc);

        canvas.drawArc(rectF, startAngle, (int) (1 * (swipeAngle)), true, paint);
    }

    private void drawInnerCircle(Canvas canvas){
        Paint paint = getPaint();
        //mPaint.setColor(ContextCompat.getColor(mContext, R.color.color_081638));
        Log.d(TAG, "Inner circle. Use default color. code: " + paint.getColor());
        canvas.drawCircle(CoorCenterX, CoorCenterY, radiusInnerCircle, paint);

    }
    private void writeText(Canvas canvas){
        Paint paint = getPaint();
        paint.setTextAlign(Paint.Align.RIGHT);

        //https://stackoverflow.com/questions/13719103/how-to-retrieve-style-attributes-programmatically-from-styles-xml
        int[] attrs = {R.attr.colorPieTextTitle, R.attr.colorPieTextSubtitle};
        TypedArray array = mContext.obtainStyledAttributes(R.style.AppTheme_NoActionBar, attrs);
        int colorTitle = array.getColor(0, Color.BLACK);
        int colorSubtitle = array.getColor(1, Color.BLACK);
        int sizeTitle = Math.round(mContext.getResources().getDimension(R.dimen.pieTextTitleSize));
        int sizeSubtitle = Math.round(mContext.getResources().getDimension(R.dimen.pieTextSubtitleSize));
        int paddingSubtitleTop = Math.round(mContext.getResources().getDimension(R.dimen.paddingSubTitleTop));
        int paddingtitleTop = Math.round(mContext.getResources().getDimension(R.dimen.paddingTitleTop));
//        int size = array.getInteger(1, 8);
        paint.setColor(colorTitle);
        paint.setTextSize(sizeTitle);
        Log.d(TAG, "Pie title color: " + Integer.toHexString(colorTitle));
        Log.d(TAG, "Pie subtitle color: " + Integer.toHexString(colorSubtitle));
        //Log.d(TAG, "Pie title size: " + Integer.toString(size));
        canvas.drawText("Google", CoorCenterX, CoorCenterY, paint);
        Rect rect1 = new Rect();
        paint.getTextBounds("Google", 0, "Google".length(), rect1);

        //Sub title
        paint.setColor(colorSubtitle);
        paint.setTextSize(sizeSubtitle);
        canvas.drawText("15.8MB", CoorCenterX, CoorCenterY+rect1.height()+paddingSubtitleTop, paint);
        Rect rect2 = new Rect();
        paint.getTextBounds("15.8MB", 0, "15.8MB".length(), rect2);

        //2nd Main title
        paint.setColor(colorTitle);
        paint.setTextSize(sizeTitle);
        canvas.drawText("OneDrive", CoorCenterX, CoorCenterY+
                rect1.height()+paddingSubtitleTop+
                rect2.height()+paddingtitleTop,
                paint);
    }

    private int calcTextBlockStartY(){
        Paint paint = getPaint();
        int textSizeTitle = Math.round(mContext.getResources().getDimension(R.dimen.pieTextTitleSize));
        int textSizeSubtitle = Math.round(mContext.getResources().getDimension(R.dimen.pieTextSubtitleSize));
        int paddingSubtitleTop = Math.round(mContext.getResources().getDimension(R.dimen.paddingSubTitleTop));
        int paddingtitleTop = Math.round(mContext.getResources().getDimension(R.dimen.paddingTitleTop));

        int titleHeightTotal = items.stream().mapToInt((item)->{
            Rect rect1 = new Rect();
            paint.getTextBounds(item.title, 0, item.title.length(), rect1);
            Rect rect2 = new Rect();
            paint.getTextBounds(item.title, 0, item.title.length(), rect2);
            return rect1.height() + rect2.height();
        }).sum();

        //height of whole text block
        int numOfItem = (int)items.stream().count();
        int textBlockHeight = titleHeightTotal +
                (numOfItem-1)*paddingtitleTop +
                numOfItem*(textSizeTitle + textSizeSubtitle + paddingSubtitleTop);

        return CoorCenterY - textBlockHeight/2;
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

    }
}
