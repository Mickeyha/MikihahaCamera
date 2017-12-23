package com.example.mickey.mikihahacamera.view;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Movie;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.ImageView;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by MICKEY on 2017/8/8.
 */

public class GifView extends android.support.v7.widget.AppCompatImageView {
    private static final String TAG = "GifView";
    private static final String COUNTDOWN_ANIMATION_FILE = "count_down.gif";
    private static boolean ENABLE_SCALE = true;

    private Context mContext;
    private View mSelfView;
    private InputStream gifInputStream;
    private Movie gifMovie;
    private String mGifName;
    private int movieWidth, movieHeight;
    private long movieDuration;
    private long mMovieStart;
    private int mScreenHeight;
    private int mScreenWidth;

    private Handler mUiHandler;
    private PlaybackCallback mPlaybackListener;
    public static int FPS = 1000/30;
    private boolean bLockFrame;
    private int mCurrentFramePos;
    private int mLastFramePos;
    private boolean bReadyToEndOnce;
    private int mNotifyByEndOfFps;
    private int mJumpOfFps;

    public interface PlaybackCallback {
        void onReadyToEnd();
        void onEnd(String gif);
    }

    public GifView(Context context) {
        super(context);
        init(context);
    }

    public GifView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public GifView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    public void init(Context context){
        mContext = context;
        mSelfView = this;
        mUiHandler = new Handler();
        setFocusable(true);
        loadGif(COUNTDOWN_ANIMATION_FILE);
    }

    public void addListener(PlaybackCallback listen) {
        mPlaybackListener = listen;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        //setMeasuredDimension(movieWidth, movieHeight);

        if (ENABLE_SCALE) {
            mScreenHeight = Resources.getSystem().getDisplayMetrics().heightPixels;
            mScreenWidth = Resources.getSystem().getDisplayMetrics().widthPixels;
            mScreenHeight += getNavigationBarHeight(mContext, Configuration.ORIENTATION_LANDSCAPE);
        } else {
            mScreenHeight = movieHeight;
            mScreenWidth = movieWidth;
        }

//        WindowManager wm = (WindowManager) mCx.getSystemService(Context.WINDOW_SERVICE);
//        DisplayMetrics displaymetrics = new DisplayMetrics();
//        wm.getDefaultDisplay().getMetrics(displaymetrics);
//        mScreenHeight = displaymetrics.heightPixels;
//        mScreenWidth = displaymetrics.widthPixels;
        Log.d(TAG, "onMeasure w:" + mScreenWidth + ", h:" + mScreenHeight);
        setMeasuredDimension(mScreenWidth, mScreenHeight);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        // TODO Auto-generated method stub
        Log.d(TAG, "onLayout, changed:" + changed + ", left:" + left + ", top:" + top + ", right:" + right + ", bottom:" + bottom);
        super.onLayout(changed, left, top, right, bottom);
    }

    public void startPlay() {
        mUiHandler.postDelayed(new Runnable(){
            public void run() {
                mSelfView.invalidate();
                startPlay();
            }
        }, FPS);
    }

    public void startPlayOnce(PlaybackCallback cb) {
        bReadyToEndOnce = true;
        mPlaybackListener = cb;
        mMovieStart = 0;
        mUiHandler.postDelayed(new Runnable(){
            public void run() {
                mSelfView.invalidate();
                startPlay();
            }
        }, FPS);
    }

    public void stopPlay() {
        Log.d(TAG, "stopPlay");
        mUiHandler.removeCallbacksAndMessages(null);
    }

    public synchronized void loadGif(String gif, int jumpOfFps, int notifyByEndOfFps, PlaybackCallback cb) {
        loadGif(gif);
        bReadyToEndOnce = true;
        mPlaybackListener = cb;
        mNotifyByEndOfFps = notifyByEndOfFps;
        mJumpOfFps = jumpOfFps;
    }

    public void lockFrame() {
        bLockFrame = true;
        mLastFramePos = mCurrentFramePos;
    }

    public void unLockFrame() {
        bLockFrame = false;
    }

