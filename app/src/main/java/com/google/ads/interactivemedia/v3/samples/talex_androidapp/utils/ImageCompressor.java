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
    // Đảm bảo dung lượng nhẹ nhưng chất lượng hình ảnh là tốt nhất
    private static final int COMPRESS_QUALITY = 95;

    /**
     * Thuật toán Cookie Cutter: Ánh xạ tọa độ UI để cắt chính xác phần ảnh bên trong khung
     */
    public static File processAndCropImage(Context context, Uri uri, String partName,
                                           int viewWidth, int viewHeight,
                                           float frameX, float frameY, float frameW, float frameH) {
        try {
            // 1. Đọc ảnh gốc chụp từ cảm biến Camera
            InputStream inputStream = context.getContentResolver().openInputStream(uri);
            Bitmap originalBitmap = BitmapFactory.decodeStream(inputStream);
            if (inputStream != null) inputStream.close();

            if (originalBitmap == null) return null;

            // 2. Chống méo: Đọc EXIF và xoay ảnh về đúng chiều gốc
            InputStream exifStream = context.getContentResolver().openInputStream(uri);
            ExifInterface exif = new ExifInterface(exifStream);
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            if (exifStream != null) exifStream.close();

            Matrix matrix = new Matrix();
            if (orientation == ExifInterface.ORIENTATION_ROTATE_90) matrix.postRotate(90);
            else if (orientation == ExifInterface.ORIENTATION_ROTATE_180) matrix.postRotate(180);
            else if (orientation == ExifInterface.ORIENTATION_ROTATE_270) matrix.postRotate(270);

            Bitmap rotatedBitmap = Bitmap.createBitmap(originalBitmap, 0, 0, originalBitmap.getWidth(), originalBitmap.getHeight(), matrix, true);
            if (rotatedBitmap != originalBitmap) originalBitmap.recycle();

            // 3. TOÁN HỌC ÁNH XẠ: Tính toán tỷ lệ Camera Preview đè lên Màn hình
            float scaleX = (float) viewWidth / rotatedBitmap.getWidth();
            float scaleY = (float) viewHeight / rotatedBitmap.getHeight();
            float scale = Math.max(scaleX, scaleY); // Lấy tỷ lệ lớn hơn do PreviewView dùng CenterCrop

            float scaledW = rotatedBitmap.getWidth() * scale;
            float scaledH = rotatedBitmap.getHeight() * scale;

            // Độ lệch tâm (Do ảnh bị phóng to tràn ra ngoài màn hình)
            float dx = (scaledW - viewWidth) / 2f;
            float dy = (scaledH - viewHeight) / 2f;

            // Ánh xạ tọa độ Khung vàng (UI) ngược về tọa độ của Ảnh gốc (Bitmap)
            int cropX = Math.round((frameX + dx) / scale);
            int cropY = Math.round((frameY + dy) / scale);
            int cropW = Math.round(frameW / scale);
            int cropH = Math.round(frameH / scale);

            // Chốt chặn an toàn: Ngăn lỗi tràn viền (Out of Bounds) nếu khung quá sát mép
            cropX = Math.max(0, cropX);
            cropY = Math.max(0, cropY);
            if (cropX + cropW > rotatedBitmap.getWidth()) cropW = rotatedBitmap.getWidth() - cropX;
            if (cropY + cropH > rotatedBitmap.getHeight()) cropH = rotatedBitmap.getHeight() - cropY;

            // 4. TIẾN HÀNH CẮT ẢNH
            Bitmap croppedBitmap = Bitmap.createBitmap(rotatedBitmap, cropX, cropY, cropW, cropH);
            if (croppedBitmap != rotatedBitmap) rotatedBitmap.recycle();

            // 5. Lưu thành File thực tế chuẩn bị gửi Backend
            File tempFile = new File(context.getCacheDir(), "ekyc_" + partName + "_" + System.currentTimeMillis() + ".jpg");
            FileOutputStream outStream = new FileOutputStream(tempFile);
            croppedBitmap.compress(Bitmap.CompressFormat.JPEG, COMPRESS_QUALITY, outStream);
            outStream.flush();
            outStream.close();

            // 6. TẠO FILE DEBUG ĐỂ BẠN TỰ KIỂM TRA MẮT THƯỜNG
            File debugDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
            File debugFile = new File(debugDir, "DEBUG_CROPPED_" + partName + "_" + System.currentTimeMillis() + ".jpg");
            FileOutputStream debugOut = new FileOutputStream(debugFile);
            croppedBitmap.compress(Bitmap.CompressFormat.JPEG, COMPRESS_QUALITY, debugOut);
            debugOut.flush();
            debugOut.close();

            // Log chi tiết dữ liệu
            long fileSizeKB = tempFile.length() / 1024;
            Log.d(TAG, "========== CROPPED PAYLOAD ==========");
            Log.d(TAG, "[Field Name]: " + partName);
            Log.d(TAG, "[Cropped Resolution]: " + croppedBitmap.getWidth() + "x" + croppedBitmap.getHeight());
            Log.d(TAG, "[File Size]: " + fileSizeKB + " KB");
            Log.d(TAG, "[Debug Path]: " + debugFile.getAbsolutePath());
            Log.d(TAG, "=====================================");

            croppedBitmap.recycle();
            return tempFile;

        } catch (Exception e) {
            Log.e(TAG, "Lỗi cắt gọt ảnh", e);
            return null;
        }
    }

    /**
     * Đóng gói File thành MultipartBody
     */
    public static MultipartBody.Part buildMultipart(String partName, File file) {
        if (file == null || !file.exists()) return null;
        RequestBody requestFile = RequestBody.create(MediaType.parse("image/jpeg"), file);
        return MultipartBody.Part.createFormData(partName, file.getName(), requestFile);
    }
}