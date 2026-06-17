package com.google.ads.interactivemedia.v3.samples.talex_androidapp.ui.ekyc;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.Image;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
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
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;
import java.util.Map;
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
    private static final long LIVENESS_RECORDING_MS = 5000L;
    private static final String EKYC_TEMP_PREFS = "EkycTempPrefs";
    private static final String KEY_FRONT_IMAGE_PATH_PREFIX = "front_image_path_";

    private PreviewView viewFinder;
    private ImageView ivFreezeFrame;
    private TextView tvStepInstruction;
    private TextView tvSubInstruction;
    private TextView tvFaceGuide;
    private TextView tvFaceFooter;
    private View btnCapture;
    private View bottomPanel;
    private View frameIdCard;
    private View dimTop;
    private View dimBottom;
    private View dimLeft;
    private View dimRight;
    private FaceOverlayView faceOverlayView;
    private ProgressBar progressBar;
    private ImageView btnBack;

    private ExecutorService cameraExecutor;
    private ProcessCameraProvider cameraProvider;
    private ImageCapture imageCapture;
    private VideoCapture<Recorder> videoCapture;
    private ImageAnalysis imageAnalysis;
    private Recording currentRecording;
    private FaceDetector faceDetector;

    private final Handler livenessHandler = new Handler(Looper.getMainLooper());
    private Runnable countdownRunnable;
    private Runnable autoStopRunnable;

    private String kycSessionId;
    private String lastDebugVideoPath;
    private ApiService apiService;
    private int currentStep = 1;
    private File frontCroppedFile;
    private boolean isFaceTaskRunning;
    private boolean isRecordingCanceled;
    private boolean isFinalizingRecording;
    private boolean isLivenessUploading;
    private long recordingEndsAtMillis;

    private ActivityResultLauncher<String[]> requestPermissionsLauncher;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        cameraExecutor = Executors.newSingleThreadExecutor();

        if (getArguments() != null) {
            kycSessionId = getArguments().getString("KYC_SESSION_ID");
        }

        FaceDetectorOptions detectorOptions = new FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .enableTracking()
                .build();
        faceDetector = FaceDetection.getClient(detectorOptions);

        requestPermissionsLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                permissions -> {
                    boolean cameraGranted = Boolean.TRUE.equals(permissions.get(Manifest.permission.CAMERA));
                    boolean audioGranted = Boolean.TRUE.equals(permissions.get(Manifest.permission.RECORD_AUDIO));

                    if (cameraGranted && audioGranted) {
                        startCameraForCurrentStep();
                    } else {
                        showToast("Cần cấp quyền Máy ảnh và Ghi âm", Toast.LENGTH_LONG);
                        if (isAdded()) {
                            getParentFragmentManager().popBackStack();
                        }
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
        tvFaceGuide = view.findViewById(R.id.tvFaceGuide);
        tvFaceFooter = view.findViewById(R.id.tvFaceFooter);
        btnCapture = view.findViewById(R.id.btnCapture);
        bottomPanel = view.findViewById(R.id.bottomPanel);
        frameIdCard = view.findViewById(R.id.frameIdCard);
        dimTop = view.findViewById(R.id.dimTop);
        dimBottom = view.findViewById(R.id.dimBottom);
        dimLeft = view.findViewById(R.id.dimLeft);
        dimRight = view.findViewById(R.id.dimRight);
        faceOverlayView = view.findViewById(R.id.faceOverlayView);
        progressBar = view.findViewById(R.id.progressBar);
        btnBack = view.findViewById(R.id.btnBack);

        apiService = ApiClient.getApiService();
        restoreFrontImageFile();

        getParentFragmentManager().setFragmentResultListener(
                ReviewEkycDataFragment.REQUEST_REVIEW_CONFIRMED,
                getViewLifecycleOwner(),
                (requestKey, result) -> onReviewConfirmed()
        );

        setupUIForStep(currentStep);
        btnCapture.setOnClickListener(v -> takePhoto());
        btnBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());

        if (allPermissionsGranted()) {
            startCameraForCurrentStep();
        } else {
            requestPermissionsLauncher.launch(new String[]{
                    Manifest.permission.CAMERA,
                    Manifest.permission.RECORD_AUDIO
            });
        }
    }

    public void onReviewConfirmed() {
        currentStep = 3;
        setupUIForStep(currentStep);
        startCameraForCurrentStep();
    }

    public void onRetakeRequested() {
        currentStep = 1;
        clearFrontImageTempFile();
        isFaceTaskRunning = false;
        isRecordingCanceled = false;
        isFinalizingRecording = false;
        isLivenessUploading = false;
        setupUIForStep(currentStep);
        startCameraForCurrentStep();
    }

    private boolean allPermissionsGranted() {
        return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
    }

    private void setupUIForStep(int step) {
        ivFreezeFrame.setVisibility(View.GONE);
        progressBar.setVisibility(View.GONE);

        boolean isLivenessStep = step == 3;
        setViewVisible(dimTop, !isLivenessStep);
        setViewVisible(dimBottom, !isLivenessStep);
        setViewVisible(dimLeft, !isLivenessStep);
        setViewVisible(dimRight, !isLivenessStep);
        setViewVisible(frameIdCard, !isLivenessStep);
        setViewVisible(btnCapture, !isLivenessStep);
        setViewVisible(bottomPanel, !isLivenessStep);
        setViewVisible(tvFaceGuide, isLivenessStep);
        setViewVisible(tvFaceFooter, isLivenessStep);
        faceOverlayView.setVisibility(isLivenessStep ? View.VISIBLE : View.GONE);
        tvStepInstruction.setTextColor(Color.parseColor(isLivenessStep ? "#FFFFFF" : "#D4AF37"));

        if (step == 1) {
            tvStepInstruction.setText("Bước 1/3: Mặt trước CCCD");
            tvSubInstruction.setText("Vui lòng căn chỉnh giấy tờ vào trong khung hình.");
        } else if (step == 2) {
            tvStepInstruction.setText("Bước 2/3: Mặt sau CCCD");
            tvSubInstruction.setText("Lật mặt sau giấy tờ và giữ cố định thiết bị.");
        } else {
            faceOverlayView.setBorderState(FaceOverlayView.OverlayState.DEFAULT);
            tvStepInstruction.setText("Bước 3/3: Xác thực khuôn mặt");
            setLivenessInstruction("Đưa khuôn mặt vào khung");
        }
    }

    private void setViewVisible(View view, boolean visible) {
        if (view != null) {
            view.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }

    private void setLivenessInstruction(String message) {
        tvSubInstruction.setText(message);
        if (tvFaceGuide != null) {
            tvFaceGuide.setText(message);
        }
    }

    private void startCameraForCurrentStep() {
        Context context = getContext();
        if (!isAdded() || context == null) {
            return;
        }

        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(context);
        cameraProviderFuture.addListener(() -> {
            if (!hasLiveUi()) {
                return;
            }

            try {
                cameraProvider = cameraProviderFuture.get();
                cameraProvider.unbindAll();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(viewFinder.getSurfaceProvider());

                if (currentStep == 1 || currentStep == 2) {
                    imageCapture = new ImageCapture.Builder().build();
                    videoCapture = null;
                    imageAnalysis = null;
                    cameraProvider.bindToLifecycle(
                            getViewLifecycleOwner(),
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            imageCapture
                    );
                } else {
                    bindLivenessUseCases(preview);
                }
            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Cannot start camera", e);
            }
        }, ContextCompat.getMainExecutor(context));
    }

    private void bindLivenessUseCases(Preview preview) {
        imageCapture = null;

        Recorder recorder = new Recorder.Builder()
                .setQualitySelector(QualitySelector.from(Quality.HD))
                .build();

        videoCapture = VideoCapture.withOutput(recorder);
        imageAnalysis = new ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();
        imageAnalysis.setAnalyzer(cameraExecutor, this::analyzeFaceFrame);

        cameraProvider.bindToLifecycle(
                getViewLifecycleOwner(),
                CameraSelector.DEFAULT_FRONT_CAMERA,
                preview,
                videoCapture,
                imageAnalysis
        );
    }

    private void takePhoto() {
        if (imageCapture == null || currentStep == 3) {
            return;
        }

        if (viewFinder.getBitmap() != null) {
            ivFreezeFrame.setImageBitmap(viewFinder.getBitmap());
            ivFreezeFrame.setVisibility(View.VISIBLE);
        }

        File photoFile = new File(requireContext().getCacheDir(), "capture_" + System.currentTimeMillis() + ".jpg");
        ImageCapture.OutputFileOptions outputOptions = new ImageCapture.OutputFileOptions.Builder(photoFile).build();

        showLoading(true);
        imageCapture.takePicture(outputOptions, ContextCompat.getMainExecutor(requireContext()), new ImageCapture.OnImageSavedCallback() {
            @Override
            public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                if (!hasLiveUi()) return;
                Uri savedUri = Uri.fromFile(photoFile);
                if (currentStep == 1) {
                    uploadFrontId(savedUri);
                } else if (currentStep == 2) {
                    uploadBackId(savedUri);
                }
            }

            @Override
            public void onError(@NonNull ImageCaptureException exception) {
                if (!hasLiveUi()) return;
                showLoading(false);
                ivFreezeFrame.setVisibility(View.GONE);
                showToast("Lỗi chụp ảnh: " + exception.getMessage(), Toast.LENGTH_SHORT);
            }
        });
    }

    private void uploadFrontId(Uri uri) {
        frontCroppedFile = cropImage(uri, "frontImage");
        MultipartBody.Part imagePart = ImageCompressor.buildMultipart("frontImage", frontCroppedFile);

        if (imagePart == null) {
            showLoading(false);
            ivFreezeFrame.setVisibility(View.GONE);
            return;
        }

        apiService.uploadFrontId(getAuthToken(), kycSessionId, imagePart).enqueue(new Callback<EKycResultResponse>() {
            @Override
            public void onResponse(@NonNull Call<EKycResultResponse> call, @NonNull Response<EKycResultResponse> response) {
                if (!hasLiveUi()) return;
                showLoading(false);

                if (isSuccessResponse(response)) {
                    persistFrontImageFile(frontCroppedFile);
                    currentStep = 2;
                    setupUIForStep(currentStep);
                    showToast("Xong mặt trước! Vui lòng lật mặt sau.", Toast.LENGTH_SHORT);
                } else {
                    ivFreezeFrame.setVisibility(View.GONE);
                    deleteQuietly(frontCroppedFile);
                    frontCroppedFile = null;
                    showToast(getErrorMessage(response, "Thẻ không hợp lệ"), Toast.LENGTH_LONG);
                }
            }

            @Override
            public void onFailure(@NonNull Call<EKycResultResponse> call, @NonNull Throwable t) {
                if (!hasLiveUi()) return;
                showLoading(false);
                ivFreezeFrame.setVisibility(View.GONE);
                showToast("Lỗi mạng: " + t.getMessage(), Toast.LENGTH_SHORT);
            }
        });
    }

    private void uploadBackId(Uri uri) {
        File backCroppedFile = cropImage(uri, "backImage");
        MultipartBody.Part imagePart = ImageCompressor.buildMultipart("backImage", backCroppedFile);

        if (imagePart == null) {
            showLoading(false);
            ivFreezeFrame.setVisibility(View.GONE);
            return;
        }

        apiService.uploadBackId(getAuthToken(), kycSessionId, imagePart).enqueue(new Callback<EKycResultResponse>() {
            @Override
            public void onResponse(@NonNull Call<EKycResultResponse> call, @NonNull Response<EKycResultResponse> response) {
                if (!hasLiveUi()) return;

                if (isSuccessResponse(response)) {
                    fetchCreatorIdentityAndOpenReview();
                } else {
                    showLoading(false);
                    ivFreezeFrame.setVisibility(View.GONE);
                    showToast(getErrorMessage(response, "Ảnh mặt sau không hợp lệ"), Toast.LENGTH_LONG);
                }
            }

            @Override
            public void onFailure(@NonNull Call<EKycResultResponse> call, @NonNull Throwable t) {
                if (!hasLiveUi()) return;
                showLoading(false);
                ivFreezeFrame.setVisibility(View.GONE);
                showToast("Lỗi mạng: " + t.getMessage(), Toast.LENGTH_SHORT);
            }
        });
    }

    private File cropImage(Uri uri, String partName) {
        int pW = viewFinder.getWidth();
        int pH = viewFinder.getHeight();
        float fX = frameIdCard.getX();
        float fY = frameIdCard.getY();
        float fW = frameIdCard.getWidth();
        float fH = frameIdCard.getHeight();
        return ImageCompressor.processAndCropImage(requireContext(), uri, partName, pW, pH, fX, fY, fW, fH);
    }

    private void fetchCreatorIdentityAndOpenReview() {
        apiService.getCreatorIdentities(getAuthToken()).enqueue(new Callback<JsonElement>() {
            @Override
            public void onResponse(@NonNull Call<JsonElement> call, @NonNull Response<JsonElement> response) {
                if (!hasLiveUi()) return;
                showLoading(false);

                JsonElement body = response.body();
                String fullName = readIdentityValue(body, "fullName", "fullname", "name", "hoTen", "ownerName");
                String idNumber = readIdentityValue(body, "idNumber", "identityNumber", "identityCardNumber", "cardNumber", "citizenId", "cccd", "personalId");
                String dob = readIdentityValue(body, "dateOfBirth", "dob", "birthDate", "ngaySinh");

                openReviewFragment(idNumber, fullName, dob);
            }

            @Override
            public void onFailure(@NonNull Call<JsonElement> call, @NonNull Throwable t) {
                if (!hasLiveUi()) return;
                showLoading(false);
                showToast("Không lấy được thông tin OCR: " + t.getMessage(), Toast.LENGTH_SHORT);
                openReviewFragment(null, null, null);
            }
        });
    }

    private void openReviewFragment(String idNumber, String fullName, String dob) {
        File frontImageFile = resolveFrontImageFile();
        String frontImagePath = frontImageFile != null ? frontImageFile.getAbsolutePath() : null;
        ReviewEkycDataFragment fragment = ReviewEkycDataFragment.newInstance(frontImagePath, idNumber, fullName, dob);
        getParentFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();
    }

    @OptIn(markerClass = ExperimentalGetImage.class)
    private void analyzeFaceFrame(@NonNull ImageProxy imageProxy) {
        if (!isAdded() || getActivity() == null) {
            imageProxy.close();
            return;
        }

        if (currentStep != 3 || isFaceTaskRunning || isLivenessUploading) {
            imageProxy.close();
            return;
        }

        Image mediaImage = imageProxy.getImage();
        if (mediaImage == null) {
            imageProxy.close();
            return;
        }

        isFaceTaskRunning = true;
        InputImage inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.getImageInfo().getRotationDegrees());
        faceDetector.process(inputImage)
                .addOnSuccessListener(this::handleFaceResult)
                .addOnFailureListener(e -> Log.e(TAG, "Face detection failed", e))
                .addOnCompleteListener(task -> {
                    isFaceTaskRunning = false;
                    imageProxy.close();
                });
    }

    private void handleFaceResult(List<Face> faces) {
        if (!hasLiveUi() || currentStep != 3 || isLivenessUploading) {
            return;
        }

        getActivity().runOnUiThread(() -> {
            if (!hasLiveUi()) {
                return;
            }

            if (faces.size() == 1) {
                faceOverlayView.setBorderState(FaceOverlayView.OverlayState.SUCCESS);
                if (currentRecording == null && !isFinalizingRecording) {
                    startLivenessRecording();
                }
            } else {
                if (currentRecording != null) {
                    cancelLivenessRecording();
                } else if (!isFinalizingRecording) {
                    faceOverlayView.setBorderState(FaceOverlayView.OverlayState.DEFAULT);
                    setLivenessInstruction("Đưa đúng một khuôn mặt vào khung");
                }
            }
        });
    }

    private void startLivenessRecording() {
        if (videoCapture == null || currentRecording != null || isFinalizingRecording || isLivenessUploading) {
            return;
        }

        Context context = getContext();
        if (!hasLiveUi() || context == null) {
            return;
        }

        File videoFile = new File(context.getCacheDir(), "liveness_" + System.currentTimeMillis() + ".mp4");
        FileOutputOptions outputOptions = new FileOutputOptions.Builder(videoFile).build();
        isRecordingCanceled = false;

        try {
            currentRecording = videoCapture.getOutput()
                    .prepareRecording(context, outputOptions)
                    .withAudioEnabled()
                    .start(ContextCompat.getMainExecutor(context), event -> handleVideoEvent(event, videoFile));
        } catch (SecurityException e) {
            showToast("Lỗi quyền truy cập Microphone", Toast.LENGTH_SHORT);
        }
    }

    private void handleVideoEvent(VideoRecordEvent event, File videoFile) {
        if (!hasLiveUi()) {
            return;
        }

        if (event instanceof VideoRecordEvent.Start) {
            recordingEndsAtMillis = System.currentTimeMillis() + LIVENESS_RECORDING_MS;
            setLivenessInstruction("Đang ghi hình: 5s");
            startCountdownText();
            autoStopRunnable = () -> stopLivenessRecording(false);
            livenessHandler.postDelayed(autoStopRunnable, LIVENESS_RECORDING_MS);
        } else if (event instanceof VideoRecordEvent.Finalize) {
            VideoRecordEvent.Finalize finalizeEvent = (VideoRecordEvent.Finalize) event;
            livenessHandler.removeCallbacksAndMessages(null);
            currentRecording = null;
            isFinalizingRecording = false;

            if (isRecordingCanceled) {
                deleteQuietly(videoFile);
                faceOverlayView.setBorderState(FaceOverlayView.OverlayState.ERROR);
                setLivenessInstruction("Mặt rời khỏi khung");
                livenessHandler.postDelayed(() -> {
                    if (!hasLiveUi() || currentStep != 3) return;
                    faceOverlayView.setBorderState(FaceOverlayView.OverlayState.DEFAULT);
                    setLivenessInstruction("Đưa khuôn mặt vào khung");
                }, 1200L);
                return;
            }

            if (!finalizeEvent.hasError()) {
                uploadLiveness(videoFile);
            } else {
                deleteQuietly(videoFile);
                faceOverlayView.setBorderState(FaceOverlayView.OverlayState.ERROR);
                setLivenessInstruction("Lỗi quay video. Vui lòng thử lại");
                showToast("Lỗi quay video: " + finalizeEvent.getError(), Toast.LENGTH_SHORT);
            }
        }
    }

    private void startCountdownText() {
        countdownRunnable = new Runnable() {
            @Override
            public void run() {
                if (!hasLiveUi() || currentRecording == null) {
                    return;
                }

                long remainingMs = Math.max(0L, recordingEndsAtMillis - System.currentTimeMillis());
                int remainingSeconds = (int) Math.ceil(remainingMs / 1000.0);
                setLivenessInstruction("Đang ghi hình: " + remainingSeconds + "s");

                if (remainingMs > 0L) {
                    livenessHandler.postDelayed(this, 250L);
                }
            }
        };
        livenessHandler.post(countdownRunnable);
    }

    private void cancelLivenessRecording() {
        if (!hasLiveUi()) {
            return;
        }

        faceOverlayView.setBorderState(FaceOverlayView.OverlayState.ERROR);
        setLivenessInstruction("Mặt rời khỏi khung. Đang hủy video...");
        stopLivenessRecording(true);
    }

    private void stopLivenessRecording(boolean canceled) {
        if (currentRecording == null) {
            return;
        }

        isRecordingCanceled = canceled;
        isFinalizingRecording = true;
        if (autoStopRunnable != null) {
            livenessHandler.removeCallbacks(autoStopRunnable);
        }
        if (countdownRunnable != null) {
            livenessHandler.removeCallbacks(countdownRunnable);
        }
        currentRecording.stop();
    }

    private void uploadLiveness(File videoFile) {
        if (!hasLiveUi()) {
            return;
        }

        isLivenessUploading = true;
        showLoading(true);
        setLivenessInstruction("Đang xác thực khuôn mặt...");

        File debugVideoFile = copyLivenessVideoForDebug(videoFile);
        if (debugVideoFile != null) {
            lastDebugVideoPath = debugVideoFile.getAbsolutePath();
            Log.d(TAG, "Liveness debug video: " + lastDebugVideoPath);
        }
        Log.d(TAG, "Liveness cache video: " + videoFile.getAbsolutePath() + " (" + (videoFile.length() / 1024) + " KB)");

        File cmndFile = resolveFrontImageFile();
        if (cmndFile == null || !cmndFile.exists()) {
            isLivenessUploading = false;
            showLoading(false);
            faceOverlayView.setBorderState(FaceOverlayView.OverlayState.ERROR);
            setLivenessInstruction("Thiếu ảnh mặt trước CCCD");
            showToast("Thiếu ảnh mặt trước CCCD để gửi kèm video. Vui lòng chụp lại từ bước 1.", Toast.LENGTH_LONG);
            currentStep = 1;
            setupUIForStep(currentStep);
            startCameraForCurrentStep();
            return;
        }

        Log.d(TAG, "Liveness cmnd image path: " + cmndFile.getAbsolutePath() + " (" + (cmndFile.length() / 1024) + " KB)");
        MultipartBody.Part cmndPart = ImageCompressor.buildMultipart("cmnd", cmndFile);
        RequestBody reqFile = RequestBody.create(MediaType.parse("video/mp4"), videoFile);
        MultipartBody.Part videoPart = MultipartBody.Part.createFormData("video", videoFile.getName(), reqFile);

        if (cmndPart == null) {
            isLivenessUploading = false;
            showLoading(false);
            showToast("Không tìm thấy ảnh mặt trước CCCD", Toast.LENGTH_SHORT);
            return;
        }

        apiService.verifyLiveness(getAuthToken(), kycSessionId, videoPart, cmndPart).enqueue(new Callback<JsonElement>() {
            @Override
            public void onResponse(@NonNull Call<JsonElement> call, @NonNull Response<JsonElement> response) {
                if (!hasLiveUi()) return;
                showLoading(false);
                isLivenessUploading = false;
                Log.d(TAG, "Liveness request URL: " + response.raw().request().url());
                Log.d(TAG, "Liveness HTTP code: " + response.code());
                Log.d(TAG, "Liveness response body: " + (response.body() != null ? response.body().toString() : "null"));

                if (isSuccessfulJsonResponse(response)) {
                    Log.d(TAG, "Liveness response message: " + getJsonResponseMessage(response.body(), "eKYC thành công"));
                    clearFrontImageTempFile();
                    showToast("eKYC thành công!", Toast.LENGTH_LONG);
                    if (getActivity() instanceof EkycActivity) {
                        ((EkycActivity) getActivity()).finishWithSuccess();
                    }
                } else {
                    String errorMessage = getJsonErrorMessage(response, "Xác thực khuôn mặt thất bại");
                    Log.e(TAG, "Liveness failed: " + errorMessage);
                    faceOverlayView.setBorderState(FaceOverlayView.OverlayState.ERROR);
                    setLivenessInstruction("Lỗi: " + shortMessage(errorMessage));
                    showToast(errorMessage, Toast.LENGTH_LONG);
                }
            }

            @Override
            public void onFailure(@NonNull Call<JsonElement> call, @NonNull Throwable t) {
                if (!hasLiveUi()) return;
                showLoading(false);
                isLivenessUploading = false;
                faceOverlayView.setBorderState(FaceOverlayView.OverlayState.ERROR);
                setLivenessInstruction("Lỗi mạng. Thử lại");
                showToast("Lỗi mạng: " + t.getMessage(), Toast.LENGTH_SHORT);
            }
        });
    }

    private boolean isSuccessResponse(Response<EKycResultResponse> response) {
        if (!response.isSuccessful() || response.body() == null) {
            return false;
        }
        EKycResultResponse.EKycData data = response.body().getData();
        return data != null && data.isSuccess();
    }

    private boolean isSuccessfulJsonResponse(Response<JsonElement> response) {
        if (!response.isSuccessful() || response.body() == null || !response.body().isJsonObject()) {
            return false;
        }

        JsonObject object = response.body().getAsJsonObject();
        JsonElement code = object.get("code");
        if (code != null && code.isJsonPrimitive() && code.getAsJsonPrimitive().isNumber()) {
            int codeValue = code.getAsInt();
            return codeValue == 0 || codeValue == 200;
        }

        JsonElement data = object.get("data");
        if (data != null && data.isJsonObject()) {
            JsonElement isSuccess = data.getAsJsonObject().get("isSuccess");
            return isSuccess != null && isSuccess.isJsonPrimitive() && isSuccess.getAsBoolean();
        }

        return false;
    }

    private String getJsonErrorMessage(Response<JsonElement> response, String fallback) {
        String httpPrefix = response.code() > 0 ? "HTTP " + response.code() + ": " : "";
        if (response.body() != null) {
            return httpPrefix + getJsonResponseMessage(response.body(), fallback);
        }

        String errorBody = readErrorBody(response);
        String serverMessage = extractServerMessage(errorBody);
        if (serverMessage != null) {
            return httpPrefix + serverMessage;
        }
        if (errorBody != null && !errorBody.trim().isEmpty()) {
            return httpPrefix + errorBody;
        }
        return httpPrefix + fallback;
    }

    private String getJsonResponseMessage(JsonElement body, String fallback) {
        if (body == null || !body.isJsonObject()) {
            return fallback;
        }

        JsonObject object = body.getAsJsonObject();
        JsonElement message = object.get("message");
        if (message != null && message.isJsonPrimitive()) {
            return message.getAsString();
        }

        String dataMessage = readNestedMessage(object, "data");
        if (dataMessage != null) {
            return dataMessage;
        }

        JsonElement data = object.get("data");
        if (data != null && data.isJsonPrimitive()) {
            return data.getAsString();
        }

        return fallback;
    }

    private String getErrorMessage(Response<EKycResultResponse> response, String fallback) {
        String httpPrefix = response.code() > 0 ? "HTTP " + response.code() + ": " : "";

        if (response.body() == null) {
            String errorBody = readErrorBody(response);
            String serverMessage = extractServerMessage(errorBody);
            if (serverMessage != null) {
                return httpPrefix + serverMessage;
            }
            if (errorBody != null && !errorBody.trim().isEmpty()) {
                return httpPrefix + errorBody;
            }
            return httpPrefix + fallback;
        }

        EKycResultResponse result = response.body();
        EKycResultResponse.EKycData data = result.getData();
        if (result.getMessage() != null) {
            return httpPrefix + result.getMessage();
        }
        if (data != null && data.getMessage() != null) {
            return httpPrefix + data.getMessage();
        }
        return httpPrefix + fallback;
    }

    private String readErrorBody(Response<?> response) {
        try {
            if (response.errorBody() == null) {
                return null;
            }
            String rawError = response.errorBody().string();
            Log.e(TAG, "Liveness error body: " + rawError);
            return rawError;
        } catch (IOException e) {
            Log.e(TAG, "Cannot read liveness error body", e);
            return null;
        }
    }

    private String extractServerMessage(String rawJson) {
        if (rawJson == null || rawJson.trim().isEmpty()) {
            return null;
        }

        try {
            JsonElement element = JsonParser.parseString(rawJson);
            if (!element.isJsonObject()) {
                return null;
            }

            JsonObject object = element.getAsJsonObject();
            JsonElement message = object.get("message");
            if (message != null && message.isJsonPrimitive()) {
                return message.getAsString();
            }

            String dataMessage = readNestedMessage(object, "data");
            if (dataMessage != null) {
                return dataMessage;
            }
        } catch (Exception e) {
            Log.e(TAG, "Cannot parse liveness error body", e);
        }
        return null;
    }

    private String readNestedMessage(JsonObject object, String childKey) {
        JsonElement child = object.get(childKey);
        if (child == null || !child.isJsonObject()) {
            return null;
        }

        JsonElement message = child.getAsJsonObject().get("message");
        if (message != null && message.isJsonPrimitive()) {
            String value = message.getAsString();
            if (value != null && !value.trim().isEmpty()) {
                return value;
            }
        }
        return null;
    }

    private String shortMessage(String message) {
        if (message == null) {
            return "Xác thực thất bại";
        }
        return message.length() > 42 ? message.substring(0, 42) + "..." : message;
    }

    private File copyLivenessVideoForDebug(File sourceFile) {
        Context context = getContext();
        if (context == null || sourceFile == null || !sourceFile.exists()) {
            return null;
        }

        File moviesDir = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES);
        if (moviesDir == null) {
            return null;
        }

        File debugDir = new File(moviesDir, "ekyc_debug");
        if (!debugDir.exists() && !debugDir.mkdirs()) {
            Log.e(TAG, "Cannot create liveness debug dir: " + debugDir.getAbsolutePath());
            return null;
        }

        File latestFile = new File(debugDir, "liveness_latest.mp4");
        File timestampFile = new File(debugDir, "liveness_" + System.currentTimeMillis() + ".mp4");

        try {
            copyFile(sourceFile, latestFile);
            copyFile(sourceFile, timestampFile);
            Log.d(TAG, "Liveness latest video path: " + latestFile.getAbsolutePath());
            Log.d(TAG, "Liveness timestamp video path: " + timestampFile.getAbsolutePath());
            return latestFile;
        } catch (IOException e) {
            Log.e(TAG, "Cannot copy liveness debug video", e);
            return null;
        }
    }

    private void persistFrontImageFile(File file) {
        Context context = getContext();
        if (context == null || file == null || !file.exists()) {
            return;
        }

        frontCroppedFile = file;
        context.getSharedPreferences(EKYC_TEMP_PREFS, Context.MODE_PRIVATE)
                .edit()
                .putString(getFrontImagePathKey(), file.getAbsolutePath())
                .apply();
        Log.d(TAG, "Persisted front image for liveness: " + file.getAbsolutePath() + " (" + (file.length() / 1024) + " KB)");
    }

    private void restoreFrontImageFile() {
        File file = resolveFrontImageFile();
        if (file != null) {
            frontCroppedFile = file;
            Log.d(TAG, "Restored front image for liveness: " + file.getAbsolutePath() + " (" + (file.length() / 1024) + " KB)");
        }
    }

    private File resolveFrontImageFile() {
        if (frontCroppedFile != null && frontCroppedFile.exists()) {
            return frontCroppedFile;
        }

        Context context = getContext();
        if (context == null) {
            return null;
        }

        String path = context.getSharedPreferences(EKYC_TEMP_PREFS, Context.MODE_PRIVATE)
                .getString(getFrontImagePathKey(), null);
        if (path == null || path.trim().isEmpty()) {
            return null;
        }

        File file = new File(path);
        if (!file.exists()) {
            Log.e(TAG, "Persisted front image path is missing: " + path);
            return null;
        }

        frontCroppedFile = file;
        return file;
    }

    private void clearFrontImageTempFile() {
        Context context = getContext();
        File file = resolveFrontImageFile();
        deleteQuietly(file);
        frontCroppedFile = null;

        if (context != null) {
            context.getSharedPreferences(EKYC_TEMP_PREFS, Context.MODE_PRIVATE)
                    .edit()
                    .remove(getFrontImagePathKey())
                    .apply();
        }
        Log.d(TAG, "Cleared temporary front image for liveness");
    }

    private String getFrontImagePathKey() {
        return KEY_FRONT_IMAGE_PATH_PREFIX + (kycSessionId != null ? kycSessionId : "unknown");
    }

    private void copyFile(File sourceFile, File targetFile) throws IOException {
        byte[] buffer = new byte[8192];
        try (FileInputStream inputStream = new FileInputStream(sourceFile);
             FileOutputStream outputStream = new FileOutputStream(targetFile)) {
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            outputStream.flush();
        }
    }

    private String readIdentityValue(JsonElement element, String... keys) {
        if (element == null || element.isJsonNull()) {
            return null;
        }

        if (element.isJsonObject()) {
            JsonObject object = element.getAsJsonObject();
            for (String key : keys) {
                String directValue = readDirectValue(object, key);
                if (directValue != null) {
                    return directValue;
                }
            }

            for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
                String nestedValue = readIdentityValue(entry.getValue(), keys);
                if (nestedValue != null) {
                    return nestedValue;
                }
            }
        } else if (element.isJsonArray()) {
            JsonArray array = element.getAsJsonArray();
            for (JsonElement item : array) {
                String nestedValue = readIdentityValue(item, keys);
                if (nestedValue != null) {
                    return nestedValue;
                }
            }
        }

        return null;
    }

    private String readDirectValue(JsonObject object, String targetKey) {
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            if (!entry.getKey().equalsIgnoreCase(targetKey)) {
                continue;
            }

            JsonElement value = entry.getValue();
            if (value != null && value.isJsonPrimitive()) {
                String text = value.getAsString();
                if (text != null && !text.trim().isEmpty()) {
                    return text.trim();
                }
            }
        }
        return null;
    }

    private String getAuthToken() {
        Context context = getContext();
        if (context == null) {
            return "";
        }

        try {
            String masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC);
            SharedPreferences securePrefs = EncryptedSharedPreferences.create(
                    "TaleXSecurePref",
                    masterKeyAlias,
                    context,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
            String token = securePrefs.getString("ACCESS_TOKEN", "");
            return (!token.isEmpty() && !token.startsWith("Bearer ")) ? "Bearer " + token : token;
        } catch (GeneralSecurityException | IOException e) {
            Log.e(TAG, "Cannot read secure token", e);
            return "";
        }
    }

    private void showLoading(boolean isLoading) {
        if (!hasLiveUi()) {
            return;
        }

        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        btnCapture.setEnabled(!isLoading);
        btnBack.setEnabled(!isLoading);
    }

    private boolean hasLiveUi() {
        return isAdded()
                && getContext() != null
                && getActivity() != null
                && getView() != null;
    }

    private void showToast(String message, int duration) {
        Context context = getContext();
        if (!isAdded() || context == null) {
            return;
        }
        Toast.makeText(context, message, duration).show();
    }

    private void deleteQuietly(File file) {
        if (file != null && file.exists() && !file.delete()) {
            Log.w(TAG, "Cannot delete temp file: " + file.getAbsolutePath());
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        livenessHandler.removeCallbacksAndMessages(null);
        if (currentRecording != null) {
            currentRecording.stop();
            currentRecording = null;
        }
        if (cameraProvider != null) {
            cameraProvider.unbindAll();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (faceDetector != null) {
            faceDetector.close();
        }
        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
        }
    }
}
