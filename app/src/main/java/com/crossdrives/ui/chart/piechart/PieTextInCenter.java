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
import android.util.Pair;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.crossdrives.R;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.stream.IntStream;

public class PieTextInCenter extends PieBase {
    private final String TAG = "CD.PieTextInCenter";
    private static PieTextInCenter instance;
    final private float radiusArc;
    final private float diameterArc;
    final private float radiusInnerCircle;
    final private float diameterInnerCircle;
    final private float ratioInnerCircle = 1.2f;
    final private float radiusIndicator;
    final private float diameterIndicator;
    final private int startAngleArc = 270;
    private int CoorCenterPieX, CoorCenterPieY; //coordinate X and Y center of pie
    private final int MAX_NO_ARC = 3;
    private final int[] colorsPie = new int[MAX_NO_ARC];

    private Collection<Item> mItems = new ArrayList<>();


    public PieTextInCenter(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        //Initialize style and size parameters
        radiusArc = mContext.getResources().getDimension(R.dimen.radiusArc);
        diameterArc = radiusArc*2;
        radiusInnerCircle = radiusArc/ratioInnerCircle;
        diameterInnerCircle = radiusInnerCircle*2;
        radiusIndicator = mContext.getResources().getDimension(R.dimen.indicatorRadius);
        diameterIndicator = radiusIndicator*2;
        CoorCenterPieX = getDisplayWidth()/2;
        CoorCenterPieY = Math.round(radiusArc);

        int[] attributes = {R.attr.colorPieArc1, R.attr.colorPieArc2, R.attr.colorPieArc3};
        TypedArray array = mContext.obtainStyledAttributes(R.style.AppTheme_NoActionBar, attributes);
        for (int i = 0; i< colorsPie.length; i++){
            colorsPie[i] = array.getColor(i, Color.BLACK);
        }
        array.recycle();

//        Item item1 = new Item();
//        item1.title = "TeraboxDrive"; item1.percentage = 0.5f; item1.subtitle = "15.8MB";
//        items.add(item1);
//        Item item2 = new Item();
//        item2.title = "Onedrive"; item2.percentage = 0.5f; item2.subtitle = "15.8MB";
//        items.add(item2);
    }

    @Override
    public void onDraw(@NonNull Canvas canvas) {

        drawArc(canvas);
        drawInnerCircle(canvas);
        writeAllocInfo(canvas);

    }

    public void clearBeforeAddItems(Collection<Item> items){
        mItems.clear();

        //Number of items overs the maximum?
        if(items.size() > MAX_NO_ARC){
            throw new IllegalArgumentException();
        }

        items.addAll(items);

    }
    public void addItems(Collection<Item> items){

        //Number of items overs the maximum?
        if((mItems.size() + items.size()) > MAX_NO_ARC){
            throw new IllegalArgumentException();
        }

        items.addAll(items);
    }

    private void drawArc(Canvas canvas){
        Paint paint = getPaint();

        RectF rectF = new RectF(CoorCenterPieX - radiusArc,0,
                CoorCenterPieX + radiusArc, CoorCenterPieY + radiusArc);

        List<Item> list = new ArrayList<>(mItems);
        Iterator<Item> iterator = list.iterator();
        int startAngle = this.startAngleArc;
        int swipeAngle;
        while(iterator.hasNext()){
            Item item = iterator.next();
            paint.setColor(colorsPie[list.indexOf(item)]);
            swipeAngle = Math.round(item.percentage*360);
            canvas.drawArc(rectF, startAngle, swipeAngle, true, paint);
            startAngle+=swipeAngle;
            Log.d(TAG, "startAngle:" + startAngle);
        }
    }

    private void drawInnerCircle(Canvas canvas){
        Paint paint = getPaint();
        //mPaint.setColor(ContextCompat.getColor(mContext, R.color.color_081638));
        Log.d(TAG, "Inner circle. Use default color. code: " + Integer.toHexString(paint.getColor()));
        canvas.drawCircle(CoorCenterPieX, CoorCenterPieY, radiusInnerCircle, paint);

    }
    private void writeAllocInfo(Canvas canvas){
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
        final int paddingIndicatorRight = Math.round(mContext.getResources().getDimension(R.dimen.indicatorPaddingRight));

        Log.d(TAG, "Pie title color: " + Integer.toHexString(colorTitle));
        Log.d(TAG, "Pie subtitle color: " + Integer.toHexString(colorSubtitle));
        //Log.d(TAG, "Pie title size: " + Integer.toString(size));

        Pair<Integer, Integer> centerAllocInfo = calcAllocInfoBlockStarts();
        Log.d(TAG, "StartY alloc detail block: " + centerAllocInfo.second);
        int nextTextStartY = centerAllocInfo.second;
        int nextTextBaseline;
        final int startXIndicator = centerAllocInfo.first;
        Log.d(TAG, "StartX indicator: " + startXIndicator);
        final int startXTitles = centerAllocInfo.first + Math.round(diameterIndicator) + paddingIndicatorRight;
        Log.d(TAG, "StartX titles: " + startXTitles);
        Rect rect = new Rect();
        Iterator<Item> iterator = mItems.iterator();
        Item item;
        String textSubtitle;
        int i = 0;
        while(iterator.hasNext()){
            item = iterator.next();

            //draw title
            paint.setColor(colorTitle);
            paint.setTextSize(textSizeTitle);
            //https://proandroiddev.com/android-and-typography-101-5f06722dd611

            nextTextBaseline = nextTextStartY + (-paint.getFontMetricsInt().top);  //top is a negative value
            Log.d(TAG, "Next baseline title: " + nextTextBaseline);
            canvas.drawText(item.title, startXTitles, nextTextBaseline, paint);

            //indicator
            paint.setColor(colorsPie[i]); i++;
            canvas.drawCircle(startXIndicator, nextTextBaseline, radiusIndicator, paint);

            //next Y for subtitle
            paint.getTextBounds(item.title, 0, item.title.length(), rect);
            nextTextStartY += rect.height() + paddingSubtitleTop;

            //draw subtitle
            paint.setColor(colorSubtitle);
            paint.setTextSize(textSizeSubtitle);
            textSubtitle = item.subtitle;
            nextTextBaseline = nextTextStartY + (-paint.getFontMetricsInt().top); //top is a negative value
            Log.d(TAG, "Next baseline Subtitle: " + nextTextBaseline);
            canvas.drawText(textSubtitle, startXTitles, nextTextBaseline, paint);

            //calculate Y for next title
            paint.getTextBounds(textSubtitle, 0, textSubtitle.length(), rect);
            nextTextStartY += rect.height() + paddingTitleTop;
        }
    }

