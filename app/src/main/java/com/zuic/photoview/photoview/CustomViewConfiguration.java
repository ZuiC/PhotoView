package com.zuic.photoview.photoview;

import android.content.Context;
import android.nfc.Tag;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ViewConfiguration;

/**
 * Created by ZuiC on 2017/6/13.
 */

public class CustomViewConfiguration {
    private final String TAG = "CustomViewConfiguration";
    private static CustomViewConfiguration INSTANCE = null;

    private static final int LONGPRESS_TIMEOUT = ViewConfiguration.getLongPressTimeout();
    private static final int TAP_TIMEOUT = ViewConfiguration.getTapTimeout();
    private static final int DOUBLE_TAP_MIN_TIME = 40;
    private static final int DOUBLE_TAP_TIMEOUT = ViewConfiguration.getDoubleTapTimeout();

    private Context mContext;
    private int mDoubleTapSlopSquare;
    private int mTouchSlopSquare;
    private int mSpanSlop;


    public static CustomViewConfiguration getInstance(Context context){
        if (INSTANCE == null){
            INSTANCE = new CustomViewConfiguration(context);
        }
        return  INSTANCE;
    }

    private CustomViewConfiguration(Context context){
        mContext = context;
        init();
    }

    private void init(){
        int touchSlop, doubleTapSlop, doubleTapTouchSlop;
        final ViewConfiguration configuration = ViewConfiguration.get(mContext);

        touchSlop = configuration.getScaledTouchSlop();
        doubleTapSlop = configuration.getScaledDoubleTapSlop();

        mTouchSlopSquare = touchSlop * touchSlop;
        mDoubleTapSlopSquare = doubleTapSlop * doubleTapSlop;
        mSpanSlop = configuration.getScaledTouchSlop() * 2;
    }

    public boolean isInTapRegion(float currentX, float currentY,float firstDownX, float firstDownY){
        final int deltaX = (int) (currentX - firstDownX);
        final int deltaY = (int) (currentY - firstDownY);
        int distance = (deltaX * deltaX) + (deltaY * deltaY);
        if (distance > mTouchSlopSquare){
            return false;
        }
        return true;
    }

    public boolean isConsideredDoubleTap(MotionEvent firstDown, MotionEvent firstUp,
                                          MotionEvent secondDown){
        final long deltaTime = secondDown.getEventTime() - firstUp.getEventTime();

        if (deltaTime > DOUBLE_TAP_TIMEOUT || deltaTime < DOUBLE_TAP_MIN_TIME) {
            return false;
        }
        int deltaX = (int) firstDown.getX() - (int) secondDown.getX();
        int deltaY = (int) firstDown.getY() - (int) secondDown.getY();

        return (deltaX * deltaX + deltaY * deltaY < mDoubleTapSlopSquare);
    }


    public boolean isConsideredScaleCondition(float preSpan, float curSpan, int size){
        if (size < 2 || Math.abs(curSpan - preSpan) < 16){
            return false;
        }
        return true;
    }

}
