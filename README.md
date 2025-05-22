# Performance Evaluation Tool (PET)

A comprehensive Android application for evaluating candidate performance during interviews using real ML models.

[![Live Working Video](https://github.com/user-attachments/assets/09092d3f-1bd5-4546-b01d-cd11a43e8c0d)](https://drive.google.com/file/d/1soBkk-BFoFTxkaIB4Rg-ydKMeg68UbCO/view?usp=sharing)

Overleaf Link for the Project Report: https://www.overleaf.com/read/bwgvstdxqrhv#6d139e

## Overview

PET is designed to assess candidates based on multiple parameters:

-   Resume analysis
-   Academic performance (CGPA)
-   Video presentation skills (fluency, vocabulary, communication)

The app integrates real machine learning models to analyze these parameters and provide a comprehensive evaluation of each candidate. This updated version replaces mock data with actual ML model training and inference.

## Project Structure

-   `app/`: Android application source code
-   `ml_models/`: Machine learning models for different analyses
    -   `video_analysis/`: ML models for analyzing video presentations
    -   `resume_analysis/`: ML models for parsing and analyzing resumes
    -   `evaluation/`: Final evaluation models that combine all features
-   `docs/`: Documentation and implementation details

## Features

-   Resume upload and analysis
-   CGPA input and validation
-   Video recording or upload functionality
-   Real-time video analysis for:
    -   Speech fluency
    -   Vocabulary assessment
    -   Presentation skills
-   Comprehensive candidate evaluation based on all parameters
-   Candidate ranking and shortlisting

## Technical Stack

-   Android (Java/Kotlin) for mobile application
-   Python for machine learning models
-   TensorFlow/PyTorch for deep learning components
-   Natural Language Processing for resume and speech analysis
-   OpenCV for video processing
-   Flask for API endpoints to connect the app with ML models

## Using Real ML Models

### Installation & Setup

1. Install Python dependencies:

```
pip install -r requirements.txt
```

2. Generate sample data (optional):

```
python generate_sample_data.py
```

3. Run the setup script to train models and start the backend service:

```
python setup_and_run.py
```

### Training ML Models with Your Own Data

1. For resume analysis:

    - Place labeled resume data in the appropriate format
    - Run `python ml_models/resume_analysis/train_resume_model.py`

2. For video analysis:

    - Place labeled video interview data in the appropriate format
    - Run `python ml_models/video_analysis/train_video_models.py`

3. For overall evaluation:

    - Run `python ml_models/evaluation/train_evaluation_model.py`

4. Convert the trained models for Android:

```
python ml_models/convert_models.py
```

### Running the Application

1. Start the ML backend API:

```
python ml_models/api.py
```

2. Open the Android app in Android Studio and run it on a device/emulator
3. Make sure the backend API address in `MLModelManager.java` matches your setup

### Extending the Models

To improve model accuracy:

1. Collect more training data
2. Enhance feature extraction in the analyzer classes
3. Experiment with different model architectures in the training scripts
4. Update the conversion process to TFLite format as needed

## Network Resilience

PET is designed to handle various network conditions and connectivity issues:

-   **Automatic Fallbacks**: If the ML server is unavailable, the app gracefully falls back to on-device processing
-   **Timeout Handling**: The app properly handles connection timeouts with helpful user messaging
-   **Mock URI Support**: For development and testing purposes, the app supports mock URIs
-   **Offline Mode**: Continues to function with locally cached models when network is unavailable
-   **Error Recovery**: Clear error messages help diagnose and recover from connection issues

A test utility (`test_network_resilience.py`) is included to help verify network resilience:

```bash
# Run the test server with 10-second delay
python test_network_resilience.py --delay 10
```

This helps simulate slow server responses or timeouts for testing purposes.
