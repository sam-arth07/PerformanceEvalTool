// Enhanced MainActivity with improved error handling and diagnostics
package com.pet.evaluator;

import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.snackbar.Snackbar;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Enhanced MainActivity with improved error handling and diagnostics
 */
public class MainActivity extends AppCompatActivity {
    private static final String TAG = "PET_MainActivity";
    private NavController navController;
    private SharedViewModel sharedViewModel;
    private ExecutorService executorService;
    private boolean apiChecked = false;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // Initialize executor service for background tasks
        executorService = Executors.newCachedThreadPool();
        
        // Initialize the shared ViewModel
        sharedViewModel = new ViewModelProvider(this).get(SharedViewModel.class);

        BottomNavigationView navView = findViewById(R.id.nav_view);
        // Set up the navigation controller with the bottom navigation
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_home, R.id.navigation_resume, R.id.navigation_video, R.id.navigation_results)
                .build();
        navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(navView, navController);
        
        // Observe shared data
        setupObservers();
        
        // Check API connectivity
        checkApiConnectivity();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Shutdown the executor service
        if (executorService != null) {
            executorService.shutdown();
        }
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_diagnostics) {
            runDiagnostics();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    /**
     * Set up observers for the shared ViewModel
     */
    private void setupObservers() {
        // Observe error messages
        sharedViewModel.getErrorMessage().observe(this, errorMessage -> {
            if (errorMessage != null && !errorMessage.isEmpty()) {
                showError(errorMessage);
                // Clear the error message after showing it
                sharedViewModel.setErrorMessage("");
            }
        });
        
        // Observe processing state
        sharedViewModel.getIsProcessing().observe(this, isProcessing -> {
            // Update UI based on processing state
            if (isProcessing) {
                Log.d(TAG, "Processing in progress...");
                // Show loading indicator if needed
            } else {
                Log.d(TAG, "Processing completed");
                // Hide loading indicator if needed
            }
        });
    }
    
    /**
     * Show an error message to the user
     * 
     * @param message Error message to show
     */
    private void showError(String message) {
        Log.e(TAG, "Error: " + message);
        Snackbar.make(
                findViewById(android.R.id.content),
                message,
                Snackbar.LENGTH_LONG
        ).show();
    }
    
    /**
     * Check API connectivity in the background
     */
    private void checkApiConnectivity() {
        if (apiChecked) {
            return;
        }
        
        executorService.execute(() -> {
            try {
                // Try to connect to the health check endpoint
                URL url = new URL("http://10.0.2.2:5000/health");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);
                connection.setRequestMethod("GET");
                
                int responseCode = connection.getResponseCode();
                if (responseCode == 200) {
                    // API is available
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(connection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();
                    
                    Log.i(TAG, "API health check successful: " + response.toString());
                    apiChecked = true;
                    
                    // Show success message on UI thread
                    runOnUiThread(() -> 
                        Toast.makeText(MainActivity.this, 
                            "Connected to evaluation server", Toast.LENGTH_SHORT).show()
                    );
                } else {
                    // API returned an error
                    Log.e(TAG, "API health check failed with code: " + responseCode);
                    showApiConnectionError();
                }
                
                connection.disconnect();
            } catch (IOException e) {
                Log.e(TAG, "Error connecting to API: " + e.getMessage(), e);
                showApiConnectionError();
            }
        });
    }
    
    /**
     * Show API connection error dialog
     */
    private void showApiConnectionError() {
        runOnUiThread(() -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setTitle("Connection Error")
                   .setMessage("Could not connect to the evaluation server. Some features may not work correctly. Would you like to retry?")
                   .setPositiveButton("Retry", (dialog, which) -> {
                       apiChecked = false;
                       checkApiConnectivity();
                   })
                   .setNegativeButton("Continue Offline", (dialog, which) -> {
                       Toast.makeText(MainActivity.this, 
                           "Using offline mode with limited features", Toast.LENGTH_LONG).show();
                   })
                   .setCancelable(false)
                   .show();
        });
    }
    
    /**
     * Run diagnostics to help troubleshoot issues
     */
    private void runDiagnostics() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Running Diagnostics");
        builder.setMessage("Checking system status...");
        AlertDialog dialog = builder.create();
        dialog.show();
        
        executorService.execute(() -> {
            StringBuilder diagnostics = new StringBuilder();
            diagnostics.append("=== PET Diagnostics ===\n\n");
            
            // Check API connectivity
            try {
                URL url = new URL("http://10.0.2.2:5000/health");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);
                
                int responseCode = connection.getResponseCode();
                diagnostics.append("API Connection: ");
                if (responseCode == 200) {
                    diagnostics.append("SUCCESS\n");
                    
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(connection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();
                    
                    diagnostics.append("API Response: ").append(response.toString()).append("\n");
                } else {
                    diagnostics.append("FAILED (").append(responseCode).append(")\n");
                }
                
                connection.disconnect();
            } catch (IOException e) {
                diagnostics.append("API Connection: ERROR - ").append(e.getMessage()).append("\n");
                
                // Try alternate URL (localhost)
                try {
                    URL localUrl = new URL("http://localhost:5000/health");
                    HttpURLConnection localConn = (HttpURLConnection) localUrl.openConnection();
                    localConn.setConnectTimeout(3000);
                    
                    int localResponseCode = localConn.getResponseCode();
                    diagnostics.append("Local API Connection: ");
                    if (localResponseCode == 200) {
                        diagnostics.append("SUCCESS\n");
                        diagnostics.append("Use 'localhost' instead of '10.0.2.2' in config\n");
                    } else {
                        diagnostics.append("FAILED\n");
                    }
                    
                    localConn.disconnect();
                } catch (IOException ex) {
                    diagnostics.append("Local API Connection: ERROR\n");
                }
            }
            
            // Check TFLite model availability
            String[] models = {"resume_model.tflite", "video_model.tflite", "evaluation_model.tflite"};
            diagnostics.append("\nTFLite Models:\n");
            
            for (String model : models) {
                try {
                    String[] assets = getAssets().list("");
                    boolean found = false;
                    for (String asset : assets) {
                        if (asset.equals(model)) {
                            found = true;
                            break;
                        }
                    }
                    
                    if (found) {
                        diagnostics.append("- ").append(model).append(": AVAILABLE\n");
                    } else {
                        diagnostics.append("- ").append(model).append(": MISSING\n");
                    }
                } catch (IOException e) {
                    diagnostics.append("- ").append(model).append(": ERROR checking\n");
                }
            }
            
            // Memory info
            Runtime runtime = Runtime.getRuntime();
            long maxMemory = runtime.maxMemory() / (1024 * 1024);
            long totalMemory = runtime.totalMemory() / (1024 * 1024);
            long freeMemory = runtime.freeMemory() / (1024 * 1024);
            
            diagnostics.append("\nMemory Status:\n");
            diagnostics.append("- Max memory: ").append(maxMemory).append(" MB\n");
            diagnostics.append("- Total memory: ").append(totalMemory).append(" MB\n");
            diagnostics.append("- Free memory: ").append(freeMemory).append(" MB\n");
            diagnostics.append("- Used memory: ").append(totalMemory - freeMemory).append(" MB\n");
            
            String finalDiagnostics = diagnostics.toString();
            
            // Update UI on main thread
            runOnUiThread(() -> {
                if (dialog.isShowing()) {
                    dialog.dismiss();
                }
                
                AlertDialog.Builder resultBuilder = new AlertDialog.Builder(MainActivity.this);
                resultBuilder.setTitle("Diagnostics Results")
                       .setMessage(finalDiagnostics)
                       .setPositiveButton("Copy to Clipboard", (d, which) -> {
                           android.content.ClipboardManager clipboard = 
                               (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                           android.content.ClipData clip = 
                               android.content.ClipData.newPlainText("PET Diagnostics", finalDiagnostics);
                           clipboard.setPrimaryClip(clip);
                           Toast.makeText(MainActivity.this, 
                               "Diagnostics copied to clipboard", Toast.LENGTH_SHORT).show();
                       })
                       .setNegativeButton("Close", null)
                       .show();
            });
        });
    }
}
