import java.util.Properties // ◄ 1. BẮT BUỘC PHẢI CÓ DÒNG NÀY Ở ĐỈNH FILE

plugins {
    alias(libs.plugins.android.application)
}

// ◄ 2. ĐOẠN CODE NÀY DÙNG ĐỂ ĐỌC FILE local.properties (BỊ THIẾU CỦA TRIỆU)
val properties = Properties().apply {
    val localPropertiesFile = project.rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        load(localPropertiesFile.inputStream())
    }
}

android {
    namespace = "com.google.ads.interactivemedia.v3.samples.talex_androidapp"
    compileSdk = 36 // Đã sửa nhẹ lại cú pháp gán chuẩn của Kotlin DSL

    defaultConfig {
        applicationId = "com.google.ads.interactivemedia.v3.samples.talex_androidapp"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Đọc dữ liệu từ file local.properties an toàn (Giờ đã có biến properties ở trên)
        val baseUrl = properties.getProperty("BASE_URL") ?: "\"http://103.200.20.228:8080/\""
        val cloudName = properties.getProperty("CLOUDINARY_CLOUD_NAME") ?: "\"\""
        val uploadPreset = properties.getProperty("CLOUDINARY_UPLOAD_PRESET") ?: "\"\""
        val folder = properties.getProperty("CLOUDINARY_FOLDER") ?: "\"\""
        val hlsProfile = properties.getProperty("CLOUDINARY_HLS_PROFILE") ?: "\"\""

        // Sinh file BuildConfig cho Java gọi
        buildConfigField("String", "BASE_URL", baseUrl)
        buildConfigField("String", "CLOUDINARY_CLOUD_NAME", cloudName)
        buildConfigField("String", "CLOUDINARY_UPLOAD_PRESET", uploadPreset)
        buildConfigField("String", "CLOUDINARY_FOLDER", folder)
        buildConfigField("String", "CLOUDINARY_HLS_PROFILE", hlsProfile)
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    // ◄ 3. BẮT BUỘC PHẢI BẬT KHỐI NÀY ĐỂ SINH FILE BuildConfig CHO CÁC CLASS JAVA GỌI
    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.11.0")
    implementation("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}