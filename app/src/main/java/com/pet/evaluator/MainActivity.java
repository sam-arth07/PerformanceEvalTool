package com.pet.evaluator;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.snackbar.Snackbar;

public class MainActivity extends AppCompatActivity {

    private NavController navController;
    private SharedViewModel sharedViewModel;
    private TextView offlineModeIndicator;
    private SharedPreferences preferences;
    private boolean isOfflineMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize preferences and get offline mode state
        preferences = getSharedPreferences("pet_ml_preferences", MODE_PRIVATE);
        isOfflineMode = preferences.getBoolean("offline_mode_preferred", false);

        // Find the offline mode indicator
        offlineModeIndicator = findViewById(R.id.offline_mode_indicator);
        updateOfflineModeIndicator();

        // Initialize the shared ViewModel
        sharedViewModel = new ViewModelProvider(this).get(SharedViewModel.class);

        // Set initial offline mode in SharedViewModel
        sharedViewModel.setIsOfflineMode(isOfflineMode);

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
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        MenuItem offlineModeItem = menu.findItem(R.id.action_offline_mode);
        if (offlineModeItem != null) {
            offlineModeItem.setChecked(isOfflineMode);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_offline_mode) {
            // Toggle offline mode
            isOfflineMode = !isOfflineMode;
            item.setChecked(isOfflineMode);

            // Save preference
            preferences.edit().putBoolean("offline_mode_preferred", isOfflineMode).apply();

            // Update shared view model (will update the UI via observer)
            sharedViewModel.setIsOfflineMode(isOfflineMode);

            // Show confirmation
            Snackbar.make(
                    findViewById(R.id.container),
                    isOfflineMode ? R.string.offline_mode_enabled : R.string.online_mode_enabled,
                    Snackbar.LENGTH_LONG).show();

            return true;
        } else if (item.getItemId() == R.id.action_diagnostics) {
            // Show diagnostics dialog
            showServerDiagnosticsDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Update the visibility and text of the offline mode indicator
     */
    private void updateOfflineModeIndicator() {
        if (isOfflineMode) {
            offlineModeIndicator.setVisibility(View.VISIBLE);
        } else {
            offlineModeIndicator.setVisibility(View.GONE);
        }
    }

    /**
     * Show a dialog to run server diagnostics
     */
    private void showServerDiagnosticsDialog() {
        // Create dialog
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_server_diagnostics, null);
        builder.setView(dialogView);
        builder.setCancelable(false);

        // Find views
        ProgressBar progressBar = dialogView.findViewById(R.id.diagnostics_progress);
        TextView messageView = dialogView.findViewById(R.id.diagnostics_message);
        View resultContainer = dialogView.findViewById(R.id.diagnostics_result_container);
        TextView resultTitleView = dialogView.findViewById(R.id.diagnostics_result_title);
        TextView resultMessageView = dialogView.findViewById(R.id.diagnostics_result_message);

        // Setup buttons
        builder.setNegativeButton(R.string.close, (dialog, which) -> dialog.dismiss());

        // Create dialog
        androidx.appcompat.app.AlertDialog dialog = builder.create();
        dialog.show();

        // Run diagnostics in background
        new Thread(() -> {
            boolean serverAvailable = false;
            try {
                // Try connecting to local emulator port
                java.net.Socket socket = new java.net.Socket();
                socket.connect(new java.net.InetSocketAddress("10.0.2.2", 5000), 3000);
                serverAvailable = true;
                socket.close();
            } catch (Exception e) {
                try {
                    // If emulator port failed, try local device port
                    java.net.Socket socket = new java.net.Socket();
                    socket.connect(new java.net.InetSocketAddress("127.0.0.1", 5000), 3000);
                    serverAvailable = true;
                    socket.close();
                } catch (Exception e2) {
                    serverAvailable = false;
                }
            }

            // Update UI on main thread
            final boolean finalServerAvailable = serverAvailable;
            runOnUiThread(() -> {
                // Hide progress indicator
                progressBar.setVisibility(View.GONE);
                messageView.setVisibility(View.GONE);
                resultContainer.setVisibility(View.VISIBLE);

                if (finalServerAvailable) {
                    resultTitleView.setText(R.string.server_available);
                    resultMessageView.setText(R.string.server_available_message);

                    // Replace negative button with different text
                    dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEGATIVE).setText(R.string.close);

                    // Add positive button to disable offline mode if currently enabled
                    if (isOfflineMode) {
                        dialog.setButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE,
                                getString(R.string.online_mode_enabled), (dialogInterface, i) -> {
                                    // Switch to online mode
                                    isOfflineMode = false;
                                    preferences.edit().putBoolean("offline_mode_preferred", false).apply();
                                    sharedViewModel.setIsOfflineMode(false);
                                    dialog.dismiss();

                                    // Show confirmation
                                    Snackbar.make(
                                            findViewById(R.id.container),
                                            R.string.online_mode_enabled,
                                            Snackbar.LENGTH_LONG).show();
                                });
                    }
                } else {
                    resultTitleView.setText(R.string.server_unavailable);
                    resultMessageView.setText(R.string.server_unavailable_message);

                    // Replace negative button with different text
                    dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEGATIVE).setText(R.string.close);

                    // Add positive button to enable offline mode if not already enabled
                    if (!isOfflineMode) {
                        dialog.setButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE,
                                getString(R.string.enable_offline_mode), (dialogInterface, i) -> {
                                    // Switch to offline mode
                                    isOfflineMode = true;
                                    preferences.edit().putBoolean("offline_mode_preferred", true).apply();
                                    sharedViewModel.setIsOfflineMode(true);
                                    dialog.dismiss();

                                    // Show confirmation
                                    Snackbar.make(
                                            findViewById(R.id.container),
                                            R.string.offline_mode_enabled,
                                            Snackbar.LENGTH_LONG).show();
                                });
                    }
                }
            });
        }).start();
    }

    private void setupObservers() {
        // Show any error messages
        sharedViewModel.getErrorMessage().observe(this, errorMessage -> {
            if (errorMessage != null && !errorMessage.isEmpty()) {
                Snackbar.make(
                        findViewById(R.id.container),
                        errorMessage,
                        Snackbar.LENGTH_LONG).show();
            }
        });

        // Show loading indicator
        sharedViewModel.getIsProcessing().observe(this, isProcessing -> {
            if (Boolean.TRUE.equals(isProcessing)) {
                // Show processing indicator (could be a progress dialog or something similar)
                // For simplicity, we'll just show a toast
                Toast.makeText(this, "Processing...", Toast.LENGTH_SHORT).show();
            }
        });

        // Update UI based on offline mode status changes from other components
        sharedViewModel.getIsOfflineMode().observe(this, offlineMode -> {
            if (offlineMode != null && offlineMode != isOfflineMode) {
                isOfflineMode = offlineMode;
                updateOfflineModeIndicator();

                // Update menu checkbox if menu is created
                invalidateOptionsMenu();

                // Save preference
                preferences.edit().putBoolean("offline_mode_preferred", isOfflineMode).apply();
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        return navController.navigateUp() || super.onSupportNavigateUp();
    }
}
