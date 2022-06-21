package com.example.myapplication;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.ArrayList;

public class RangeSeekBarView extends View {
    public static final int PREVIEW_WIDTH = 37; //dp

    private static int UNSELECTED_MARK_COLOR;
    private static int PROGRESS_POSITION_COLOR = 0xffff0000;
    private final ArrayList<Bitmap> listPreviewBitmaps = new ArrayList<>();
    private ViewSate mViewSate;

    private Bitmap thumbLeftNormal, thumbLeftPressed;
    private Bitmap thumbRightNormal, thumbRightPressed;
    private Rect thumbRect;
    private RectF thumbDrawRect;
    private Rect previewRect = new Rect();
    private RectF previewDrawRect;
    private Paint mPaint;
    private RectF tempRect;

    private int width;
    private int widthDuration;
    private int height;
    private float thumbTouchPadding;
    private int progressWidth;
    private int maxPreview;
    private int previewWidth;

    private long duration;
    private long curPosition;
    private long startPosition;
    private long endPosition;

    public boolean isPaused = true;
    private boolean mIsDragging = false;

    private OnRangeSeekBarChangeListener mListener;
    private float mTouchDownX;
    private long tempStartPosition, tempEndPosition, tempCurPosition;

    public RangeSeekBarView(Context context) {
        super(context);
        init(context);
    }

    public RangeSeekBarView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public RangeSeekBarView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {

        initThumb();

        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        tempRect = new RectF();

        thumbTouchPadding = Utils.dpToPx(context, 32);
        progressWidth = Utils.dpToPx(context, 2);

        maxPreview = getResources().getDisplayMetrics().widthPixels / Utils.dpToPx(context, PREVIEW_WIDTH);

        UNSELECTED_MARK_COLOR = context.getResources().getColor(R.color.black_translucent);
        setDuration(1000);
    }


    private void initThumb() {
        thumbLeftNormal = BitmapFactory.decodeResource(getResources(), R.drawable.range_seek_bar_thumb_left_normal);
        thumbLeftPressed = BitmapFactory.decodeResource(getResources(), R.drawable.range_seek_bar_thumb_left_pressed);

        thumbRightNormal = BitmapFactory.decodeResource(getResources(), R.drawable.range_seek_bar_thumb_right_normal);
        thumbRightPressed = BitmapFactory.decodeResource(getResources(), R.drawable.range_seek_bar_thumb_right_pressed);

        thumbRect = new Rect(0, 0, thumbLeftNormal.getWidth(), thumbLeftNormal.getHeight());
    }

    public void setDuration(long duration) {
        this.duration = duration;
        endPosition = duration;
        startPosition = 0;
    }

    public void setCurrentPosition(long curPosition) {
        this.curPosition = curPosition;
        postInvalidate();
    }

    public void setOnRangeSeekBarChangeListener(OnRangeSeekBarChangeListener listener) {
        mListener = listener;
    }

    public void addPreviewBitmap(Bitmap bitmap) {
        synchronized (listPreviewBitmaps) {
            Bitmap temp = Utils.getResizedBitmap(bitmap, 300);
            Log.d("phi.hd", "addPreviewBitmap");
            listPreviewBitmaps.add(temp);
            bitmap.recycle();
            postInvalidate();
        }
    }

    public int getMaxPreview() {
        return maxPreview;
    }

    private Bitmap getThumbLeft() {
        if (mViewSate == ViewSate.LEFT_PRESSED)
            return thumbLeftPressed;

        return thumbLeftNormal;
    }

    private float getThumbLeftPosition() {
        float portion = (float) startPosition / (float) duration;
        return portion * widthDuration + thumbDrawRect.width();
    }

    private Bitmap getThumbRight() {
        if (mViewSate == ViewSate.RIGHT_PRESSED)
            return thumbRightPressed;

        return thumbRightNormal;
    }

    private float getThumbRightPosition() {
        float portion = (float) endPosition / (float) duration;
        return portion * widthDuration + thumbDrawRect.width();
    }

    private float durationToPosition(long ms) {
        float portion = (float) ms / (float) duration;
        return portion * width;
    }

