package com.zuic.photoview.photoview;

import android.view.MotionEvent;

/**
 * Created by ZuiC on 2017/6/5.
 */

public interface IOnCustomGestureListener {
    void onDoubleTap(MotionEvent ev);
    void onScroll(MotionEvent e1, MotionEvent e2, float scrollX, float scrollY);
    void onScale(float focusX, float focusY, float factor);
    void onPointerUp();
}
