#!/bin/bash
set -e

echo "Starting setup_for_render.sh..."

# Debug information
echo "Current working directory: $(pwd)"
echo "Directory contents: $(ls -la)"

# Create symbolic links for the ML model directories if they exist
if [ -d "resume_analysis" ]; then
    echo "Creating symlink for resume_analysis"
    ln -sf $(pwd)/resume_analysis ml_models/
else
    echo "Warning: resume_analysis directory not found"
fi

if [ -d "video_analysis" ]; then
    echo "Creating symlink for video_analysis"
    ln -sf $(pwd)/video_analysis ml_models/
else
    echo "Warning: video_analysis directory not found"
fi

if [ -d "evaluation" ]; then
    echo "Creating symlink for evaluation"
    ln -sf $(pwd)/evaluation ml_models/
else
    echo "Warning: evaluation directory not found"
fi

if [ -d "tflite_models" ]; then
    echo "Creating symlink for tflite_models"
    ln -sf $(pwd)/tflite_models ml_models/
else
    echo "Warning: tflite_models directory not found"
fi

# Install dependencies
echo "Installing Python dependencies..."
pip install -r ml_models/requirements.txt

# Make sure gunicorn is installed explicitly
echo "Ensuring gunicorn is installed..."
pip install gunicorn

# Set environment variables
echo "Setting PYTHONPATH..."
export PYTHONPATH=$PYTHONPATH:$(pwd)

echo "Setup completed successfully."