    public long getStartPosition() {
        return startPosition;
    }

    public void setStartPosition(long startPosition) {
        this.startPosition = startPosition;
        postInvalidate();
    }

    public long getEndPosition() {
        return endPosition;
    }

    public void setEndPosition(long endPosition) {
        this.endPosition = endPosition;
        postInvalidate();
    }

    public long getCurPosition() {
        return curPosition;
    }

    public Bitmap getPreviewAt(int index) {
        if (listPreviewBitmaps.size() > 0) {
            if (index < listPreviewBitmaps.size())
                return listPreviewBitmaps.get(index);
            else
                return listPreviewBitmaps.get(0);
        }

        return null;
    }

    public boolean isPaused() {
        return isPaused;
    }

    public void setPaused(boolean paused) {
        isPaused = paused;
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (w != oldw || h != oldh) {
            width = w;
            height = h;
            configDrawRect();
            configPreviewBitmap();
        }
    }

    private void configDrawRect() {
        float thumbScale = (float) height / (float) thumbRect.height();
        thumbDrawRect = new RectF(0, 0, thumbScale * thumbRect.width(), height);
        widthDuration = (int) (width - 2 * thumbDrawRect.width());
    }

    private void configPreviewBitmap() {
        previewWidth = width / maxPreview;
        previewDrawRect = new RectF(0, 0, previewWidth, height);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (isPaused)
            return onTouchEventPausedMode(event);
        else
            return onTouchEventPlayMode(event);
    }

