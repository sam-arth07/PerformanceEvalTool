package com.pet.evaluator.util;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.VideoCapture;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import com.google.common.util.concurrent.ListenableFuture;
import com.pet.evaluator.R;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;

/**
 * Enhanced camera recorder utility to gracefully handle recording in different
 * Android versions
 */
public class CameraRecorderHelper {
    private static final String TAG = "CameraRecorderHelper";

    public interface RecordingCallback {
        void onVideoRecordingStarted();

        void onVideoRecordingSuccess(Uri videoUri);

        void onVideoRecordingFailure(String errorMessage);
    }

    /**
     * Safely start video recording with appropriate error handling for different
     * Android versions
     *
     * @param activity       Activity context
     * @param lifecycleOwner The lifecycle owner
     * @param cameraSelector Which camera to use (front/back)
     * @param callback       Callback for recording events
     * @return true if recording started successfully, false otherwise
     */
    @SuppressWarnings("deprecation") // For compatibility with older Android versions
    public static boolean startVideoRecording(@NonNull Activity activity,
            @NonNull LifecycleOwner lifecycleOwner,
            @NonNull CameraSelector cameraSelector,
            @NonNull RecordingCallback callback) {
        if (activity == null || activity.isFinishing()) {
            Log.e(TAG, "Cannot start recording: activity is null or finishing");
            return false;
        }

        try {
            ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(activity);

            ProcessCameraProvider cameraProvider;
            try {
                cameraProvider = cameraProviderFuture.get();
            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Failed to get camera provider", e);
                callback.onVideoRecordingFailure("Failed to initialize camera provider: " + e.getMessage());
                return false;
            } // Prepare video capture use case with quality settings
            int width = activity.getResources().getInteger(R.integer.video_quality_medium_resolution_width);
            int height = activity.getResources().getInteger(R.integer.video_quality_medium_resolution_height);
            int bitRate = activity.getResources().getInteger(R.integer.video_bitrate_medium);
            int frameRate = activity.getResources().getInteger(R.integer.video_fps);

            VideoCapture videoCapture = new VideoCapture.Builder()
                    .setTargetRotation(activity.getWindowManager().getDefaultDisplay().getRotation())
                    .setVideoFrameRate(frameRate)
                    .setBitRate(bitRate)
                    .setTargetResolution(new android.util.Size(width, height))
                    .build();

            // Unbind all use cases before rebinding
            cameraProvider.unbindAll();

            // Bind use case to camera
            cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    videoCapture);

            // Create output file
            File videoFile = createVideoFile(activity);
            if (videoFile == null) {
                callback.onVideoRecordingFailure("Failed to create video file");
                return false;
            }

            // Prepare output options
            VideoCapture.OutputFileOptions outputFileOptions = new VideoCapture.OutputFileOptions.Builder(videoFile)
                    .build();

            // Start recording
            videoCapture.startRecording(
                    outputFileOptions,
                    ContextCompat.getMainExecutor(activity),
                    new VideoCapture.OnVideoSavedCallback() {
                        @Override
                        public void onVideoSaved(@NonNull VideoCapture.OutputFileResults outputFileResults) {
                            Uri savedUri = outputFileResults.getSavedUri();
                            if (savedUri == null) {
                                savedUri = Uri.fromFile(videoFile);
                            }
                            callback.onVideoRecordingSuccess(savedUri);
                        }

                        @Override
                        public void onError(int videoCaptureError, @NonNull String message, @NonNull Throwable cause) {
                            Log.e(TAG, "Video recording failed: " + message, cause);
                            callback.onVideoRecordingFailure("Recording failed: " + message);
                        }
                    });

            callback.onVideoRecordingStarted();
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Failed to start video recording", e);
            callback.onVideoRecordingFailure("Failed to start video recording: " + e.getMessage());
            return false;
        }
    }

    /**
     * Create a file to store the video
     * 
     * @param context Context
     * @return The created file or null if failed
     */
    private static File createVideoFile(Context context) {
        try {
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                    .format(System.currentTimeMillis());
            String videoFileName = "VIDEO_" + timeStamp + "_";

            File storageDir;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // For Android 10 and above, use app-specific directory
                storageDir = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES);
            } else {
                // For older versions
                storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES);

                // Ensure directory exists
                if (!storageDir.exists() && !storageDir.mkdirs()) {
                    Log.e(TAG, "Failed to create directory for video");
                    return null;
                }
            }

            return File.createTempFile(videoFileName, ".mp4", storageDir);
        } catch (Exception e) {
            Log.e(TAG, "Error creating video file", e);
            return null;
        }
    }

    /**
     * Check if device supports the minimal camera features needed
     * 
     * @param context Context
     * @return true if supported, false otherwise
     */
    public static boolean deviceSupportsCameraFeatures(Context context) {
        // Check for camera hardware
        if (!context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
            Log.e(TAG, "Device does not have a camera");
            return false;
        }

        // Check for camera2 API support
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP &&
                !context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_LEVEL_FULL)) {
            // Camera2 API with FULL support is preferred but not required
            Log.w(TAG, "Device does not support Camera2 API FULL level");
            // Continue anyway as CameraX might still work
        }

        return true;
    }
}
