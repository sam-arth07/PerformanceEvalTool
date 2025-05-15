#!/usr/bin/env python3
"""
API health check script to verify deployment on Render.com
Run this after deploying to ensure the API is accessible
"""

import argparse
import sys
import time

import requests

# Set default API URL 
DEFAULT_URL = "https://pet-ml-api-sqwo.onrender.com"

def check_api_health(api_url):
    """Check if the API is running and healthy"""
    health_url = f"{api_url}/health"
    print(f"Checking API health at: {health_url}")
    
    # Retry parameters
    max_retries = 5
    retry_delay = 10  # seconds
    
    for attempt in range(max_retries):
        try:
            response = requests.get(health_url, timeout=10)
            if response.status_code == 200:
                health_data = response.json()
                print(f"✅ API is healthy! Status: {health_data.get('status')}, Version: {health_data.get('version')}")
                return True
            else:
                print(f"❌ API returned status code {response.status_code}")
        except requests.RequestException as e:
            print(f"❌ Connection error (attempt {attempt+1}/{max_retries}): {str(e)}")
        
        if attempt < max_retries - 1:
            print(f"Retrying in {retry_delay} seconds...")
            time.sleep(retry_delay)
    
    print("❌ API health check failed after multiple attempts")
    return False

def main():
    parser = argparse.ArgumentParser(description="Check PET API health")
    parser.add_argument("--url", default=DEFAULT_URL, help=f"API base URL (default: {DEFAULT_URL})")
    args = parser.parse_args()
    
    # Ensure URL doesn't end with a slash
    api_url = args.url.rstrip('/')
    
    print(f"🔍 Checking PET API deployment on {api_url}")
    success = check_api_health(api_url)
    
    if not success:
        print("\n⚠️ The API might be starting up if it was just deployed.")
        print("  - Free tier on Render.com can take 1-2 minutes to wake up")
        print("  - Check the Render dashboard for deployment status")
        sys.exit(1)
    
    print("\n✅ API deployment verification successful!")
    sys.exit(0)

if __name__ == "__main__":
    main()
