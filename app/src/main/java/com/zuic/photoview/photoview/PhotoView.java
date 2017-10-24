package com.zuic.photoview.photoview;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.ImageView;



/**
 * Created by ZuiC on 2017/6/1.
 */

public class PhotoView extends android.support.v7.widget.AppCompatImageView{

    private final String TAG = "PhotoView";

    private final float MAXIMUM_SCALE = 2.3F;
    private final float SECOND_MAXIMUM_SCALE = 1.9F;

    private boolean isInitial = false;
    private boolean isOnAdjusting = false;
    private Matrix mSuppMatrix = new Matrix();
    private Matrix mBaseMatrix = new Matrix();
    private Matrix mDrawMatrix = new Matrix();
    private RectF mDisplayRect = new RectF();


    private float mBaseWidth, mBaseHeight;
    // Gesture Detectors
    private CustomGestureDetector mCustomGestureDetector = new CustomGestureDetector(this);


    public PhotoView(Context context) {
        super(context);
        this.setScaleType(ScaleType.MATRIX);
        init();
    }

    public PhotoView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.setScaleType(ScaleType.MATRIX);
        init();
    }

    public PhotoView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.setScaleType(ScaleType.MATRIX);
        init();
    }

    @Override
    protected boolean setFrame(int l, int t, int r, int b) {
        boolean changed = super.setFrame(l, t, r, b);
        if (changed) {
            updateBaseMatrix();
        }
        return changed;
    }

    private void init(){
        setGestureDetector();
    }

    private void setGestureDetector(){
        this.setOnTouchListener(mCustomGestureDetector);
        mCustomGestureDetector.setOnGestureListener(new IOnCustomGestureListener() {
            @Override
            public void onDoubleTap(MotionEvent e) {
                Log.e(TAG, isOnAdjusting + " ");
                if (!isOnAdjusting){
                    processOnDoubleTapCondition(e);
                }
            }

            @Override
            public void onScroll(MotionEvent e1, MotionEvent e2, float scrollX, float scrollY) {
                if (!isOnAdjusting){
                    processOnScrollCondition(scrollX, scrollY);
                }
            }

            @Override
            public void onScale(float focusX, float focusY, float factor) {
                if (!isOnAdjusting){
                    processScaleCondition(focusX, focusY, factor);
                }
            }

            @Override
            public void onPointerUp() {
                if (!isOnAdjusting){
                    processOnPointerUp();
                }
            }
        });
    }

    private void processOnDoubleTapCondition(MotionEvent event){
        if (!isInitial){
            resetMatrix();
            isInitial = true;
            return;
        }

        mSuppMatrix.setScale(SECOND_MAXIMUM_SCALE, SECOND_MAXIMUM_SCALE);
        final RectF rect = getDisplayRect(getDrawMatrix());
        if (rect == null) {
            return;
        }
        final float centerX = mBaseWidth / 2.0F;
        final float disFromFocusToCenterX = event.getX() * SECOND_MAXIMUM_SCALE - centerX;
        final float deltaX, deltaY;
        final float disFromDrawBoundToImageViewX;

        if (disFromFocusToCenterX >= 0){
            disFromDrawBoundToImageViewX = rect.right - mBaseWidth;
            deltaX = disFromDrawBoundToImageViewX > disFromFocusToCenterX ?
                    0 - disFromFocusToCenterX : 0 - disFromDrawBoundToImageViewX;
        }else {
            disFromDrawBoundToImageViewX = rect.left;
            deltaX = disFromDrawBoundToImageViewX > disFromFocusToCenterX ?
                    0 - disFromDrawBoundToImageViewX : 0 - disFromDrawBoundToImageViewX;
        }

        deltaY = getImageViewHeight(this) - rect.bottom;
        mSuppMatrix.postTranslate(deltaX, deltaY);
        this.setImageMatrix(getDrawMatrix());
        isInitial = false;
    }

    private void processOnScrollCondition(float scrollX, float scrollY){
        final RectF rect = getDisplayRect(getDrawMatrix());
        if (rect == null || isInitial) {
            return;
        }

        final float deltaX, deltaY;
        if (rect.width() > getImageViewWidth(this)){
            if (scrollX >= 0) {
                final float leftScrollLimit = getImageViewWidth(this) * 0.25F;
                deltaX = rect.left <= leftScrollLimit ? (rect.left + scrollX > leftScrollLimit ?
                leftScrollLimit - rect.left + (rect.left + scrollX - leftScrollLimit) / 7.0F :
                        scrollX) : scrollX / 7.0F;
            }else {
                final float rightScrollLimit = getImageViewWidth(this) * 0.75F;
                deltaX = rect.right >= rightScrollLimit ? (rect.right + scrollX < rightScrollLimit ?
                        -(rect.right - rightScrollLimit + (rightScrollLimit - rect.right - scrollX)
                                / 7.0F) : scrollX) : scrollX / 7.0F;
            }
        }else {
            deltaX = scrollX;
        }

        if (rect.height() <= getImageViewHeight(this)){
            deltaY = 0;
        }else {
            deltaY = scrollY;
        }

        mSuppMatrix.postTranslate(deltaX, deltaY);
        this.setImageMatrix(getDrawMatrix());
    }

    private void processScaleCondition(float focusX, float focusY, float factor){
        Matrix tempMatrix = new Matrix(mSuppMatrix);
        mSuppMatrix.postScale(factor, factor, focusX, focusY);
        final RectF rect = getDisplayRect(getDrawMatrix());

        if (rect == null || rect.width() > mBaseWidth * MAXIMUM_SCALE) {
            mSuppMatrix.set(tempMatrix);
            return;
        }

        this.setImageMatrix(getDrawMatrix());
        isInitial = false;
    }

    private void processOnPointerUp(){
        RectF rectF = getDisplayRect(getDrawMatrix());
        if (rectF == null){
            return;
        }
        float destLeft = rectF.left, destTop = rectF.top,
                destRight = rectF.right, destBottom = rectF.bottom;
        boolean isBackToInit = false;
        if (rectF.width() < mBaseWidth && rectF.height() < mBaseHeight){
            destLeft = 0;
            destTop = getImageViewHeight(this) * 0.222F;
            destRight = getImageViewWidth(this);
            destBottom = getImageViewHeight(this) * 0.778F;
            isBackToInit = true;
        }else if (rectF.width() > mBaseWidth && rectF.width() <= mBaseWidth * SECOND_MAXIMUM_SCALE ){
            if (rectF.left > 0){
                destLeft = 0;
                destRight = rectF.right - rectF.left;
            }else if (rectF.right < getImageViewWidth(this)){
                destRight = getImageViewWidth(this);
                destLeft = rectF.left + getImageViewWidth(this) - rectF.right;
            }
            if (rectF.height() <= getImageViewHeight(this)){
                destTop = (getImageViewHeight(this) - rectF.height()) / 2.0F;
                destBottom = getImageViewHeight(this) - destTop;
            }else {
                if (rectF.top > 0){
                    destTop = 0;
                    destBottom = rectF.bottom - rectF.top;
                }

                if (rectF.bottom < getImageViewHeight(this)){
                    destBottom = getImageViewHeight(this);
                    destTop = rectF.top + getImageViewHeight(this) - rectF.bottom;
                }
            }
        }else if (rectF.width() > SECOND_MAXIMUM_SCALE * mBaseWidth + 1){
            destLeft = rectF.left + (rectF.width() - mBaseWidth * SECOND_MAXIMUM_SCALE) / 2;
            destRight = rectF.right - (rectF.width() - mBaseWidth * SECOND_MAXIMUM_SCALE) / 2;
            destTop = rectF.top + (rectF.height() / rectF.width()) * (rectF.width() - mBaseWidth * SECOND_MAXIMUM_SCALE) / 2;
            destBottom = rectF.bottom - (rectF.height() / rectF.width()) * (rectF.width() - mBaseWidth * SECOND_MAXIMUM_SCALE) / 2;

            if (destLeft > 0){
                destLeft = 0;
                destRight = destRight - destLeft;
            }else if (destTop > 0){
                destTop = 0;
                destBottom = destBottom - destTop;
            }else if (destRight < getImageViewWidth(this)){
                destRight = getImageViewWidth(this);
                destLeft = destLeft + (getImageViewWidth(this) - destRight);
            }else if (destBottom < getImageViewHeight(this)){
                destBottom = getImageViewHeight(this);
                destTop = destTop + (getImageViewHeight(this) - destBottom);
            }
        }

        if (isNeededAdjust(rectF, destLeft, destTop, destRight, destBottom)){
            PhotoView.this.post(new AnimatedRunnable(destLeft, destTop, destRight, destBottom, isBackToInit));
        }
    }

    private boolean isNeededAdjust(RectF rectF, float destLeft, float destTop, float destRight, float destBottom){
        if (rectF.left != destLeft || rectF.top != destTop || rectF.right != destRight || rectF.bottom != destBottom){
            return true;
        }
        return false;
    }

    private int getImageViewWidth(ImageView imageView) {
        return imageView.getWidth() - imageView.getPaddingLeft() - imageView.getPaddingRight();
    }

    private int getImageViewHeight(ImageView imageView) {
        return imageView.getHeight() - imageView.getPaddingTop() - imageView.getPaddingBottom();
    }

    private void updateBaseMatrix(){
        Drawable drawable = super.getDrawable();
        if (drawable == null) {
            return;
        }
        final float viewWidth = getImageViewWidth(this);
        final float viewHeight = getImageViewHeight(this);
        final int drawableWidth = drawable.getIntrinsicWidth();
        final int drawableHeight = drawable.getIntrinsicHeight();

        mBaseMatrix.reset();

        RectF mTempSrc = new RectF(0, 0, drawableWidth, drawableHeight);
        RectF mTempDst = new RectF(0, viewHeight * 0.222f, viewWidth, viewHeight * 0.778f);
        mBaseWidth = mTempDst.width();
        mBaseHeight = mTempDst.height();
        mBaseMatrix.setRectToRect(mTempSrc, mTempDst, Matrix.ScaleToFit.FILL);
        resetMatrix();
        isInitial = true;
    }

    private void resetMatrix(){
        mSuppMatrix.reset();
        this.setImageMatrix(getDrawMatrix());
    }

    private Matrix getDrawMatrix() {
        mDrawMatrix.set(mBaseMatrix);
        mDrawMatrix.postConcat(mSuppMatrix);
        return mDrawMatrix;
    }

    /**
     * Helper method that maps the supplied Matrix to the current Drawable
     *
     * @param matrix - Matrix to map Drawable against
     * @return RectF - Displayed Rectangle
     */
    private RectF getDisplayRect(Matrix matrix) {
        Drawable d = this.getDrawable();
        if (d != null) {
            mDisplayRect.set(0, 0, d.getIntrinsicWidth(),
                    d.getIntrinsicHeight());
            matrix.mapRect(mDisplayRect);
            return mDisplayRect;
        }
        return null;
    }

    private class AnimatedRunnable implements Runnable{

        private final float mDestLeft, mDestTop, mDestRight, mDestBottom;
        private final float mInitLeft, mInitTop, mInitRight, mInitBottom;
        private final long mStartTime;
        private boolean isBackToInit;

        public AnimatedRunnable(float destLeft, float destTop, float destRight, float destBottom,
                                boolean isBackToInit){
            RectF rectF = getDisplayRect(getDrawMatrix());
            mDestLeft = destLeft;
            mDestTop = destTop;
            mDestRight = destRight;
            mDestBottom = destBottom;
            mInitLeft = rectF.left;
            mInitTop = rectF.top;
            mInitRight = rectF.right;
            mInitBottom = rectF.bottom;
            mStartTime = System.currentTimeMillis();
            isOnAdjusting = true;
            this.isBackToInit = isBackToInit;
        }

        @Override
        public void run() {

            float t = 1f * (System.currentTimeMillis() - mStartTime) / 200;
            t = Math.min(1f, t);

            final float curLeft = mInitLeft - (mInitLeft - mDestLeft) * t;
            final float curTop = mInitTop - (mInitTop - mDestTop) * t;
            final float curRight = mInitRight - (mInitRight - mDestRight) * t;
            final float curBottom = mInitBottom - (mInitBottom - mDestBottom) * t;

            final float viewWidth = getImageViewWidth(PhotoView.this);
            final float viewHeight = getImageViewHeight(PhotoView.this);
            mSuppMatrix.reset();
            RectF mTempDst = new RectF(curLeft, curTop, curRight, curBottom);
            RectF mTempSrc = new RectF(0, viewHeight * 0.222f, viewWidth, viewHeight * 0.778f);
            mSuppMatrix.setRectToRect(mTempSrc, mTempDst, Matrix.ScaleToFit.FILL);
            setImageMatrix(getDrawMatrix());

            if (t < 1.0f){
                Compat.postOnAnimation(PhotoView.this, this);
            }else {
                isOnAdjusting = false;
                PhotoView.this.isInitial = isBackToInit;
            }

        }
    }

}
