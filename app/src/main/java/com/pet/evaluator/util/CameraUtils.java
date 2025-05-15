package com.pet.evaluator.util;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

/**
 * Utility class for camera-related diagnostics and permissions
 */
public class CameraUtils {

    private static final String TAG = "CameraUtils";
    private static final int REQUEST_CAMERA_PERMISSION = 100;

    /**
     * Check if the device has at least one camera
     */
    public static boolean deviceHasCamera(Context context) {
        return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY);
    }

    /**
     * Check if the app has been granted camera permission
     */
    public static boolean hasCameraPermission(Context context) {
        return ContextCompat.checkSelfPermission(context,
                Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Request camera permission
     */
    public static void requestCameraPermission(Activity activity) {
        ActivityCompat.requestPermissions(activity,
                new String[] { Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO },
                REQUEST_CAMERA_PERMISSION);
    }

    /**
     * Open device camera settings
     */
    public static void openCameraSettings(Context context) {
        Intent intent = new Intent();
        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", context.getPackageName(), null);
        intent.setData(uri);
        context.startActivity(intent);
    }

    /**
     * Test camera by launching the system camera app
     */
    public static void launchSystemCamera(Activity activity, int requestCode) {
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (cameraIntent.resolveActivity(activity.getPackageManager()) != null) {
            activity.startActivityForResult(cameraIntent, requestCode);
        } else {
            Toast.makeText(activity, "No camera app found!", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Log camera information for debugging
     */
    public static void logCameraInfo(Context context) {
        try {
            PackageManager pm = context.getPackageManager();
            Log.d(TAG, "Device has camera: " + pm.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY));
            Log.d(TAG, "Device has front camera: " + pm.hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT));
            Log.d(TAG, "Device has flash: " + pm.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH));
            Log.d(TAG, "Device has autofocus: " + pm.hasSystemFeature(PackageManager.FEATURE_CAMERA_AUTOFOCUS));
            Log.d(TAG, "Camera permission granted: " + hasCameraPermission(context));
            Log.d(TAG, "Android SDK version: " + Build.VERSION.SDK_INT);
        } catch (Exception e) {
            Log.e(TAG, "Error logging camera info", e);
        }
    }

    /**
     * Show camera troubleshooting tips
     */
    public static void showTroubleshootingTips(FragmentActivity activity) {
        StringBuilder tips = new StringBuilder();
        tips.append("Camera Troubleshooting Tips:\n\n");
        tips.append("1. Check if camera permission is granted in Settings\n");
        tips.append("2. Restart the application\n");
        tips.append("3. Try using the front camera if rear camera doesn't work\n");
        tips.append("4. Make sure no other app is using the camera\n");
        tips.append("5. Check if the device camera works with the default camera app\n");

        Toast.makeText(activity, "Check log for troubleshooting tips", Toast.LENGTH_LONG).show();
        Log.i(TAG, tips.toString());
    }
}
