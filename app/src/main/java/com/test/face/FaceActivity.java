package com.test.face;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.AnimationDrawable;
import android.hardware.Camera;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import androidx.annotation.RequiresApi;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.viewpager.widget.PagerAdapter;
import cn.cloudwalk.CloudwalkSDK;
import cn.cloudwalk.FaceInterface;
import cn.cloudwalk.FaceInterface.CW_LivenessCode;
import cn.cloudwalk.FaceInterface.LivessType;
import cn.cloudwalk.callback.FaceInfoCallback;
import cn.cloudwalk.callback.FrameInfoCallback;
import cn.cloudwalk.callback.LivessCallBack;
import cn.cloudwalk.jni.FaceInfo;
import cn.cloudwalk.libproject.BaseActivity;
import cn.cloudwalk.libproject.Bulider;
import cn.cloudwalk.libproject.Contants;
import cn.cloudwalk.libproject.LiveResultActivity;
import cn.cloudwalk.libproject.LiveServerActivity;
import cn.cloudwalk.libproject.TemplatedActivity;
import cn.cloudwalk.libproject.camera.CameraPreview;
import cn.cloudwalk.libproject.progressHUD.CwProgressHUD;
import cn.cloudwalk.libproject.util.CameraUtil;
import cn.cloudwalk.libproject.util.DisplayUtil;
import cn.cloudwalk.libproject.util.NullUtils;
import cn.cloudwalk.libproject.util.UIUtils;
import cn.cloudwalk.libproject.util.Util;
import cn.cloudwalk.libproject.view.CustomViewPager;
import cn.cloudwalk.libproject.view.RoundProgressBarWidthNumber;

import static cn.cloudwalk.libproject.Bulider.totalLiveList;

/**
 * ClassName: FaceActivity <br/>
 * Description: Living face <br/>
 * date: 2021-7-9 ??????9:17:24 <br/>
 *
 * @author keyt
 * @since JDK 1.7
 */
public class FaceActivity extends BaseActivity implements LivessCallBack, FaceInfoCallback, CameraPreview.CWPreviewCallback, FrameInfoCallback {

    final static int NEXT_STEP = 101, UPDATE_STEP_PROCRESS = 106, SET_RESULT = 122,
            UPDATESTEPLAYOUT = 124,
            PLAYMAIN_END = 125,
            DETECT_READY = 126,
            SOUND_AGAIN = 200,
            FRAME_INFO = 201;
    boolean isStop;// ????????????
    boolean isLivePass;// ??????????????????
    boolean isSetResult = false;// ????????????
    volatile boolean isStartDetectFace;// ????????????

    // ???????????????????????????
    public SoundPool sndPool;
    public Map<String, Integer> poolMap;
    int currentStreamID;
    boolean isLoadmain;
    boolean isPlayMain = true;

    CameraPreview mPreview;
    int caremaId;

    ImageView mIv_top;
    RelativeLayout mRl_bottom;

    CustomViewPager mViewPager;
    ViewPagerAdapter viewPagerAdapter;

    RoundProgressBarWidthNumber mPb_step;
    ImageView mIv_step;
    TextView mTv_step;
    TimerRunnable faceTimerRunnable;

    private AnimationDrawable animationDrawable;

    // ????????????
    int totalStep;
    int currentStep;
    ArrayList<View> viewList;

    MainHandler mMainHandler;

    public CloudwalkSDK cloudwalkSDK;
    public int initRet;

    public List<Integer> execLiveList;

    public CwProgressHUD processDialog;// ?????????
    // ???????????????????????????
    LocalBroadcastManager localBroadcastManager;
    LiveBroadcastReceiver liveBroadcastReceiver;
    LiveServerBroadcastReceiver liveServerBroadcastReceiver;
    long mLastPrepareInfoTime = 0;//????????????????????????????????????
    static final int DURATION_PREPARE_INFO = 1000;//??????????????????????????????,UI??????????????????
    public static TextView txtFrame;
    Bundle bundle = new Bundle();

