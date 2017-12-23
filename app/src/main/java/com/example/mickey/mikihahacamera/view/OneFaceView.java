package com.example.mickey.mikihahacamera.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by MICKEY on 2017/8/9.
 */

public class OneFaceView  extends View {
    private static final String TAG = "OneFaceView";

    private Paint mLocalPaint = null;
    private List<Rect> mFaceRectList;
    private Rect mBiggestFace = null;

    private RectF mRect = new RectF();
    private float mPreviewWidth = 640.0F;
    private float mPreviewHeight = 480.0F;
    private float mWidthRate = -1.0F;
    private float mHeightRate = -1.0F;

    public OneFaceView(Context context, AttributeSet atti) {
        super(context, atti);
        this.mLocalPaint = new Paint();
        this.mLocalPaint.setColor(Color.RED);
        this.mLocalPaint.setStrokeWidth(3.0F);
        this.mLocalPaint.setTextSize(50.0F);
        this.mLocalPaint.setStyle(Paint.Style.STROKE);
    }

    public void setPreviewSize(float w, float h) {
        this.mPreviewWidth = w;
        this.mPreviewHeight = h;
    }

    public void updateFaceRect(List<Rect> faceRectList) {
        this.mFaceRectList = faceRectList;
        this.invalidate();
    }

    protected void onDraw(Canvas canvas) {
        Log.d(TAG, "onDraw");

        super.onDraw(canvas);
        if(this.mFaceRectList != null) {
            if(this.mWidthRate < 0.0F) {
                this.mWidthRate = (float)this.getWidth() / this.mPreviewWidth;
                this.mHeightRate = (float)this.getHeight() / this.mPreviewHeight;
            }

            if(mFaceRectList != null && mFaceRectList.size()>0) {
                mBiggestFace = selectBigFace(mFaceRectList).get(0); //只偵測最大的臉
            }

            Iterator var3 = this.mFaceRectList.iterator();

            while (var3.hasNext()) {
                Rect rect = (Rect) var3.next();

                this.mRect.set(this.mWidthRate * (this.mPreviewWidth - (float) rect.right),
                        this.mHeightRate * (float) rect.top,
                        this.mWidthRate * (this.mPreviewWidth - (float) rect.left),
                        this.mHeightRate * (float) rect.bottom);

                Log.d(TAG, "mRect = " + this.mRect.right + ", " + this.mRect.top + ", " + this.mRect.left + ", " + this.mRect.bottom);
                canvas.drawRect(this.mRect, this.mLocalPaint);
            }

        }
    }

    public Rect getBiggestFace() {
        return mBiggestFace;
    }

    public List<Rect> selectBigFace(List<Rect> list) {

//        Log.v(TAG,"selectBigFace");
        List<Rect> updateList = new ArrayList();

        if (list.size() > 1) {
            int w, h, area1, area2;
            updateList.add(new Rect(0, 0, 0, 0));
            for (int i = 0; i < list.size() - 1; i++) {

                w = (int) (list.get(i).right - list.get(i).left);
                h = (int) (list.get(i).bottom - list.get(i).top);
                area1 = w * h;
                w = (int) (list.get(i + 1).right - list.get(i + 1).left);
                h = (int) (list.get(i + 1).bottom - list.get(i + 1).top);
                area2 = w * h;
                if (area1 > area2) {
                    updateList.set(0, list.get(i));
                } else {
                    updateList.set(0, list.get(i + 1));
                }
            }
        } else {
            updateList = list;
        }
        return updateList;
    }
}
