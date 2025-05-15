# PET Deployment Guide

This guide will help you deploy the Performance Evaluation Tool (PET) ML backend to Render.com and connect the Android app to the deployed service.

## Prerequisites

1. A Render.com account (free tier is sufficient for testing)
2. Git repository with the PET code
3. Android Studio for building and deploying the Android app

## Deploying the ML Backend

### Option 1: Direct from GitHub

1. Log in to Render.com
2. Click "New" and select "Web Service"
3. Connect your GitHub repository
4. Configure the service with the following settings:
    - **Name**: `pet-ml-api` (or your preferred name)
    - **Environment**: Python
    - **Build Command**: `chmod +x ./setup_for_render.sh ./build.sh && (./setup_for_render.sh || ./build.sh)`
    - **Start Command**: `cd ml_models && gunicorn --log-file=- wsgi:app`
    - **Add Environment Variable**: `PYTHONPATH=.:/app`

### Option 2: Using render.yaml (recommended)

The repository includes a `render.yaml` file that automates the deployment configuration. To use it:

1. Fork the repository to your GitHub account
2. In your Render.com dashboard, click "New" and select "Blueprint"
3. Connect your forked repository
4. Render will use the `render.yaml` configuration to set up the service

### Verifying Deployment

After deploying, verify that the API is working correctly:

```bash
python check_api_deployment.py --url https://pet-ml-api-sqwo.onrender.com
```

This script will check if the API is accessible and properly responding.

## Connecting the Android App

### Update the API URL

1. Open the Android app project in Android Studio
2. Locate `MLModelManager.java` in `app/src/main/java/com/pet/evaluator/`
3. Update the `DEMO_URL` constant with your deployed service URL (ensure it ends with a `/`):

```java
private static final String DEMO_URL = "https://pet-ml-api-sqwo.onrender.com/";
```

### Test Android-API Connectivity

To verify that the Android app can connect to your deployed API, run:

```bash
python test_android_api_connectivity.py --url https://your-service-name.onrender.com
```

This script will test the connection from both your computer and a connected Android device or emulator.

## Troubleshooting

### API Not Responding

-   **Free tier cold start**: On Render's free tier, services spin down after inactivity and may take 30-60 seconds to start up when accessed again.
-   **Deployment errors**: Check the Render logs for any errors in your deployment.

### Android Connectivity Issues

-   Ensure your AndroidManifest.xml includes:

    ```xml
    <uses-permission android:name="android.permission.INTERNET" />
    <application android:networkSecurityConfig="@xml/network_security_config" ... >
    ```

-   For Android 9+ (API 28+), ensure the network security configuration permits your API domain.

### Resource Limitations

The free tier has limitations:

-   512 MB RAM
-   Shared CPU
-   90-minute request timeout (Render will kill long-running operations)

For production use, consider upgrading to a paid plan.

## Monitoring and Maintenance

-   Set up automatic backups of your ML models
-   Implement monitoring to detect service outages
-   Plan for periodic redeployments if the free tier limitations impact service availability

## Additional Resources

-   [Render.com Documentation](https://render.com/docs)
-   [Flask Deployment Best Practices](https://flask.palletsprojects.com/en/2.0.x/deploying/)
-   [ML Model Deployment Patterns](https://www.tensorflow.org/tfx/guide/serving)
