package com.test.face;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.hardware.input.InputManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.InputDevice;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.HashMap;


import cn.cloudwalk.FaceInterface;
import cn.cloudwalk.libproject.Bulider;
import cn.cloudwalk.libproject.callback.ResultCallBack;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static cn.cloudwalk.libproject.Bulider.liveLevel;

public class MainActivity extends AppCompatActivity {
    private Button button1;
    private Button button;
    private Button button2;
    private Button button3;
    private ImageView imageView;
    private TextView tvResult;
    public static String LICENCE="MDM1MzEwbm9kZXZpY2Vjd2F1dGhvcml6ZZfn5OXl5+Tq3+bg5efm5ef65OXl4Obg5Yjm5uvl5ubrkeXm5uvl5uai6+Xm5uvl5qTm6+Xm5ufk++bn5uQ=";
    /**
     * 活体配置 默认值
     */
    public static int mLiveCount = 2;
    public static int mLiveTime=8;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        button1 = (Button)findViewById(R.id.btn_mnq);
        button = (Button)findViewById(R.id.btn_login_by_face);
        button2 = (Button)findViewById(R.id.btn_login_by_face_living);
        button3 = (Button)findViewById(R.id.btn_face_circular);
        imageView = (ImageView)findViewById(R.id.image);
        tvResult = (TextView) findViewById(R.id.tv_result);
        button1.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.M)
            @Override
            public void onClick(View v) {
                getBatter();
                if(checkCPU()) {
                    Toast.makeText(MainActivity.this, "这是模拟器", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this, "这是真机 ", Toast.LENGTH_SHORT).show();
                }
            }
        });
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                boolean s = checkUSB();
                requestPermission(1);
            }
        });
        button2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestPermission(2);
            }
        });
        button3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestPermission(3);
            }
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void getBatter() {
        BatteryManager manager = (BatteryManager) getSystemService(BATTERY_SERVICE);
        int currentLevel = manager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
        Log.i("TAG", "getBatter: " + currentLevel);
    }

    /**
     * 获取设备的CPU类型（ABIs） 判断是否是模拟器，如果为x86则疑似为模拟器
     * armeabiv-v7a: 第7代及以上的 ARM 处理器。2011年15月以后的生产的大部分Android设备都使用它.
     * arm64-v8a: 第8代、64位ARM处理器，很少设备，三星 Galaxy S6是其中之一。
     * armeabi: 第5代、第6代的ARM处理器，早期的手机用的比较多。
     * x86: 平板、模拟器用得比较多。
     * x86_64: 64位的平板。
     * @return true 是模拟器
     */
    private boolean checkCPU() {
        String[] cpuInfo = Build.SUPPORTED_ABIS;
        StringBuilder str = new StringBuilder();
        for (int i = 0; i < cpuInfo.length; i++) {
            str.append(cpuInfo[i]).append(",");
        }
        /**
         * 如果包含x86就疑似为模拟器
         */
        if(str.toString().contains("x86")) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * 判断是否包含SIM卡
     * @return false 没有sim卡默认为模拟器
     */
    private boolean checkSIM() {
        TelephonyManager telMgr = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
        int simState = telMgr.getSimState();
        Log.i("TAG", "hasSimCard: " + simState);
        Toast.makeText(MainActivity.this, "" + simState, Toast.LENGTH_SHORT).show();
        boolean result = true;
        switch (simState) {
            case TelephonyManager.SIM_STATE_ABSENT:
            case TelephonyManager.SIM_STATE_UNKNOWN:
                result = false; // 没有SIM卡
                break;
            default:
                result = true;
        }
        return result;
    }

    /**
     * 检测usb接口的信息
     * 测试模拟器 蓝叠、夜神、MUMU、雷电都包含 Android Power Button 这一条信息。
     * 测试真机 不包含
     * @return true 疑似为模拟器
     */
    private boolean checkUSB() {
        InputManager im = (InputManager) getSystemService(INPUT_SERVICE);
        int[] devices = im.getInputDeviceIds();
        StringBuilder str = new StringBuilder();
        for (int id : devices) {
            InputDevice device = im.getInputDevice(id);
            str.append(id).append("-").append(device.getName()).append(" ; ");
            Log.d("----------------", "-------===id: " + id);
            Log.d("----------------", "-------===name: " + device.getName());
        }
        Toast.makeText(MainActivity.this, "" + str, Toast.LENGTH_SHORT).show();
        if(str.toString().contains("Power Button")) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * 检测相机和读写权限
     */
    public void requestPermission(int t) {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PERMISSION_GRANTED) {
            //先判断有没有权限 ，没有就在这里进行权限的申请
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
        } else {
            //说明已经获取到摄像头权限了
            if (t == 1) {
                faceCollect();
            } else if(t == 2) {
                Intent intent = new Intent(MainActivity.this, FaceScanActivity.class);
                startActivityForResult(intent, 9);
//                faceImgCollect();
            } else if(t == 3) {
                Intent intent = new Intent(MainActivity.this, FaceCircularActivity.class);
                startActivityForResult(intent, 10);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == 9 && data != null) {
            Bitmap bitmap = data.getParcelableExtra("face");
            if(bitmap != null) {
                String str = BitmapUtils.getBitmapStrBase64(bitmap);
                Bitmap bitmap2 = BitmapUtils.cropBitmap(bitmap);
                String str2 = BitmapUtils.getBitmapStrBase64(bitmap2);
                LogUtil.loge("TAG", str2);
                Glide.with(MainActivity.this).load(bitmap2).into(imageView);
            }
        } else if(resultCode == 10 && data != null) {
            Bitmap bitmap = data.getParcelableExtra("face");
            if(bitmap != null) {
                String str = BitmapUtils.getBitmapStrBase64(bitmap);
                Bitmap bitmap2 = BitmapUtils.cropBitmap(bitmap);
                String str2 = BitmapUtils.getBitmapStrBase64(bitmap2);
                LogUtil.loge("TAG", str2);
                Glide.with(MainActivity.this).load(bitmap2).into(imageView);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            for (int i = 0; i < permissions.length; i++) {
                if (grantResults[i] == PERMISSION_GRANTED) {

                } else {
                    Toast.makeText(this, "" + "Please open " + permissions[i] + " permissions and try again", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    /**
     * 人脸采集
     */
    private void faceImgCollect() {
        ArrayList<Integer> liveList = new ArrayList<Integer>();
        liveList.add(FaceInterface.LivessType.LIVESS_MOUTH);
        liveList.add(FaceInterface.LivessType.LIVESS_HEAD_LEFT);
        liveList.add(FaceInterface.LivessType.LIVESS_HEAD_RIGHT);
        liveList.add(FaceInterface.LivessType.LIVESS_EYE);
        final Bulider bulider = new Bulider();
        bulider.setLicence(LICENCE).setResultCallBack(new ResultCallBack() {
            @Override
            public void result(boolean isLivePass, boolean isVerfyPass, String faceSessionId, double face_score, int resultType, byte[] bestFaceImgData, byte[] clipedBestFaceImgData, HashMap<Integer, byte[]> liveImgDatas) {
                Log.i("TAG", "result: " + liveImgDatas.toString());
                if (bestFaceImgData != null) {
                    if (isLivePass) {
                        bulider.setFaceResult(getApplication(), Bulider.FACE_VERFY_PASS, 0d, "", "");
                        //前端检测成功后存储相关人脸信息
                        if (null != bestFaceImgData && bestFaceImgData.length > 0) {
                            String str = BitmapUtils.byte2Base64(bestFaceImgData);
                            LogUtil.loge("TAG", str);
                            Bitmap bitmap = BitmapUtils.byteToBitmap(bestFaceImgData);
                            Glide.with(MainActivity.this).load(bitmap).into(imageView);
                            Log.i("TAG", "result: " + bitmap.getHeight());
                            Log.i("TAG", "result: " + bitmap.getWidth());
                            tvResult.setText("The test is successful");
                        } else {
                            Log.i("TAG", "result: 没有获取到清晰的人脸照片1");
                            tvResult.setText("No clear face photos were obtained 1");
                        }
                    } else {
                        bulider.setFaceResult(getApplication(), Bulider.FACE_VERFY_FAIL, 0d, "", "");
                        Log.i("TAG", "result: 没有获取到清晰的人脸2");
                        tvResult.setText("No clear face photos were obtained 2");
                    }
                } else {
                    bulider.setFaceResult(getApplication(), Bulider.FACE_VERFY_FAIL, 0d, "", "");
                    Log.i("TAG", "result: 没有获取到清晰的人脸3");
                    tvResult.setText("No clear face photos were obtained 3");
                }
            }
        }).isFrontHack(true)//前端防hack
                .isServerLive(false)
                .isResultPage(false)//活体检测页面开关
                .setLives(liveList, mLiveCount, true, true, liveLevel).setLiveTime(mLiveTime)
                .startActivity(MainActivity.this, FaceImgActivity.class);
    }

    /**
     * 活体验证
     */
    private void faceCollect() {
        ArrayList<Integer> liveList = new ArrayList<Integer>();
        liveList.add(FaceInterface.LivessType.LIVESS_MOUTH);
        liveList.add(FaceInterface.LivessType.LIVESS_HEAD_LEFT);
        liveList.add(FaceInterface.LivessType.LIVESS_HEAD_RIGHT);
        liveList.add(FaceInterface.LivessType.LIVESS_EYE);
        final Bulider bulider = new Bulider();
        bulider.setLicence(LICENCE).setResultCallBack(new ResultCallBack() {
            @Override
            public void result(boolean isLivePass, boolean isVerfyPass, String faceSessionId, double face_score, int resultType, byte[] bestFaceImgData, byte[] clipedBestFaceImgData, HashMap<Integer, byte[]> liveImgDatas) {
                if (bestFaceImgData != null) {
                    if (isLivePass) {
                        bulider.setFaceResult(getApplication(), Bulider.FACE_VERFY_PASS, 0d, "", "");
                        //前端检测成功后存储相关人脸信息
                        if (null != bestFaceImgData && bestFaceImgData.length > 0) {
                            String str = BitmapUtils.byte2Base64(bestFaceImgData);
                            LogUtil.loge("TAG", str);
                            Bitmap bitmap = BitmapUtils.byteToBitmap(bestFaceImgData);
                            Glide.with(MainActivity.this).load(bitmap).into(imageView);
                            Log.i("TAG", "result: " + bitmap.getHeight());
                            Log.i("TAG", "result: " + bitmap.getWidth());
                            tvResult.setText("The test is successful");
                        } else {
                            Log.i("TAG", "result: 没有获取到清晰的人脸照片1");
                            tvResult.setText("No clear face photos were obtained 1");
                        }
                    } else {
                        bulider.setFaceResult(getApplication(), Bulider.FACE_VERFY_FAIL, 0d, "", "");
                        Log.i("TAG", "result: 没有获取到清晰的人脸2");
                        tvResult.setText("No clear face photos were obtained 2");
                    }
                } else {
                    bulider.setFaceResult(getApplication(), Bulider.FACE_VERFY_FAIL, 0d, "", "");
                    Log.i("TAG", "result: 没有获取到清晰的人脸3");
                    tvResult.setText("No clear face photos were obtained 3");
                }
            }
        }).isFrontHack(true)//前端防hack
                .isServerLive(false)
                .isResultPage(false)//活体检测页面开关
//                .setPublicFilePath(publicFilePath)
                .setLives(liveList, mLiveCount, true, false/*不返回动作图*/, liveLevel/*废弃*/).setLiveTime(mLiveTime)
                .startActivity(MainActivity.this, FaceActivity.class);
    }
}