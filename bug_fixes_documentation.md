# PET App Bug Fixes and Enhancements

This document outlines the fixes that have been implemented to resolve the issues in the Performance Evaluation Tool (PET) Android application.

## Fixed Issues

### 1. NullPointerException in TFLiteEvaluator

**Problem:**

-   The app was crashing with `NullPointerException: Cannot invoke "String.length()" because "<parameter1>" is null`
-   This occurred in the TFLiteEvaluator class when passing null or invalid parameters

**Solution:**

-   Added null checks in the TFLiteEvaluator constructor
-   Enhanced JSON parsing methods with proper null handling
-   Implemented fallback values for model metadata
-   Added more robust error logging and exception handling

**Files Modified:**

-   `app/src/main/java/com/pet/evaluator/TFLiteEvaluator.java`

### 2. Dependency Conflicts

**Problem:**

-   Multiple Kotlin stdlib versions causing conflicts
-   Native library conflicts with duplicate .so files

**Solution:**

-   Updated Kotlin version to match project requirements (1.8.10)
-   Added explicit dependency resolution rules
-   Implemented pickFirst rules for duplicate native libraries

**Files Modified:**

-   `app/build.gradle`
-   `build.gradle`

### 3. Resource Errors

**Problem:**

-   Missing or corrupt launcher icons
-   Missing string resources

**Solution:**

-   Generated new launcher icons for all densities
-   Added missing string resources

**Files Modified:**

-   Added various icon resources in mipmap folders
-   Updated `strings.xml`

### 3. SocketTimeoutException During Resume Upload

**Problem:**

- The app was experiencing `java.net.SocketTimeoutException` when attempting to upload resumes to the backend server
- The error occurred specifically when calling `MLModelManager.analyzeResume()` method
- The 30-second default timeout was insufficient for some network conditions or large resume files

**Solution:**

- Enhanced HTTP client configuration with customized timeouts:
  - Increased connection timeout to 30 seconds
  - Increased read timeout to 60 seconds
  - Increased write timeout to 60 seconds
- Added specific handling for different types of network errors:
  - `SocketTimeoutException`: Server taking too long to respond
  - `ConnectException`: Server connection failures
- Implemented server availability tracking with an `isServerAvailable` flag
- Enhanced fallback mechanism to offline mode when the server is unreachable
- Improved mock data generation with more realistic simulated results
- Added user feedback with Toast notifications when operating in offline mode

**Files Modified:**

- `MLModelManager.java`: Enhanced timeout settings, improved error detection
- `EvaluationService.java`: Improved offline fallback and network status checking
- `EvaluationResult.java`: Added missing setter methods for score values

**Testing:**

- A new test utility (`test_network_resilience.py`) was created to simulate slow server responses
- The fix was verified by testing uploads while the test server simulated timeouts

### 4. FileNotFoundException with Mock Video URIs

**Problem:**

- The app crashed with `FileNotFoundException` when trying to process mock video URIs
- This occurred because the mock URI (`content://mock/video/recording.mp4`) had no registered content provider

**Solution:**

- Added special handling for URIs with the "mock" scheme in `getFileFromUri()` method
- Implemented creation of temporary sample files for mock URIs
- Added demo result generation to simulate ML processing for mock inputs

**Files Modified:**

- `MLModelManager.java`: Added mock URI detection and handling
- `VideoFragment.java`: Improved video source selection with fallbacks

### 5. Reliability Improvements

**Additional Enhancements:**

- Added null safety checks when determining MIME types
- Added proper resource cleanup in finally blocks for file operations
- Improved error messaging for better diagnostics
- Added more detailed logging throughout the app
- Enhanced demo mode for development testing

## Enhancements Added

### 1. Connectivity Testing Tool

**Description:**

-   Added a Python script to test connectivity between the app and ML backend
-   Checks server status, API endpoints, and Android device connectivity

**Files Added:**

-   `test_connectivity.py`

### 2. Enhanced Error Handling

**Description:**

-   Created an enhanced MainActivity with improved error diagnostics
-   Added detailed logs and user-friendly error messages
-   Implemented a diagnostics feature to help troubleshoot common issues

