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
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

public class PieTextInCenter extends IPie{
    private final String TAG = "CD.PieTextInCenter";
    private float radiusArc;
    private float radiusInnerCircle;
    private float radiusIndicator;
    private  int startAngle;
    private int CoorCenterX, CoorCenterY;
    private final int MAX_NO_ARC = 3;

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
        radiusArc = getDimensionInt(R.dimen.radiusArc) ;
        radiusInnerCircle = radiusArc/getDimensionInt(R.dimen.ratioInnerCircle);
        radiusIndicator = getDimensionInt(R.dimen.indicatorRadius);

        startAngle = getDimensionInt(R.dimen.startAngle);

        CoorCenterX = getDisplayWidth()/2;
        CoorCenterY = Math.round(radiusArc);

        Item item1 = new Item();
        item1.title = "Google"; item1.percentage = 50; item1.subtitle = "15.8MB";
        items.add(item1);
        Item item2 = new Item();
        item2.title = "Onedrive"; item2.percentage = 50; item2.subtitle = "15.8MB";
        items.add(item2);
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

        int[] attrs = {R.attr.colorPieArc1, R.attr.colorPieArc2, R.attr.colorPieArc3};
        TypedArray array = mContext.obtainStyledAttributes(R.style.AppTheme_NoActionBar, attrs);
        final int[] colorsPie = new int[MAX_NO_ARC];

        for (int i = 0; i< colorsPie.length; i++){
            colorsPie[i] = array.getColor(i, Color.BLACK);
        }
        array.recycle();

        RectF rectF = new RectF(CoorCenterX - radiusArc,0,
                CoorCenterX + radiusArc, CoorCenterY + radiusArc);

        List<Item> list = new ArrayList<>(items);
        Iterator<Item> iterator = list.iterator();
        int startAngle = this.startAngle;
        int swipeAngle;
        while(iterator.hasNext()){
            Item item = iterator.next();
            paint.setColor(colorsPie[list.indexOf(item)]);
            swipeAngle = item.percentage/100*360;
            canvas.drawArc(rectF, startAngle, swipeAngle, true, paint);
            startAngle+=swipeAngle;
        }
    }

    private void drawInnerCircle(Canvas canvas){
        Paint paint = getPaint();
        //mPaint.setColor(ContextCompat.getColor(mContext, R.color.color_081638));
        Log.d(TAG, "Inner circle. Use default color. code: " + Integer.toHexString(paint.getColor()));
        canvas.drawCircle(CoorCenterX, CoorCenterY, radiusInnerCircle, paint);

    }
    private void writeText(Canvas canvas){
        Paint paint = getPaint();
        paint.setTextAlign(Paint.Align.LEFT);

        //https://stackoverflow.com/questions/13719103/how-to-retrieve-style-attributes-programmatically-from-styles-xml
        int[] attrs = {R.attr.colorPieTextTitle, R.attr.colorPieTextSubtitle};
        TypedArray array = mContext.obtainStyledAttributes(R.style.AppTheme_NoActionBar, attrs);
        final int colorTitle = array.getColor(0, Color.BLACK);
        final int colorSubtitle = array.getColor(1, Color.BLACK);
        array.recycle();
        final int textSizeTitle = Math.round(mContext.getResources().getDimension(R.dimen.pieTextTitleSize));
        final int textSizeSubtitle = Math.round(mContext.getResources().getDimension(R.dimen.pieTextSubtitleSize));
        final int paddingSubtitleTop = Math.round(mContext.getResources().getDimension(R.dimen.paddingSubTitleTop));
        final int paddingTitleTop = Math.round(mContext.getResources().getDimension(R.dimen.paddingTitleTop));
        final int offsetX = Math.round(mContext.getResources().getDimension(R.dimen.textOffsetX));

        Log.d(TAG, "Pie title color: " + Integer.toHexString(colorTitle));
        Log.d(TAG, "Pie subtitle color: " + Integer.toHexString(colorSubtitle));
        //Log.d(TAG, "Pie title size: " + Integer.toString(size));

        final int startY = calcTextBlockStartY();
        Log.d(TAG, "StartY text block: " + startY);
        int nextTextStartY = startY;
        int nextTextBaseline;
        final int StartX = CoorCenterX - offsetX;
        Rect rect = new Rect();
        Iterator<Item> iterator = items.iterator();
        Item item;
        String textSubtitle;
        while(iterator.hasNext()){
            item = iterator.next();

            //draw title
            paint.setColor(colorTitle);
            paint.setTextSize(textSizeTitle);
            //https://proandroiddev.com/android-and-typography-101-5f06722dd611

            nextTextBaseline = nextTextStartY + (-paint.getFontMetricsInt().top);  //top is a negative value
            Log.d(TAG, "Next baseline title: " + nextTextBaseline);
            canvas.drawText(item.title, StartX, nextTextBaseline, paint);

            //indicator
            paint.setColor(Color.BLUE);
            canvas.drawCircle(StartX - radiusIndicator*2 - 12*getDensity(), nextTextBaseline, radiusIndicator, paint);

            //next Y for subtitle
            paint.getTextBounds(item.title, 0, item.title.length(), rect);
            nextTextStartY += rect.height() + paddingSubtitleTop;

            //draw subtitle
            paint.setColor(colorSubtitle);
            paint.setTextSize(textSizeSubtitle);
            textSubtitle = item.subtitle;
            nextTextBaseline = nextTextStartY + (-paint.getFontMetricsInt().top); //top is a negative value
            Log.d(TAG, "Next baseline Subtitle: " + nextTextBaseline);
            canvas.drawText(textSubtitle, StartX, nextTextBaseline, paint);

            //calculate Y for next title
            paint.getTextBounds(textSubtitle, 0, textSubtitle.length(), rect);
            nextTextStartY += rect.height() + paddingTitleTop;
        }
    }

    private int calcTextBlockStartY(){
        Paint paint = getPaint();
        int textSizeTitle = Math.round(mContext.getResources().getDimension(R.dimen.pieTextTitleSize));
        int textSizeSubtitle = Math.round(mContext.getResources().getDimension(R.dimen.pieTextSubtitleSize));
        int paddingSubtitleTop = Math.round(mContext.getResources().getDimension(R.dimen.paddingSubTitleTop));
        int paddingtitleTop = Math.round(mContext.getResources().getDimension(R.dimen.paddingTitleTop));

        int titlesHeightTotal = items.stream().mapToInt((item)->{
            Rect rect1 = new Rect();
            paint.setTextSize(textSizeTitle);
            paint.getTextBounds(item.title, 0, item.title.length(), rect1);
            Rect rect2 = new Rect();
            paint.setTextSize(textSizeSubtitle);
            paint.getTextBounds(item.subtitle, 0, item.subtitle.length(), rect2);
            return rect1.height() + rect2.height();
        }).sum();

        //height of whole text block. This will used to calculate the baseline of whole text block
        int numOfItem = (int)items.stream().count();
        int textBlockHeight = titlesHeightTotal +
                (numOfItem-1)*paddingtitleTop + numOfItem*paddingSubtitleTop;

        Log.d(TAG, "CoorCenterY: " + CoorCenterY + " ;textBlockHeight: " + textBlockHeight);
        int startY = CoorCenterY - textBlockHeight/2;
        if(startY < 0){
            Log.d(TAG, "The text block height exceeds arc");
            startY = 0;
        }
        return startY;
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

    }

    private int getDimensionInt(int id){
        return Math.round(mContext.getResources().getDimension(id));
    }
}
