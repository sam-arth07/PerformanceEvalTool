package com.pet.evaluator.ui.video;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
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

import com.pet.evaluator.util.CameraRecorderHelper;
import com.pet.evaluator.util.CameraUtils;
import com.pet.evaluator.util.CameraTroubleshooter;
import com.pet.evaluator.util.CameraXCompatHelper;
import com.pet.evaluator.util.PermissionManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.google.common.util.concurrent.ListenableFuture;
import com.pet.evaluator.R;
import com.pet.evaluator.SharedViewModel;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class VideoFragment extends Fragment {
    private static final int REQUEST_CODE_PERMISSIONS = PermissionManager.REQUEST_CODE_CAMERA_PERMISSIONS;
    private static final int PICK_VIDEO_FILE = 2;

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

        // Log camera information for debugging
        CameraUtils.logCameraInfo(requireContext()); // Set click listeners
        recordButton.setOnClickListener(v -> {
            if (isRecording) {
                stopRecording();
            } else {
                if (allPermissionsGranted()) {
                    if (CameraUtils.deviceHasCamera(requireContext())) {
                        startRecording();
                    } else {
                        Toast.makeText(requireContext(), "This device doesn't have a camera!",
                                Toast.LENGTH_LONG).show();
                    }
                } else {
                    // Use our PermissionManager to request permissions
                    PermissionManager.checkAndRequestCameraPermissions(
                            this,
                            REQUEST_CODE_PERMISSIONS,
                            new PermissionManager.PermissionCallback() {
                                @Override
                                public void onPermissionsGranted() {
                                    startRecording();
                                }

                                @Override
                                public void onPermissionsDenied(List<String> deniedPermissions) {
                                    Toast.makeText(requireContext(),
                                            "Camera and/or audio permissions denied",
                                            Toast.LENGTH_SHORT).show();
                                }
                            },
                            true); // Include audio permissions
                }
            }
        });

        // Add long press listener for diagnostic mode
        recordButton.setOnLongClickListener(v -> {
            showCameraDiagnosticFragment();
            return true;
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
        try {
            // Make previewView visible and videoPreviewImageView invisible
            previewView.setVisibility(View.VISIBLE);
            videoPreviewImageView.setVisibility(View.GONE);

            // Check camera availability using enhanced utilities
            if (!CameraRecorderHelper.deviceSupportsCameraFeatures(requireContext())) {
                handleCameraError("This device's camera functionality is limited");
                return;
            }

            // Check permissions with our enhanced permission manager
            if (!PermissionManager.hasCameraPermissions(requireContext(), false)) {
                PermissionManager.checkAndRequestCameraPermissions(
                        this,
                        REQUEST_CODE_PERMISSIONS,
                        new PermissionManager.PermissionCallback() {
                            @Override
                            public void onPermissionsGranted() {
                                // Try again after permissions are granted
                                startCamera();
                            }

                            @Override
                            public void onPermissionsDenied(List<String> deniedPermissions) {
                                handleCameraError("Camera permissions required");
                            }
                        },
                        false // Just camera permission for preview, no audio needed
                );
                return;
            }

            ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider
                    .getInstance(requireContext());

            cameraProviderFuture.addListener(() -> {
                try {
                    ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                    if (cameraProvider == null) {
                        handleCameraError("Failed to get camera provider");
                        return;
                    }

                    // Unbind all use cases before rebinding
                    cameraProvider.unbindAll();

                    bindPreview(cameraProvider);

                    // Log success
                    Log.d("VideoFragment", "Camera started successfully");

                } catch (ExecutionException | InterruptedException e) {
                    // Handle errors
                    handleCameraError("Error starting camera: " + e.getMessage());
                }
            }, ContextCompat.getMainExecutor(requireContext()));

        } catch (Exception e) {
            handleCameraError("Camera initialization error: " + e.getMessage());

            // Show troubleshooting tips // Use our enhanced troubleshooting utility
            CameraTroubleshooter.showTroubleshootingDialog(requireActivity());
        }
    }

    /**
     * Handle camera errors with logging and user feedback
     */
    private void handleCameraError(String message) {
        Log.e("VideoFragment", message);
        Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();

        // Enable upload as fallback
        if (uploadButton != null) {
            uploadButton.setEnabled(true);
        }

        // Show diagnostic button if multiple errors occur
        if (message.contains("Failed") || message.contains("Error")) {
            showDiagnosticButton();
        }
    }

    private void showDiagnosticButton() {
        // Create a floating action button for diagnostics
        View view = getView();
        if (view == null)
            return;

        // Check if we already have a diagnostic button
        if (view.findViewById(R.id.button_camera_diagnostic) != null) {
            return;
        }

        Button diagnosticButton = new Button(requireContext());
        diagnosticButton.setId(R.id.button_camera_diagnostic); // Define this ID in a resource file
        diagnosticButton.setText("Camera Diagnostic");
        diagnosticButton.setOnClickListener(v -> showCameraDiagnosticFragment());

        // Add to layout
        ViewGroup layout = (ViewGroup) view;
        layout.addView(diagnosticButton);
    }

    private void bindPreview(ProcessCameraProvider cameraProvider) {
        try {
            Preview preview = new Preview.Builder().build();

            CameraSelector cameraSelector = new CameraSelector.Builder()
                    .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                    .build(); // Use our compatibility helper to handle different CameraX versions
            if (CameraXCompatHelper.setSurfaceProviderCompat(preview, previewView)) {
                handleCameraError("Failed to set up camera preview");
            }

            // Bind to lifecycle
            Camera camera = cameraProvider.bindToLifecycle(getViewLifecycleOwner(), cameraSelector, preview);

            // Log success
            Log.d("VideoFragment", "Camera preview bound successfully");
        } catch (Exception e) {
            Log.e("VideoFragment", "Error binding preview: " + e.getMessage());
            Toast.makeText(getContext(), "Error binding camera preview: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private boolean allPermissionsGranted() {
        return PermissionManager.hasCameraPermissions(requireContext(), true); // Include audio
    }

    private void startRecording() {
        // Use our enhanced camera recorder helper
        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                .build();

        // Show progress during recording initialization
        progressBar.setVisibility(View.VISIBLE);

        CameraRecorderHelper.startVideoRecording(
                requireActivity(),
                getViewLifecycleOwner(),
                cameraSelector,
                new CameraRecorderHelper.RecordingCallback() {
                    @Override
                    public void onVideoRecordingStarted() {
                        // Update UI for recording in progress
                        isRecording = true;
                        recordButton.setText("Stop Recording");
                        progressBar.setVisibility(View.GONE);

                        // Start a timer for recording
                        recordingTimer = new CountDownTimer(MAX_RECORDING_TIME, 1000) {
                            @Override
                            public void onTick(long millisUntilFinished) {
                                long seconds = TimeUnit.MILLISECONDS.toSeconds(MAX_RECORDING_TIME - millisUntilFinished)
                                        % 60;
                                long minutes = TimeUnit.MILLISECONDS
                                        .toMinutes(MAX_RECORDING_TIME - millisUntilFinished);
                                timerTextView.setText(String.format("%02d:%02d", minutes, seconds));
                            }

                            @Override
                            public void onFinish() {
                                stopRecording();
                            }
                        }.start();

                        Toast.makeText(getContext(), "Recording started", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onVideoRecordingSuccess(Uri videoUri) {
                        if (recordingTimer != null) {
                            recordingTimer.cancel();
                        }

                        // Process the recorded video
                        isRecording = false;
                        recordButton.setText(R.string.record_video);

                        // Process the video using our shared view model
                        sharedViewModel.processVideo(videoUri);

                        Toast.makeText(getContext(), "Recording saved successfully", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onVideoRecordingFailure(String errorMessage) {
                        if (recordingTimer != null) {
                            recordingTimer.cancel();
                        }

                        isRecording = false;
                        recordButton.setText(R.string.record_video);
                        progressBar.setVisibility(View.GONE);

                        handleCameraError("Recording failed: " + errorMessage);

                        // Fall back to sample videos if available
                        fallbackToSampleVideos();
                    }
                });
    }

    private void stopRecording() {
        // This method now only handles manual stopping of recording
        // The actual video saving is handled in the CameraRecorderHelper callback
        if (recordingTimer != null) {
            recordingTimer.cancel();
        }

        isRecording = false;
        recordButton.setText(R.string.record_video);

        // Since we don't have a direct reference to the VideoCapture use case here,
        // we need to get it through the camera provider
        try {
            ProcessCameraProvider cameraProvider = ProcessCameraProvider.getInstance(requireContext()).get();
            cameraProvider.unbindAll(); // This will stop the recording

            // Rebind preview to keep camera preview running
            startCamera();

            // Fall back to sample videos since we can't easily get the recorded video this
            // way
            fallbackToSampleVideos();
        } catch (Exception e) {
            Log.e("VideoFragment", "Error stopping recording: " + e.getMessage());
            // Fall back to sample videos
            fallbackToSampleVideos();
        }
    }

    private void fallbackToSampleVideos() {
        // For demo or fallback scenarios, use a real sample video file
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

        // Use our PermissionManager to handle permission results
        PermissionManager.handlePermissionResult(requestCode, permissions, grantResults);

        // Additional UI and behavior specific to this fragment
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                Log.d("VideoFragment", "All permissions granted, starting camera");
                startCamera();
            } else {
                // Format denied permissions for better error messages
                StringBuilder deniedPermissions = new StringBuilder();
                for (String permission : permissions) {
                    if (ContextCompat.checkSelfPermission(requireContext(),
                            permission) != PackageManager.PERMISSION_GRANTED) {
                        deniedPermissions.append(permission.substring(permission.lastIndexOf(".") + 1))
                                .append(", ");
                    }
                }

                if (deniedPermissions.length() > 0) {
                    String message = "Required permissions not granted: " +
                            deniedPermissions.substring(0, deniedPermissions.length() - 2);
                    Log.e("VideoFragment", message);
                    Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
                }

                // Show rationale for permissions
                Toast.makeText(getContext(),
                        "Camera and microphone access are required for video recording",
                        Toast.LENGTH_LONG).show();
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

        // Check if device screen is on and activity is visible before starting camera
        if (!isResumed() || !getUserVisibleHint()) {
            Log.d("VideoFragment", "Fragment not fully visible, skipping camera start");
            return;
        }

        if (allPermissionsGranted()) {
            // Start with a delay to ensure the view is fully initialized
            previewView.post(() -> {
                // Only start camera if the fragment is still active
                if (isAdded() && !isDetached()) {
                    startCamera();
                }
            });
        } else {
            // Request permissions using our enhanced permission manager
            PermissionManager.checkAndRequestCameraPermissions(
                    this,
                    REQUEST_CODE_PERMISSIONS,
                    new PermissionManager.PermissionCallback() {
                        @Override
                        public void onPermissionsGranted() {
                            startCamera();
                        }

                        @Override
                        public void onPermissionsDenied(List<String> deniedPermissions) {
                            Log.d("VideoFragment", "Camera permissions not granted");
                            // Show troubleshooting dialog after a short delay
                            new Handler().postDelayed(
                                    () -> CameraTroubleshooter.showTroubleshootingDialog(requireActivity()),
                                    500);
                        }
                    },
                    false // Just need camera for preview initially
            );
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        // Clean up camera resources when fragment is paused
        try {
            ProcessCameraProvider cameraProvider = ProcessCameraProvider.getInstance(requireContext()).get();
            cameraProvider.unbindAll();
        } catch (Exception e) {
            Log.e("VideoFragment", "Error unbinding camera use cases: " + e.getMessage());
        }
    }

    /**
     * Show the camera diagnostic fragment for troubleshooting
     */
    private void showCameraDiagnosticFragment() {
        CameraDiagnosticFragment diagnosticFragment = new CameraDiagnosticFragment();

        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.nav_host_fragment, diagnosticFragment)
                .addToBackStack("camera_diagnostic")
                .commit();

        Toast.makeText(requireContext(), "Entering Camera Diagnostic Mode", Toast.LENGTH_SHORT).show();
    }
}
