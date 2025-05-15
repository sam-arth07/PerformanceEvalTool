# Network Timeout Handling Improvements - Bug Fixes Documentation

## Issue Summary

The PET Android app was experiencing `java.net.SocketTimeoutException` errors when trying to connect to the ML backend server. These occurred because the ML backend URL was unreachable, causing the app to hang or crash instead of gracefully falling back to offline mode.

## Implemented Fixes

### 1. MLModelManager Class Enhancements

-   Added multiple URL options for different environments:
    -   `LOCAL_EMULATOR_URL` for emulator testing (`10.0.2.2:5000`)
    -   `LOCAL_DEVICE_URL` for device testing (`127.0.0.1:5000`)
    -   `DEMO_URL` for production deployments
-   Added `isEmulator()` method to detect running environment
-   Enhanced `getPreferences()` method for offline mode preference management
-   Added `checkServerAvailabilityAsync()` to proactively test server connectivity
-   Default server availability to false (assume offline mode first)
-   Added `setUseOfflineMode()` method to explicitly control online/offline status

### 2. Enhanced URI Mock Handling for Offline Mode

-   Added special handling in `getFileFromUri()` for mock resume files
-   Added special handling in `getFileFromUri()` for mock video files
-   Improved detection of sample files based on path naming conventions

### 3. Enhanced Fallback Mechanisms

-   Improved `fallbackVideoProcessing()` to generate realistic mock data
-   Enhanced `fallbackResumeProcessing()` to adapt scoring based on file type:
    -   Senior resume samples (higher scores, more skills, more experience)
    -   Mid-level resume samples (moderate scores and skills)
    -   Entry-level resume samples (lower baseline scores)
-   Added verbose offline mode indications in mock transcriptions

### 4. UI Enhancements for Offline Mode

-   Added a visible offline mode indicator at the top of the screen
-   Added menu option to toggle offline mode
-   Updated toast messages to clearly indicate offline status
-   Added a server diagnostics dialog to test connectivity
-   Integrated offline mode status with SharedViewModel for app-wide awareness

### 5. Other Improvements

-   Added SharedPreferences integration for persisting offline mode preference
-   Enhanced error messaging to be more descriptive about connection issues
-   Improved thread safety in network operations

## Testing Instructions

1. Test with server running:

    - Launch the app with ML server running
    - Upload a resume and video
    - Verify that processing uses the server

2. Test with server unavailable:

    - Launch the app with ML server stopped
    - Enable "Use Offline Mode" from the menu
    - Upload a resume and video
    - Verify that processing uses on-device fallback models

3. Test server diagnostics:

    - Select "Run Diagnostics" from the menu
    - Verify connectivity status is correctly reported
    - Test toggling between online/offline modes

4. Test with sample files:
    - Try uploading various sample resumes and videos from the sample_data directory
    - Verify that scores are appropriate for each level (senior, mid, entry)
    - Check that offline transcriptions include the "OFFLINE ANALYSIS MODE" header

## Remaining Work

-   Implement local TFLite model loading for more accurate offline analysis
-   Add offline mode synchronization across multiple app instances
-   Enhance UI to show more details about server connectivity status
-   Add retry mechanism for reconnecting to server periodically
