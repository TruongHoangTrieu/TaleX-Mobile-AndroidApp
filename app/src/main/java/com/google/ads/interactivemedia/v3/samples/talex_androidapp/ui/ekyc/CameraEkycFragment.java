package com.google.ads.interactivemedia.v3.samples.talex_androidapp.ui.ekyc;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
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
import androidx.annotation.OptIn;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
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
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import org.json.JSONObject;

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
    private TextView tvStepInstruction, tvSubInstruction, tvTimer;
    private View btnCapture;
    private View frameIdCard, dimTop, dimBottom, dimLeft, dimRight;
    private FaceOverlayView faceOverlay; // Lớp mặt nạ chuẩn TP Bank
    private ProgressBar progressBar;
    private ImageView btnBack;

    // CameraX Core
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private ExecutorService cameraExecutor;
    private ProcessCameraProvider cameraProvider;

    // CameraX UseCases
    private ImageCapture imageCapture;
    private VideoCapture<Recorder> videoCapture;
    private ImageAnalysis imageAnalysis; // Dùng ML Kit để quét mặt liên tục
    private Recording currentRecording = null;
    private CountDownTimer countDownTimer;

    // ML Kit Face Detector
    private FaceDetector faceDetector;

    // Logic State
    private String kycSessionId;
    private ApiService apiService;
    private int currentStep = 1; // 1: Front, 2: Back, 3: Liveness
    private File frontCroppedFile = null;
    private boolean isRecording = false; // Cờ kiểm tra đang quay
    private boolean isLivenessCompleted = false;

    private ActivityResultLauncher<String[]> requestPermissionsLauncher;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        cameraExecutor = Executors.newSingleThreadExecutor();

        // Cấu hình ML Kit: Chế độ FAST để không làm lag camera
        FaceDetectorOptions options = new FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .build();
        faceDetector = FaceDetection.getClient(options);

        if (getArguments() != null) {
            kycSessionId = getArguments().getString("KYC_SESSION_ID");
        }

        requestPermissionsLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                permissions -> {
                    boolean cameraGranted = Boolean.TRUE.equals(permissions.get(Manifest.permission.CAMERA));
                    boolean audioGranted = Boolean.TRUE.equals(permissions.get(Manifest.permission.RECORD_AUDIO));

                    if (cameraGranted && audioGranted) {
                        startCameraForCurrentStep();
                    } else {
                        Toast.makeText(requireContext(), "Cần cấp quyền để tiếp tục", Toast.LENGTH_LONG).show();
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
        tvTimer = view.findViewById(R.id.tvTimer);
        btnCapture = view.findViewById(R.id.btnCapture);
        frameIdCard = view.findViewById(R.id.frameIdCard);
        faceOverlay = view.findViewById(R.id.faceOverlay);
        progressBar = view.findViewById(R.id.progressBar);
        btnBack = view.findViewById(R.id.btnBack);

        // Ánh xạ các Dim cũ
        dimTop = view.findViewById(R.id.dimTop);
        dimBottom = view.findViewById(R.id.dimBottom);
        dimLeft = view.findViewById(R.id.dimLeft);
        dimRight = view.findViewById(R.id.dimRight);

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

        // Nút bấm chỉ dùng cho Bước 1 & 2
        btnCapture.setOnClickListener(v -> {
            if (currentStep == 1 || currentStep == 2) {
                takePhoto();
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
        tvTimer.setVisibility(View.GONE);

        if (step == 1) {
            tvStepInstruction.setText("Mặt trước CCCD");
            tvSubInstruction.setText("Vui lòng căn chỉnh giấy tờ vào trong khung hình.");
            btnCapture.setVisibility(View.VISIBLE);
            setDimVisibility(View.VISIBLE);
            faceOverlay.setVisibility(View.GONE);

        } else if (step == 2) {
            tvStepInstruction.setText("Mặt sau CCCD");
            tvSubInstruction.setText("Lật mặt sau giấy tờ và giữ cố định thiết bị.");
            btnCapture.setVisibility(View.VISIBLE);
            setDimVisibility(View.VISIBLE);
            faceOverlay.setVisibility(View.GONE);

        } else if (step == 3) {
            tvStepInstruction.setText("Xác thực Khuôn mặt");
            tvSubInstruction.setText("Đưa khuôn mặt vào giữa khung hình.");

            // Ẩn nút bấm, ẩn Dim chữ nhật, bật Mặt nạ đục lỗ TPBank
            btnCapture.setVisibility(View.GONE);
            setDimVisibility(View.GONE);
            frameIdCard.setVisibility(View.GONE);
            faceOverlay.setVisibility(View.VISIBLE);
            faceOverlay.setBorderState(FaceOverlayView.OverlayState.DEFAULT);
        }
    }

    private void setDimVisibility(int visibility) {
        dimTop.setVisibility(visibility);
        dimBottom.setVisibility(visibility);
        dimLeft.setVisibility(visibility);
        dimRight.setVisibility(visibility);
        frameIdCard.setVisibility(visibility);
    }

    private void startCameraForCurrentStep() {
        cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext());
        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();
                cameraProvider.unbindAll();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(viewFinder.getSurfaceProvider());

                if (currentStep == 1 || currentStep == 2) {
                    imageCapture = new ImageCapture.Builder().build();
                    cameraProvider.bindToLifecycle(getViewLifecycleOwner(), CameraSelector.DEFAULT_BACK_CAMERA, preview, imageCapture);

                } else if (currentStep == 3) {
                    // Bước 3: VideoCapture + ImageAnalysis (Face Tracking)
                    Recorder recorder = new Recorder.Builder()
                            .setQualitySelector(QualitySelector.from(Quality.FHD))
                            .build();
                    videoCapture = VideoCapture.withOutput(recorder);

                    imageAnalysis = new ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build();

                    imageAnalysis.setAnalyzer(cameraExecutor, this::processImageProxy);

                    cameraProvider.bindToLifecycle(getViewLifecycleOwner(), CameraSelector.DEFAULT_FRONT_CAMERA, preview, videoCapture, imageAnalysis);
                }

            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Lỗi khởi tạo camera", e);
            }
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    // =========================================================================
    // LÕI AI FACE TRACKING & AUTO RECORD
    // =========================================================================
    @OptIn(markerClass = ExperimentalGetImage.class)
    private void processImageProxy(ImageProxy imageProxy) {
        if (isLivenessCompleted) {
            imageProxy.close();
            return;
        }

        Image mediaImage = imageProxy.getImage();
        if (mediaImage != null) {
            InputImage image = InputImage.fromMediaImage(mediaImage, imageProxy.getImageInfo().getRotationDegrees());
            faceDetector.process(image)
                    .addOnSuccessListener(faces -> {
                        if (faces.size() == 1) {
                            // Phát hiện thấy mặt -> Bắt đầu quay tự động nếu chưa quay
                            if (!isRecording) {
                                requireActivity().runOnUiThread(() -> {
                                    faceOverlay.setBorderState(FaceOverlayView.OverlayState.SUCCESS);
                                    startAutoRecording();
                                });
                            }
                        } else {
                            // Mất mặt hoặc có nhiều hơn 1 mặt -> Hủy quay ngay lập tức
                            if (isRecording) {
                                requireActivity().runOnUiThread(this::cancelAutoRecording);
                            }
                        }
                    })
                    .addOnFailureListener(e -> Log.e(TAG, "Lỗi nhận diện khuôn mặt", e))
                    .addOnCompleteListener(task -> imageProxy.close());
        } else {
            imageProxy.close();
        }
    }

    @SuppressLint("MissingPermission")
    private void startAutoRecording() {
        if (videoCapture == null || isRecording) return;
        isRecording = true;

        tvSubInstruction.setText("Giữ nguyên... Đang ghi hình");
        tvTimer.setVisibility(View.VISIBLE);

        File videoFile = new File(requireContext().getCacheDir(), "liveness_" + System.currentTimeMillis() + ".mp4");
        FileOutputOptions outputOptions = new FileOutputOptions.Builder(videoFile).build();

        currentRecording = videoCapture.getOutput()
                .prepareRecording(requireContext(), outputOptions)
                .withAudioEnabled()
                .start(ContextCompat.getMainExecutor(requireContext()), videoRecordEvent -> {
                    if (videoRecordEvent instanceof VideoRecordEvent.Start) {
                        // Đếm ngược 5 giây
                        countDownTimer = new CountDownTimer(6000, 1000) {
                            public void onTick(long millisUntilFinished) {
                                tvTimer.setText("00:0" + (millisUntilFinished / 1000));
                            }
                            public void onFinish() {
                                finishAutoRecording(videoFile);
                            }
                        }.start();

                    } else if (videoRecordEvent instanceof VideoRecordEvent.Finalize) {
                        VideoRecordEvent.Finalize finalizeEvent = (VideoRecordEvent.Finalize) videoRecordEvent;
                        if (finalizeEvent.hasError() && isRecording) {
                            cancelAutoRecording();
                        }
                    }
                });
    }

    private void cancelAutoRecording() {
        isRecording = false;
        if (countDownTimer != null) countDownTimer.cancel();
        if (currentRecording != null) {
            currentRecording.stop();
            currentRecording = null;
        }

        tvTimer.setVisibility(View.GONE);
        faceOverlay.setBorderState(FaceOverlayView.OverlayState.ERROR);
        tvSubInstruction.setText("Khuôn mặt bị lệch. Vui lòng đưa lại vào giữa khung!");
    }

    private void finishAutoRecording(File videoFile) {
        isRecording = false;
        isLivenessCompleted = true; // Chặn AI quét tiếp
        if (currentRecording != null) {
            currentRecording.stop();
            currentRecording = null;
        }

        tvTimer.setVisibility(View.GONE);
        faceOverlay.setBorderState(FaceOverlayView.OverlayState.DEFAULT);
        tvSubInstruction.setText("Đang phân tích dữ liệu khuôn mặt...");

        uploadLiveness(videoFile);
    }

    // =========================================================================
    // LUỒNG CHỤP & UPLOAD CCCD
    // =========================================================================
    private void takePhoto() {
        if (imageCapture == null) return;
        showLoading(true);

        File photoFile = new File(requireContext().getCacheDir(), "capture_" + System.currentTimeMillis() + ".jpg");
        ImageCapture.OutputFileOptions outputOptions = new ImageCapture.OutputFileOptions.Builder(photoFile).build();

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
                showLoading(false);
                Toast.makeText(getContext(), "Lỗi chụp ảnh: " + exception.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void uploadFrontId(Uri uri) {
        int pW = viewFinder.getWidth(), pH = viewFinder.getHeight();
        float fX = frameIdCard.getX(), fY = frameIdCard.getY(), fW = frameIdCard.getWidth(), fH = frameIdCard.getHeight();

        frontCroppedFile = ImageCompressor.processAndCropImage(requireContext(), uri, "frontImage", pW, pH, fX, fY, fW, fH);
        MultipartBody.Part imagePart = ImageCompressor.buildMultipart("frontImage", frontCroppedFile);

        apiService.uploadFrontId(getAuthToken(), kycSessionId, imagePart).enqueue(new Callback<EKycResultResponse>() {
            @Override
            public void onResponse(@NonNull Call<EKycResultResponse> call, @NonNull Response<EKycResultResponse> response) {
                showLoading(false);
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null && response.body().getData().isSuccess()) {
                    // Thành công mặt trước -> Sang chụp mặt sau (KHÔNG GỌI REVIEW)
                    currentStep = 2;
                    setupUIForStep(currentStep);
                } else {
                    Toast.makeText(getContext(), "Lỗi mặt trước, vui lòng chụp lại!", Toast.LENGTH_SHORT).show();
                }
            }
            @Override public void onFailure(@NonNull Call<EKycResultResponse> call, @NonNull Throwable t) { showLoading(false); }
        });
    }

    private void uploadBackId(Uri uri) {
        int pW = viewFinder.getWidth(), pH = viewFinder.getHeight();
        float fX = frameIdCard.getX(), fY = frameIdCard.getY(), fW = frameIdCard.getWidth(), fH = frameIdCard.getHeight();
        File backCroppedFile = ImageCompressor.processAndCropImage(requireContext(), uri, "backImage", pW, pH, fX, fY, fW, fH);
        MultipartBody.Part imagePart = ImageCompressor.buildMultipart("backImage", backCroppedFile);

        apiService.uploadBackId(getAuthToken(), kycSessionId, imagePart).enqueue(new Callback<EKycResultResponse>() {
            @Override
            public void onResponse(@NonNull Call<EKycResultResponse> call, @NonNull Response<EKycResultResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null && response.body().getData().isSuccess()) {
                    // Đã xong 2 mặt -> Gọi API Get Identity để Review
                    fetchIdentityData();
                } else {
                    showLoading(false);
                    Toast.makeText(getContext(), "Lỗi mặt sau, vui lòng chụp lại!", Toast.LENGTH_SHORT).show();
                }
            }
            @Override public void onFailure(@NonNull Call<EKycResultResponse> call, @NonNull Throwable t) { showLoading(false); }
        });
    }

    // Gọi API lấy dữ liệu trích xuất của cả 2 mặt và mở màn hình Review
    private void fetchIdentityData() {
        // LƯU Ý: Backend của bạn có thể tên hàm khác trong ApiService, hãy điều chỉnh tên hàm nếu cần!
        apiService.getCreatorIdentities(getAuthToken()).enqueue(new Callback<JsonElement>() {
            @Override
            public void onResponse(@NonNull Call<JsonElement> call, @NonNull Response<JsonElement> response) {
                showLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JsonObject obj = response.body().getAsJsonObject().getAsJsonObject("data");
                        String idStr = obj.has("idNumber") ? obj.get("idNumber").getAsString() : "---";
                        String nameStr = obj.has("fullName") ? obj.get("fullName").getAsString() : "---";
                        String dobStr = obj.has("dob") ? obj.get("dob").getAsString() : "---";

                        Bitmap bitmap = BitmapFactory.decodeFile(frontCroppedFile.getAbsolutePath());
                        ReviewEkycDataFragment reviewFrag = new ReviewEkycDataFragment(bitmap, idStr, nameStr, dobStr);
                        getParentFragmentManager().beginTransaction()
                                .add(R.id.fragment_container, reviewFrag)
                                .addToBackStack("ReviewData")
                                .commit();
                    } catch (Exception e) {
                        Toast.makeText(getContext(), "Lỗi đọc dữ liệu trích xuất", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(getContext(), "Không lấy được dữ liệu trích xuất", Toast.LENGTH_SHORT).show();
                }
            }
            @Override public void onFailure(@NonNull Call<JsonElement> call, @NonNull Throwable t) { showLoading(false); }
        });
    }

    /** Gọi từ ReviewEkycDataFragment khi bấm Xác nhận */
    public void onReviewConfirmed() {
        currentStep = 3;
        setupUIForStep(currentStep);
        startCameraForCurrentStep();
    }

    private void uploadLiveness(File videoFile) {
        showLoading(true);
        MultipartBody.Part cmndPart = ImageCompressor.buildMultipart("cmnd", frontCroppedFile);
        RequestBody reqFile = RequestBody.create(MediaType.parse("video/mp4"), videoFile);
        MultipartBody.Part videoPart = MultipartBody.Part.createFormData("video", videoFile.getName(), reqFile);

        apiService.verifyLiveness(getAuthToken(), kycSessionId, videoPart, cmndPart).enqueue(new Callback<EKycResultResponse>() {
            @Override
            public void onResponse(@NonNull Call<EKycResultResponse> call, @NonNull Response<EKycResultResponse> response) {
                showLoading(false);
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null && response.body().getData().isSuccess()) {
                    Toast.makeText(getContext(), "eKYC THÀNH CÔNG TỐT ĐẸP!", Toast.LENGTH_LONG).show();
                    if (getActivity() != null) ((EkycActivity) getActivity()).finishWithSuccess();
                } else {
                    Toast.makeText(getContext(), "Xác thực khuôn mặt thất bại. Vui lòng thử lại.", Toast.LENGTH_LONG).show();
                    isLivenessCompleted = false; // Mở lại cho quét tiếp
                }
            }
            @Override public void onFailure(@NonNull Call<EKycResultResponse> call, @NonNull Throwable t) {
                showLoading(false);
                isLivenessCompleted = false;
            }
        });
    }

    private String getAuthToken() {
        try {
            SharedPreferences prefs = EncryptedSharedPreferences.create("TaleXSecurePref", MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC), requireContext(), EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV, EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM);
            String t = prefs.getString("ACCESS_TOKEN", "");
            return (!t.isEmpty() && !t.startsWith("Bearer ")) ? "Bearer " + t : t;
        } catch (GeneralSecurityException | IOException e) { return ""; }
    }

    private void showLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        if(btnCapture != null) btnCapture.setEnabled(!isLoading);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) countDownTimer.cancel();
        if (currentRecording != null) currentRecording.stop();
        if (faceDetector != null) faceDetector.close();
        cameraExecutor.shutdown();
    }
}