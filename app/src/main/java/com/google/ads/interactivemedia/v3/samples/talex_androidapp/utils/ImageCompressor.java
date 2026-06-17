package com.google.ads.interactivemedia.v3.samples.talex_androidapp.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;

public class ImageCompressor {

    private static final String TAG = "EKYC_PAYLOAD_DEBUG";

    // Kích thước chuẩn 2K để FPT.AI đọc rõ chữ
    private static final int MAX_WIDTH = 2560;
    private static final int MAX_HEIGHT = 1440;
    // Chất lượng 95% để ảnh không bị vỡ hạt (Dung lượng < 3MB)
    private static final int COMPRESS_QUALITY = 95;

    public static MultipartBody.Part getMultipartFromUri(Context context, Uri uri, String partName) {
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(uri);
            Bitmap originalBitmap = BitmapFactory.decodeStream(inputStream);
            if (inputStream != null) inputStream.close();

            if (originalBitmap == null) return null;

            int width = originalBitmap.getWidth();
            int height = originalBitmap.getHeight();
            float ratioBitmap = (float) width / (float) height;
            float ratioMax = (float) MAX_WIDTH / (float) MAX_HEIGHT;

            int finalWidth = width;
            int finalHeight = height;
            if (ratioMax > ratioBitmap) {
                if (height > MAX_HEIGHT) {
                    finalHeight = MAX_HEIGHT;
                    finalWidth = (int) ((float) height * ratioBitmap);
                }
            } else {
                if (width > MAX_WIDTH) {
                    finalWidth = MAX_WIDTH;
                    finalHeight = (int) ((float) width / ratioBitmap);
                }
            }

            Bitmap resizedBitmap = Bitmap.createScaledBitmap(originalBitmap, finalWidth, finalHeight, true);

            // 1. Tạo file tạm ẩn để gửi API
            File tempFile = new File(context.getCacheDir(), "ekyc_upload_" + System.currentTimeMillis() + ".jpg");
            FileOutputStream outStream = new FileOutputStream(tempFile);
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, COMPRESS_QUALITY, outStream);
            outStream.flush();
            outStream.close();

            // 2. TẠO FILE DEBUG ĐỂ BẠN LẤY RA KIỂM TRA MẮT THƯỜNG (Sẽ xóa đoạn này khi app lên store)
            File debugDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
            File debugFile = new File(debugDir, "DEBUG_" + partName + "_" + System.currentTimeMillis() + ".jpg");
            FileOutputStream debugOut = new FileOutputStream(debugFile);
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, COMPRESS_QUALITY, debugOut);
            debugOut.flush();
            debugOut.close();

            // 3. LOG THÔNG SỐ CHI TIẾT RA MÀN HÌNH
            long fileSizeKB = tempFile.length() / 1024;
            Log.d(TAG, "========== REQUEST PAYLOAD (MULTIPART) ==========");
            Log.d(TAG, "[Field Name]: " + partName);
            Log.d(TAG, "[File Name]: " + tempFile.getName());
            Log.d(TAG, "[Resolution]: " + finalWidth + " x " + finalHeight);
            Log.d(TAG, "[File Size]: " + fileSizeKB + " KB");
            Log.d(TAG, "[Debug Path]: " + debugFile.getAbsolutePath()); // Đường dẫn để bạn tìm ảnh
            Log.d(TAG, "=================================================");

            originalBitmap.recycle();
            if (originalBitmap != resizedBitmap) {
                resizedBitmap.recycle();
            }

            RequestBody requestFile = RequestBody.create(MediaType.parse("image/jpeg"), tempFile);
            return MultipartBody.Part.createFormData(partName, tempFile.getName(), requestFile);

        } catch (Exception e) {
            Log.e(TAG, "Lỗi nén ảnh", e);
            return null;
        }
    }
}