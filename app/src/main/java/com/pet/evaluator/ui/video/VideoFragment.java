package com.pet.evaluator.ui.video;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import java.io.File;
import android.os.CountDownTimer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.google.common.util.concurrent.ListenableFuture;
import com.pet.evaluator.R;
import com.pet.evaluator.SharedViewModel;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class VideoFragment extends Fragment {

    private static final int REQUEST_CODE_PERMISSIONS = 10;
    private static final int PICK_VIDEO_FILE = 2;
    private static final String[] REQUIRED_PERMISSIONS = new String[] {
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
    };

    private VideoViewModel videoViewModel;
    private SharedViewModel sharedViewModel;
    private PreviewView previewView;
    private ImageView videoPreviewImageView;
    private TextView timerTextView;
    private Button recordButton;
    private Button uploadButton;
    private Button continueButton;
    private ProgressBar progressBar;

    private boolean isRecording = false;
    private CountDownTimer recordingTimer;
    private static final long MAX_RECORDING_TIME = 120000; // 2 minutes

    public View onCreateView(@NonNull LayoutInflater inflater,
            ViewGroup container, Bundle savedInstanceState) {
        videoViewModel = new ViewModelProvider(this).get(VideoViewModel.class);
        // Use activity scope to share data with other fragments
        sharedViewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);
        return inflater.inflate(R.layout.fragment_video, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize UI components
        previewView = view.findViewById(R.id.preview_view);
        videoPreviewImageView = view.findViewById(R.id.image_video_preview);
        timerTextView = view.findViewById(R.id.text_timer);
        recordButton = view.findViewById(R.id.button_record_video);
        uploadButton = view.findViewById(R.id.button_upload_video);
        continueButton = view.findViewById(R.id.button_continue_to_results);
        progressBar = view.findViewById(R.id.progress_video);

        // Set click listeners
        recordButton.setOnClickListener(v -> {
            if (isRecording) {
                stopRecording();
            } else {
                if (allPermissionsGranted()) {
                    startRecording();
                } else {
                    ActivityCompat.requestPermissions(requireActivity(), REQUIRED_PERMISSIONS,
                            REQUEST_CODE_PERMISSIONS);
                }
            }
        });

        uploadButton.setOnClickListener(v -> openVideoPicker());

        continueButton
                .setOnClickListener(v -> Navigation.findNavController(requireView()).navigate(R.id.navigation_results));

        // Observe shared view model data
        sharedViewModel.getIsProcessing().observe(getViewLifecycleOwner(), isProcessing -> {
            if (Boolean.TRUE.equals(isProcessing)) {
                progressBar.setVisibility(View.VISIBLE);
                recordButton.setEnabled(false);
                uploadButton.setEnabled(false);
                continueButton.setEnabled(false);
            } else {
                progressBar.setVisibility(View.GONE);
                recordButton.setEnabled(true);
                uploadButton.setEnabled(true);
                continueButton.setEnabled(true);
            }
        });

        sharedViewModel.getIsVideoProcessed().observe(getViewLifecycleOwner(), isProcessed -> {
            if (Boolean.TRUE.equals(isProcessed)) {
                continueButton.setVisibility(View.VISIBLE);
            } else {
                continueButton.setVisibility(View.GONE);
            }
        });

        videoViewModel.getProcessingResult().observe(getViewLifecycleOwner(), result -> {
            if (result != null && !result.isEmpty()) {
                Toast.makeText(getContext(), result, Toast.LENGTH_SHORT).show();
            }
        });

        // Initialize camera if permissions are granted
        if (allPermissionsGranted()) {
            startCamera();
        }
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider
                .getInstance(requireContext());

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                bindPreview(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                // Handle errors
                Toast.makeText(getContext(), "Error starting camera", Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    private void bindPreview(ProcessCameraProvider cameraProvider) {
        Preview preview = new Preview.Builder().build();
        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                .build();

        preview.setSurfaceProvider(previewView.createSurfaceProvider());

        Camera camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview);
    }

    private boolean allPermissionsGranted() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(requireContext(), permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private void startRecording() {
        isRecording = true;
        recordButton.setText("Stop Recording");

        // In a real app, you would start actual video recording here

        // Start a timer for recording
        recordingTimer = new CountDownTimer(MAX_RECORDING_TIME, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                long seconds = TimeUnit.MILLISECONDS.toSeconds(MAX_RECORDING_TIME - millisUntilFinished) % 60;
                long minutes = TimeUnit.MILLISECONDS.toMinutes(MAX_RECORDING_TIME - millisUntilFinished);
                timerTextView.setText(String.format("%02d:%02d", minutes, seconds));
            }

            @Override
            public void onFinish() {
                stopRecording();
            }
        }.start();

        Toast.makeText(getContext(), "Recording started", Toast.LENGTH_SHORT).show();
    }

    private void stopRecording() {
        if (recordingTimer != null) {
            recordingTimer.cancel();
        }

        isRecording = false;
        recordButton.setText(R.string.record_video);
        // In a real app, you would stop actual video recording here
        // and process the recorded video

        // For demo, we'll use a real sample video file if available, or create a mock
        // URI
        // that our updated MLModelManager can handle
        Uri videoUri;

        // Check if we can access sample videos
        File samplesDir = new File(requireContext().getExternalFilesDir(null), "../../../sample_data/videos");
        if (samplesDir.exists() && samplesDir.listFiles() != null && samplesDir.listFiles().length > 0) {
            // Use a real sample video
            File[] sampleFiles = samplesDir.listFiles();
            File sampleVideo = sampleFiles[0]; // Use the first available sample
            videoUri = Uri.fromFile(sampleVideo);
            Log.d("VideoFragment", "Using sample video: " + sampleVideo.getAbsolutePath());
        } else {
            // Use a mock URI that our MLModelManager can handle
            videoUri = Uri.parse("content://mock/video/recording.mp4");
            Log.d("VideoFragment", "Using mock video URI");
        }

        // Process with shared view model
        sharedViewModel.processVideo(videoUri);

        Toast.makeText(getContext(), "Recording stopped and video processing started", Toast.LENGTH_SHORT).show();
    }

    private void openVideoPicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("video/*");
        startActivityForResult(intent, PICK_VIDEO_FILE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera();
            } else {
                Toast.makeText(getContext(), "Permissions not granted", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_VIDEO_FILE && resultCode == Activity.RESULT_OK) {
            if (data != null) {
                Uri videoUri = data.getData();

                // Process the selected video using shared view model
                sharedViewModel.processVideo(videoUri);

                // Show a preview of the video
                videoPreviewImageView.setVisibility(View.VISIBLE);
                previewView.setVisibility(View.GONE);

                Toast.makeText(getContext(), R.string.video_uploaded_success, Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (allPermissionsGranted()) {
            startCamera();
        }
    }
}
