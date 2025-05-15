package com.pet.evaluator.ui.video;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.common.util.concurrent.ListenableFuture;
import com.pet.evaluator.R;
import com.pet.evaluator.util.CameraXCompatHelper;

import java.util.concurrent.ExecutionException;

/**
 * A diagnostic fragment to test camera functionality
 * This can be used to troubleshoot camera issues
 */
public class CameraDiagnosticFragment extends Fragment {

    private static final String TAG = "CameraDiagnostic";
    private static final int REQUEST_CODE_PERMISSIONS = 10;
    private static final String[] REQUIRED_PERMISSIONS = {
            Manifest.permission.CAMERA
    };

    private PreviewView previewView;
    private TextView diagnosticText;
    private Button switchCameraButton;
    private Button testButton;    private int currentCameraFacing = CameraSelector.LENS_FACING_FRONT;
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_camera_diagnostic, container, false);
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize views        previewView = view.findViewById(R.id.diagnostic_preview_view);
        diagnosticText = view.findViewById(R.id.text_diagnostic_info);
        switchCameraButton = view.findViewById(R.id.button_switch_camera);
        testButton = view.findViewById(R.id.button_test_camera);
        
        // Initial diagnostic info
        updateDiagnosticText("Camera Diagnostic Tool\n\nInitializing...\n\n" +
                             "CameraX compatibility mode: " + 
                             (CameraXCompatHelper.getCameraXVersion().equals("unknown") ? 
                                "Runtime detection" : "Version " + CameraXCompatHelper.getCameraXVersion()));

        // Set up button listeners
        switchCameraButton.setOnClickListener(v -> {
            currentCameraFacing = (currentCameraFacing == CameraSelector.LENS_FACING_FRONT)
                    ? CameraSelector.LENS_FACING_BACK
                    : CameraSelector.LENS_FACING_FRONT;
            startCamera();
        });

        testButton.setOnClickListener(v -> runCameraDiagnostics());
        
        // Add a way to get back to the video screen
        view.setOnLongClickListener(v -> {
            requireActivity().getSupportFragmentManager().popBackStack();
            Toast.makeText(requireContext(), "Returning to Video Screen", Toast.LENGTH_SHORT).show();
            return true;
        });

        // Check for camera permissions
        if (allPermissionsGranted()) {
            startCamera();
            runCameraDiagnostics();
        } else {
            ActivityCompat.requestPermissions(
                    requireActivity(),
                    REQUIRED_PERMISSIONS,
                    REQUEST_CODE_PERMISSIONS);
        }
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider
                .getInstance(requireContext());

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                bindCameraPreview(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Error starting camera", e);
                updateDiagnosticText("Error starting camera: " + e.getMessage());
            }
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    private void bindCameraPreview(ProcessCameraProvider cameraProvider) {
        cameraProvider.unbindAll();        // Set up the camera selector
        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(currentCameraFacing)
                .build();
                
        // Set up the preview
        Preview preview = new Preview.Builder().build();
        
        // Use our compatibility helper to set the surface provider
        if (!CameraXCompatHelper.setSurfaceProviderCompat(preview, previewView)) {
            Log.e(TAG, "Failed to set surface provider using compatibility helper");
            updateDiagnosticText("Failed to set up camera preview");
        }

        try {
            // Bind the camera
            cameraProvider.bindToLifecycle(getViewLifecycleOwner(), cameraSelector, preview);
            updateDiagnosticText("Camera preview bound successfully with " +
                    (currentCameraFacing == CameraSelector.LENS_FACING_FRONT ? "FRONT" : "BACK") +
                    " camera");
        } catch (Exception e) {
            updateDiagnosticText("Failed to bind camera: " + e.getMessage());
            Log.e(TAG, "Failed to bind camera", e);
        }
    }

    private void runCameraDiagnostics() {
        StringBuilder diagnosticInfo = new StringBuilder();

        // Check if device has camera
        if (requireContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
            diagnosticInfo.append("✅ Device has camera\n");
        } else {
            diagnosticInfo.append("❌ Device does not have camera\n");
        }

        // Check camera permissions
        if (allPermissionsGranted()) {
            diagnosticInfo.append("✅ Camera permission granted\n");
        } else {
            diagnosticInfo.append("❌ Camera permission NOT granted\n");
        }

        // Get camera info
        CameraManager cameraManager = (CameraManager) requireContext().getSystemService(Context.CAMERA_SERVICE);

        try {
            String[] cameraIds = cameraManager.getCameraIdList();
            diagnosticInfo.append("📷 Found ").append(cameraIds.length).append(" cameras\n");

            for (String cameraId : cameraIds) {
                CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);
                Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                String facingText;

                if (facing != null) {
                    switch (facing) {
                        case CameraCharacteristics.LENS_FACING_FRONT:
                            facingText = "FRONT";
                            break;
                        case CameraCharacteristics.LENS_FACING_BACK:
                            facingText = "BACK";
                            break;
                        case CameraCharacteristics.LENS_FACING_EXTERNAL:
                            facingText = "EXTERNAL";
                            break;
                        default:
                            facingText = "UNKNOWN";
                    }                    diagnosticInfo.append("Camera ID ").append(cameraId)
                            .append(": ").append(facingText).append("\n");
                }
            }
        } catch (CameraAccessException e) {
            diagnosticInfo.append("❌ Error accessing camera: ").append(e.getMessage()).append("\n");
            Log.e(TAG, "Error accessing camera", e);
        }
        
        // Check CameraX version
        try {
            // Try to get CameraX version through reflection to avoid direct BuildConfig reference
            Class<?> buildConfigClass = Class.forName("androidx.camera.core.BuildConfig");
            java.lang.reflect.Field versionField = buildConfigClass.getField("VERSION_NAME");
            String version = (String) versionField.get(null);
            diagnosticInfo.append("CameraX: androidx.camera core version: ")
                    .append(version).append("\n");
        } catch (Exception e) {
            diagnosticInfo.append("CameraX: version info not available\n");
            // Get class version info instead
            try {
                String cameraCoreVersion = String.valueOf(CameraSelector.class.getPackage().getImplementationVersion());
                diagnosticInfo.append("CameraX package version: ")
                        .append(cameraCoreVersion != null ? cameraCoreVersion : "unknown").append("\n");
            } catch (Exception ex) {
                diagnosticInfo.append("Could not determine CameraX version\n");
            }
        }

        updateDiagnosticText(diagnosticInfo.toString());
    }

    private void updateDiagnosticText(String text) {
        if (diagnosticText != null) {
            diagnosticText.setText(text);
        }
        Log.d(TAG, text);
    }

    private boolean allPermissionsGranted() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(requireContext(), permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera();
                runCameraDiagnostics();
            } else {
                updateDiagnosticText("Permissions not granted by the user.");
            }
        }
    }
}
