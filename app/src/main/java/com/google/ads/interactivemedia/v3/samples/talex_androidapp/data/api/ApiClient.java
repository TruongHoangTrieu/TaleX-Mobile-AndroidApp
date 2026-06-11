package com.google.ads.interactivemedia.v3.samples.talex_androidapp.data.api;

import com.google.ads.interactivemedia.v3.samples.talex_androidapp.BuildConfig;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {
    private static Retrofit retrofit = null;

    public static ApiService getApiService() {
        if (retrofit == null) {
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();

            // 📍 ĐƠN THUỐC BẢO MẬT: Phân tách môi trường chạy App tự động
            if (BuildConfig.DEBUG) {
                // Khi đang code bằng máy ảo/cắm cáp: Chỉ in HEADERS (Phương thức POST/GET, Mã 200/400, Độ dài)
                // Tuyệt đối KHÔNG in BODY để giấu nhẹm mật khẩu và token mới tinh đi
                logging.setLevel(HttpLoggingInterceptor.Level.HEADERS);
            } else {
                // Khi đóng gói file APK thành phẩm nộp bài hoặc phát hành: Tắt hẳn Log bảo mật
                logging.setLevel(HttpLoggingInterceptor.Level.NONE);
            }

            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(logging)
                    .build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(BuildConfig.BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(client)
                    .build();
        }
        return retrofit.create(ApiService.class);
    }
}