    public synchronized void loadGif(String gif) {
        try {
            Log.d(TAG, "loadGif:" + gif);
            if (mGifName != null && mGifName.equals(gif)) {
                Log.d(TAG, "ignore loadGif:" + gif);
            }
            mGifName = gif;
            gifInputStream = mContext.getAssets().open(gif);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        gifMovie = Movie.decodeStream(gifInputStream);
        movieWidth = gifMovie.width();
        movieHeight = gifMovie.height();
        movieDuration = gifMovie.duration();
        mMovieStart = 0;
    }

    public String getGifName() {
        return mGifName;
    }

    private void checkPlayback() {

    }

    @Override
    protected synchronized void onDraw(Canvas canvas) {
        long now = android.os.SystemClock.uptimeMillis();
        if (mMovieStart == 0) { // first time
            mMovieStart = now;
        }

        // jump frames of beginning of the gif
        if (mJumpOfFps != 0) {
            mMovieStart -= FPS * mJumpOfFps;
            mJumpOfFps = 0;
        }

        if (gifMovie != null) {
            int dur = gifMovie.duration();
            if (dur == 0) {
                dur = 1000;
            }

            Log.d(TAG, "onDraw:" + (now - mMovieStart) + ", " + mGifName);
            if ((now - mMovieStart) > dur) {
                //Log.d(TAG, "2nd round...");

                if (mPlaybackListener != null) {
                    Log.d(TAG, "1st round finished ..................." + mMovieStart);
                    mPlaybackListener.onEnd(mGifName);
                    mPlaybackListener = null;
                }
            } else {
                //Log.d(TAG, "1st round...");

                // inform caller by the frames of end of the gif
                if ((dur - (now - mMovieStart)) < FPS * (mNotifyByEndOfFps == 0 ? 3 : mNotifyByEndOfFps)) {
                    if (mPlaybackListener != null && bReadyToEndOnce) {
                        Log.d(TAG, "1st round is about to finish ..................." + mMovieStart);
                        mPlaybackListener.onReadyToEnd();
                        bReadyToEndOnce = false;
                    }
                }
            }

            int relTime = (int) ((now - mMovieStart) % dur);

            // save current pos
            mCurrentFramePos = relTime;

            if (bLockFrame) {
                Log.d(TAG, "onDraw: lock" + ", " + mGifName + ", mLastFramePos:" + mLastFramePos);
                relTime = mLastFramePos;
            }

            // set next frame pos
            gifMovie.setTime(relTime);

            float scaleWidth = (float) ((mScreenWidth / (1f*movieWidth)));
            float scaleHeight = (float) ((mScreenHeight / (1f*movieHeight)));
            canvas.scale(scaleWidth, scaleHeight);

//            Paint p = new Paint();
//            p.setAntiAlias(true);
            gifMovie.draw(canvas, 0, 0);
//            invalidate();
            checkPlayback();
        }
    }

    public int getStatusBarHeight() {
        int result = 0;
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }

    public int getNavigationBarHeight()
    {
        boolean hasMenuKey = ViewConfiguration.get(mContext).hasPermanentMenuKey();
        int resourceId = getResources().getIdentifier("navigation_bar_height", "dimen", "android");
        if (resourceId > 0 && !hasMenuKey)
        {
            return getResources().getDimensionPixelSize(resourceId);
        }
        return 0;
    }

    private int getNavigationBarHeight(Context context, int orientation) {
        Resources resources = context.getResources();
        int id = resources.getIdentifier(
                orientation == Configuration.ORIENTATION_PORTRAIT ? "navigation_bar_height" : "navigation_bar_height_landscape",
                "dimen", "android");
        if (id > 0) {
            return resources.getDimensionPixelSize(id);
        }
        return 0;
    }

    public int getNavBarHeight(Context c) {
        int result = 0;
        boolean hasMenuKey = ViewConfiguration.get(c).hasPermanentMenuKey();
        boolean hasBackKey = KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_BACK);

        if(!hasMenuKey && !hasBackKey) {
            //The device has a navigation bar
            Resources resources = getContext().getResources();

            int orientation = getResources().getConfiguration().orientation;
            int resourceId;
            if (isTablet(c)){
                resourceId = resources.getIdentifier(orientation == Configuration.ORIENTATION_PORTRAIT ? "navigation_bar_height" : "navigation_bar_height_landscape", "dimen", "android");
            }  else {
                resourceId = resources.getIdentifier(orientation == Configuration.ORIENTATION_PORTRAIT ? "navigation_bar_height" : "navigation_bar_width", "dimen", "android");
            }

            if (resourceId > 0) {
                return getResources().getDimensionPixelSize(resourceId);
            }
        }
        return result;
    }


    private boolean isTablet(Context c) {
        return (c.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_LARGE;
    }

    private static Bitmap eraseBG(Bitmap src, int color) {
//        bitmap = eraseBG(bitmap, -1);         // use for white background
//        bitmap = eraseBG(bitmap, -16777216);  // use for black background
        int width = src.getWidth();
        int height = src.getHeight();
        Bitmap b = src.copy(Bitmap.Config.ARGB_8888, true);
        b.setHasAlpha(true);

        int[] pixels = new int[width * height];
        src.getPixels(pixels, 0, width, 0, 0, width, height);

        for (int i = 0; i < width * height; i++) {
            if (pixels[i] == color) {
                pixels[i] = 0;
            }
        }

        b.setPixels(pixels, 0, width, 0, 0, width, height);

        return b;
    }
}
