package com.pet.evaluator.util;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility class for handling camera and related permissions at runtime
 */
public class PermissionManager {
    private static final String TAG = "PermissionManager";

    // Static permission request codes
    public static final int REQUEST_CODE_CAMERA_PERMISSIONS = 10;
    public static final int REQUEST_CODE_STORAGE_PERMISSIONS = 20;

    // Basic camera permissions needed
    private static final String[] BASIC_CAMERA_PERMISSIONS = {
            Manifest.permission.CAMERA
    };

    // Camera permissions with audio recording
    private static final String[] CAMERA_WITH_AUDIO_PERMISSIONS = {
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
    };

    // Storage permissions needed
    private static final String[] STORAGE_PERMISSIONS = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    // Storage permissions for Android 10+ (API 29+)
    private static final String[] STORAGE_PERMISSIONS_API_29_PLUS = {
            Manifest.permission.READ_EXTERNAL_STORAGE
    };

    // Callback interface for permission results
    public interface PermissionCallback {
        void onPermissionsGranted();

        void onPermissionsDenied(List<String> deniedPermissions);
    }

    // Keep track of callbacks
    private static final Map<Integer, PermissionCallback> callbackMap = new HashMap<>();

    /**
     * Check and request camera permissions
     * 
     * @param activity     Activity requesting permissions
     * @param requestCode  Request code to use
     * @param callback     Callback to notify of the result
     * @param includeAudio Whether to include audio recording permissions
     */
    public static void checkAndRequestCameraPermissions(
            @NonNull Activity activity,
            int requestCode,
            @NonNull PermissionCallback callback,
            boolean includeAudio) {

        String[] permissions = includeAudio ? CAMERA_WITH_AUDIO_PERMISSIONS : BASIC_CAMERA_PERMISSIONS;
        checkAndRequestPermissions(activity, permissions, requestCode, callback);
    }

    /**
     * Check and request camera permissions within a fragment
     * 
     * @param fragment     Fragment requesting permissions
     * @param requestCode  Request code to use
     * @param callback     Callback to notify of the result
     * @param includeAudio Whether to include audio recording permissions
     */
    public static void checkAndRequestCameraPermissions(
            @NonNull Fragment fragment,
            int requestCode,
            @NonNull PermissionCallback callback,
            boolean includeAudio) {

        String[] permissions = includeAudio ? CAMERA_WITH_AUDIO_PERMISSIONS : BASIC_CAMERA_PERMISSIONS;
        checkAndRequestPermissions(fragment, permissions, requestCode, callback);
    }

    /**
     * Check and request storage permissions
     * 
     * @param activity    Activity requesting permissions
     * @param requestCode Request code to use
     * @param callback    Callback to notify of the result
     */
    public static void checkAndRequestStoragePermissions(
            @NonNull Activity activity,
            int requestCode,
            @NonNull PermissionCallback callback) {

        String[] permissions = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ? STORAGE_PERMISSIONS_API_29_PLUS
                : STORAGE_PERMISSIONS;
        checkAndRequestPermissions(activity, permissions, requestCode, callback);
    }

    /**
     * Check and request storage permissions within a fragment
     * 
     * @param fragment    Fragment requesting permissions
     * @param requestCode Request code to use
     * @param callback    Callback to notify of the result
     */
    public static void checkAndRequestStoragePermissions(
            @NonNull Fragment fragment,
            int requestCode,
            @NonNull PermissionCallback callback) {

        String[] permissions = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ? STORAGE_PERMISSIONS_API_29_PLUS
                : STORAGE_PERMISSIONS;
        checkAndRequestPermissions(fragment, permissions, requestCode, callback);
    }

    /**
     * Check if all camera permissions are granted
     * 
     * @param context      Context
     * @param includeAudio Whether to include audio recording permissions
     * @return true if all permissions are granted
     */
    public static boolean hasCameraPermissions(@NonNull Context context, boolean includeAudio) {
        String[] permissions = includeAudio ? CAMERA_WITH_AUDIO_PERMISSIONS : BASIC_CAMERA_PERMISSIONS;
        return hasPermissions(context, permissions);
    }

    /**
     * Check if all storage permissions are granted
     * 
     * @param context Context
     * @return true if all permissions are granted
     */
    public static boolean hasStoragePermissions(@NonNull Context context) {
        String[] permissions = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ? STORAGE_PERMISSIONS_API_29_PLUS
                : STORAGE_PERMISSIONS;
        return hasPermissions(context, permissions);
    }

    /**
     * Handle permission results from Activity or Fragment
     * 
     * @param requestCode  The request code
     * @param permissions  The requested permissions
     * @param grantResults The grant results
     */
    public static void handlePermissionResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults) {

        PermissionCallback callback = callbackMap.get(requestCode);
        if (callback == null) {
            Log.w(TAG, "No callback found for request code: " + requestCode);
            return;
        }

        // Remove the callback from our map
        callbackMap.remove(requestCode);

        // Check results
        List<String> deniedPermissions = new ArrayList<>();
        for (int i = 0; i < permissions.length; i++) {
            if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                deniedPermissions.add(permissions[i]);
            }
        }

        if (deniedPermissions.isEmpty()) {
            callback.onPermissionsGranted();
        } else {
            callback.onPermissionsDenied(deniedPermissions);
        }
    }

    // Private helper methods

    private static void checkAndRequestPermissions(
            @NonNull Activity activity,
            @NonNull String[] permissions,
            int requestCode,
            @NonNull PermissionCallback callback) {

        if (hasPermissions(activity, permissions)) {
            // All permissions are already granted
            callback.onPermissionsGranted();
            return;
        }

        // Store callback for later
        callbackMap.put(requestCode, callback);

        // Request the permissions
        ActivityCompat.requestPermissions(activity, permissions, requestCode);
    }

    private static void checkAndRequestPermissions(
            @NonNull Fragment fragment,
            @NonNull String[] permissions,
            int requestCode,
            @NonNull PermissionCallback callback) {

        if (hasPermissions(fragment.requireContext(), permissions)) {
            // All permissions are already granted
            callback.onPermissionsGranted();
            return;
        }

        // Store callback for later
        callbackMap.put(requestCode, callback);

        // Request the permissions
        fragment.requestPermissions(permissions, requestCode);
    }

    private static boolean hasPermissions(@NonNull Context context, @NonNull String[] permissions) {
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }
}