    @RequiresApi(api = Build.VERSION_CODES.FROYO)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_face);
        setTitle(R.string.cloudwalk_live_title);
        mMainHandler = new MainHandler(this);
        fullScreen(this);
        initSoundPool(this);
        initView();
        initStepViews();
        // FaceRecognize???????????????
        initcloudwalkSDK();
        initCallBack();
        if (Bulider.isServerLive) {
            processDialog = CwProgressHUD.create(this).setStyle(CwProgressHUD.Style
                    .SPIN_INDETERMINATE)
                    .setLabel(getString(R.string.cloudwalk_faceserver_live)).setCancellable(true)
                    .setAnimationSpeed(2)
                    .setDimAmount(0.5f);
        } else {
            processDialog = CwProgressHUD.create(this).setStyle(CwProgressHUD.Style
                    .SPIN_INDETERMINATE)
                    .setLabel(getString(R.string.cloudwalk_faceverifying)).setCancellable(true)
                    .setAnimationSpeed(2)
                    .setDimAmount(0.5f);
        }
    }

    /**
     * ??????????????????????????????????????????
     *
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

    @Override
    public void detectFrameInfo(int frame) {
        Log.i("frame", frame + "");
        Message message = Message.obtain();
        message.what = FRAME_INFO;
        if (bundle != null) {
            bundle.putInt("frame", frame);
            message.setData(bundle);
            mMainHandler.sendMessage(message);
        }
    }

    public class LiveBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context arg0, Intent intent) {
            int faceCompareType = intent.getIntExtra("isFaceComparePass", Bulider.FACE_VERFY_FAIL);
            String faceCompareSessionId = intent.getStringExtra("faceSessionId");
            String faceCompareTipMsg = intent.getStringExtra("faceCompareTipMsg");
            double faceScore = intent.getDoubleExtra("faceCompareScore", 0d);

            setFaceResult(faceCompareType, faceScore, faceCompareSessionId, faceCompareTipMsg);
        }
    }

    public class LiveServerBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context arg0, Intent intent) {
            int extInfo = intent.getIntExtra("extInfo", Bulider.FACE_LIVE_FAIL);
            int result = intent.getIntExtra("result", Bulider.FACE_LIVE_FAIL);

            setFaceLiveResult(extInfo, result);
        }
    }

    private void setFaceLiveResult(int extInfo, int result) {
        boolean isLivePass = false;
        if (result == Bulider.FACE_LIVE_PASS && extInfo == 1) {
            isLivePass = true;
        } else if (result == Bulider.FACE_LIVE_FAIL || extInfo != 1) {
            isLivePass = false;
        }
        if (processDialog != null && processDialog.isShowing()) {
            processDialog.dismiss();
        }
        mMainHandler.removeCallbacksAndMessages(null);
        if (isSetResult || isStop)
            return;
        isSetResult = true;
        if (Bulider.isResultPage) {
            Intent mIntent = new Intent(FaceActivity.this, LiveServerActivity.class);
            mIntent.putExtra(LiveServerActivity.FACEDECT_RESULT_ISSUCCESS, isLivePass);
            startActivity(mIntent);
        }
        finish();

    }

    private void initcloudwalkSDK() {
        cloudwalkSDK = new CloudwalkSDK();
        // ??????????????????
        cloudwalkSDK.cwSetLivessLevel(Bulider.liveLevel);
        // ?????????
        initRet = cloudwalkSDK.cwInit(this, Bulider.licence);

    }

    private void playMain() {
        if (isPlayMain && isLoadmain) {
            isPlayMain = false;
            currentStreamID = 1;// ???????????????????????????1
//            sndPool.play(currentStreamID, 1.0f, 1.0f, 0, 0, 1.0f);
            mMainHandler.sendEmptyMessageDelayed(PLAYMAIN_END, 3000);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.FROYO)
    public void initSoundPool(Context ctx) {
        poolMap = new HashMap<String, Integer>();
        sndPool = new SoundPool(1, AudioManager.STREAM_MUSIC, 100);
        sndPool.setOnLoadCompleteListener(new SoundPool.OnLoadCompleteListener() {
            @Override
            public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {
                if (1 == sampleId) {
                    isLoadmain = true;
                    playMain();
                }
            }
        });
        poolMap.put("main", sndPool.load(ctx, R.raw.cloudwalk_main, 1));//
        poolMap.put("mouth_open", sndPool.load(ctx, R.raw.cloudwalk_live_mouth, 1));//
        poolMap.put("head_up", sndPool.load(ctx, R.raw.cloudwalk_live_top, 1));//
        poolMap.put("head_down", sndPool.load(ctx, R.raw.cloudwalk_live_down, 1));//
        poolMap.put("head_left", sndPool.load(ctx, R.raw.cloudwalk_live_left, 1));//
        poolMap.put("head_right", sndPool.load(ctx, R.raw.cloudwalk_live_right, 1));//
        poolMap.put("eye_blink", sndPool.load(ctx, R.raw.cloudwalk_live_eye, 1));//
        poolMap.put("good", sndPool.load(ctx, R.raw.cloudwalk_good, 1));//
        poolMap.put("try_again", sndPool.load(ctx, R.raw.cloudwalk_again, 1));//
        poolMap.put("open_mouth_widely", sndPool.load(ctx, R.raw.cloudwalk_open_widely, 1));//
    }

    @RequiresApi(api = Build.VERSION_CODES.FROYO)
    public void releaseSoundPool() {
        if (sndPool != null) {
            sndPool.setOnLoadCompleteListener(null);//??????Activity?????????????????????????????????????????????
            sndPool.release();
            sndPool = null;
        }

    }

    private void initCallBack() {
        cloudwalkSDK.cwFaceInfoCallback(this);
        cloudwalkSDK.cwLivessInfoCallback(this);
        mPreview.setCWPreviewCallback(this);
        cloudwalkSDK.setFrameInfoCallback(this);
    }

    private void initView() {
        // ???????????????
        DisplayMetrics dm = new DisplayMetrics();
        this.getWindowManager().getDefaultDisplay().getMetrics(dm);
        int width = dm.widthPixels;
        int height;
        // ViewPager
        mViewPager = (CustomViewPager) findViewById(R.id.viewpager);
        // ???????????????????????????Preview??????
        mPreview = (CameraPreview) findViewById(R.id.preview);
        if (CameraUtil.isHasCamera(Camera.CameraInfo.CAMERA_FACING_FRONT)) {
            caremaId = Camera.CameraInfo.CAMERA_FACING_FRONT;
            mPreview.setCaremaId(caremaId);
        } else {
            caremaId = Camera.CameraInfo.CAMERA_FACING_BACK;
            mPreview.setCaremaId(caremaId);
        }
        mIv_top = (ImageView) findViewById(R.id.top_iv);

        mRl_bottom = (RelativeLayout) findViewById(R.id.bottom_rl);
        mPb_step = (RoundProgressBarWidthNumber) findViewById(R.id.cloudwalk_face_step_procress);
        txtFrame = (TextView) findViewById(R.id.tv_frame_info);
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
        mIv_top.setLayoutParams(params);
        mIv_top.setImageResource(R.drawable.cloudwalk_face_main_camera_mask);

        params = new RelativeLayout.LayoutParams(bottomW, bottomH);
        params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        mRl_bottom.setLayoutParams(params);
    }

    private void initStepViews() {

        getExecLive();

        // viewList
        LayoutInflater lf = getLayoutInflater().from(this);
        viewList = new ArrayList<View>();
        View view;
        // ????????????item
        view = lf.inflate(R.layout.cloudwalk_layout_facedect_step_start, null);//
        addView(view);
        // ??????item
        int size = execLiveList.size();
        for (int i = 0; i < size; i++) {
            view = lf.inflate(R.layout.cloudwalk_layout_facedect_step, null);//
            addView(view);
        }

        viewPagerAdapter = new ViewPagerAdapter(viewList);
        mViewPager.setAdapter(viewPagerAdapter);

    }

    /**
     * ??????????????????????????????
     * ???????????????????????????,??????,???????????????????????????????????????????????????????????????????????????????????????????????????
     * ???????????????????????????????????? ,??????????????????
     */
    private void getExecLive() {
        if (1 < Bulider.execLiveCount && Bulider.execLiveCount <= 4) {
            boolean isTotalHasLR = totalLiveList.contains(LivessType.LIVESS_HEAD_LEFT)
                    || totalLiveList.contains(LivessType.LIVESS_HEAD_RIGHT);
            boolean isTotalHasME = totalLiveList.contains(LivessType.LIVESS_MOUTH)
                    || totalLiveList.contains(LivessType.LIVESS_EYE);
            // ????????????????????? ????????? ???????????????????????????
            if (isTotalHasLR && isTotalHasME) {
                boolean isSubHasLRME = false;
                while (!isSubHasLRME) {
                    Collections.shuffle(totalLiveList);
                    execLiveList = new CopyOnWriteArrayList<>(totalLiveList.subList(0, Bulider.execLiveCount));
                    isSubHasLRME = (execLiveList.contains(LivessType.LIVESS_HEAD_LEFT)
                            || execLiveList.contains(LivessType.LIVESS_HEAD_RIGHT))
                            && (execLiveList.contains(LivessType.LIVESS_MOUTH)
                            || execLiveList.contains(LivessType.LIVESS_EYE));
                }
            } else if (isTotalHasLR) {// isTotalHasLR
                boolean isSubHasLR = false;
                while (!isSubHasLR) {
                    Collections.shuffle(totalLiveList);
                    execLiveList = new CopyOnWriteArrayList<>(totalLiveList.subList(0, Bulider.execLiveCount));
                    isSubHasLR = (execLiveList.contains(LivessType.LIVESS_HEAD_LEFT)
                            || execLiveList.contains(LivessType.LIVESS_HEAD_RIGHT));
                }
            } else if (isTotalHasME) {// isTotalHasME
                boolean isSubHasME = false;
                while (!isSubHasME) {
                    Collections.shuffle(totalLiveList);
                    execLiveList = new CopyOnWriteArrayList<>(totalLiveList.subList(0, Bulider.execLiveCount));
                    isSubHasME = (execLiveList.contains(LivessType.LIVESS_MOUTH)
                            || execLiveList.contains(LivessType.LIVESS_EYE));
                }
            } else {
                Collections.shuffle(totalLiveList);
                execLiveList = new CopyOnWriteArrayList<>(totalLiveList.subList(0, Bulider.execLiveCount));
            }
        } else {
            Collections.shuffle(totalLiveList);
            execLiveList = new CopyOnWriteArrayList<>(totalLiveList.subList(0, Bulider.execLiveCount));
        }
    }

    private void addView(View view) {
        viewList.add(view);
        totalStep++;
    }

    /**
     * ??????????????????
     */
    private void getBestFace() {
        Bulider.clipedBestFaceData = cloudwalkSDK.cwGetClipedBestFace();
        Bulider.bestFaceData = cloudwalkSDK.cwGetOriBestFace();
        Bulider.nextFaceData = cloudwalkSDK.cwGetNextFace();
        Bulider.bestInfo = cloudwalkSDK.cwGetBestInfo();
        Bulider.nextInfo = cloudwalkSDK.cwGetNextInfo();
    }

    /**
     * ??????????????????
     */
    private void clearBestFace() {
        cloudwalkSDK.cwClearBestFace();

    }

    private void doFaceSerLivess() {
        isLivePass = true;

        if (Bulider.mFrontLiveCallback == null) {
            mMainHandler.obtainMessage(SET_RESULT, CW_LivenessCode.CW_FACE_LIVENESS_FACEDEC_OK)
                    .sendToTarget();
        } else {

            if (Bulider.isServerLive) {
                localBroadcastManager = LocalBroadcastManager.getInstance(this);
                IntentFilter filter = new IntentFilter();
                filter.addAction(Contants.ACTION_BROADCAST_SERVER_LIVE);
                liveServerBroadcastReceiver = new LiveServerBroadcastReceiver();
                localBroadcastManager.registerReceiver(liveServerBroadcastReceiver, filter);
                processDialog.show();
                Bulider.mFrontLiveCallback.onFrontLivessFinished(Bulider.bestFaceData, Bulider.bestInfo,
                        Bulider.nextFaceData, Bulider.nextInfo, Bulider.clipedBestFaceData, true);
            } else {
                mMainHandler.obtainMessage(SET_RESULT, CW_LivenessCode.CW_FACE_LIVENESS_FACEDEC_OK)
                        .sendToTarget();
            }
        }
    }

    private void doFrontFaceLivess() {
        isLivePass = cloudwalkSDK.cwVerifyBestImg() == 0 ? true : false;//????????????????????????
        if (Bulider.mFrontLiveCallback == null) {
            mMainHandler.obtainMessage(SET_RESULT, CW_LivenessCode.CW_FACE_LIVENESS_FACEDEC_OK)
                    .sendToTarget();
        } else {
            if (Bulider.isFrontHack && Bulider.mFrontLiveCallback != null) {
                IntentFilter filter = new IntentFilter();
                filter.addAction(Contants.ACTION_BROADCAST_LIVE);
                liveBroadcastReceiver = new LiveBroadcastReceiver();
                localBroadcastManager = LocalBroadcastManager.getInstance(this);
                localBroadcastManager.registerReceiver(liveBroadcastReceiver, filter);
                Bulider.mFrontLiveCallback.onFrontLivessFinished(Bulider.bestFaceData, Bulider.bestInfo,
                        Bulider.nextFaceData, Bulider.nextInfo, Bulider.clipedBestFaceData, isLivePass);
            } else {
                mMainHandler.obtainMessage(SET_RESULT, CW_LivenessCode.CW_FACE_LIVENESS_FACEDEC_OK)
                        .sendToTarget();
            }
        }
    }

    private void doFaceVerify() {
        isLivePass = true;

        if (Bulider.dfvCallBack == null) {

            mMainHandler.obtainMessage(SET_RESULT, CW_LivenessCode.CW_FACE_LIVENESS_FACEDEC_OK)
                    .sendToTarget();

            return;
        } else {
            localBroadcastManager = LocalBroadcastManager.getInstance(this);
            IntentFilter filter = new IntentFilter();
            filter.addAction(Contants.ACTION_BROADCAST_LIVE);
            liveBroadcastReceiver = new LiveBroadcastReceiver();
            localBroadcastManager.registerReceiver(liveBroadcastReceiver, filter);
            processDialog.show();
            Bulider.dfvCallBack.OnDefineFaceVerifyResult(Bulider.bestFaceData);
        }

    }

    /**
     * ??????????????????
     *
     * @param faceCompareType ????????????????????????
     * @param faceScore       ????????????
     * @param sessionId       sessionId
     * @param tipMsg          ?????????????????????
     */
    public void setFaceResult(int faceCompareType, double faceScore, String sessionId, String
            tipMsg) {
        int resultType;
        boolean isVerfyPass = false;
        if (Bulider.FACE_VERFY_PASS == faceCompareType) {
            resultType = Bulider.FACE_VERFY_PASS;
            isVerfyPass = true;
        } else if (Bulider.FACE_VERFY_FAIL == faceCompareType) {
            resultType = Bulider.FACE_VERFY_FAIL;
        } else {
            resultType = Bulider.FACE_VERFY_NETFAIL;
        }
        if (processDialog != null && processDialog.isShowing()) {
            processDialog.dismiss();
        }

        setFaceResult(isVerfyPass, faceScore, sessionId, resultType, tipMsg);

    }

    private void setFaceResult(boolean isVerfyPass, double faceScore, String sessionId, int
            resultType, String tipMsg) {
        mMainHandler.removeCallbacksAndMessages(null);
        if (isSetResult || isStop)
            return;
        isSetResult = true;

        if (Bulider.isResultPage) {

            Intent mIntent = new Intent(this, LiveResultActivity.class);
            mIntent.putExtra(LiveResultActivity.FACEDECT_RESULT_TYPE, resultType);// ????????????
            if (NullUtils.isNotEmpty(tipMsg))
                mIntent.putExtra(LiveResultActivity.FACEDECT_RESULT_MSG, tipMsg);// ?????????????????????
            mIntent.putExtra(LiveResultActivity.ISLIVEPASS, isLivePass);// ??????????????????
            mIntent.putExtra(LiveResultActivity.ISVERFYPASS, isVerfyPass);// ??????????????????
            mIntent.putExtra(LiveResultActivity.FACESCORE, faceScore);// ????????????
            mIntent.putExtra(LiveResultActivity.SESSIONID, sessionId);// ????????????id

            startActivity(mIntent);
            finish();
        } else {

            if (Bulider.mResultCallBack != null)
                Bulider.mResultCallBack.result(isLivePass, isVerfyPass, sessionId, faceScore,
                        resultType,
                        Bulider.bestFaceData, Bulider.clipedBestFaceData, Bulider.liveDatas);
            finish();
        }

    }

    private void doNextStep() {

        int nextDelayTime = 10;
        if (currentStep == 1) {// ?????????????????????
            nextDelayTime = 500; //delay?????????????????????,???????????????
            mMainHandler.sendEmptyMessageDelayed(UPDATESTEPLAYOUT, nextDelayTime / 2);
        } else if (totalStep == currentStep) {// ??????????????????
            isLivePass = true;
            currentStreamID = poolMap.get("good");
            if (sndPool != null) {
//                sndPool.play(currentStreamID, 1.0f, 1.0f, 0, 0, 1.0f);
            }
            nextDelayTime = 500;
        } else {
            currentStreamID = poolMap.get("good");
            if (sndPool != null) {
//                sndPool.play(currentStreamID, 1.0f, 1.0f, 0, 0, 1.0f);
            }
            nextDelayTime = 1000;
            // ??????????????????????????????????????????
            mMainHandler.sendEmptyMessageDelayed(UPDATESTEPLAYOUT, nextDelayTime / 2);
        }

        mMainHandler.sendEmptyMessageDelayed(NEXT_STEP, nextDelayTime);
    }

    void resetLive() {
        // ??????????????????
        mMainHandler.removeCallbacksAndMessages(null);
        isPlayMain = true;// ????????????????????????
        Bulider.bestFaceData = null;// ??????????????????
        Bulider.clipedBestFaceData = null;// ???????????????????????????
        if (Bulider.isLivesPicReturn)
            Bulider.liveDatas = new HashMap<Integer, byte[]>();// ????????????????????????
        isLivePass = false;// ???????????????????????????
        playMain();// ????????????????????????
        isSetResult = false;
        // ????????????????????????
        synchronized (FaceActivity.class) {
            currentStep = 0;
        }
        mViewPager.setCurrentItem(currentStep);
        mPb_step.setVisibility(View.GONE);
        isStartDetectFace = false;// ??????????????????????????????
        if (initRet == 0) {// ??????????????????
            //doNextStep();
            cloudwalkSDK.setWorkType(CloudwalkSDK.DetectType.LIVE_DETECT);//?????????????????????????????????
            cloudwalkSDK.cwResetLivenessTarget();
            cloudwalkSDK.setStageflag(FaceInterface.LivessFlag.LIVE_PREPARE);//??????????????????
            mPreview.setPushFrame(true);
        } else {

            mMainHandler.obtainMessage(SET_RESULT, CW_LivenessCode.CW_FACE_LIVENESS_AUTH_ERROR)
                    .sendToTarget();

        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        isStop = false;
        resetLive();

        mPreview.cwStartCamera();

    }

    @Override
    public void onCWPreviewFrame(byte[] frameData, int frameW, int frameH, int frameFormat, int frameAngle, int frameMirror) {
        //??????????????????sdk
        cloudwalkSDK.cwPushFrame(frameData, frameW, frameH, frameFormat, frameAngle, frameMirror);
    }

    @Override
    protected void onRestart() {
        super.onRestart();

    }

    @RequiresApi(api = Build.VERSION_CODES.FROYO)
    @Override
    protected void onDestroy() {
        super.onDestroy();

        mPreview.setCWPreviewCallback(null);//????????????
        mMainHandler.removeCallbacksAndMessages(null);
        cloudwalkSDK.cwDestory();
        releaseSoundPool();
        if (processDialog != null && processDialog.isShowing()) {
            processDialog.dismiss();
        }
        if (localBroadcastManager != null) {
            if (liveBroadcastReceiver != null) {
                localBroadcastManager.unregisterReceiver(liveBroadcastReceiver);
            }
            if (liveServerBroadcastReceiver != null) {
                localBroadcastManager.unregisterReceiver(liveServerBroadcastReceiver);
            }
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        mPreview.cwStopCamera();
        stopTimerRunnable();
        isStop = true;
        mMainHandler.removeCallbacksAndMessages(null);
        sndPool.stop(currentStreamID);
    }

    /**
     * startLivessDetect:???????????? <br/>
     *
     * @param livessType
     * @author:284891377 Date: 2016-5-20 ??????10:18:55
     * @since JDK 1.7
     */
    private void startLivessDetect(final int livessType) {
        switch (livessType) {
            case LivessType.LIVESS_HEAD_LEFT:

                currentStreamID = poolMap.get("head_left");
//                sndPool.play(currentStreamID, 1.0f, 1.0f, 0, 0, 1.0f);
                //????????????????????????????????????,???????????????,?????????????????????
                mMainHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        startTimerRunnable(Bulider.timerCount);
                        cloudwalkSDK.cwStartLivess(livessType);
                    }
                }, 100);
                break;
            case LivessType.LIVESS_HEAD_RIGHT://

                currentStreamID = poolMap.get("head_right");
//                sndPool.play(currentStreamID, 1.0f, 1.0f, 0, 0, 1.0f);
                //????????????????????????????????????,???????????????,?????????????????????
                mMainHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        startTimerRunnable(Bulider.timerCount);
                        cloudwalkSDK.cwStartLivess(livessType);
                    }
                }, 100);
                break;

            case LivessType.LIVESS_MOUTH://
                // mIv_step.setImageResource(R.drawable.biyan);//

                currentStreamID = poolMap.get("mouth_open");
