# Android - Python ML Model Integration Guide

This document explains how the Performance Evaluation Tool (PET) integrates Python ML models with the Android application.

## Architecture Overview

The application uses a hybrid architecture:

1. **Android Application**: The main user interface built with Java/Kotlin
2. **Python ML Models**: Machine learning models for video analysis, resume parsing, and evaluation
3. **Flask API**: RESTful API that connects the Android app to the Python models
4. **TensorFlow Lite**: For on-device inference with simplified models

## Integration Methods

The app uses two primary integration methods:

### 1. Client-Server Model (Primary Method)

This method sends data to a remote server running the Python ML models and receives the analysis results.

**Flow:**

1. User uploads resume/video or provides CGPA in the Android app
2. App sends this data to a Flask API server via HTTP requests
3. Server processes data using Python ML models
4. Server returns results to the Android app
5. App displays the results to the user

**Components:**

-   `MLModelManager.java`: Android client for the Flask API
-   `api.py`: Python Flask server with API endpoints
-   ML model implementations in `/ml_models` directory

**Pros:**

-   Full access to advanced ML libraries like TensorFlow, PyTorch, OpenCV, etc.
-   No size limitations for complex models
-   No compatibility issues between Python libraries and Android

**Cons:**

-   Requires internet connection
-   Server deployment and maintenance needed
-   Possible latency issues

### 2. On-Device Inference with TensorFlow Lite (Partial Features)

This method uses optimized, simplified versions of the models running directly on the Android device.

**Flow:**

1. ML models are trained in Python and exported to TensorFlow Lite format
2. TFLite models are included in the Android app assets
3. App uses these models for simpler analyses without internet connection

**Components:**

-   `TFLiteEvaluator.java`: Wrapper for TFLite models
-   TFLite model files (would be in `/app/src/main/assets/`)

**Pros:**

-   Works offline
-   Lower latency
-   Better privacy (data stays on device)

**Cons:**

-   Limited to simpler models due to device constraints
-   Less accurate than full Python models
-   Not all features supported (e.g., deep video analysis)

## API Endpoints

The Flask API server provides the following endpoints:

-   `/api/analyze_resume`: Analyzes a resume file and extracts relevant features
-   `/api/analyze_video`: Analyzes a video file for English speaking skills
-   `/api/evaluate`: Performs comprehensive evaluation using resume, video, and CGPA

## Running the System

### Development Setup

1. Start the Flask server:

    ```
    cd ml_models
    python -m api
    ```

2. The Android app is configured to connect to `http://10.0.2.2:5000/` which reaches the host machine's port 5000 from the Android emulator.

### Production Setup

For a production environment, you would:

1. Deploy the Flask API to a cloud server
2. Update the `BASE_URL` in `MLModelManager.java` to point to your production server
3. Implement proper authentication and security measures

## Example ML Model Integration Code

```java
// In the Android app:
MLModelManager modelManager = new MLModelManager(context);

// To analyze a resume
modelManager.analyzeResume(resumeUri, new MLModelManager.EvaluationCallback() {
    @Override
    public void onSuccess(EvaluationResult result) {
        float resumeScore = result.getScore();
        // Update UI with the score
    }

    @Override
    public void onError(String errorMessage) {
        // Handle error
    }
});
```

## Troubleshooting

Common issues and solutions:

1. **Connection refused errors**: Ensure Flask server is running and accessible from the Android device/emulator
2. **File upload issues**: Check file permissions and content URI handling in `MLModelManager.java`
3. **Long processing times**: Consider implementing progress updates in the API
4. **Out of memory errors**: Optimize ML models or process data in chunks
