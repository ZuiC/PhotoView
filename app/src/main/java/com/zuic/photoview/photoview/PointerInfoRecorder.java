package com.zuic.photoview.photoview;

import android.content.Context;
import android.view.MotionEvent;
import android.view.ViewConfiguration;

/**
 * Created by ZuiC on 2017/6/13.
 */

public class PointerInfoRecorder {

    private float mDownFocusX, mDownFocusY;
    private float mLastFocusX, mLastFocusY;
    private int mPointerId;
    private boolean mIsInTapRegion;

    private PointerInfoRecorder(){

    }

    public static PointerInfoRecorder create(MotionEvent event){
        PointerInfoRecorder pointerInfoRecorder = new PointerInfoRecorder();
        pointerInfoRecorder.initPointerInfo(event);
        return pointerInfoRecorder;
    }

    private void initPointerInfo(MotionEvent event){
        final int index = event.getActionIndex();
        mDownFocusX = mLastFocusX = event.getX(index);
        mDownFocusY = mLastFocusY = event.getY(index);
        mIsInTapRegion = true;
        mPointerId = event.getPointerId(index);
    }


    public boolean getIsInTapRegion(){
        return mIsInTapRegion;
    }

    public void setIsInTapRegion(boolean isInTapRegion){
        mIsInTapRegion = isInTapRegion;
    }

    public float getLastFocusX(){
        return mLastFocusX;
    }

    public void setLastFocusX(float focusX){
        mLastFocusX = focusX;
    }

    public float getLastFocusY(){
        return mLastFocusY;
    }

    public void setLastFocusY(float focusY){
        mLastFocusY = focusY;
    }

    public float getDownFocusX(){
        return mDownFocusX;
    }

    public float getDownFocusY(){
        return mDownFocusY;
    }

}