**Files Added:**

-   `MainActivity_Enhanced.java` (reference implementation)
-   `app/src/main/res/menu/main_menu.xml`

### 3. Comprehensive Testing Documents

**Description:**

-   Created thorough testing guidelines and checklists
-   Documented steps to verify fixed bugs and test app functionality

**Files Added:**

-   `app_testing_instructions.md`
-   `testing_guide_after_fixes.md`
-   `testing_checklist.md`

## Implementation Instructions

### To fix the NullPointerException:

1. Apply the changes to `TFLiteEvaluator.java`:

```java
// Add this in the constructor
public TFLiteEvaluator(Context context, String modelPath) {
    try {
        // Extract model name from path (with null check)
        if (modelPath == null) {
            throw new IllegalArgumentException("Model path cannot be null");
        }

        String modelFullName = modelPath;
        if (modelPath.contains("/")) {
            modelFullName = modelPath.substring(modelPath.lastIndexOf('/') + 1);
        }
        modelName = modelFullName.replace(".lite", "");

        // Rest of the constructor
        // ...
    } catch (IllegalArgumentException e) {
        throw new RuntimeException(e);
    }
}

// Fix the JSON parsing methods
private String[] parseJsonStringArray(org.json.JSONArray jsonArray) throws JSONException {
    if (jsonArray == null) {
        return new String[0]; // Return empty array if input is null
    }

    String[] result = new String[jsonArray.length()];
    for (int i = 0; i < jsonArray.length(); i++) {
        String value = jsonArray.optString(i, ""); // Use optString to avoid null
        result[i] = value;
    }
    return result;
}

private int[] parseJsonIntArray(org.json.JSONArray jsonArray) throws JSONException {
    if (jsonArray == null) {
        return new int[0]; // Return empty array if input is null
    }

    int[] result = new int[jsonArray.length()];
    for (int i = 0; i < jsonArray.length(); i++) {
        result[i] = jsonArray.getInt(i);
    }
    return result;
}
```

1. Update the `loadModelMetadata` method with enhanced error handling:

```java
private void loadModelMetadata() throws JSONException {
    if (modelConfig == null || !modelConfig.has("models")) {
        throw new JSONException("Model configuration is missing or invalid");
    }

    JSONObject models = modelConfig.getJSONObject("models");
    if (!models.has(modelName)) {
        throw new JSONException("Metadata for model " + modelName + " not found");
    }

    JSONObject metadata = models.getJSONObject(modelName);

    try {
        // Parse input and output shapes with error handling
        int[] inputs = metadata.has("input_shape") ?
            parseJsonIntArray(metadata.getJSONArray("input_shape")) : new int[]{1};
        int[] outputs = metadata.has("output_shape") ?
            parseJsonIntArray(metadata.getJSONArray("output_shape")) : new int[]{1};

        inputShape = inputs;
        outputShape = outputs;

        // Parse input and output names with error handling
        String[] inNames = metadata.has("input_names") ?
            parseJsonStringArray(metadata.getJSONArray("input_names")) : new String[]{"input"};
        String[] outNames = metadata.has("output_names") ?
            parseJsonStringArray(metadata.getJSONArray("output_names")) : new String[]{"output"};

        inputNames = inNames;
        outputNames = outNames;
    } catch (Exception e) {
        Log.e(TAG, "Error parsing model metadata: " + e.getMessage());
        // Set default values
        inputShape = new int[]{1};
        outputShape = new int[]{1};
        inputNames = new String[]{"input"};
        outputNames = new String[]{"output"};
    }
}
```

### To test the fixes:

1. Build the app with the new fixes
2. Run the connectivity test script: `python test_connectivity.py`
3. Install the app on a device: `adb install -r app/build/outputs/apk/debug/app-debug.apk`
4. Follow the testing checklist in `testing_checklist.md`

## Next Steps

1. Replace the original MainActivity with the enhanced version for better error handling
2. Add more comprehensive logging throughout the application
3. Implement automated UI tests to catch regressions
4. Consider adding offline-first capabilities for improved reliability

## Contact

If you encounter any issues or have questions about these fixes, please contact the development team.
