package com.pet.evaluator.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Utility class for server connection configuration and health checks
 */
public class ServerConfig {
    private static final String TAG = "ServerConfig";

    // Server URLs
    public static final String LOCAL_EMULATOR_URL = "http://10.0.2.2:5000/";
    public static final String LOCAL_DEVICE_URL = "http://127.0.0.1:5000/";
    public static final String RENDER_URL = "https://pet-ml-api-sqwo.onrender.com/";

    // Shared Preference Keys
    private static final String PREFS_NAME = "pet_server_config";
    private static final String KEY_USE_LOCAL_SERVER = "use_local_server";
    private static final String KEY_OFFLINE_MODE = "offline_mode";
    private static final String KEY_LAST_SERVER_URL = "last_server_url";
    private static final String KEY_SERVER_AVAILABLE = "server_available";

    // Singleton instance
    private static ServerConfig instance;

    // Server status callbacks
    public interface ServerStatusCallback {
        void onServerStatus(boolean isAvailable);
    }

    private final Context context;
    private final SharedPreferences preferences;
    private final ExecutorService executorService;

    /**
     * Get the singleton instance
     * 
     * @param context Application context
     * @return ServerConfig instance
     */
    public static synchronized ServerConfig getInstance(Context context) {
        if (instance == null) {
            instance = new ServerConfig(context.getApplicationContext());
        }
        return instance;
    }

    private ServerConfig(Context context) {
        this.context = context;
        this.preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.executorService = Executors.newSingleThreadExecutor();

        // Set default values if not already set
        if (!preferences.contains(KEY_USE_LOCAL_SERVER)) {
            preferences.edit().putBoolean(KEY_USE_LOCAL_SERVER, false).apply();
        }
        if (!preferences.contains(KEY_OFFLINE_MODE)) {
            preferences.edit().putBoolean(KEY_OFFLINE_MODE, false).apply();
        }
        if (!preferences.contains(KEY_LAST_SERVER_URL)) {
            preferences.edit().putString(KEY_LAST_SERVER_URL, RENDER_URL).apply();
        }
    }

    /**
     * Set whether to use offline mode
     * 
     * @param useOfflineMode true to use offline mode, false for online mode
     */
    public void setOfflineMode(boolean useOfflineMode) {
        preferences.edit().putBoolean(KEY_OFFLINE_MODE, useOfflineMode).apply();
        Log.d(TAG, "Offline mode set to: " + useOfflineMode);
    }

    /**
     * Set whether to use a local development server
     * 
     * @param useLocalServer true for local server, false for production server
     *                       (Render)
     */
    public void setLocalServerMode(boolean useLocalServer) {
        preferences.edit().putBoolean(KEY_USE_LOCAL_SERVER, useLocalServer).apply();

        // Set the current server URL based on this setting
        String serverUrl;
        if (useLocalServer) {
            serverUrl = isEmulator() ? LOCAL_EMULATOR_URL : LOCAL_DEVICE_URL;
        } else {
            serverUrl = RENDER_URL;
        }

        preferences.edit().putString(KEY_LAST_SERVER_URL, serverUrl).apply();
        Log.d(TAG, "Server URL set to: " + serverUrl);

        // Check if the server is available
        checkServerAvailability(null);
    }

    /**
     * Get the current server URL to use
     * 
     * @return Server URL string
     */
    public String getServerUrl() {
        if (isOfflineMode()) {
            return ""; // No server in offline mode
        }

        return preferences.getString(KEY_LAST_SERVER_URL, RENDER_URL);
    }

    /**
     * Check if offline mode is enabled
     * 
     * @return true if offline mode is enabled
     */
    public boolean isOfflineMode() {
        return preferences.getBoolean(KEY_OFFLINE_MODE, false);
    }

    /**
     * Check if using local server
     * 
     * @return true if using local server, false for production server
     */
    public boolean isUsingLocalServer() {
        return preferences.getBoolean(KEY_USE_LOCAL_SERVER, false);
    }

    /**
     * Check if the server is available
     * 
     * @param callback Optional callback to receive the result
     */
    public void checkServerAvailability(final ServerStatusCallback callback) {
        if (isOfflineMode()) {
            preferences.edit().putBoolean(KEY_SERVER_AVAILABLE, false).apply();
            if (callback != null) {
                new Handler(Looper.getMainLooper()).post(() -> callback.onServerStatus(false));
            }
            return;
        }

        final String serverUrl = getServerUrl();

        executorService.execute(() -> {
            boolean isAvailable = false;
            try {
                URL url = new URL(serverUrl + "api/health");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setConnectTimeout(5000); // 5 seconds
                connection.setReadTimeout(5000);
                connection.setRequestMethod("GET");
                int responseCode = connection.getResponseCode();
                isAvailable = responseCode == 200;
                connection.disconnect();
                Log.d(TAG, "Server health check: " + (isAvailable ? "Available" : "Unavailable") +
                        " (Status code: " + responseCode + ")");
            } catch (IOException e) {
                Log.e(TAG, "Error checking server availability: " + e.getMessage());
                isAvailable = false;
            }

            // Save the result
            final boolean finalIsAvailable = isAvailable;
            preferences.edit().putBoolean(KEY_SERVER_AVAILABLE, finalIsAvailable).apply();

            // Notify via callback on main thread
            if (callback != null) {
                new Handler(Looper.getMainLooper()).post(() -> callback.onServerStatus(finalIsAvailable));
            }
        });
    }

    /**
     * Get the last known server availability status
     * 
     * @return true if the server was available in the last check
     */
    public boolean isServerAvailable() {
        return !isOfflineMode() && preferences.getBoolean(KEY_SERVER_AVAILABLE, false);
    }

    /**
     * Check if network connectivity is available
     * 
     * @return true if the device has internet connectivity
     */
    public boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) {
            return false;
        }

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }

    /**
     * Check if the device is running in an emulator
     * 
     * @return true if running in emulator
     */
    private boolean isEmulator() {
        return android.os.Build.PRODUCT.contains("sdk") ||
                android.os.Build.MODEL.contains("sdk") ||
                android.os.Build.MODEL.toLowerCase().contains("emulator") ||
                android.os.Build.HARDWARE.contains("goldfish") ||
                android.os.Build.HARDWARE.contains("ranchu");
    }
}
