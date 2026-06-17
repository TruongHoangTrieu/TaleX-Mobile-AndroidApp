package com.google.ads.interactivemedia.v3.samples.talex_androidapp.ui.ekyc;

import android.Manifest;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.video.FileOutputOptions;
import androidx.camera.video.Quality;
import androidx.camera.video.QualitySelector;
import androidx.camera.video.Recorder;
import androidx.camera.video.Recording;
import androidx.camera.video.VideoCapture;
import androidx.camera.video.VideoRecordEvent;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKeys;

import com.google.ads.interactivemedia.v3.samples.talex_androidapp.R;
import com.google.ads.interactivemedia.v3.samples.talex_androidapp.data.api.ApiClient;
import com.google.ads.interactivemedia.v3.samples.talex_androidapp.data.api.ApiService;
import com.google.ads.interactivemedia.v3.samples.talex_androidapp.data.model.EKycResultResponse;
import com.google.ads.interactivemedia.v3.samples.talex_androidapp.utils.ImageCompressor;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CameraEkycFragment extends Fragment {

    private static final String TAG = "CameraEkycFragment";

    // UI
    private PreviewView viewFinder;
    private ImageView ivFreezeFrame;
    private TextView tvStepInstruction, tvSubInstruction;
    private View btnCapture;
    private View frameIdCard;
    private ProgressBar progressBar;
    private ImageView btnBack;

    // CameraX Core
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private ExecutorService cameraExecutor;
    private ProcessCameraProvider cameraProvider;

    // CameraX UseCases
    private ImageCapture imageCapture; // Dùng cho B1, B2
    private VideoCapture<Recorder> videoCapture; // Dùng cho B3
    private Recording currentRecording = null; // Trạng thái đang quay video

    // Logic State
    private String kycSessionId;
    private ApiService apiService;
    private int currentStep = 1;
    private File frontCroppedFile = null;

    private ActivityResultLauncher<String[]> requestPermissionsLauncher;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        cameraExecutor = Executors.newSingleThreadExecutor();

        if (getArguments() != null) {
            kycSessionId = getArguments().getString("KYC_SESSION_ID");
        }

        // Đã sửa: Phải xin quyền cả CAMERA và AUDIO vì quay video liveness cần ghi âm
        requestPermissionsLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                permissions -> {
                    Boolean cameraGranted = permissions.getOrDefault(Manifest.permission.CAMERA, false);
                    Boolean audioGranted = permissions.getOrDefault(Manifest.permission.RECORD_AUDIO, false);

                    if (cameraGranted && audioGranted) {
                        startCameraForCurrentStep();
                    } else {
                        Toast.makeText(requireContext(), "Cần cấp quyền Máy ảnh và Ghi âm", Toast.LENGTH_LONG).show();
                        getParentFragmentManager().popBackStack();
                    }
                }
        );
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_camera_ekyc, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewFinder = view.findViewById(R.id.viewFinder);
        ivFreezeFrame = view.findViewById(R.id.ivFreezeFrame);
        tvStepInstruction = view.findViewById(R.id.tvStepInstruction);
        tvSubInstruction = view.findViewById(R.id.tvSubInstruction);
        btnCapture = view.findViewById(R.id.btnCapture);
        frameIdCard = view.findViewById(R.id.frameIdCard);
        progressBar = view.findViewById(R.id.progressBar);
        btnBack = view.findViewById(R.id.btnBack);

        apiService = ApiClient.getApiService();

        setupUIForStep(currentStep);

        if (allPermissionsGranted()) {
            startCameraForCurrentStep();
        } else {
            requestPermissionsLauncher.launch(new String[]{
                    Manifest.permission.CAMERA,
                    Manifest.permission.RECORD_AUDIO
            });
        }

        btnCapture.setOnClickListener(v -> {
            if (currentStep == 1 || currentStep == 2) {
                takePhoto();
            } else if (currentStep == 3) {
                captureVideo();
            }
        });

        btnBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());
    }

    private boolean allPermissionsGranted() {
        return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
    }

    private void setupUIForStep(int step) {
        ivFreezeFrame.setVisibility(View.GONE);

        if (step == 1) {
            tvStepInstruction.setText("Bước 1/3: Mặt trước CCCD");
            tvSubInstruction.setText("Vui lòng căn chỉnh giấy tờ vào trong khung hình.");

            frameIdCard.setBackgroundResource(R.drawable.bg_ekyc_id_frame);
            ViewGroup.LayoutParams params = frameIdCard.getLayoutParams();
            params.width = ViewGroup.LayoutParams.MATCH_PARENT;
            params.height = (int) (220 * getResources().getDisplayMetrics().density);
            frameIdCard.setLayoutParams(params);

        } else if (step == 2) {
            tvStepInstruction.setText("Bước 2/3: Mặt sau CCCD");
            tvSubInstruction.setText("Lật mặt sau giấy tờ và giữ cố định thiết bị.");

        } else if (step == 3) {
            tvStepInstruction.setText("Bước 3/3: Xác thực khuôn mặt");
            tvSubInstruction.setText("Nhấn nút để bắt đầu quay video khuôn mặt (3-5 giây).");

            frameIdCard.setBackgroundResource(R.drawable.bg_ekyc_face_frame);
            ViewGroup.LayoutParams params = frameIdCard.getLayoutParams();
            // ĐÃ SỬA: Giảm kích thước khung tròn xuống 260dp để không bị vỡ font chữ
            int size = (int) (260 * getResources().getDisplayMetrics().density);
            params.width = size;
            params.height = size;
            frameIdCard.setLayoutParams(params);
        }
    }

    /**
     * Khởi động Camera linh hoạt dựa trên Bước hiện tại (Mặt trước/Mặt sau -> Cam sau, Liveness -> Cam trước)
     */
    private void startCameraForCurrentStep() {
        cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext());
        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();
                cameraProvider.unbindAll(); // Reset toàn bộ

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(viewFinder.getSurfaceProvider());

                if (currentStep == 1 || currentStep == 2) {
                    // Dùng Camera Sau để chụp ảnh
                    imageCapture = new ImageCapture.Builder().build();
                    CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;
                    cameraProvider.bindToLifecycle(getViewLifecycleOwner(), cameraSelector, preview, imageCapture);

                } else if (currentStep == 3) {
                    // Dùng Camera Trước để quay Video (Chất lượng HD 720p để đảm bảo dung lượng < 15MB)
                    Recorder recorder = new Recorder.Builder()
                            .setQualitySelector(QualitySelector.from(Quality.HD))
                            .build();
                    videoCapture = VideoCapture.withOutput(recorder);
                    CameraSelector cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA;
                    cameraProvider.bindToLifecycle(getViewLifecycleOwner(), cameraSelector, preview, videoCapture);
                }

            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Lỗi khởi tạo camera", e);
            }
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    private void takePhoto() {
        if (imageCapture == null) return;

        Bitmap frozenBitmap = viewFinder.getBitmap();
        if (frozenBitmap != null) {
            ivFreezeFrame.setImageBitmap(frozenBitmap);
            ivFreezeFrame.setVisibility(View.VISIBLE);
        }

        File photoFile = new File(requireContext().getCacheDir(), "capture_" + System.currentTimeMillis() + ".jpg");
        ImageCapture.OutputFileOptions outputOptions = new ImageCapture.OutputFileOptions.Builder(photoFile).build();

        showLoading(true);

        imageCapture.takePicture(outputOptions, ContextCompat.getMainExecutor(requireContext()), new ImageCapture.OnImageSavedCallback() {
            @Override
            public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                Uri savedUri = Uri.fromFile(photoFile);
                if (currentStep == 1) {
                    uploadFrontId(savedUri);
                } else if (currentStep == 2) {
                    uploadBackId(savedUri);
                }
            }

            @Override
            public void onError(@NonNull ImageCaptureException exception) {
                if (!isAdded()) return;
                showLoading(false);
                ivFreezeFrame.setVisibility(View.GONE);
                Toast.makeText(getContext(), "Lỗi chụp ảnh: " + exception.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Logic quay video trực tiếp trong App
     */
    private void captureVideo() {
        if (videoCapture == null) return;

        // Nếu đang quay rồi thì bấm phát nữa là Dừng lại
        if (currentRecording != null) {
            currentRecording.stop();
            currentRecording = null;
            return;
        }

        // Bắt đầu quay
        File videoFile = new File(requireContext().getCacheDir(), "liveness_" + System.currentTimeMillis() + ".mp4");
        FileOutputOptions outputOptions = new FileOutputOptions.Builder(videoFile).build();

        try {
            currentRecording = videoCapture.getOutput()
                    .prepareRecording(requireContext(), outputOptions)
                    .withAudioEnabled()
                    .start(ContextCompat.getMainExecutor(requireContext()), videoRecordEvent -> {
                        if (videoRecordEvent instanceof VideoRecordEvent.Start) {
                            // UI: Đổi text hướng dẫn để user biết đang quay
                            tvSubInstruction.setText("Đang ghi hình... Nhấn nút lần nữa để Dừng.");
                            tvSubInstruction.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_red_light));
                            btnCapture.setAlpha(0.5f); // Làm mờ nút đi 1 xíu
                        } else if (videoRecordEvent instanceof VideoRecordEvent.Finalize) {
                            VideoRecordEvent.Finalize finalizeEvent = (VideoRecordEvent.Finalize) videoRecordEvent;

                            // Reset UI
                            btnCapture.setAlpha(1.0f);
                            tvSubInstruction.setText("Đang xử lý dữ liệu khuôn mặt...");
                            tvSubInstruction.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white));

                            if (!finalizeEvent.hasError()) {
                                uploadLiveness(videoFile);
                            } else {
                                if (currentRecording != null) currentRecording.close();
                                currentRecording = null;
                                Toast.makeText(requireContext(), "Lỗi quay video: " + finalizeEvent.getError(), Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        } catch (SecurityException e) {
            Toast.makeText(requireContext(), "Lỗi quyền truy cập Microphone", Toast.LENGTH_SHORT).show();
        }
    }

    private void uploadFrontId(Uri uri) {
        int pW = viewFinder.getWidth();
        int pH = viewFinder.getHeight();
        float fX = frameIdCard.getX();
        float fY = frameIdCard.getY();
        float fW = frameIdCard.getWidth();
        float fH = frameIdCard.getHeight();

        frontCroppedFile = ImageCompressor.processAndCropImage(requireContext(), uri, "frontImage", pW, pH, fX, fY, fW, fH);
        MultipartBody.Part imagePart = ImageCompressor.buildMultipart("frontImage", frontCroppedFile);

        if (imagePart == null) {
            showLoading(false);
            ivFreezeFrame.setVisibility(View.GONE);
            return;
        }

        apiService.uploadFrontId(getAuthToken(), kycSessionId, imagePart).enqueue(new Callback<EKycResultResponse>() {
            @Override
            public void onResponse(@NonNull Call<EKycResultResponse> call, @NonNull Response<EKycResultResponse> response) {
                if (!isAdded()) return;
                showLoading(false);

                try {
                    if (response.isSuccessful() && response.body() != null) {
                        EKycResultResponse result = response.body();
                        EKycResultResponse.EKycData data = result.getData();

                        // ĐÃ SỬA: Đọc chuẩn isSuccess từ Data của Backend trả về
                        if (data != null && data.isSuccess()) {
                            currentStep = 2;
                            setupUIForStep(currentStep);
                            Toast.makeText(getContext(), "Xong mặt trước! Vui lòng lật mặt sau.", Toast.LENGTH_SHORT).show();
                        } else {
                            ivFreezeFrame.setVisibility(View.GONE);
                            String errorMsg = (data != null && data.getMessage() != null) ? data.getMessage() : result.getMessage();
                            Toast.makeText(getContext(), "Lỗi: " + errorMsg, Toast.LENGTH_LONG).show();
                        }
                    } else {
                        ivFreezeFrame.setVisibility(View.GONE);
                        Toast.makeText(getContext(), "Thẻ không hợp lệ (HTTP " + response.code() + ")", Toast.LENGTH_LONG).show();
                    }
                } catch (Exception e) {
                    ivFreezeFrame.setVisibility(View.GONE);
                    Log.e(TAG, "Lỗi Parse Data: ", e);
                }
            }

            @Override
            public void onFailure(@NonNull Call<EKycResultResponse> call, @NonNull Throwable t) {
                if (!isAdded()) return;
                showLoading(false);
                ivFreezeFrame.setVisibility(View.GONE);
                Toast.makeText(getContext(), "Lỗi mạng: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void uploadBackId(Uri uri) {
        int pW = viewFinder.getWidth();
        int pH = viewFinder.getHeight();
        float fX = frameIdCard.getX();
        float fY = frameIdCard.getY();
        float fW = frameIdCard.getWidth();
        float fH = frameIdCard.getHeight();

        File backCroppedFile = ImageCompressor.processAndCropImage(requireContext(), uri, "backImage", pW, pH, fX, fY, fW, fH);
        MultipartBody.Part imagePart = ImageCompressor.buildMultipart("backImage", backCroppedFile);

        if (imagePart == null) {
            showLoading(false);
            ivFreezeFrame.setVisibility(View.GONE);
            return;
        }

        apiService.uploadBackId(getAuthToken(), kycSessionId, imagePart).enqueue(new Callback<EKycResultResponse>() {
            @Override
            public void onResponse(@NonNull Call<EKycResultResponse> call, @NonNull Response<EKycResultResponse> response) {
                if (!isAdded()) return;
                showLoading(false);

                if (response.isSuccessful() && response.body() != null) {
                    try {
                        EKycResultResponse result = response.body();
                        EKycResultResponse.EKycData data = result.getData();

                        if (data != null && data.isSuccess()) {
                            currentStep = 3;
                            setupUIForStep(currentStep);
                            startCameraForCurrentStep(); // Chuyển sang Camera trước
                            Toast.makeText(getContext(), "Xong mặt sau! Chuẩn bị quay video.", Toast.LENGTH_SHORT).show();
                        } else {
                            ivFreezeFrame.setVisibility(View.GONE);
                            String errorMsg = (data != null && data.getMessage() != null) ? data.getMessage() : result.getMessage();
                            Toast.makeText(getContext(), "Lỗi: " + errorMsg, Toast.LENGTH_LONG).show();
                        }
                    } catch (Exception e) {
                        ivFreezeFrame.setVisibility(View.GONE);
                        Log.e(TAG, "Lỗi Parse Data: ", e);
                    }
                } else {
                    ivFreezeFrame.setVisibility(View.GONE);
                    Toast.makeText(getContext(), "Ảnh mặt sau không hợp lệ.", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<EKycResultResponse> call, @NonNull Throwable t) {
                if (!isAdded()) return;
                showLoading(false);
                ivFreezeFrame.setVisibility(View.GONE);
                Toast.makeText(getContext(), "Lỗi mạng: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void uploadLiveness(File videoFile) {
        showLoading(true);

        MultipartBody.Part cmndPart = ImageCompressor.buildMultipart("cmnd", frontCroppedFile);

        RequestBody reqFile = RequestBody.create(MediaType.parse("video/mp4"), videoFile);
        MultipartBody.Part videoPart = MultipartBody.Part.createFormData("video", videoFile.getName(), reqFile);

        if (cmndPart == null || videoPart == null) {
            showLoading(false);
            Toast.makeText(requireContext(), "Lỗi trích xuất file Media", Toast.LENGTH_SHORT).show();
            return;
        }

        apiService.verifyLiveness(getAuthToken(), kycSessionId, videoPart, cmndPart).enqueue(new Callback<EKycResultResponse>() {
            @Override
            public void onResponse(@NonNull Call<EKycResultResponse> call, @NonNull Response<EKycResultResponse> response) {
                if (!isAdded()) return;
                showLoading(false);

                if (response.isSuccessful() && response.body() != null) {
                    try {
                        EKycResultResponse result = response.body();
                        EKycResultResponse.EKycData data = result.getData();

                        if (data != null && data.isSuccess()) {
                            Toast.makeText(getContext(), "eKYC THÀNH CÔNG TỐT ĐẸP!", Toast.LENGTH_LONG).show();
                            if (getActivity() != null) getActivity().finish();
                        } else {
                            String errorMsg = (data != null && data.getMessage() != null) ? data.getMessage() : result.getMessage();
                            Toast.makeText(getContext(), "Lỗi Khuôn mặt: " + errorMsg, Toast.LENGTH_LONG).show();
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Lỗi Parse Data: ", e);
                    }
                } else {
                    Toast.makeText(getContext(), "Xác thực khuôn mặt thất bại.", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<EKycResultResponse> call, @NonNull Throwable t) {
                if (!isAdded()) return;
                showLoading(false);
                Toast.makeText(getContext(), "Lỗi mạng: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String getAuthToken() {
        try {
            String masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC);
            SharedPreferences securePrefs = EncryptedSharedPreferences.create(
                    "TaleXSecurePref", masterKeyAlias, requireContext(),
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
            String token = securePrefs.getString("ACCESS_TOKEN", "");
            return (!token.isEmpty() && !token.startsWith("Bearer ")) ? "Bearer " + token : token;
        } catch (GeneralSecurityException | IOException e) {
            return "";
        }
    }

    private void showLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        btnCapture.setEnabled(!isLoading);
        btnBack.setEnabled(!isLoading);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (currentRecording != null) {
            currentRecording.stop();
        }
        cameraExecutor.shutdown();
    }
}