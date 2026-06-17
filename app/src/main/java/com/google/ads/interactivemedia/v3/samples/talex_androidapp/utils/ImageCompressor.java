package com.google.ads.interactivemedia.v3.samples.talex_androidapp.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
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

    // Khóa kích thước tối đa 2K để FPT.AI phân tích mượt nhất
    private static final int MAX_WIDTH = 2560;
    private static final int MAX_HEIGHT = 1440;
    private static final int COMPRESS_QUALITY = 95;

    public static MultipartBody.Part getMultipartFromUri(Context context, Uri uri, String partName) {
        try {
            // 1. Đọc ảnh gốc
            InputStream inputStream = context.getContentResolver().openInputStream(uri);
            Bitmap originalBitmap = BitmapFactory.decodeStream(inputStream);
            if (inputStream != null) inputStream.close();

            if (originalBitmap == null) return null;

            // 2. Sửa lỗi bóp méo do mất EXIF Rotation (Góc xoay camera)
            InputStream exifStream = context.getContentResolver().openInputStream(uri);
            ExifInterface exif = new ExifInterface(exifStream);
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            if (exifStream != null) exifStream.close();

            Matrix matrix = new Matrix();
            if (orientation == ExifInterface.ORIENTATION_ROTATE_90) {
                matrix.postRotate(90);
            } else if (orientation == ExifInterface.ORIENTATION_ROTATE_180) {
                matrix.postRotate(180);
            } else if (orientation == ExifInterface.ORIENTATION_ROTATE_270) {
                matrix.postRotate(270);
            }

            // Xoay ảnh về đúng chiều chuẩn của mắt người
            Bitmap rotatedBitmap = Bitmap.createBitmap(originalBitmap, 0, 0, originalBitmap.getWidth(), originalBitmap.getHeight(), matrix, true);
            if (rotatedBitmap != originalBitmap) {
                originalBitmap.recycle();
            }

            // 3. Khóa chết Aspect Ratio (Chống kéo giãn ảnh)
            int width = rotatedBitmap.getWidth();
            int height = rotatedBitmap.getHeight();
            float scale = Math.min((float) MAX_WIDTH / width, (float) MAX_HEIGHT / height);

            Bitmap finalBitmap = rotatedBitmap;
            // Chỉ thu nhỏ nếu ảnh quá lớn, tuyệt đối không phóng to ảnh nhỏ làm nhòe
            if (scale < 1f) {
                int finalWidth = Math.round(width * scale);
                int finalHeight = Math.round(height * scale);
                finalBitmap = Bitmap.createScaledBitmap(rotatedBitmap, finalWidth, finalHeight, true);
                if (finalBitmap != rotatedBitmap) {
                    rotatedBitmap.recycle();
                }
            }

            // 4. Lưu ảnh thực tế bay lên Server API
            File tempFile = new File(context.getCacheDir(), "ekyc_upload_" + System.currentTimeMillis() + ".jpg");
            FileOutputStream outStream = new FileOutputStream(tempFile);
            finalBitmap.compress(Bitmap.CompressFormat.JPEG, COMPRESS_QUALITY, outStream);
            outStream.flush();
            outStream.close();

            // 5. TẠO FILE DEBUG CHO BẠN (Lưu vào thư mục Pictures của máy điện thoại)
            File debugDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
            File debugFile = new File(debugDir, "DEBUG_" + partName + "_" + System.currentTimeMillis() + ".jpg");
            FileOutputStream debugOut = new FileOutputStream(debugFile);
            finalBitmap.compress(Bitmap.CompressFormat.JPEG, COMPRESS_QUALITY, debugOut);
            debugOut.flush();
            debugOut.close();

            // 6. LOG THÔNG SỐ VÀ ĐƯỜNG DẪN ẢNH ĐỂ BẠN TÌM
            long fileSizeKB = tempFile.length() / 1024;
            Log.d(TAG, "========== REQUEST PAYLOAD (MULTIPART) ==========");
            Log.d(TAG, "[Field Name]: " + partName);
            Log.d(TAG, "[File Name]: " + tempFile.getName());
            Log.d(TAG, "[Resolution]: " + finalBitmap.getWidth() + " x " + finalBitmap.getHeight());
            Log.d(TAG, "[File Size]: " + fileSizeKB + " KB");
            Log.d(TAG, "[Debug Path]: " + debugFile.getAbsolutePath());
            Log.d(TAG, "=================================================");

            finalBitmap.recycle();

            // Chuyển file thành Request để bắn qua Retrofit
            RequestBody requestFile = RequestBody.create(MediaType.parse("image/jpeg"), tempFile);
            return MultipartBody.Part.createFormData(partName, tempFile.getName(), requestFile);

        } catch (Exception e) {
            Log.e(TAG, "Lỗi nén ảnh", e);
            return null;
        }
    }
}