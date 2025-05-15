# PET ML API Backend

This repository contains the backend ML API for the Performance Evaluation Tool (PET) Android application.

## API Endpoints

-   `/health` - Health check endpoint
-   `/api/analyze_resume` - Resume analysis endpoint
-   `/api/analyze_video` - Video analysis endpoint
-   `/api/evaluate` - Overall candidate evaluation endpoint
-   `/api/reset_evaluation` - Reset evaluation state endpoint

## Deployment to Render.com

1. Create a free account at [Render.com](https://render.com/)
2. Create a new Web Service
3. Connect to your GitHub repository
4. Configure as follows:
    - Name: `pet-ml-api`
    - Environment: `Python 3.9`
    - Build Command: `./setup_for_render.sh`
    - Start Command: `gunicorn --chdir ml_models api:app`

## Local Testing

```bash
cd ml_models
export PYTHONPATH=$PYTHONPATH:$(pwd)/..
python api.py
```

The API should be accessible at `http://localhost:5000`
