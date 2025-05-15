#!/usr/bin/env python3
"""
Camera and Video Processing Test Script for PET
This will test video upload and analysis functions
"""

import os
import sys
import argparse
import requests
import time
import json
from concurrent.futures import ThreadPoolExecutor
import logging

# Set up logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(levelname)s - %(message)s',
    handlers=[
        logging.StreamHandler(sys.stdout)
    ]
)

# Constants
DEFAULT_URL = "https://pet-ml-api-sqwo.onrender.com"
TIMEOUT = 30  # seconds for video processing
SAMPLE_VIDEO_DIR = "sample_data/videos"
TEST_ENDPOINTS = [
    "/video_analysis/analyze",
    "/health",
    "/video_analysis/features"
]

def setup_argument_parser():
    """Set up command line argument parsing"""
    parser = argparse.ArgumentParser(description="Test video processing API functionality")
    parser.add_argument("--url", default=DEFAULT_URL, help=f"API URL (default: {DEFAULT_URL})")
    parser.add_argument("--samples", type=int, default=2, help="Number of video samples to test (default: 2)")
    parser.add_argument("--concurrent", action="store_true", help="Run tests concurrently")
    parser.add_argument("--verbose", "-v", action="store_true", help="Verbose output")
    return parser.parse_args()

def check_api_health(api_url):
    """Check if the API is responding to health checks"""
    logging.info(f"Checking API health at {api_url}/health")
    try:
        start_time = time.time()
        response = requests.get(f"{api_url}/health", timeout=TIMEOUT)
        elapsed = time.time() - start_time
        
        if response.status_code == 200:
            logging.info(f"✅ API is healthy (response time: {elapsed:.2f}s)")
            return True
        else:
            logging.error(f"❌ API health check failed: {response.status_code} - {response.text}")
            return False
    except requests.exceptions.RequestException as e:
        logging.error(f"❌ API connection error: {e}")
        return False

def test_video_upload(api_url, video_path):
    """Test uploading a video file for analysis"""
    endpoint = f"{api_url}/video_analysis/analyze"
    logging.info(f"Testing video upload: {video_path}")
    
    if not os.path.exists(video_path):
        logging.error(f"❌ Video file not found: {video_path}")
        return False
    
    try:
        with open(video_path, 'rb') as video_file:
            files = {'video': (os.path.basename(video_path), video_file, 'video/mp4')}
            
            start_time = time.time()
            response = requests.post(
                endpoint,
                files=files,
                timeout=TIMEOUT
            )
            elapsed = time.time() - start_time
            
            if response.status_code == 200:
                result = response.json()
                logging.info(f"✅ Video analysis successful ({elapsed:.2f}s):")
                logging.info(f"   - Fluency score: {result.get('fluency', 'N/A')}")
                logging.info(f"   - Vocabulary score: {result.get('vocabulary', 'N/A')}")
                return True
            else:
                logging.error(f"❌ Video analysis failed: {response.status_code} - {response.text}")
                return False
                
    except requests.exceptions.RequestException as e:
        logging.error(f"❌ Request error: {e}")
        return False
    except Exception as e:
        logging.error(f"❌ Error: {e}")
        return False

def test_feature_endpoint(api_url):
    """Test the video features endpoint"""
    endpoint = f"{api_url}/video_analysis/features"
    logging.info(f"Testing features endpoint: {endpoint}")
    
    try:
        response = requests.get(endpoint, timeout=TIMEOUT)
        if response.status_code == 200:
            features = response.json()
            logging.info(f"✅ Features retrieved successfully: {len(features)} features available")
            return True
        else:
            logging.error(f"❌ Features request failed: {response.status_code} - {response.text}")
            return False
    except requests.exceptions.RequestException as e:
        logging.error(f"❌ Request error: {e}")
        return False

def find_sample_videos(limit=2):
    """Find available sample videos"""
    if not os.path.exists(SAMPLE_VIDEO_DIR):
        logging.warning(f"⚠️ Sample video directory not found: {SAMPLE_VIDEO_DIR}")
        return []
        
    video_files = [
        os.path.join(SAMPLE_VIDEO_DIR, f) 
        for f in os.listdir(SAMPLE_VIDEO_DIR) 
        if f.endswith(('.mp4', '.avi', '.mov'))
    ]
    
    if not video_files:
        logging.warning("⚠️ No sample video files found")
        return []
        
    logging.info(f"Found {len(video_files)} sample videos, using {min(limit, len(video_files))}")
    return video_files[:limit]

def run_tests_concurrently(api_url, video_files):
    """Run tests concurrently"""
    with ThreadPoolExecutor(max_workers=4) as executor:
        feature_future = executor.submit(test_feature_endpoint, api_url)
        video_futures = [executor.submit(test_video_upload, api_url, video) for video in video_files]
        
        # Wait for all to complete
        feature_result = feature_future.result()
        video_results = [future.result() for future in video_futures]
        
        return feature_result and all(video_results)

def run_tests_sequentially(api_url, video_files):
    """Run tests sequentially"""
    # First test features
    feature_result = test_feature_endpoint(api_url)
    
    # Then test video uploads
    video_results = []
    for video in video_files:
        video_results.append(test_video_upload(api_url, video))
        
    return feature_result and all(video_results)

def main():
    """Main function"""
    args = setup_argument_parser()
    api_url = args.url.rstrip('/')
    
    # Set log level
    if args.verbose:
        logging.getLogger().setLevel(logging.DEBUG)
    
    print("\n===================================================")
    print("    PET Video Processing API Test Tool")
    print("===================================================\n")
    
    # Check API health first
    if not check_api_health(api_url):
        sys.exit(1)
    
    # Find sample videos
    video_files = find_sample_videos(args.samples)
    if not video_files:
        logging.error("❌ No video files available for testing")
        sys.exit(1)
    
    # Run the tests
    success = False
    if args.concurrent:
        logging.info("Running tests concurrently")
        success = run_tests_concurrently(api_url, video_files)
    else:
        logging.info("Running tests sequentially")
        success = run_tests_sequentially(api_url, video_files)
    
    # Print summary
    print("\n===================================================")
    if success:
        print("✅ All tests passed successfully!")
    else:
        print("❌ One or more tests failed")
    print("===================================================\n")
    
    sys.exit(0 if success else 1)

if __name__ == "__main__":
    main()
