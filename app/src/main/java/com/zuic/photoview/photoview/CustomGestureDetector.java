package com.zuic.photoview.photoview;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;

import java.util.HashMap;
import java.util.Map;


/**
 * Created by ZuiC on 2017/6/13.
 */

public class CustomGestureDetector implements View.OnTouchListener{

    private final static String TAG = "CustomGestureDetector";


    private Map<Integer, PointerInfoRecorder> mPointerInfoRecorder;
    private MotionEvent mLastDownMotionEvent,mLastUpMotionEvent;
    private Boolean isTappedBefore;

    // constants for Message.what used by GestureHandler below
    private static final int SHOW_PRESS = 1;
    private static final int LONG_PRESS = 2;
    private static final int TAP = 3;

    private VelocityTracker mVelocityTracker;
    private CustomViewConfiguration mCustomViewConfiguration;
    private IOnCustomGestureListener mIOnGestureListener;

    private float mInitSpan, mPreSpan, mCurSpan;
    private int mScaleFirstOrScrollFirstFlag = 0;

    private Handler mHandler;
    private Context mContext;

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        processTouchEvent(event);
        return true;
    }

    public CustomGestureDetector(View view){
        mContext = view.getContext();
        init();
    }

    private void init(){
        mPointerInfoRecorder = new HashMap<>();
        mHandler = new GestureHandler();
        mCustomViewConfiguration = CustomViewConfiguration.getInstance(mContext);
    }

    private void processTouchEvent(MotionEvent event){

        final int action = event.getActionMasked();

        if (mVelocityTracker == null){
            mVelocityTracker = VelocityTracker.obtain();
        }
        mVelocityTracker.addMovement(event);

        switch (action){
            case MotionEvent.ACTION_DOWN:
                Log.e(TAG,"ActionDown");
                processActionDown(event);
                break;
            case MotionEvent.ACTION_MOVE:
                processActionMove(event);
                break;
            case MotionEvent.ACTION_UP:
                Log.e(TAG, "ActionUp");
                processActionUp(event);
                break;
            case MotionEvent.ACTION_POINTER_UP:
                Log.e(TAG, "PointerUp");
                processActionPointerUp(event);
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                Log.e(TAG, "PointerDown");
                processActionPointerDown(event);
                break;
        }
    }

    private void processActionDown(MotionEvent event){
        final int index = event.getActionIndex();
        if (index == 0 || index == 1){
            mPointerInfoRecorder.put(event.getPointerId(index),
                    PointerInfoRecorder.create(event));
        }

        if (mLastDownMotionEvent != null && mLastUpMotionEvent != null && isTappedBefore
                && mCustomViewConfiguration.isConsideredDoubleTap(mLastDownMotionEvent
                        , mLastUpMotionEvent, event)){
            isTappedBefore = false;
            mIOnGestureListener.onDoubleTap(event);
        }else {
            isTappedBefore = true;
        }

        if (mLastDownMotionEvent != null){
            mLastDownMotionEvent.recycle();
        }
        mLastDownMotionEvent = MotionEvent.obtain(event);
/*        mHandler.sendEmptyMessageAtTime(LONG_PRESS, mLastDownMotionEvent.getDownTime()
                + 1 + 1);*/
    }

    private void processActionMove(MotionEvent event){
        final int size =  mPointerInfoRecorder.size();
        if (size <= 0){
            return;
        }

        updateSpanValue(event, false);

        if (mCustomViewConfiguration.isConsideredScaleCondition(mPreSpan, mCurSpan, size) &&
                mScaleFirstOrScrollFirstFlag != 2){
            processScaleCondition(event);
        }else {
            processScrollCondition(event);
        }

        for (int i = 0; i < mPointerInfoRecorder.size(); i++){
            mPointerInfoRecorder.get(event.getPointerId(i)).setLastFocusX(event.getX(i));
            mPointerInfoRecorder.get(event.getPointerId(i)).setLastFocusY(event.getY(i));
        }
    }

    private void processActionUp(MotionEvent event){
        if (mPointerInfoRecorder.size() != 0){
            mIOnGestureListener.onPointerUp();
            mScaleFirstOrScrollFirstFlag = 0;
        }
        mPointerInfoRecorder.clear();
        mHandler.removeMessages(LONG_PRESS);
        if (mLastUpMotionEvent != null) {
            mLastUpMotionEvent.recycle();
        }
        mLastUpMotionEvent = MotionEvent.obtain(event);
        if (mVelocityTracker != null) {
            mVelocityTracker.recycle();
            mVelocityTracker = null;
        }

    }

    private void processActionPointerUp(MotionEvent event){
        final int index = event.getActionIndex();
        if (index == 0 || index == 1){
            mPointerInfoRecorder.remove(event.getPointerId(index));
            mIOnGestureListener.onPointerUp();
            if (mPointerInfoRecorder.size() == 0){
                mScaleFirstOrScrollFirstFlag = 0;
            }
        }
    }

    private void processActionPointerDown(MotionEvent event){
        final int index = event.getActionIndex();
        if (index == 0 || index == 1){
            mPointerInfoRecorder.put(event.getPointerId(index), PointerInfoRecorder.create(event));
        }
        updateSpanValue(event, true);
    }

    private void processScaleCondition(MotionEvent event){
        final int pointerId_Index0 = event.getPointerId(0);
        final int pointerId_Index1 = event.getPointerId(1);

        if (mScaleFirstOrScrollFirstFlag == 0){
            mScaleFirstOrScrollFirstFlag = 1;
        }

        final float focusX = (mPointerInfoRecorder.get(pointerId_Index0).getLastFocusX() +
                mPointerInfoRecorder.get(pointerId_Index1).getLastFocusX()) / 2.0F;
        final float focusY = (mPointerInfoRecorder.get(pointerId_Index0).getLastFocusY() +
                mPointerInfoRecorder.get(pointerId_Index1).getLastFocusY()) / 2.0F;
        final float factor = mPreSpan > 0 ? (mCurSpan / mPreSpan) : 1;

        mPreSpan = mCurSpan;
        mIOnGestureListener.onScale(focusX, focusY, factor);

    }

    private void processScrollCondition(MotionEvent event){
        float sumX = 0, sumY = 0;

        for (int i = 0; i < mPointerInfoRecorder.size(); i++){
            final int pointerId = event.getPointerId(i);
            if (mPointerInfoRecorder.get(pointerId).getIsInTapRegion()){
                if (!mCustomViewConfiguration.isInTapRegion(event.getX(), event.getY(),
                        mPointerInfoRecorder.get(pointerId).getDownFocusX(), mPointerInfoRecorder.get(pointerId).getDownFocusY())){
                    mPointerInfoRecorder.get(pointerId).setIsInTapRegion(false);
                    if (mScaleFirstOrScrollFirstFlag == 0){
                        mScaleFirstOrScrollFirstFlag = 2;
                    }
                }else {
                    return;
                }
            }
            sumX += event.getX(i) - mPointerInfoRecorder.get(pointerId).getLastFocusX();
            sumY += event.getY(i) - mPointerInfoRecorder.get(pointerId).getLastFocusY();
        }

        if (mPointerInfoRecorder.size() == 2){
            sumX = sumX / 2.0F;
            sumY = sumY / 2.0F;
        }
        mIOnGestureListener.onScroll(mLastDownMotionEvent, event, sumX, sumY);
    }

    private void updateSpanValue(MotionEvent event, boolean isInit){
        if (mPointerInfoRecorder.size() != 2){
            return;
        }
        final float spanX, spanY;
        final int pointerId_Index0 = event.getPointerId(0);
        final int pointerId_Index1 = event.getPointerId(1);

        if (isInit){
            spanX = Math.abs(mPointerInfoRecorder.get(pointerId_Index0).getDownFocusX() -
                    mPointerInfoRecorder.get(pointerId_Index1).getDownFocusX());
            spanY = Math.abs(mPointerInfoRecorder.get(pointerId_Index0).getDownFocusY() -
                    mPointerInfoRecorder.get(pointerId_Index1).getDownFocusY());
            mPreSpan = mCurSpan = mInitSpan = (float) Math.hypot(spanX, spanY);
        }else {
            spanX = Math.abs(mPointerInfoRecorder.get(pointerId_Index0).getLastFocusX() -
                    mPointerInfoRecorder.get(pointerId_Index1).getLastFocusX());
            spanY = Math.abs(mPointerInfoRecorder.get(pointerId_Index0).getLastFocusY() -
                    mPointerInfoRecorder.get(pointerId_Index1).getLastFocusY());
            mCurSpan = (float) Math.hypot(spanX, spanY);
        }
    }

    public void setOnGestureListener(IOnCustomGestureListener iOnGestureListener){
        mIOnGestureListener = iOnGestureListener;
    }

    private class GestureHandler extends Handler {
        GestureHandler() {
            super();
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case SHOW_PRESS:
                    break;
                case LONG_PRESS:
                    break;
                case TAP:
                    break;
                default:
                    throw new RuntimeException("Unknown message " + msg); //never
            }
        }
    }
}
