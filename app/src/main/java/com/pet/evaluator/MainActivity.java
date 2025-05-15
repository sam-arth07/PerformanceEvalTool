package com.pet.evaluator;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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
    }

    private void setupObservers() {
        // Show any error messages
        sharedViewModel.getErrorMessage().observe(this, errorMessage -> {
            if (errorMessage != null && !errorMessage.isEmpty()) {
                Snackbar.make(
                        findViewById(R.id.container),
                        errorMessage,
                        Snackbar.LENGTH_LONG
                ).show();
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
    }

    @Override
    public boolean onSupportNavigateUp() {
        return navController.navigateUp() || super.onSupportNavigateUp();
    }
}
