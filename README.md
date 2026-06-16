# TaleX-AndroidApp

## 1. Tổng quan dự án

TaleX-AndroidApp là một ứng dụng di động được phát triển trên nền tảng Android, cung cấp một giao diện người dùng để tương tác với các dịch vụ backend của TaleX. Dự án này tập trung vào việc hiển thị thông tin phim ảnh, quản lý tài khoản người dùng, đăng nhập, đăng ký và các tính năng liên quan.

## 2. Công nghệ và Thư viện sử dụng

Dự án này sử dụng các công nghệ và thư viện hiện đại cho phát triển Android:

*   **Ngôn ngữ lập trình:** Java
*   **Nền tảng:** Android
*   **Kiến trúc:** Fragment-based UI, MVP/MVVM (dựa trên cấu trúc dự án hiện có)
*   **UI/UX:**
    *   `androidx.appcompat`: Hỗ trợ tương thích ngược cho các tính năng của Android.
    *   `androidx.activity`: Cải thiện việc xử lý Activity.
    *   `androidx.constraintlayout`: Xây dựng giao diện người dùng linh hoạt và phẳng.
    *   `com.google.android.material`: Các Material Design Components cho UI đẹp và hiện đại.
*   **Networking:**
    *   `com.squareup.retrofit2:retrofit`: REST Client cho Android và Java.
    *   `com.squareup.retrofit2:converter-gson`: Bộ chuyển đổi Gson cho Retrofit để xử lý JSON.
    *   `com.squareup.okhttp3:logging-interceptor`: Logging cho OkHttp để giám sát các yêu cầu mạng.
*   **Quản lý hình ảnh:**
    *   `com.github.bumptech.glide:glide`: Thư viện tải và hiển thị hình ảnh hiệu quả.
*   **Bảo mật:**
    *   `androidx.security:security-crypto`: Mã hóa dữ liệu trong SharedPreferences.
    *   `com.google.android.gms:play-services-auth`: Tích hợp xác thực Google.
*   **Gradle:** Hệ thống xây dựng dự án.

## 3. Cấu trúc thư mục dự án

Dự án tuân theo cấu trúc thư mục tiêu chuẩn của một ứng dụng Android:

```
TaleX-AndroidApp/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/google/ads/interactivemedia/v3/samples/talex_androidapp/
│   │   │   │   ├── data/                 # Chứa các lớp liên quan đến dữ liệu (API, Models)
│   │   │   │   │   ├── api/              # Định nghĩa Retrofit Service, ApiClient
│   │   │   │   │   └── model/            # Các lớp POJO (Request, Response)
│   │   │   │   └── ui/                   # Chứa các lớp liên quan đến giao diện người dùng (Activities, Fragments)
│   │   │   │   │   ├── account/          # Fragment cho màn hình tài khoản
│   │   │   │   │   ├── login/            # Activities cho đăng nhập, đăng ký, xác thực OTP
│   │   │   │   │   └── movies/           # Fragment cho màn hình danh sách phim
│   │   │   │   └── MainActivity.java     # Activity chính của ứng dụng
│   │   │   └── res/                      # Tài nguyên của ứng dụng (layouts, drawables, values, menu)
│   │   │       ├── drawable/             # Hình ảnh, icons
│   │   │       ├── layout/               # Các tệp layout XML
│   │   │       ├── menu/                 # Các tệp menu XML (ví dụ: bottom_nav_menu.xml)
│   │   │       ├── mipmap/               # Launcher icons
│   │   │       └── values/               # Strings, colors, styles, themes
│   │   └── AndroidManifest.xml           # Khai báo các thành phần của ứng dụng
│   ├── build.gradle.kts                  # Cấu hình Gradle cho module `app`
│   └── ...
├── build.gradle.kts                      # Cấu hình Gradle tổng thể của dự án
├── settings.gradle.kts                   # Định nghĩa các module trong dự án
└── README.md                             # Tài liệu dự án này
```

## 4. Cài đặt môi trường

Để cài đặt môi trường phát triển cho dự án này, bạn cần:

