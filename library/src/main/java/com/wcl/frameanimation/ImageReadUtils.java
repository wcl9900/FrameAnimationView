package com.wcl.frameanimation;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * <p>Describe:图片读取工具类
 * <p>Author:王春龙
 */
public class ImageReadUtils {
    /**
     * 压缩读取资源图片
     * @param context
     * @param assetPath
     * @param compressSize
     * @param config
     * @return
     */
    public static Bitmap getAssetBitmap(Context context , String assetPath, int compressSize, Bitmap.Config config){
        InputStream is;
        try {
            is = context.getResources().getAssets().open(assetPath);
            return getAssetBitmap(is, compressSize, config);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
    /**
     * 压缩读取资源图片
     * @param context
     * @param is
     * @param compressSize
     * @param config
     * @return
     */
    public static Bitmap getAssetBitmap(InputStream is, int compressSize, Bitmap.Config config){
        BitmapFactory.Options opt = new BitmapFactory.Options();
        opt.inPreferredConfig = config;
        opt.inPurgeable = true;
        opt.inInputShareable = true;
        opt.inSampleSize = compressSize;

        Bitmap bm = null;
        try {
            bm = BitmapFactory.decodeStream(is, null, opt);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        finally{
            if(is != null){
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return bm;
    }

    public static String getFolderName(String filePath) {

        if (filePath == null || filePath.length() == 0) {
            return filePath;
        }

        int filePos = filePath.lastIndexOf(File.separator);
        return (filePos == -1) ? "" : filePath.substring(0, filePos);
    }
}
