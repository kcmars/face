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
     * 通过设置全屏，设置状态栏透明
     * @param activity
     */
    private void fullScreen(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                //5.x开始需要把颜色设置透明，否则导航栏会呈现系统默认的浅灰色
                Window window = activity.getWindow();
                View decorView = window.getDecorView();
                //两个 flag 要结合使用，表示让应用的主体内容占用系统状态栏的空间
                int option = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
                decorView.setSystemUiVisibility(option);
                window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                window.setStatusBarColor(Color.TRANSPARENT);
                //导航栏颜色也可以正常设置
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
        // 屏幕分辨率
        DisplayMetrics dm = new DisplayMetrics();
        this.getWindowManager().getDefaultDisplay().getMetrics(dm);
        int width = dm.widthPixels;
        int height;
        // 根据预览分辨率设置Preview尺寸
        mPreview = (CameraPreview) findViewById(R.id.preview);
        if (CameraUtil.isHasCamera(Camera.CameraInfo.CAMERA_FACING_FRONT)) {
            mPreview.setCaremaId(Camera.CameraInfo.CAMERA_FACING_FRONT);
        } else {
            mPreview.setCaremaId(Camera.CameraInfo.CAMERA_FACING_BACK);
        }
        tvTime = (TextView) findViewById(R.id.time);
        // 屏幕方向
        int previewW = 0, previewH = 0;
        previewW = width;
        previewH = (int) (width * 1.0 * Contants.PREVIEW_W / Contants.PREVIEW_H);

        // 调整布局大小
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(previewW, previewH);
        mPreview.setLayoutParams(params);
    }

    /**
     * 初始化sdk
     */
    private void initcloudwalkSDK() {
        if(cloudwalkSDK == null) {
            cloudwalkSDK = new CloudwalkSDK();
        }
        // 设置活体等级
        cloudwalkSDK.cwSetLivessLevel(Bulider.liveLevel);
        // 初始化
        initRet = cloudwalkSDK.cwInit(this, LICENCE);
        cloudwalkSDK.setWorkType(CloudwalkSDK.DetectType.LIVE_DETECT);
        cloudwalkSDK.cwResetLivenessTarget();
        cloudwalkSDK.setStageflag(FaceInterface.LivessFlag.LIVE_PREPARE);
        cloudwalkSDK.setPushFrame(true);
        cloudwalkSDK.cwFaceInfoCallback(this); // 设置人脸检测回调
        mPreview.setCWPreviewCallback(new CameraPreview.CWPreviewCallback() {
            @Override
            public void onCWPreviewFrame(byte[] frameData, int frameW, int frameH, int frameFormat, int frameAngle,
                                         int frameMirror) {
                //将预览帧传入sdk
                cloudwalkSDK.cwPushFrame(frameData, frameW, frameH, frameFormat, frameAngle, frameMirror);
            }
        });
        //重置数据，开启扫描
        startRecognition();
    }

    //开始识别
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
        mPreview.cwStopCamera();//关闭相机
        mMainHandler.removeCallbacksAndMessages(null);//清除handler数据
        cloudwalkSDK.cwDestory();//销毁sdk
    }

    @Override
    protected void onStop() {
        super.onStop();
        stopTimerRunnable();
        mPreview.cwStopCamera();//关闭相机
        mMainHandler.removeCallbacksAndMessages(null);
    }

    /**
     * 检测到人脸拍照，或者时间到了自动拍照
     */
    private void doFaceResult() {
        stopTimerRunnable();
        mPreview.takePicture(new Camera.PictureCallback() {
            @Override
            public void onPictureTaken(byte[] data, Camera camera) {
                mPreview.stopCameraPreview();//停止相机预览
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
     * 更新视图，显示三秒倒计时
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
     * 检测人脸
     * @param faceInfos 人脸信息
     * @param i 人脸个数
     */
    @Override
    public void detectFaceInfo(FaceInfo[] faceInfos, int i) {
        if(i > 0) {
            Log.i("TAG", "detectFaceInfo: 有人脸来啦");
            mMainHandler.removeCallbacksAndMessages(null);
            doFaceResult();
        }
    }

    public class MainHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {

            switch (msg.what) {
                case SET_RESULT://超时拍照
                    mMainHandler.removeCallbacksAndMessages(null);
                    doFaceResult();
                    break;

                case SET_TIME://超过五秒显示三秒倒计时
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
                // 超时了，拍照
                act.mMainHandler.sendEmptyMessage(SET_RESULT);
            }
        }
    }
}