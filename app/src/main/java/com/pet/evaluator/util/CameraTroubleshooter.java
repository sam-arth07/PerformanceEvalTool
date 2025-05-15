package com.pet.evaluator.util;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.camera.core.CameraSelector;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Extended camera utility class for additional diagnostics
 */
public class CameraTroubleshooter {
    private static final String TAG = "CameraTroubleshooter";

    /**
     * Check for camera permissions and camera hardware
     * 
     * @param context Context
     * @return A list of issues found, empty if none
     */
    public static List<String> runCameraChecks(Context context) {
        List<String> issues = new ArrayList<>();

        // Check for camera hardware
        if (!context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
            issues.add("This device does not have a camera");
            return issues; // No need to continue if no camera
        }

        // Check for camera permissions
        if (ContextCompat.checkSelfPermission(context,
                Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            issues.add("Camera permission not granted");
        }

        // Check for CameraX dependencies
        try {
            Class.forName("androidx.camera.core.CameraSelector");
        } catch (ClassNotFoundException e) {
            issues.add("CameraX library not available");
        }

        // Check for front camera
        try {
            android.hardware.camera2.CameraManager cameraManager = (android.hardware.camera2.CameraManager) context
                    .getSystemService(Context.CAMERA_SERVICE);
            boolean hasFrontCamera = false;
            boolean hasBackCamera = false;

            for (String cameraId : cameraManager.getCameraIdList()) {
                android.hardware.camera2.CameraCharacteristics characteristics = cameraManager
                        .getCameraCharacteristics(cameraId);
                Integer facing = characteristics.get(android.hardware.camera2.CameraCharacteristics.LENS_FACING);
                if (facing != null) {
                    if (facing == android.hardware.camera2.CameraCharacteristics.LENS_FACING_FRONT) {
                        hasFrontCamera = true;
                    } else if (facing == android.hardware.camera2.CameraCharacteristics.LENS_FACING_BACK) {
                        hasBackCamera = true;
                    }
                }
            }

            if (!hasFrontCamera) {
                issues.add("No front camera detected");
            }

            if (!hasBackCamera) {
                issues.add("No back camera detected");
            }

        } catch (Exception e) {
            issues.add("Error checking camera availability: " + e.getMessage());
        }

        return issues;
    }

    /**
     * Generate a diagnostic report for the device and cameras
     * 
     * @param context Context
     * @return The path to the report file, or null if failed
     */
    public static String generateDiagnosticReport(Context context) {
        StringBuilder report = new StringBuilder();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

        report.append("# Camera Diagnostic Report\n");
        report.append("Generated: ").append(sdf.format(new Date())).append("\n\n");

        // Device information
        report.append("## Device Information\n");
        report.append("- Manufacturer: ").append(Build.MANUFACTURER).append("\n");
        report.append("- Model: ").append(Build.MODEL).append("\n");
        report.append("- Android Version: ").append(Build.VERSION.RELEASE)
                .append(" (API ").append(Build.VERSION.SDK_INT).append(")\n");

        // App information
        report.append("\n## App Information\n");
        try {
            PackageInfo pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            report.append("- App Version: ").append(pInfo.versionName)
                    .append(" (").append(pInfo.versionCode).append(")\n");
        } catch (PackageManager.NameNotFoundException e) {
            report.append("- App Version: Unknown\n");
        }

        // Camera information
        report.append("\n## Camera Information\n");
        List<String> issues = runCameraChecks(context);

        if (issues.isEmpty()) {
            report.append("- No camera issues detected\n");
        } else {
            report.append("- Issues found:\n");
            for (String issue : issues) {
                report.append("  - ").append(issue).append("\n");
            }
        }

        // Library versions
        report.append("\n## Library Versions\n");
        try {
            // Try to get CameraX version
            Class<?> buildConfigClass = Class.forName("androidx.camera.core.BuildConfig");
            java.lang.reflect.Field versionField = buildConfigClass.getField("VERSION_NAME");
            String version = (String) versionField.get(null);
            report.append("- CameraX Version: ").append(version).append("\n");
        } catch (Exception e) {
            report.append("- CameraX Version: Unknown\n");
        }

        // Write to file
        try {
            File outputDir = context.getCacheDir();
            File outputFile = new File(outputDir, "camera_diagnostic_report.txt");

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {
                writer.write(report.toString());
            }

            return outputFile.getAbsolutePath();
        } catch (IOException e) {
            Log.e(TAG, "Error writing diagnostic report", e);
            return null;
        }
    }

    /**
     * Show a dialog with camera troubleshooting tips
     * 
     * @param activity Activity context
     */
    public static void showTroubleshootingDialog(final Activity activity) {
        if (activity == null || activity.isFinishing()) {
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(activity)
                .setTitle("Camera Troubleshooting")
                .setMessage("Having trouble with the camera? Try these steps:\n\n" +
                        "1. Make sure camera permissions are granted\n" +
                        "2. Restart the app\n" +
                        "3. Check if other camera apps work\n" +
                        "4. Clear app cache\n" +
                        "5. Check if your device has the latest updates")
                .setPositiveButton("Open Settings", (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    Uri uri = Uri.fromParts("package", activity.getPackageName(), null);
                    intent.setData(uri);
                    activity.startActivity(intent);
                })
                .setNeutralButton("Generate Report", (dialog, which) -> {
                    String reportPath = generateDiagnosticReport(activity);
                    if (reportPath != null) {
                        showReportGeneratedDialog(activity, reportPath);
                    }
                })
                .setNegativeButton("Close", null);

        builder.show();
    }

    private static void showReportGeneratedDialog(Activity activity, String reportPath) {
        new AlertDialog.Builder(activity)
                .setTitle("Diagnostic Report Generated")
                .setMessage("A camera diagnostic report was generated at:\n" + reportPath +
                        "\n\nYou can share this with technical support.")
                .setPositiveButton("OK", null)
                .show();
    }
}
