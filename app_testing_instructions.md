# PET App Testing Instructions

## Prerequisites

-   Android emulator or physical device connected
-   ML backend server running

## Installation Steps

1. **Connect Your Device/Emulator**

    - Make sure your device is connected and showing as "device" in `adb devices` output
    - For emulator issues, try the following:
        ```
        adb kill-server
        adb start-server
        ```

2. **Install the APK**

    - Use the following command:
        ```
        adb install -r app/build/outputs/apk/debug/app-debug.apk
        ```
    - Or install directly from Android Studio by clicking the "Run" button

3. **Configure API Connection**
    - If using an emulator, the ML backend API is accessible at `http://10.0.2.2:5000`
    - If using a physical device on the same network as the backend, use your computer's IP address
    - The app should automatically detect the API endpoint

## Testing the App

1. **Resume Analysis**

    - Upload a sample resume from the resume tab
    - Sample resumes are available in the `sample_data/resumes/` folder
    - Check that the resume score is calculated correctly

2. **Video Analysis**

    - Record a video sample or upload one from the video tab
    - Sample videos are available in the `sample_data/videos/` folder
    - Verify that fluency and vocabulary scores are displayed

3. **Final Evaluation**
    - After providing resume and video data, go to the results tab
    - An overall evaluation score should be displayed
    - Check that all component scores are correctly calculated

## Troubleshooting

-   **App crashes on startup**

    -   Check logcat for error details
    -   Verify that all necessary ML model files are in the assets folder

-   **Resume analysis fails**

    -   Ensure the backend API is running
    -   Check that the resume file format is supported (PDF or DOCX)

-   **Video analysis is slow**

    -   This is normal for the first run as ML models are loading
    -   Subsequent analyses should be faster

-   **"Connection error" message**
    -   Check that the backend API is running
    -   Verify network connectivity between device and API server

## Reporting Issues

Please include the following information when reporting issues:

1. Device/emulator details
2. Steps to reproduce the issue
3. Error messages from logcat
4. Screenshots if applicable
