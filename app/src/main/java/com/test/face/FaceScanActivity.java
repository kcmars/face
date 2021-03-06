package com.test.face;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
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
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import java.lang.ref.WeakReference;
import java.util.Map;

import cn.cloudwalk.CloudwalkSDK;
import cn.cloudwalk.FaceInterface;
import cn.cloudwalk.callback.LivessCallBack;
import cn.cloudwalk.libproject.Bulider;
import cn.cloudwalk.libproject.Contants;
import cn.cloudwalk.libproject.TemplatedActivity;
import cn.cloudwalk.libproject.camera.CameraPreview;
import cn.cloudwalk.libproject.util.CameraUtil;
import cn.cloudwalk.libproject.util.DisplayUtil;
import cn.cloudwalk.libproject.util.UIUtils;
import cn.cloudwalk.libproject.util.Util;

public class FaceScanActivity extends TemplatedActivity {

    final static int BESTFACE = 101, SET_RESULT = 122, SET_TIME = 123, CLEARFACE = 126;
    public MainHandler mMainHandler = null;
    public CloudwalkSDK cloudwalkSDK = null;
    public int initRet;
    public static String LICENCE = "MDM1MzEwbm9kZXZpY2Vjd2F1dGhvcml6ZZfn5OXl5+Tq3+bg5efm5ef65OXl4Obg5Yjm5uvl5ubrkeXm5uvl5uai6+Xm5uvl5qTm6+Xm5ufk++bn5uQ=";

    CameraPreview mPreview;
    ImageView ivHead;
    RelativeLayout mRl_bottom;
    TextView tvTime;
    TimerRunnable faceTimerRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_face_scan);
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
        mRl_bottom = (RelativeLayout) findViewById(R.id.bottom_rl);
        tvTime = (TextView) findViewById(R.id.time);
        ivHead = (ImageView) findViewById(R.id.iv_head);
        // ????????????
        int previewW = 0, previewH = 0, flTopW = 0, flTopH = 0, bottomW = 0, bottomH = 0;
        //????????????-?????????titlebar??????-???????????????-NavigationBar??????
        int navigationBarnH = 0;
        if (UIUtils.checkDeviceHasNavigationBar(this)) {
            navigationBarnH = UIUtils.getNavigationBarHeight(this);
        }
        height = dm.heightPixels - DisplayUtil.dip2px(this, 45) - Util.getStatusBarHeight(this);
        previewW = width;
        previewH = (int) (width * 1.0 * Contants.PREVIEW_W / Contants.PREVIEW_H);
        flTopW = width;
        flTopH = width;
        bottomW = width;
        if (height - width < DisplayUtil.dip2px(this, 185)) {
            bottomH = DisplayUtil.dip2px(this, 185);
        } else {
            bottomH = height - width;
        }

        // ??????????????????
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(previewW,
                previewH);
        mPreview.setLayoutParams(params);

        params = new RelativeLayout.LayoutParams(flTopW, flTopH);
        params.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        params.addRule(RelativeLayout.ABOVE, R.id.bottom_rl);
        ivHead.setLayoutParams(params);
        ivHead.setImageResource(R.drawable.cloudwalk_face_main_camera_mask);

        params = new RelativeLayout.LayoutParams(bottomW, bottomH);
        params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        mRl_bottom.setLayoutParams(params);
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
        initCallBack();
        //???????????????????????????
        startRecognition();
    }

    private void initCallBack() {
        cloudwalkSDK.cwLivessInfoCallback(new LivessCallBack() {
            @Override
            public void detectInfo(int i) {
            }

            @Override
            public void detectReady() {
                cloudwalkSDK.cwResetLiving();
                if (mMainHandler != null) {
                    //??????????????????,1s???????????????
                    mMainHandler.sendEmptyMessageDelayed(BESTFACE, 1000);
                }
            }

            @Override
            public void detectFinished() {

            }

            @Override
            public void detectLivess(int i, byte[] bytes) {

            }

            @Override
            public void OnActionNotStandard(int notStandardType) {
                if (mMainHandler != null) {
                    mMainHandler.obtainMessage(SET_RESULT, notStandardType).sendToTarget();
                }
            }
        });
        mPreview.setCWPreviewCallback(new CameraPreview.CWPreviewCallback() {
            @Override
            public void onCWPreviewFrame(byte[] frameData, int frameW, int frameH, int frameFormat, int frameAngle,
                                         int frameMirror) {
                //??????????????????sdk
                cloudwalkSDK.cwPushFrame(frameData, frameW, frameH, frameFormat, frameAngle, frameMirror);
            }
        });
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
     * ??????????????????
     */
    private void getBestFace() {//???sdk????????????????????????????????????bulider???
        Bulider.clipedBestFaceData = cloudwalkSDK.cwGetClipedBestFace();
        Bulider.bestFaceData = cloudwalkSDK.cwGetOriBestFace();
        Bulider.nextFaceData = cloudwalkSDK.cwGetNextFace();
        Bulider.bestInfo = cloudwalkSDK.cwGetBestInfo();
        Bulider.nextInfo = cloudwalkSDK.cwGetNextInfo();
    }

    private void doFaceVerify() {
        stopTimerRunnable();
        mPreview.stopCameraPreview();//??????????????????
        //????????????????????????????????????????????????????????????????????????????????????????????????
        byte[] faceData = Bulider.bestFaceData;
        //??????????????????
        Bitmap bitmap = BitmapUtils.byteToBitmap(faceData);
        Bitmap bitmap1 = BitmapUtils.zoomBitmap(bitmap, 300, 300);
        Intent intent = new Intent();
        intent.putExtra("face", bitmap1);
        setResult(9, intent);
        FaceScanActivity.this.finish();
    }

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
                FaceScanActivity.this.finish();
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

                case CLEARFACE://??????
                    cloudwalkSDK.cwClearBestFace();
                    break;

                case BESTFACE://????????????????????????????????????
                    getBestFace();
                    if (Bulider.bestFaceData == null || (Bulider.bestFaceData != null && Bulider.bestFaceData.length == 0) || Bulider.clipedBestFaceData == null ||
                            (Bulider.clipedBestFaceData != null && Bulider.clipedBestFaceData.length == 0)) {
                        cloudwalkSDK.cwResetLivenessTarget();
                        cloudwalkSDK.setStageflag(FaceInterface.LivessFlag.LIVE_PREPARE);
                        cloudwalkSDK.setPushFrame(true);
                    } else {
                        mMainHandler.removeCallbacksAndMessages(null);
                        doFaceVerify();
                    }
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

        private final WeakReference<FaceScanActivity> mActivity;

        int djsCount;
        boolean flag = true;

        public boolean isFlag() {
            return flag;
        }

        public void setFlag(boolean flag) {
            this.flag = flag;
        }

        public TimerRunnable(int djsCount, FaceScanActivity activity) {
            super();
            this.djsCount = djsCount;
            mActivity = new WeakReference<FaceScanActivity>(activity);
        }

        public void run() {
            FaceScanActivity act = mActivity.get();
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