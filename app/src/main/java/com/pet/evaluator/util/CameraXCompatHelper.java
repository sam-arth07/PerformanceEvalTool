package com.pet.evaluator.util;

import android.util.Log;

import androidx.camera.core.Preview;
import androidx.camera.view.PreviewView;

/**
 * Helper class to handle CameraX compatibility issues across different versions
 */
public class CameraXCompatHelper {
    private static final String TAG = "CameraXCompatHelper";

    /**
     * Safely set the surface provider for a preview, handling different CameraX API versions
     * @param preview The Preview instance
     * @param previewView The PreviewView instance
     * @return true if successful, false otherwise
     */
    public static boolean setSurfaceProviderCompat(Preview preview, PreviewView previewView) {
        if (preview == null || previewView == null) {
            Log.e(TAG, "Preview or PreviewView is null");
            return false;
        }

        try {
            // First try the newer API method (CameraX 1.0.0+)
            try {
                java.lang.reflect.Method getSurfaceProviderMethod = 
                        previewView.getClass().getMethod("getSurfaceProvider");
                Object surfaceProvider = getSurfaceProviderMethod.invoke(previewView);

                java.lang.reflect.Method setSurfaceProviderMethod = 
                        preview.getClass().getMethod("setSurfaceProvider", surfaceProvider.getClass());
                setSurfaceProviderMethod.invoke(preview, surfaceProvider);
                
                Log.d(TAG, "Using newer CameraX API (getSurfaceProvider)");
                return true;
            } catch (NoSuchMethodException | ReflectiveOperationException e) {
                Log.d(TAG, "Newer method not available, trying older API");
                
                // Fall back to the older API method (beta releases)
                try {
                    java.lang.reflect.Method createSurfaceProviderMethod = 
                            previewView.getClass().getMethod("createSurfaceProvider");
                    Object surfaceProvider = createSurfaceProviderMethod.invoke(previewView);
                    
                    java.lang.reflect.Method setSurfaceProviderMethod = 
                            preview.getClass().getMethod("setSurfaceProvider", surfaceProvider.getClass());
                    setSurfaceProviderMethod.invoke(preview, surfaceProvider);
                    
                    Log.d(TAG, "Using older CameraX API (createSurfaceProvider)");
                    return true;
                } catch (NoSuchMethodException | ReflectiveOperationException e2) {
                    // Try direct method call as last resort
                    try {
                        // This will work only if the app is compiled with matching CameraX version
                        preview.setSurfaceProvider(previewView.createSurfaceProvider());
                        Log.d(TAG, "Using direct method call");
                        return true;
                    } catch (Exception e3) {
                        Log.e(TAG, "All attempts to set surface provider failed", e3);
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error setting surface provider", e);
        }

        return false;
    }

    /**
     * Get the CameraX version information
     * @return The version string, or "unknown" if not available
     */
    public static String getCameraXVersion() {
        try {
            Class<?> buildConfigClass = Class.forName("androidx.camera.core.BuildConfig");
            java.lang.reflect.Field versionField = buildConfigClass.getField("VERSION_NAME");
            return (String) versionField.get(null);
        } catch (Exception e) {
            Log.d(TAG, "Could not get CameraX version: " + e.getMessage());
            return "unknown";
        }
    }
}