    private boolean onTouchEventPausedMode(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mViewSate = checkViewSate(event.getX());
                mTouchDownX = event.getX();
                tempStartPosition = startPosition;
                tempEndPosition = endPosition;
                invalidate();
                break;
            case MotionEvent.ACTION_MOVE:
                if (mViewSate != ViewSate.NORMAL) {
                    long diff = (long) ((event.getX() - mTouchDownX) / width * duration);
                    long temp;
                    if (mViewSate == ViewSate.LEFT_PRESSED) {
                        temp = tempStartPosition + diff;
                        if (temp < 0)
                            startPosition = 0;
                        else if (temp > endPosition)
                            startPosition = endPosition;
                        else
                            startPosition = temp;

                    } else if (mViewSate == ViewSate.RIGHT_PRESSED) {
                        temp = tempEndPosition + diff;
                        if (temp < startPosition)
                            endPosition = startPosition;
                        else if (temp > duration)
                            endPosition = duration;
                        else
                            endPosition = temp;
                    }
                    if (mListener != null)
                        mListener.onRangeChanged(this, startPosition, endPosition, mViewSate == ViewSate.LEFT_PRESSED);
                    invalidate();
                }
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                mViewSate = ViewSate.NORMAL;
                invalidate();
                break;
        }
        return true;
    }

    private ViewSate checkViewSate(float x) {
        float thumbLeftPosition = getThumbLeftPosition() - thumbDrawRect.width() / 2;
        float thumbRightPosition = getThumbRightPosition() + thumbDrawRect.width() / 2;

        if (x > thumbLeftPosition && x < thumbRightPosition
                && thumbRightPosition - thumbLeftPosition < 2 * thumbTouchPadding) {
            float tempPadding = (thumbRightPosition - thumbLeftPosition) / 2;
            if (x < thumbLeftPosition + tempPadding)
                return ViewSate.LEFT_PRESSED;
            else
                return ViewSate.RIGHT_PRESSED;
        } else if (x > thumbLeftPosition - thumbTouchPadding && x < thumbLeftPosition + thumbTouchPadding) {
            return ViewSate.LEFT_PRESSED;
        } else if (x > thumbRightPosition - thumbTouchPadding && x < thumbRightPosition + thumbTouchPadding) {
            return ViewSate.RIGHT_PRESSED;
        }

        return ViewSate.NORMAL;
    }

    private boolean onTouchEventPlayMode(MotionEvent event) {
        float x = event.getX();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                float position = durationToPosition(curPosition);

                if (x > position - thumbTouchPadding && x < position + thumbTouchPadding) {
                    mIsDragging = true;
                    mTouchDownX = event.getX();
                    tempCurPosition = curPosition;
                    if (mListener != null)
                        mListener.onStartTrackingTouch(this);

                    invalidate();
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (mIsDragging) {
                    long diff = (long) ((event.getX() - mTouchDownX) / width * duration);
                    long temp = tempCurPosition + diff;
                    if (temp < startPosition)
                        curPosition = startPosition;
                    else if (temp > endPosition)
                        curPosition = endPosition;
                    else
                        curPosition = temp;

                    if (mListener != null)
                        mListener.onProgressChanged(this, curPosition);

                    invalidate();
                }
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                mIsDragging = false;
                if (mListener != null)
                    mListener.onStopTrackingTouch(this);
                break;
        }
        return true;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (width == 0 || height == 0)
            return;

        drawPreview(canvas);

        if (isPaused)
            drawThumb(canvas);
        else
            drawProgress(canvas);
    }

    private void drawPreview(Canvas canvas) {
        if (listPreviewBitmaps.size() > 0) {
            synchronized (listPreviewBitmaps) {
                for (int index = 0; index < maxPreview; index++) {
                    Bitmap bitmap = getPreviewAt(index);
                    previewRect.set(0, 0, bitmap.getWidth(), bitmap.getHeight());
                    tempRect.set(previewDrawRect);
                    tempRect.offset(index * previewWidth, 0);

                    mPaint.setAlpha(255);
                    canvas.drawBitmap(bitmap, tempRect.left, 0, mPaint);
                }
            }
        }
    }

    private void drawThumb(Canvas canvas) {
        //draw thumb left
        Bitmap thumbLeft = getThumbLeft();
        float leftPosition = getThumbLeftPosition() - thumbLeft.getWidth();

        tempRect.set(thumbDrawRect);
        tempRect.offset(leftPosition, 0);

        mPaint.setAlpha(255);
        canvas.drawBitmap(thumbLeft, thumbRect, tempRect, mPaint);

        //draw unselected left
        tempRect.set(0, 0, leftPosition, height);
        mPaint.setColor(UNSELECTED_MARK_COLOR);
        canvas.drawRect(tempRect, mPaint);

        //draw thumb right
        Bitmap thumbRight = getThumbRight();
        float rightPosition = getThumbRightPosition();

        tempRect.set(thumbDrawRect);
        tempRect.offset(rightPosition, 0);

        mPaint.setAlpha(255);
        canvas.drawBitmap(thumbRight, thumbRect, tempRect, mPaint);

        //draw unselected right
        tempRect.set(rightPosition + thumbRight.getWidth(), 0, width, height);
        mPaint.setColor(UNSELECTED_MARK_COLOR);
        canvas.drawRect(tempRect, mPaint);
    }

    private void drawProgress(Canvas canvas) {
        float position = durationToPosition(curPosition);

        tempRect.set(position - progressWidth, 0, position + progressWidth, height);
        mPaint.setColor(PROGRESS_POSITION_COLOR);

        canvas.drawRect(tempRect, mPaint);

        float leftPosition = durationToPosition(startPosition);
        tempRect.set(0, 0, leftPosition, height);
        mPaint.setColor(UNSELECTED_MARK_COLOR);
        canvas.drawRect(tempRect, mPaint);

        float rightPosition = durationToPosition(endPosition);
        tempRect.set(rightPosition, 0, width, height);
        mPaint.setColor(UNSELECTED_MARK_COLOR);
        canvas.drawRect(tempRect, mPaint);
    }

    private enum ViewSate {
        NORMAL, LEFT_PRESSED, RIGHT_PRESSED;
    }

    public interface OnRangeSeekBarChangeListener {
        void onProgressChanged(RangeSeekBarView seekBar, long progress);

        void onStartTrackingTouch(RangeSeekBarView seekBar);

        void onStopTrackingTouch(RangeSeekBarView seekBar);

        void onRangeChanged(RangeSeekBarView seekBar, long start, long end, boolean leftChanged);
    }

}

