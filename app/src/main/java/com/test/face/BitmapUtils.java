package com.test.face;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.media.FaceDetector;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Base64;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class BitmapUtils {
    /**
     * 采样率压缩(尺寸压缩）  按照图片宽高自动计算缩放比，图片质量有保障
     *
     * @param filePath  设置宽高并不是设置图片实际宽高，而是根据宽高自动计算缩放比，压缩后图片不会变形，宽高会根据计算的缩放比同时缩放，
     *                  宽高建议都设置300   设置300后图片大小为100-200KB，图片质量能接受；设置为400到500，图片大小为500-600kb，上传偏大，可自行设置
     * @param reqHeight
     * @param reqWidth
     * @return
     */
    public static Bitmap getSmallBitmap(String filePath, int reqHeight, int reqWidth) {
        Bitmap bm;
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.RGB_565;
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(filePath, options);
        //计算图片的缩放值
        final int height = options.outHeight;
        final int width = options.outWidth;
        if (height <= 0 && width <= 0) {
            bm = null;
        } else {
            int inSampleSize = 1;
            if (height > reqHeight || width > reqWidth) {
                final int heightRatio = Math.round((float) height / (float) reqHeight);
                final int widthRatio = Math.round((float) width / (float) reqWidth);
                inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;
            }
            options.inSampleSize = inSampleSize;
            options.inJustDecodeBounds = false;
            bm = getRotateBitmap(BitmapFactory.decodeFile(filePath, options), getExifOrientation(filePath));
        }
        return bm;
    }

    /**
     * 获取图片旋转角度
     */
    public static int getExifOrientation(String filepath) {
        int degree = 0;
        ExifInterface exif = null;
        try {
            exif = new ExifInterface(filepath);
        } catch (IOException ex) {
        }
        if (exif != null) {
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, -1);
            if (orientation != -1) {
                // We only recognize a subset of orientation tag values.
                switch (orientation) {
                    case ExifInterface.ORIENTATION_ROTATE_90:
                        degree = 90;
                        break;
                    case ExifInterface.ORIENTATION_ROTATE_180:
                        degree = 180;
                        break;
                    case ExifInterface.ORIENTATION_ROTATE_270:
                        degree = 270;
                        break;
                    default:
                        break;
                }
            }
        }
        return degree;
    }

    /**
     * 采样率压缩  按照图片宽高自动计算缩放比，图片质量有保障
     * 与上面方法以上，只是直接传入照片字节流数据
     */
    public static Bitmap getSmallBitmapByData(byte[] data, int reqHeight, int reqWidth) {
        if (data == null) {
            return null;
        }
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(data, 0, data.length, options);
        //计算图片的缩放值
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;
        if (height > reqHeight || width > reqWidth) {
            final int heightRatio = Math.round((float) height / (float) reqHeight);
            final int widthRatio = Math.round((float) width / (float) reqWidth);
            inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;
        }
        options.inSampleSize = inSampleSize;
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeByteArray(data, 0, data.length, options);
    }

    /**
     * 获取bitamap大小
     *
     * @param bitmap
     * @return
     */
    public static int getBitmapSize(Bitmap bitmap) {
        //API 19
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            return bitmap.getAllocationByteCount();
        }
        //API 12
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1)

        {
            return bitmap.getByteCount();
        }
        //earlier version
        return bitmap.getRowBytes() * bitmap.getHeight();
    }

    /**
     * 这是个保存Bitmap到sd卡中的方法，可以返回保存图片的路径
     * 保存Bitmap到sd
     *
     * @param mBitmap
     * @param bitName 图片保存的名称，返回存储图片的路径
     */
    public static String saveBitmap(Context context, Bitmap mBitmap, String bitName) {
        if (mBitmap == null) {
            return "";
        }
        File f;
        //判断是否有sd卡 有就保存到sd卡，没有就保存到app缓存目录
        if (isStorage()) {
            File file = context.getCacheDir();//保存的路径
            if (!file.exists()) {//判断目录是否存在
                file.mkdir();//不存在就创建目录
            }
            f = new File(file, bitName + ".jpg");
        } else {
            File file = new File(context.getCacheDir().toString());
            if (!file.exists()) {//判断目录是否存在
                file.mkdir();
            }
            f = new File(file, bitName + ".jpg");
        }
        FileOutputStream fOut = null;
        try {
            fOut = new FileOutputStream(f);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        if (fOut != null) {
            mBitmap.compress(Bitmap.CompressFormat.JPEG, 100, fOut);
            try {
                fOut.flush();
                fOut.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return f.toString();
    }

    /**
     * 判断是否有sd卡
     *
     * @return
     */
    public static boolean isStorage() {
        boolean isstorage = Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
        return isstorage;
    }

    /**
     * 把Bimtmap转成Base64，用于上传图片到服务器，一般是先压缩然后转成Base64，在上传
     */
    public static String getBitmapStrBase64(Bitmap bitmap) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
        byte[] bytes = baos.toByteArray();
        return Base64.encodeToString(bytes, Base64.NO_WRAP);
    }

    /**
     * 把bitmap转为base64 btye数组
     *
     * @param bitmap
     * @return
     */
    public static byte[] bitmapToBase64(Bitmap bitmap) {
        byte[] result = null;
        ByteArrayOutputStream baos = null;
        try {
            if (bitmap != null) {
                baos = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                baos.flush();
                baos.close();
                byte[] bitmapBytes = baos.toByteArray();
                result = Base64.encode(bitmapBytes, Base64.NO_WRAP);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (baos != null) {
                    baos.flush();
                    baos.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    /**
     * 把Base64转换成Bitmap
     */
    public static Bitmap getBitmapFromBase64(String iconBase64) {
        byte[] bitmapArray = Base64.decode(iconBase64, Base64.NO_WRAP);
        return BitmapFactory.decodeByteArray(bitmapArray, 0, bitmapArray.length);
    }

    /**
     * 把图片byte流转换成Base64
     */
    public static String byte2Base64(byte[] b) {
        return Base64.encodeToString(b, Base64.NO_WRAP);
    }

    /**
     * 把图片byte流转换成bitmap
     */
    public static Bitmap byteToBitmap(byte[] data) {
        if (data.length != 0) {
            return BitmapFactory.decodeByteArray(data, 0, data.length);
        } else {
            return null;
        }
    }

    /**
     * 保持bitmap 到SD卡
     *
     * @param bitmap
     * @param path
     * @throws IOException
     */
    public static void saveBitmap(Bitmap bitmap, String path) throws IOException {
        if (bitmap == null) {
            return;
        }
        File file = new File(path);
        if (file.exists()) {
            file.delete();
        }
        FileOutputStream out;
        try {
            out = new FileOutputStream(file);
            if (bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)) {//如果签名是png的话，则不管quality是多少，都不会进行质量的压缩
                out.flush();
                out.close();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 旋转Bitmap
     *
     * @param bm
     * @param degree        旋转角度
     * @param isCameraFront 是否前置摄像头
     * @return
     */
    public static Bitmap rotateBitmapByDegree(Bitmap bm, int degree, boolean isCameraFront) {
        Bitmap returnBm = null;

        // 根据旋转角度，生成旋转矩阵   一定要先旋转再缩放（镜像），不然图片会不正常显示
        Matrix matrix = new Matrix();
        matrix.postRotate(degree);
        //镜像处理  matrix.postScale(scale, isCameraFront() ? -scale : scale);   Scale是缩放   前置的话镜像为-，后置镜像为1
        matrix.postScale(1, isCameraFront ? -1 : 1);
        try {
            // 将原始图片按照旋转矩阵进行旋转，并得到新的图片
            returnBm = Bitmap.createBitmap(bm, 0, 0, bm.getWidth(), bm.getHeight(), matrix, true);
        } catch (OutOfMemoryError e) {
        }
        if (returnBm == null) {
            returnBm = bm;
        }
        if (bm != returnBm) {
            bm.recycle();
        }
        return returnBm;
    }

    /**
     * 根据uri获取bitmap
     *
     * @param context
     * @param uri
     * @return
     */
    public static Bitmap decodeUriAsBitmap(Context context, Uri uri) {
        Bitmap bitmap = null;
        try {
            if (uri != null) {
                bitmap = BitmapFactory.decodeStream(context.getContentResolver().openInputStream(uri));
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        }
        return bitmap;
    }

    /**
     * 旋转Bitmap
     *
     * @param b
     * @param rotateDegree
     * @return
     */
    public static Bitmap getRotateBitmap(Bitmap b, float rotateDegree) {
        Bitmap rotaBitmap = null;
        if (b != null) {
            Matrix matrix = new Matrix();
            matrix.postRotate(rotateDegree);
            rotaBitmap = Bitmap.createBitmap(b, 0, 0, b.getWidth(), b.getHeight(), matrix, false);
        }
        return rotaBitmap;
    }

    /**
     * 转成RGB_565照片，才能进行人脸检测
     *
     * @param tmp
     * @return
     */
    public static Bitmap getRGB_565Bitmap(byte[] tmp) {
        BitmapFactory.Options bitmapOption = new BitmapFactory.Options();
        bitmapOption.inPreferredConfig = Bitmap.Config.RGB_565;
        Bitmap map = BitmapFactory.decodeByteArray(tmp, 0, tmp.length, bitmapOption);
        return map;
    }

    public static int checkFace(Bitmap bitmap) {
        //保证 bitmap 的宽度为偶数
        if ((1 == (bitmap.getWidth() % 2))) {
            bitmap = Bitmap.createScaledBitmap(bitmap,
                    bitmap.getWidth() + 1, bitmap.getHeight(), false);
        }
        //检测人脸
        FaceDetector localFaceDetector = new FaceDetector(bitmap.getWidth(), bitmap.getHeight(), 3);
        FaceDetector.Face[] arrayOfFace = new FaceDetector.Face[3];
        //返回识别到的人脸数
        return localFaceDetector.findFaces(bitmap, arrayOfFace);
    }

    public static Bitmap zoomBitmap(Bitmap bm, int reqWidth, int reqHeight) {
        // 获得图片的宽高
        int width = bm.getWidth();
        int height = bm.getHeight();
        // 计算缩放比例
        float scaleWidth = ((float) reqWidth) / width;
        float scaleHeight = ((float) reqHeight) / height;
        float scale = Math.min(scaleWidth, scaleHeight);
        // 取得想要缩放的matrix参数
        Matrix matrix = new Matrix();
        matrix.postScale(scale, scale);
        // 得到新的图片
        Bitmap newbm = Bitmap.createBitmap(bm, 0, 0, width, height, matrix,
                true);
        return newbm;
    }

    /**
     * 裁剪
     * @param bitmap 原图
     * @return 裁剪后的图像
     */
    public static Bitmap cropBitmap(Bitmap bitmap) {
        int w = bitmap.getWidth(); // 得到图片的宽，高
        int h = bitmap.getHeight();
        int cropWidth = (int) (w * 0.8);// 裁切后所取的区域边长
        int cropHeight = (int) (h * 0.8);
        return Bitmap.createBitmap(bitmap, (int)(w * 0.1), (int)(h * 0.1), cropWidth, cropHeight, null, false);
    }

    /**
     * 质量压缩
     *
     * @param bmp
     * @param fileSize 尽量控制范围，如果质量值都是0了，还达不到要求，则不压缩了。 KB
     */
    public static boolean compressBmpToFile(Bitmap bmp, String path, int fileSize) {
        if (bmp == null || TextUtils.isEmpty(path)) {
            return false;
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int options = 80;
        bmp.compress(Bitmap.CompressFormat.JPEG, options, baos);
        while (options >= 10 && baos.toByteArray().length / 1024 > fileSize) {
            baos.reset();
            options -= 10;
            bmp.compress(Bitmap.CompressFormat.JPEG, options, baos);
        }
        try {
            FileOutputStream fos = new FileOutputStream(path);
            fos.write(baos.toByteArray());
            fos.flush();
            fos.close();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 质量压缩  子线程调用，避免ANR
     *
     * @param inPutPath
     * @param fileSize  尽量控制范围，如果质量值都是0了，还达不到要求，则不压缩了。 KB
     */
    public static boolean compressBmpToFile(String inPutPath, String outPutPath, int fileSize) {
        if (TextUtils.isEmpty(inPutPath) || TextUtils.isEmpty(outPutPath)) {
            return false;
        }
        Bitmap bmp = getSmallBitmap(inPutPath, 1280, 720);
        if (bmp == null) {
            return false;
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int options = 80;
        bmp.compress(Bitmap.CompressFormat.JPEG, options, baos);
        while (options >= 10 && baos.toByteArray().length / 1024 > fileSize) {
            baos.reset();
            options -= 10;
            bmp.compress(Bitmap.CompressFormat.JPEG, options, baos);
        }
        try {
            FileOutputStream fos = new FileOutputStream(outPutPath);
            fos.write(baos.toByteArray());
            fos.flush();
            fos.close();
            return true;
        } catch (Exception e) {
            return false;
        } finally {
            bmp.recycle();
            if (bmp != null) {
                bmp = null;
            }
            System.gc();
        }
    }
}
