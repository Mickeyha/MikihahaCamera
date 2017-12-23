package com.example.mickey.mikihahacamera;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.mickey.mikihahacamera.constant.CameraConstant;
import com.example.mickey.mikihahacamera.data.FileManager;
import com.example.mickey.mikihahacamera.view.GifView;
import com.example.mickey.mikihahacamera.view.OneFaceView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class MainActivity extends Activity implements View.OnClickListener, SurfaceHolder.Callback, Camera.PreviewCallback {

    static final String CLIENT_ID = "NuwaCamera2";
    private static final String TAG = "CameraActivity";
    private static final String FOLDER_NAME = "/NuwaCamera";
    private static final int DEFAULT_CAMERA_ID = 0; //back camera at Mibo
    private int mAlertId;

    private volatile boolean mIsCameraOpen = false;
    private boolean mIsTakePicture = true;  //for check is taking picture or taking video
    private boolean mIsTake = false;
    private boolean mNeedDetectFace = false;
    private boolean mNeedCountDown = false;

    private View mCaptureView;
    private View mConfirmView;

    private Camera mCamera = null;
    private GifView mGifView = null;
    private ImageView mTakingAnim;
    private OneFaceView mFaceMaskView;
    private SurfaceView mSurfaceView;
    private SurfaceHolder mHolder;
//    private FaceDetector mFaceDetector;

    private SoundPool mSoundPool;
    private FrameLayout mFrameLayout;
    private ImageButton mFaceButton, mShotButton, mTimerButton, mCloseButton, mBackButton, mConfirmButton;
    private TextView mConfirmTextView;

    private byte[] mCurrentData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");

        setContentView(R.layout.activity_main);

        mCaptureView = findViewById(R.id.rv_capture);
        mConfirmView = findViewById(R.id.rv_confirm);
        mTimerButton = (ImageButton) findViewById(R.id.btn_timer);
        mShotButton = (ImageButton) findViewById(R.id.btn_shot);
        mFaceButton = (ImageButton) findViewById(R.id.btn_face);
        mCloseButton = (ImageButton) findViewById(R.id.btn_close);
        mBackButton = findViewById(R.id.btn_back);
        mConfirmButton = findViewById(R.id.btn_confirm);
        mConfirmTextView = findViewById(R.id.tv_confirm);

        mTakingAnim = (ImageView) findViewById(R.id.capturing);
        mFaceMaskView = (OneFaceView) findViewById(R.id.faceMask);
        mFrameLayout = (FrameLayout) findViewById(R.id.camera_preview);
//        mGifView = (GifView) findViewById(R.id.animationView);
//        mGifView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);

        hideDecorView();
    }


    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");

        openCamera();

        mFaceButton.setOnClickListener(this);
        mShotButton.setOnClickListener(this);
        mTimerButton.setOnClickListener(this);
        mCloseButton.setOnClickListener(this);
        mBackButton.setOnClickListener(this);
        mConfirmButton.setOnClickListener(this);

        mSurfaceView = new SurfaceView(this);

        mHolder = mSurfaceView.getHolder();
        mHolder.addCallback(this);
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        mFrameLayout.addView(mSurfaceView, 0);

//        mFaceDetector = new FaceDetector(this);
//        mFaceDetector.init(CameraConstant.PREVIEW_WIDTH, CameraConstant.PREVIEW_HEIGHT);

        mSoundPool = new SoundPool(1, AudioManager.STREAM_MUSIC, 5);
        mAlertId = mSoundPool.load(this, R.raw.app_3216, 1);

    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");

