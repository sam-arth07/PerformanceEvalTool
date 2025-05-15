#!/usr/bin/env bash
# Exit on error
set -o errexit

echo "Build script starting..."

# Install Python dependencies
echo "Installing Python dependencies..."
pip install --upgrade pip
pip install -r ml_models/requirements.txt
pip install gunicorn

# Create directories if they don't exist
mkdir -p ml_models/{resume_analysis,video_analysis,evaluation,tflite_models}

# Debug output
echo "Current directory structure:"
find . -type d -not -path "*/\.*" | sort

echo "Build completed successfully!"
