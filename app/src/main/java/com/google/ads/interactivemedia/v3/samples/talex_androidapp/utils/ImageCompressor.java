package com.google.ads.interactivemedia.v3.samples.talex_androidapp.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;

public class ImageCompressor {

    private static final int MAX_WIDTH = 1920;
    private static final int MAX_HEIGHT = 1080;
    private static final int COMPRESS_QUALITY = 80;

    /**
     * Nén ảnh từ Uri và đóng gói thành MultipartBody.Part cho Retrofit.
     * @param context Context của Fragment/Activity
     * @param uri Đường dẫn ảnh chụp từ CameraX
     * @param partName Tên key (VD: "frontImage", "backImage", "cmnd") yêu cầu từ Backend
     * @return MultipartBody.Part hoặc null nếu có lỗi
     */
    public static MultipartBody.Part getMultipartFromUri(Context context, Uri uri, String partName) {
        try {
            // 1. Đọc luồng dữ liệu từ Uri
            InputStream inputStream = context.getContentResolver().openInputStream(uri);
            Bitmap originalBitmap = BitmapFactory.decodeStream(inputStream);
            if (inputStream != null) inputStream.close();

            if (originalBitmap == null) return null;

            // 2. Tính toán tỷ lệ thu nhỏ để bảo vệ RAM
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

            // 3. Resize ảnh xuống mức an toàn
            Bitmap resizedBitmap = Bitmap.createScaledBitmap(originalBitmap, finalWidth, finalHeight, true);

            // 4. Tạo file tạm thời trong thư mục Cache của App (Tự động xóa khi đầy, không cần xin quyền Storage)
            File tempFile = new File(context.getCacheDir(), "ekyc_" + System.currentTimeMillis() + ".jpg");
            FileOutputStream outStream = new FileOutputStream(tempFile);

            // Nén ảnh ra định dạng JPEG với chất lượng 80%
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, COMPRESS_QUALITY, outStream);
            outStream.flush();
            outStream.close();

            // Xóa Bitmap khỏi RAM ngay lập tức để tránh rò rỉ bộ nhớ
            originalBitmap.recycle();
            if (originalBitmap != resizedBitmap) {
                resizedBitmap.recycle();
            }

            // 5. Chuyển đổi file thành RequestBody và MultipartBody.Part
            RequestBody requestFile = RequestBody.create(MediaType.parse("image/jpeg"), tempFile);
            return MultipartBody.Part.createFormData(partName, tempFile.getName(), requestFile);

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}