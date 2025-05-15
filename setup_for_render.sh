#!/bin/bash

# Create symbolic links for the ML model directories
ln -sf $(pwd)/resume_analysis ml_models/
ln -sf $(pwd)/video_analysis ml_models/
ln -sf $(pwd)/evaluation ml_models/
ln -sf $(pwd)/tflite_models ml_models/

# Install dependencies
pip install -r ml_models/requirements.txt

# Set environment variables
export PYTHONPATH=$PYTHONPATH:$(pwd)
