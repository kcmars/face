package com.test.face;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.lang.ref.WeakReference;

import cn.cloudwalk.CloudwalkSDK;
import cn.cloudwalk.FaceInterface;
import cn.cloudwalk.callback.FaceInfoCallback;
import cn.cloudwalk.callback.LivessCallBack;
import cn.cloudwalk.jni.FaceInfo;
import cn.cloudwalk.libproject.Bulider;
import cn.cloudwalk.libproject.Contants;
import cn.cloudwalk.libproject.TemplatedActivity;
import cn.cloudwalk.libproject.camera.CameraPreview;
import cn.cloudwalk.libproject.util.CameraUtil;
import cn.cloudwalk.libproject.util.DisplayUtil;
import cn.cloudwalk.libproject.util.UIUtils;
import cn.cloudwalk.libproject.util.Util;

public class FaceCircularActivity extends TemplatedActivity implements FaceInfoCallback {

    final static int SET_RESULT = 122, SET_TIME = 123;
    public MainHandler mMainHandler = null;
    public CloudwalkSDK cloudwalkSDK = null;
    public int initRet;
    public static String LICENCE = "MDM1MzEwbm9kZXZpY2Vjd2F1dGhvcml6ZZfn5OXl5+Tq3+bg5efm5ef65OXl4Obg5Yjm5uvl5ubrkeXm5uvl5uai6+Xm5uvl5qTm6+Xm5ufk++bn5uQ=";