    private Pair calcAllocInfoBlockStarts(){
        Paint paintTitle = getPaint();
        Paint paintSubtitle = getPaint();
        int textSizeTitle = Math.round(mContext.getResources().getDimension(R.dimen.pieTextTitleSize));
        int textSizeSubtitle = Math.round(mContext.getResources().getDimension(R.dimen.pieTextSubtitleSize));
        int paddingSubtitleTop = Math.round(mContext.getResources().getDimension(R.dimen.paddingSubTitleTop));
        int paddingtitleTop = Math.round(mContext.getResources().getDimension(R.dimen.paddingTitleTop));
        int paddingIndicatorRight = Math.round(mContext.getResources().getDimension(R.dimen.paddingTitleTop));

        paintTitle.setTextSize(textSizeTitle);
        paintSubtitle.setTextSize(textSizeSubtitle);
        int titlesHeightTotal = mItems.stream().mapToInt((item)->{
            Rect rect1 = new Rect();
            paintTitle.getTextBounds(item.title, 0, item.title.length(), rect1);
            Rect rect2 = new Rect();
            paintSubtitle.getTextBounds(item.subtitle, 0, item.subtitle.length(), rect2);
            return rect1.height() + rect2.height();
        }).sum();

        //height of whole text block. This will used to calculate the baseline of whole text block
        int numOfItem = (int) mItems.stream().count();
        int textBlockHeight = titlesHeightTotal +
                (numOfItem-1)*paddingtitleTop + numOfItem*paddingSubtitleTop;

        //Log.d(TAG, "CoorCenterY: " + CoorCenterY + " ;textBlockHeight: " + textBlockHeight);
        int startY = CoorCenterPieY - textBlockHeight/2;
        if(startY < 0){
            Log.w(TAG, "The text block height exceeds bound of inner circle");
            startY = 0;
        }

        int titlesWidthMax = IntStream.concat(
                mItems.stream().mapToInt((item)->{
                    Rect rect = new Rect();
                    paintTitle.getTextBounds(item.title, 0, item.title.length(), rect);
                    return rect.width();}),
                mItems.stream().mapToInt((item)->{
                    Rect rect = new Rect();
                    paintSubtitle.getTextBounds(item.subtitle, 0, item.subtitle.length(), rect);
                    return rect.width();})
        ).max().getAsInt();

        int ContentWidthMax = titlesWidthMax + paddingIndicatorRight + Math.round(diameterIndicator);
        int startX = CoorCenterPieX - ContentWidthMax/2;
        if(startX < 0){
            Log.w(TAG, "The text block width exceeds bound of inner circle");
            startX = 0;
        }

        return new Pair(startX ,startY);
    }

    //A good article for the method: https://stackoverflow.com/questions/12266899/onmeasure-custom-view-explanation
    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int widthMode = View.MeasureSpec.getMode(widthMeasureSpec);
        int heightMode = View.MeasureSpec.getMode(heightMeasureSpec);
        int widthSize = View.MeasureSpec.getSize(widthMeasureSpec);
        int heightSize = View.MeasureSpec.getSize(heightMeasureSpec);
        Pair<Integer,Integer> pair = getMeasuredDimention(widthMode, heightMode, widthSize, heightSize);

        setMeasuredDimension(pair.first, pair.second);
    }


    private Pair<Integer,Integer> getMeasuredDimention(int widthMode, int heightMode, int widthSize, int heightSize) {
        Log.d(TAG, "spec size: " + widthSize + "," + heightSize);


        if (heightMode == View.MeasureSpec.AT_MOST) {
            Log.d(TAG, "spec heightMode: AT_MOST");
            heightSize = Math.round(diameterArc) ;
        }else if(heightMode == View.MeasureSpec.EXACTLY){
            Log.d(TAG, "spec heightMode: EXACTLY");
            heightSize = Math.round(diameterArc) ;
        }else{
            Log.d(TAG, "spec heightMode: UNSPECIFIED");
            heightSize = Math.round(diameterArc) ;
        }

        if (widthMode == View.MeasureSpec.AT_MOST) {
            Log.d(TAG, "spec widthMode: AT_MOST");
            widthSize = getDisplayWidth();
        }else if(widthMode == View.MeasureSpec.EXACTLY){
            Log.d(TAG, "spec widthMode: EXACTLY");
            widthSize = getDisplayWidth();
        }else{
            Log.d(TAG, "spec widthMode: UNSPECIFIED");
            widthSize = getDisplayWidth();
        }

        Log.d(TAG, "Calculated w and h: " + widthSize + "," + heightSize);

        return new Pair(widthSize, heightSize);
    }
}