1.  **Android Studio:** Tải xuống và cài đặt phiên bản Android Studio mới nhất từ [trang web chính thức của Android Developer](https://developer.android.com/studio).
2.  **SDK Android:** Đảm bảo bạn đã cài đặt Android SDK cho phiên bản Android mong muốn (ví dụ: API 34). Bạn có thể kiểm tra và cài đặt trong Android Studio (File -> Settings -> Appearance & Behavior -> System Settings -> Android SDK).
3.  **Clone Repository:**
    ```bash
    git clone <URL_TO_YOUR_REPOSITORY>
    cd TaleX-AndroidApp
    ```
4.  **Mở dự án trong Android Studio:**
    *   Mở Android Studio.
    *   Chọn "Open an existing Android Studio project".
    *   Điều hướng đến thư mục `TaleX-AndroidApp` mà bạn đã clone và chọn nó.
5.  **Gradle Sync:** Android Studio sẽ tự động thực hiện Gradle Sync. Nếu không, bạn có thể thực hiện thủ công bằng cách nhấp vào biểu tượng "Sync Project with Gradle Files" trên thanh công cụ hoặc qua `File -> Sync Project with Gradle Files`.

## 5. Scripts hữu ích

*   **Build Debug:**
    ```bash
    ./gradlew assembleDebug
    ```
    (Hoặc `gradlew.bat assembleDebug` trên Windows)

*   **Install Debug APK:**
    ```bash
    ./gradlew installDebug
    ```

*   **Clean Project:**
    ```bash
    ./gradlew clean
    ```

*   **Run Unit Tests:**
    ```bash
    ./gradlew testDebugUnitTest
    ```

*   **Run Android Instrumentation Tests:**
    ```bash
    ./gradlew connectedDebugAndroidTest
    ```

## 6. Quy trình phát triển đề xuất

Để đảm bảo quy trình phát triển mượt mà và chất lượng mã nguồn, chúng tôi đề xuất quy trình sau:

1.  **Tạo Branch:** Luôn làm việc trên một branch mới cho mỗi tính năng hoặc sửa lỗi. Tên branch nên mô tả rõ ràng mục đích, ví dụ: `feature/new-login-ui`, `bugfix/crash-on-profile`.
    ```bash
    git checkout -b feature/your-feature-name
    ```
2.  **Phát triển:** Viết mã, tuân thủ các nguyên tắc mã hóa và phong cách của dự án.
3.  **Test:** Chạy các bài kiểm tra đơn vị (unit tests) và kiểm tra tích hợp (instrumentation tests) để đảm bảo tính năng hoạt động như mong đợi và không gây ra lỗi hồi quy.
4.  **Commit:** Commit công việc của bạn thường xuyên với các thông điệp commit rõ ràng, súc tích.
    ```bash
    git add .
    git commit -m "feat: Add new user registration screen"
    ```
5.  **Đồng bộ với Main (hoặc Develop):** Thường xuyên rebase hoặc merge branch `main` (hoặc `develop`) vào branch của bạn để giải quyết xung đột sớm.
    ```bash
    git checkout main
    git pull origin main
    git checkout feature/your-feature-name
    git rebase main
    ```
6.  **Pull Request (PR):** Khi tính năng hoặc sửa lỗi hoàn tất, tạo một Pull Request lên branch `main` (hoặc `develop`).
    *   Mô tả rõ ràng các thay đổi, mục tiêu và cách kiểm thử trong PR.
    *   Yêu cầu review mã từ một thành viên khác trong nhóm.
7.  **Code Review:** Tham gia vào quá trình review mã, đưa ra và tiếp nhận phản hồi một cách xây dựng.
8.  **Merge:** Sau khi PR được chấp thuận, merge nó vào branch chính.
9.  **Xóa Branch:** Xóa branch tính năng/sửa lỗi sau khi đã merge thành công.
    ```bash
    git branch -d feature/your-feature-name
    git push origin --delete feature/your-feature-name
    ```

---
**Lưu ý:** `URL_TO_YOUR_REPOSITORY` cần được thay thế bằng URL thực tế của repository Git của bạn.