//        this.mFaceDetector.release();
        closeCamera();
        mSoundPool.stop(mAlertId);
        mSoundPool.release();
        mGifView.stopPlay();
        finish();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
    }


    public synchronized void openCamera() {
        Log.d(TAG, "open camera ");
        if (mIsCameraOpen) {
            return;
        }
        if (mCamera == null) {
            mCamera = checkCamera(this);
        }
        if (mCamera != null) {
            Camera.Parameters para = mCamera.getParameters();
            para.setPreviewSize(CameraConstant.PREVIEW_WIDTH, CameraConstant.PREVIEW_HEIGHT);
            mCamera.setParameters(para);
            mIsCameraOpen = true;
        } else {
            Log.d(TAG, "camera open failed.");
        }
    }

    private void closeCamera() {

        Log.d(TAG, "closeCamera");

        if (this.mCamera != null) {
            this.mCamera.setOneShotPreviewCallback(null);

            try {
                this.mCamera.setPreviewDisplay(null);
            } catch (IOException var2) {
                var2.printStackTrace();
            }

            mSurfaceView.getHolder().removeCallback(this);
            this.mCamera.stopPreview();
            mCamera.setPreviewCallback(null);
            this.mCamera.release();
            this.mCamera = null;
            this.mIsCameraOpen = false;
        }
    }


    private Camera.PictureCallback mPictureCallback = new Camera.PictureCallback() {

        @Override
        public void onPictureTaken(byte[] data, Camera camera) {

            mFaceMaskView.setVisibility(View.INVISIBLE);
            showConfirmView();
            mCurrentData = data;

//            new CountDownTimer(CameraConstant.TIME_SHOW_PICTURE, 500) {
//                @Override
//                public void onFinish() {
//                    Log.d(TAG, "finish");
//                    // After 3 sec, close the camera.
//                    finish();
//                }
//
//                @Override
//                public void onTick(long millisUntilFinished) {
//                    // DO-Nothing
//                    Log.d(TAG,"show image preview");
//                    mFaceMaskView.setVisibility(View.INVISIBLE);            //show image preview
//                }
//            }.start();
        }
    };

    private void saveCurrentData(byte[] data) {
        File pictureFile = FileManager.getInstance().getOutputMediaFile(CameraConstant.MEDIA_TYPE_IMAGE);
        if (pictureFile == null) {
            Log.d(TAG, "Error creating media file, check storage permissions: ");
            return;
        }
        try {
            FileOutputStream fos = new FileOutputStream(pictureFile);
            fos.write(data);
            fos.close();
        } catch (FileNotFoundException e) {
            Log.d(TAG, "File not found: " + e.getMessage());
        } catch (IOException e) {
            Toast.makeText(MainActivity.this, "影像儲存錯誤!", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "Error accessing file: " + e.getMessage());
        }
    }

    private void showConfirmView() {
        mCaptureView.setVisibility(View.INVISIBLE);
        mConfirmView.setVisibility(View.VISIBLE);
        mConfirmButton.setSelected(false);
        mBackButton.setSelected(false);
    }


    private void playCountdownAnim() {
        Log.d(TAG, "playCountdownAnim()");
        mGifView.setVisibility(View.VISIBLE);
        mSoundPool.play(mAlertId, 1.0F, 1.0F, 0, 0, 1.0F);
        mGifView.startPlayOnce(new GifView.PlaybackCallback() {
            @Override
            public void onReadyToEnd() {
                // TODO Auto-generated method stub
            }

            @Override
            public void onEnd(String gif) {
                Log.v(TAG, "onEnd");
                animationToTakePicture();
            }
        });
    }


    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {

//        List<Rect> faceRectList = this.mFaceDetector.findFace(data);
//        this.mFaceMaskView.updateFaceRect(faceRectList);

        if (this.mIsCameraOpen) {
            this.mCamera.setOneShotPreviewCallback(this);
        }

        if (mIsTake) { //take photo

            Log.d(TAG, "mNeedCountDown = " + mNeedCountDown + ",mNeedDetectFace = " + mNeedDetectFace);
            if (!mNeedCountDown && !mNeedDetectFace) {  //only take photo
                Log.v(TAG, "only take photo");
                animationToTakePicture();
                mIsTake = false;

            } else if (mNeedCountDown && !mNeedDetectFace) { // take photo need countdown
                Log.v(TAG, "take photo need countdown ");
                playCountdownAnim();
                mIsTake = false;

//            } else if (!mNeedCountDown && faceRectList.size() > 0) { // take photo need detect face
//                Log.v(TAG, "take photo need detect face,faceRectList.size() = " + faceRectList.size());
//                animationToTakePicture();
//                mIsTake = false;
//
//            } else if (faceRectList.size() > 0) { // take photo need detect face and countdown
//                Log.v(TAG, " take photo need detect face and countdown,faceRectList.size() = " + faceRectList.size());
//                playCountdownAnim();
//                mIsTake=false;
//
            }
        }

    }

    public void animationToTakePicture() {
        Log.d(TAG, "onAutoFocus and animation");
        Animation am = new AlphaAnimation(1.0f, 0.0f);
        am.setDuration(800);
        // 將動畫參數設定到圖片並開始執行動畫
        mTakingAnim.startAnimation(am);
        mSoundPool.stop(mAlertId);
//        mGifView.setVisibility(View.INVISIBLE);
        if (mIsTakePicture) {
            mCamera.autoFocus(new Camera.AutoFocusCallback() {
                @Override
                public void onAutoFocus(boolean success, Camera camera) {
                    if (success) {
                        Log.d(TAG, "AutoFocus success!");
                        mCamera.takePicture(null, null, mPictureCallback);
                        mIsTake = false;

                    } else {
                        Log.d(TAG, "AutoFocus did not success!");
                        mCamera.takePicture(null, null, mPictureCallback);
                        mIsTake = false;
                    }
                }
            });

        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.d(TAG, "1.surfaceCreated");
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Log.d(TAG, "2.surfaceChanged");
        if (mHolder.getSurface() == null) {
            return;
        }
        try {
            mCamera.setPreviewDisplay(holder);

        } catch (Exception e) {
            Log.d(TAG, "Error starting camera preview: " + e.getMessage());
        }
        mCamera.startPreview();
        mCamera.setOneShotPreviewCallback(this);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.d(TAG, "3.surfaceDestroyed");
    }

    private Camera checkCamera(Context context) {
     /*Check device has a camera*/
        Camera c = null;
        PackageManager packageManager = context.getPackageManager();
        if (packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
            Log.d("camera", "This device has camera!");
            int cameraId = DEFAULT_CAMERA_ID;
            c = Camera.open(cameraId);
        } else {
            Log.d("camera", "This device has no camera!");
        }
        return c;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_face:
                if (mFaceButton.isSelected()) {
                    mFaceButton.setSelected(false);
                    mNeedDetectFace = false;
                } else {
                    mFaceButton.setSelected(true);
                    mNeedDetectFace = true;
                }
                break;
            case R.id.btn_shot:
                mIsTake = true;
                break;
            case R.id.btn_timer:
                if (mTimerButton.isSelected()) {
                    mTimerButton.setSelected(false);
                    mNeedCountDown = false;
                } else {
                    mTimerButton.setSelected(true);
                    mNeedCountDown = true;
                }
                break;
            case R.id.btn_close:
                finish();
                break;
            case R.id.btn_back:
                mBackButton.setSelected(true);
                closeConfirmView();
                break;
            case R.id.btn_confirm:
                if(mConfirmButton.isSelected()) {
                    closeConfirmView();
                } else {
                    saveCurrentData(mCurrentData);
                    mConfirmButton.setSelected(true);
                    mConfirmTextView.setText(R.string.confirm_done);
                }
                break;
        }
    }

    private void closeConfirmView() {
        mCamera.startPreview();
        mConfirmView.setVisibility(View.INVISIBLE);
        mCaptureView.setVisibility(View.VISIBLE);
        return;
    }

    public void hideDecorView() {
        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        decorView.setSystemUiVisibility(uiOptions);
    }
}