//                sndPool.play(currentStreamID, 1.0f, 1.0f, 0, 0, 1.0f);
                //????????????????????????????????????,???????????????,?????????????????????
                mMainHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        startTimerRunnable(Bulider.timerCount);
                        cloudwalkSDK.cwStartLivess(livessType);
                    }
                }, 100);
                break;
            case LivessType.LIVESS_EYE://

                currentStreamID = poolMap.get("eye_blink");
//                sndPool.play(currentStreamID, 1.0f, 1.0f, 0, 0, 1.0f);
                //????????????????????????????????????,???????????????,?????????????????????
                mMainHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        startTimerRunnable(Bulider.timerCount);
                        cloudwalkSDK.cwStartLivess(livessType);
                    }
                }, 100);

                break;

        }

    }


    private void updateStepLayout(int livessType) {

        View view = viewList.get(currentStep);

        mTv_step = (TextView) view.findViewById(R.id.cloudwalk_face_step_tv);
        mIv_step = (ImageView) view.findViewById(R.id.cloudwalk_face_step_img);
        mPb_step.setVisibility(View.VISIBLE);
        mPb_step.setMax(Bulider.timerCount);
        mPb_step.setProgress(Bulider.timerCount);

        switch (livessType) {
            case LivessType.LIVESS_HEAD_LEFT:
                mIv_step.setImageResource(R.drawable.cloudwalk_left_anim);

                mTv_step.setText(R.string.cloudwalk_live_headleft);
                animationDrawable = (AnimationDrawable) mIv_step.getDrawable();
                animationDrawable.start();

                break;
            case LivessType.LIVESS_HEAD_RIGHT:
                mIv_step.setImageResource(R.drawable.cloudwalk_right_anim);
                mTv_step.setText(R.string.cloudwalk_live_headright);
                animationDrawable = (AnimationDrawable) mIv_step.getDrawable();
                animationDrawable.start();

                break;
            case LivessType.LIVESS_MOUTH://
                mIv_step.setImageResource(R.drawable.cloudwalk_mouth_anim);
                mTv_step.setText(R.string.cloudwalk_live_mouth);
                animationDrawable = (AnimationDrawable) mIv_step.getDrawable();
                animationDrawable.start();

                break;
            case LivessType.LIVESS_EYE://
                mIv_step.setImageResource(R.drawable.cloudwalk_eye_anim);
                mTv_step.setText(R.string.cloudwalk_live_eye);

                animationDrawable = (AnimationDrawable) mIv_step.getDrawable();
                animationDrawable.start();

                break;

        }

        mViewPager.setCurrentItem(currentStep, true);
    }

    private class ViewPagerAdapter extends PagerAdapter {
        private List<View> mListViews;

        public ViewPagerAdapter(List<View> mListViews) {
            this.mListViews = mListViews;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView(mListViews.get(position));
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            container.addView(mListViews.get(position), 0);
            return mListViews.get(position);
        }

        @Override
        public int getCount() {
            return mListViews.size();
        }

        @Override
        public boolean isViewFromObject(View arg0, Object arg1) {
            return arg0 == arg1;
        }
    }

    @Override
    public void detectFaceInfo(FaceInfo[] faceInfos, int faceNum) {
        if (faceNum > 0) {// ????????????, ?????????

        } else {// ???????????????,????????????

        }

    }


    @Override
    public void detectLivess(int livessType, byte[] imageData) {
        cloudwalkSDK.cwStopLivess();
        stopTimerRunnable();
        if (Bulider.isLivesPicReturn) {
            Bulider.liveDatas.put(livessType, imageData);
        }
        if (isSetResult || isStop)
            return;
        switch (livessType) {

            case CW_LivenessCode.CW_FACE_LIVENESS_HEADLEFT:
                synchronized (FaceActivity.class) {
                    currentStep++;
                }
                doNextStep();

                break;
            case CW_LivenessCode.CW_FACE_LIVENESS_HEADRIGHT:
                synchronized (FaceActivity.class) {
                    currentStep++;
                }
                doNextStep();
                break;
            case CW_LivenessCode.CW_FACE_LIVENESS_BLINK:
                synchronized (FaceActivity.class) {
                    currentStep++;
                }
                doNextStep();
                break;
            case CW_LivenessCode.CW_FACE_LIVENESS_OPENMOUTH:
                synchronized (FaceActivity.class) {
                    currentStep++;
                }
                doNextStep();
                break;
            case CW_LivenessCode.CW_FACE_LIVENESS_HEADPITCH:
                synchronized (FaceActivity.class) {
                    currentStep++;
                }
                doNextStep();
                break;
            case CW_LivenessCode.CW_FACE_LIVENESS_HEADDOWN:
                synchronized (FaceActivity.class) {
                    currentStep++;
                }
                doNextStep();
                break;

        }

    }

    /**
     * ????????????????????????
     *
     * @param info
     */
    @Override
    public void detectInfo(final int info) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //?????????????????????,??????????????????
                if (info == FaceInterface.CW_LivenessCode.CW_FACE_INFO_WAIT_NEXT_FRAME && mLastPrepareInfoTime == 0) {
                    return;
                }
                long currentInfoTime = System.currentTimeMillis();
                if (currentInfoTime - mLastPrepareInfoTime > DURATION_PREPARE_INFO) {
                    mLastPrepareInfoTime = currentInfoTime;
                    if (currentStep == 0 && viewList != null && viewList.get(0) != null) {
                        TextView tv_Info = (TextView) viewList.get(0).findViewById(R.id.cloudwalk_face_info_txt);
                        if (tv_Info != null) {
                            switch (info) {
                                case FaceInterface.CW_LivenessCode.CW_FACE_INFO_WAIT_NEXT_FRAME://too far
                                    tv_Info.setText(R.string.cloudwalk_tip_not_center);
                                    break;
                                case FaceInterface.CW_LivenessCode.CW_FACE_INFO_TOO_FAR://too far
                                    tv_Info.setText(R.string.cloudwalk_tip_too_far);
                                    break;
                                case FaceInterface.CW_LivenessCode.CW_FACE_INFO_TOO_CLOSE://too close
                                    tv_Info.setText(R.string.cloudwalk_tip_too_close);
                                    break;
                                case FaceInterface.CW_LivenessCode.CW_FACE_INFO_NOT_FRONTAL://not frontal
                                    tv_Info.setText(R.string.cloudwalk_tip_not_frontal);
                                    break;
                                case FaceInterface.CW_LivenessCode.CW_FACE_INFO_NOT_STABLE://not stable
                                    tv_Info.setText(R.string.cloudwalk_tip_not_stable);
                                    break;
                                case FaceInterface.CW_LivenessCode.CW_FACE_INFO_TOO_DARK://too dark
                                    tv_Info.setText(R.string.cloudwalk_tip_too_dark);

                                    break;
                                case FaceInterface.CW_LivenessCode.CW_FACE_INFO_TOO_BRIGHT://too bright
                                    tv_Info.setText(R.string.cloudwalk_tip_too_bright);

                                    break;
                                case FaceInterface.CW_LivenessCode.CW_FACE_INFO_NOT_CENTER://not center
                                    tv_Info.setText(R.string.cloudwalk_tip_not_center);
                                    break;
                                case FaceInterface.CW_LivenessCode.CW_FACE_INFO_FACE_SHIELD://face shield
                                    tv_Info.setText(R.string.cloudwalk_tip_face_shield);
                                    break;
                                case FaceInterface.CW_LivenessCode.CW_FACE_INFO_GLASS:
                                    tv_Info.setText(R.string.cloudwalk_tip_glass);
                                    break;
                                case FaceInterface.CW_FaceDETCode.CW_FACE_NO_FACE:


                                    tv_Info.setText(R.string.cloudwalk_tip_no_face);
                                    break;

                                //??????????????????
                                case FaceInterface.CW_FaceDETCode.CW_FACE_UNAUTHORIZED_ERR:
                                    tv_Info.setText(R.string.facedectfail_appid);
                                    break;
                            }
                        }
                    }
                }
            }
        });
    }

    @Override
    public void detectReady() {
        synchronized (FaceActivity.class) {
            currentStep++;
        }
        cloudwalkSDK.cwStopLivess();//?????????????????????
        if (isStartDetectFace) {//?????????????????????,??????????????????
            doNextStep();
        } else {
            mMainHandler.sendEmptyMessageDelayed(DETECT_READY, 400);
        }
    }

    @Override
    public void detectFinished() {
    }

    @Override
    public void OnActionNotStandard(int notStandardType) {

        if (currentStep != 0 && !isLivePass) {
            if (cloudwalkSDK != null) {
                cloudwalkSDK.cwStopLivess();
            }
            mMainHandler.obtainMessage(SET_RESULT, notStandardType).sendToTarget();
        }

    }

    public static class MainHandler extends Handler {
        private final WeakReference<FaceActivity> mActivity;

        public MainHandler(FaceActivity activity) {
            mActivity = new WeakReference<FaceActivity>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            FaceActivity activity = mActivity.get();
            if (activity == null)
                return;
            switch (msg.what) {

                case SET_RESULT:
                    Integer resultCode = (Integer) msg.obj;
                    activity.setFaceResult(false, 0d, "", resultCode, null);
                    break;

                case UPDATE_STEP_PROCRESS:

                    Integer progress = (Integer) msg.obj;
                    activity.mPb_step.setProgress(progress);
                    break;

                case UPDATESTEPLAYOUT:
                    activity.updateStepLayout(activity.execLiveList.get(activity.currentStep - 1));
                    break;
                case PLAYMAIN_END:
                    activity.isStartDetectFace = true;//
                    break;

                case NEXT_STEP:
                    if (activity.totalStep == activity.currentStep) {// ??????????????????
                        if (activity.mPreview != null) {
                            activity.mPreview.setPushFrame(false);//??????????????????,????????????push?????????
                        }
                        activity.getBestFace();//??????????????????/???????????????/???????????????/????????????????????????
                        if (Bulider.isServerLive) {
                            activity.doFaceSerLivess();
                        } else if (Bulider.isFrontHack) {
                            activity.doFrontFaceLivess();//?????????????????????
                        }
                    } else {
                        if (activity.currentStep == 1) {
//                            activity.clearBestFace();
                        }
                        activity.startLivessDetect(activity.execLiveList.get(activity.currentStep - 1));
                    }
                    break;
                case DETECT_READY:
                    removeMessages(DETECT_READY);
                    if (activity.isStartDetectFace) {
                        activity.doNextStep();
                    } else {
                        sendEmptyMessageDelayed(DETECT_READY, 400);
                    }
                    break;
                case SOUND_AGAIN:

                    break;
                case FRAME_INFO:
                    txtFrame.setText(msg.getData().getInt("frame", 0) + "fps");
                    break;

            }
            super.handleMessage(msg);
        }

    }

    private void startTimerRunnable(int count) {

        faceTimerRunnable = new TimerRunnable(count, this, poolMap, currentStreamID);

        mMainHandler.postDelayed(faceTimerRunnable, 0);

    }

    void stopTimerRunnable() {
        if (faceTimerRunnable != null)
            faceTimerRunnable.setFlag(false);
    }

    static class TimerRunnable implements Runnable {

        private final WeakReference<FaceActivity> mActivity;
        private final Map<String, Integer> poolMap;
        private final int currentStreamID;
        private final int oriCount;

        int djsCount;
        boolean flag = true;

        public boolean isFlag() {
            return flag;
        }

        public void setFlag(boolean flag) {
            this.flag = flag;
        }

        public TimerRunnable(int djsCount, FaceActivity activity, Map<String, Integer> poolMap, int currentStreamID) {
            super();
            this.djsCount = djsCount;
            this.oriCount = djsCount;
            mActivity = new WeakReference<FaceActivity>(activity);
            this.poolMap = poolMap;
            this.currentStreamID = currentStreamID;
        }

        public void run() {
            FaceActivity act = mActivity.get();
            if (!flag || act == null)
                return;

            act.mMainHandler.obtainMessage(UPDATE_STEP_PROCRESS, djsCount).sendToTarget();
            djsCount--;

            if (djsCount >= 0) {
                act.mMainHandler.postDelayed(act.faceTimerRunnable, 1000);

                //??????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
                if (Bulider.timerCount >= 7) {
                    if ((oriCount / 2 - 1) == djsCount) {
                        if (currentStreamID == poolMap.get("mouth_open")) {
//                            sndPool.play(poolMap.get("open_mouth_widely"), 1.0f, 1.0f, 0, 0, 1.0f);
                        } else {
//                            sndPool.play(poolMap.get("try_again"), 1.0f, 1.0f, 0, 0, 1.0f);
                        }
                    }
                }

            } else {
                //?????????,???????????????????????????,????????????
                if (act.cloudwalkSDK != null) {
                    act.cloudwalkSDK.cwStopLivess();//??????????????????
                }
                act.mMainHandler.obtainMessage(SET_RESULT, CW_LivenessCode.CW_FACE_LIVENESS_OVERTIME).sendToTarget();

            }

        }
    }
}