    CameraPreview mPreview;
    TextView tvTime;
    TimerRunnable faceTimerRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_face_circular);
        fullScreen(this);
        if(mMainHandler == null) {
            mMainHandler = new MainHandler();
        }
        initView();
        initcloudwalkSDK();
    }

    /**
     * ??????????????????????????????????????????
     * @param activity
     */
    private void fullScreen(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                //5.x????????????????????????????????????????????????????????????????????????????????????
                Window window = activity.getWindow();
                View decorView = window.getDecorView();
                //?????? flag ??????????????????????????????????????????????????????????????????????????????
                int option = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
                decorView.setSystemUiVisibility(option);
                window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                window.setStatusBarColor(Color.TRANSPARENT);
                //????????????????????????????????????
//                window.setNavigationBarColor(Color.TRANSPARENT);
            } else {
                Window window = activity.getWindow();
                WindowManager.LayoutParams attributes = window.getAttributes();
                int flagTranslucentStatus = WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS;
                int flagTranslucentNavigation = WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION;
                attributes.flags |= flagTranslucentStatus;
//                attributes.flags |= flagTranslucentNavigation;
                window.setAttributes(attributes);
            }
        }
    }

    private void initView() {
        // ???????????????
        DisplayMetrics dm = new DisplayMetrics();
        this.getWindowManager().getDefaultDisplay().getMetrics(dm);
        int width = dm.widthPixels;
        int height;
        // ???????????????????????????Preview??????
        mPreview = (CameraPreview) findViewById(R.id.preview);
        if (CameraUtil.isHasCamera(Camera.CameraInfo.CAMERA_FACING_FRONT)) {
            mPreview.setCaremaId(Camera.CameraInfo.CAMERA_FACING_FRONT);
        } else {
            mPreview.setCaremaId(Camera.CameraInfo.CAMERA_FACING_BACK);
        }
        tvTime = (TextView) findViewById(R.id.time);
        // ????????????
        int previewW = 0, previewH = 0;
        previewW = width;
        previewH = (int) (width * 1.0 * Contants.PREVIEW_W / Contants.PREVIEW_H);

        // ??????????????????
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(previewW, previewH);
        mPreview.setLayoutParams(params);
    }

    /**
     * ?????????sdk
     */
    private void initcloudwalkSDK() {
        if(cloudwalkSDK == null) {
            cloudwalkSDK = new CloudwalkSDK();
        }
        // ??????????????????
        cloudwalkSDK.cwSetLivessLevel(Bulider.liveLevel);
        // ?????????
        initRet = cloudwalkSDK.cwInit(this, LICENCE);
        cloudwalkSDK.setWorkType(CloudwalkSDK.DetectType.LIVE_DETECT);
        cloudwalkSDK.cwResetLivenessTarget();
        cloudwalkSDK.setStageflag(FaceInterface.LivessFlag.LIVE_PREPARE);
        cloudwalkSDK.setPushFrame(true);
        cloudwalkSDK.cwFaceInfoCallback(this); // ????????????????????????
        mPreview.setCWPreviewCallback(new CameraPreview.CWPreviewCallback() {
            @Override
            public void onCWPreviewFrame(byte[] frameData, int frameW, int frameH, int frameFormat, int frameAngle,
                                         int frameMirror) {
                //??????????????????sdk
                cloudwalkSDK.cwPushFrame(frameData, frameW, frameH, frameFormat, frameAngle, frameMirror);
            }
        });
        //???????????????????????????
        startRecognition();
    }

    //????????????
    private void startRecognition() {
        if (initRet == 0) {
            mPreview.cwStartCamera();
            startTimerRunnable(8);
        } else {
            mMainHandler.obtainMessage(SET_RESULT, FaceInterface.CW_LivenessCode.CW_FACE_LIVENESS_AUTH_ERROR).sendToTarget();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopTimerRunnable();
        mPreview.cwStopCamera();//????????????
        mMainHandler.removeCallbacksAndMessages(null);//??????handler??????
        cloudwalkSDK.cwDestory();//??????sdk
    }

    @Override
    protected void onStop() {
        super.onStop();
        stopTimerRunnable();
        mPreview.cwStopCamera();//????????????
        mMainHandler.removeCallbacksAndMessages(null);
    }

    /**
     * ??????????????????????????????????????????????????????
     */
    private void doFaceResult() {
        stopTimerRunnable();
        mPreview.takePicture(new Camera.PictureCallback() {
            @Override
            public void onPictureTaken(byte[] data, Camera camera) {
                mPreview.stopCameraPreview();//??????????????????
                final Bitmap resource = BitmapFactory.decodeByteArray(data, 0, data.length);
                Bitmap bitmap = BitmapUtils.rotateBitmapByDegree(resource, 90, true);
                Bitmap bitmap1 = BitmapUtils.zoomBitmap(bitmap, 300, 300);
                Intent intent = new Intent();
                intent.putExtra("face", bitmap1);
                setResult(9, intent);
                FaceCircularActivity.this.finish();
            }
        });
    }

    /**
     * ????????????????????????????????????
     */
    @SuppressLint("SetTextI18n")
    private void updateView(int time) {
        if(time <= 3 && time > 0) {
            tvTime.setVisibility(View.VISIBLE);
            tvTime.setText("" + time);
        } else {
            tvTime.setVisibility(View.GONE);
        }
    }

    /**
     * ????????????
     * @param faceInfos ????????????
     * @param i ????????????
     */
    @Override
    public void detectFaceInfo(FaceInfo[] faceInfos, int i) {
        if(i > 0) {
            Log.i("TAG", "detectFaceInfo: ???????????????");
            mMainHandler.removeCallbacksAndMessages(null);
            doFaceResult();
        }
    }

    public class MainHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {

            switch (msg.what) {
                case SET_RESULT://????????????
                    mMainHandler.removeCallbacksAndMessages(null);
                    doFaceResult();
                    break;

                case SET_TIME://?????????????????????????????????
                    updateView((Integer) msg.obj);
                    break;
            }
            super.handleMessage(msg);
        }
    }

    private void startTimerRunnable(int count) {
        faceTimerRunnable = new TimerRunnable(count, this);
        mMainHandler.postDelayed(faceTimerRunnable, 1000);
    }

    void stopTimerRunnable() {
        if (faceTimerRunnable != null)
            faceTimerRunnable.setFlag(false);
    }

    static class TimerRunnable implements Runnable {

        private final WeakReference<FaceCircularActivity> mActivity;

        int djsCount;
        boolean flag = true;

        public boolean isFlag() {
            return flag;
        }

        public void setFlag(boolean flag) {
            this.flag = flag;
        }

        public TimerRunnable(int djsCount, FaceCircularActivity activity) {
            super();
            this.djsCount = djsCount;
            mActivity = new WeakReference<FaceCircularActivity>(activity);
        }

        public void run() {
            FaceCircularActivity act = mActivity.get();
            if (!flag || act == null)
                return;

            act.mMainHandler.obtainMessage(SET_TIME, djsCount).sendToTarget();
            djsCount--;

            if (djsCount >= 0) {
                act.mMainHandler.postDelayed(act.faceTimerRunnable, 1000);
            } else {
                // ??????????????????
                act.mMainHandler.sendEmptyMessage(SET_RESULT);
            }
        }
    }
}