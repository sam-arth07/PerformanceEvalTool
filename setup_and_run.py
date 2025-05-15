#!/usr/bin/env python
"""
PET Application Setup Script
----------------------------

This script sets up the Performance Evaluation Tool (PET) application by:
1. Training all ML models using real data
2. Converting models to TFLite format for Android
3. Copying models to the Android app assets folder
4. Preparing the environment for running the app
"""

import logging
import os
import subprocess
import sys
from pathlib import Path

# Set up logging
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)

def check_requirements():
    """Check if required packages are installed"""
    logger.info("Checking requirements...")
    
    try:
        import numpy
        import pandas
        logger.info("Core data science packages are available")
    except ImportError:
        logger.error("Required data science packages are missing. Install requirements first.")
        logger.error("Run: pip install -r requirements.txt")
        return False
        
    try:
        import tensorflow
        logger.info("TensorFlow is available")
    except ImportError:
        logger.warning("TensorFlow is not installed. Will attempt to train models without it.")
        logger.warning("For full functionality, install TensorFlow: pip install tensorflow")
        
    try:
        import joblib
        import sklearn
        logger.info("scikit-learn and joblib are available")
    except ImportError:
        logger.warning("scikit-learn or joblib is not installed. Will use simpler models.")
        logger.warning("For better models, install: pip install scikit-learn joblib")
        
    return True

def train_resume_model():
    """Train the resume analysis model"""
    logger.info("Training resume analysis model...")
    
    try:
        resume_script = os.path.join("ml_models", "resume_analysis", "train_resume_model.py")
        if os.path.exists(resume_script):
            subprocess.run([sys.executable, resume_script], check=True)
            logger.info("Resume model training complete")
            return True
        else:
            logger.error(f"Resume model training script not found: {resume_script}")
            return False
    except Exception as e:
        logger.error(f"Error training resume model: {e}")
        return False

def train_video_models():
    """Train the video analysis models"""
    logger.info("Training video analysis models...")
    
    try:
        video_script = os.path.join("ml_models", "video_analysis", "train_video_models.py")
        if os.path.exists(video_script):
            subprocess.run([sys.executable, video_script], check=True)
            logger.info("Video models training complete")
            return True
        else:
            logger.error(f"Video model training script not found: {video_script}")
            return False
    except Exception as e:
        logger.error(f"Error training video models: {e}")
        return False

def train_evaluation_model():
    """Train the overall evaluation model"""
    logger.info("Training overall evaluation model...")
    
    try:
        eval_script = os.path.join("ml_models", "evaluation", "train_evaluation_model.py")
        if os.path.exists(eval_script):
            subprocess.run([sys.executable, eval_script], check=True)
            logger.info("Evaluation model training complete")
            return True
        else:
            logger.error(f"Evaluation model training script not found: {eval_script}")
            return False
    except Exception as e:
        logger.error(f"Error training evaluation model: {e}")
        return False

def convert_models_to_tflite():
    """Convert all models to TFLite format"""
    logger.info("Converting models to TFLite format...")
    
    try:
        convert_script = os.path.join("ml_models", "convert_models.py")
        if os.path.exists(convert_script):
            subprocess.run([sys.executable, convert_script], check=True)
            logger.info("Model conversion complete")
            return True
        else:
            logger.error(f"Model conversion script not found: {convert_script}")
            return False
    except Exception as e:
        logger.error(f"Error converting models: {e}")
        return False

def start_backend_api():
    """Start the ML model API server"""
    logger.info("Starting ML model API server...")
    
    try:
        api_script = os.path.join("ml_models", "api.py")
        if os.path.exists(api_script):
            # Start the API in a separate process
            process = subprocess.Popen([sys.executable, api_script])
            logger.info(f"API server started with PID {process.pid}")
            logger.info("API server running at http://localhost:5000")
            return True, process
        else:
            logger.error(f"API script not found: {api_script}")
            return False, None
    except Exception as e:
        logger.error(f"Error starting API server: {e}")
        return False, None

def main():
    """Main function to run the setup process"""
    logger.info("Starting PET application setup...")
    
    # Check requirements
    if not check_requirements():
        logger.error("Requirements check failed. Please install required packages.")
        return
        
    # Train all models
    resume_trained = train_resume_model()
    video_trained = train_video_models()
    eval_trained = train_evaluation_model()
    
    # Convert models to TFLite format
    if resume_trained or video_trained or eval_trained:
        convert_models_to_tflite()
    else:
        logger.warning("No models were successfully trained. Using default models.")
        convert_models_to_tflite()
    
    # Start the backend API
    api_started, api_process = start_backend_api()
    
    if api_started:
        logger.info("=" * 60)
        logger.info("PET APPLICATION SETUP COMPLETE")
        logger.info("=" * 60)
        logger.info("Backend API running at http://localhost:5000")
        logger.info("You can now run the Android application.")
        logger.info("To stop the API server, press Ctrl+C")
        
        try:
            # Keep the script running until interrupted
            api_process.wait()
        except KeyboardInterrupt:
            logger.info("Stopping API server...")
            api_process.terminate()
            logger.info("API server stopped")
    else:
        logger.error("Could not start the backend API. Please run it manually.")

if __name__ == "__main__":
    main()
