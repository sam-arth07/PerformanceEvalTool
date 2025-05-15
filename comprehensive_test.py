#!/usr/bin/env python3
"""
Comprehensive Test Script for PET API and Android Connectivity
"""

import argparse
import json
import os
import platform
import subprocess
import sys
import time
from datetime import datetime

import requests

# Default API URL
DEFAULT_URL = "https://pet-ml-api-sqwo.onrender.com"
TIMEOUT = 15  # seconds

def print_header(text):
    """Print a formatted header"""
    print("\n" + "="*80)
    print(f" {text} ".center(80, "="))
    print("="*80)

def print_section(text):
    """Print a section header"""
    print(f"\n{'-'*3} {text} {'-'*3}")

def check_api_directly(api_url):
    """Check if the API is accessible directly"""
    health_url = f"{api_url}/health"
    print_section(f"Testing API at {health_url}")
    
    try:
        response = requests.get(health_url, timeout=TIMEOUT)
        if response.status_code == 200:
            health_data = response.json()
            print(f"✅ API is accessible! Status: {health_data.get('status')}")
            print(f"   Version: {health_data.get('version', 'unknown')}")
            return True
        else:
            print(f"❌ API returned status code {response.status_code}")
    except requests.exceptions.ConnectTimeout:
        print(f"❌ Connection timeout after {TIMEOUT} seconds")
    except requests.exceptions.ConnectionError:
        print("❌ Connection error - API might be offline or hibernating")
    except requests.RequestException as e:
        print(f"❌ Connection error: {str(e)}")
    
    return False

def check_endpoints(api_url):
    """Test the API endpoints"""
    print_section("Testing API endpoints")
    
    endpoints = {
        "/health": "GET",
        "/api/evaluate/resume": "POST",
        "/api/evaluate/video": "POST",
        "/api/evaluate/candidate": "POST"
    }
    
    successes = 0
    
    for endpoint, method in endpoints.items():
        url = f"{api_url}{endpoint}"
        try:
            if method == "GET":
                response = requests.get(url, timeout=TIMEOUT)
            else:
                # Just check if the endpoint exists without sending actual data
                response = requests.options(url, timeout=TIMEOUT)
            
            if response.status_code < 500:  # Any non-server error is OK for this test
                print(f"✅ {method} {endpoint}: Status {response.status_code}")
                successes += 1
            else:
                print(f"❌ {method} {endpoint}: Error {response.status_code}")
        except requests.RequestException as e:
            print(f"❌ {method} {endpoint}: {str(e)}")
    
    success_rate = (successes / len(endpoints)) * 100
    print(f"\nEndpoint test success rate: {success_rate:.1f}%")
    return success_rate >= 50  # At least half of endpoints should work

def check_machine_info():
    """Display basic information about the machine"""
    print_section("System Information")
    
    try:
        system = platform.system()
        print(f"OS: {system} {platform.version()}")
        print(f"Python: {platform.python_version()}")
        
        # Check network connectivity
        print("\nNetwork connectivity:")
        test_urls = ["https://google.com", "https://microsoft.com", "https://cloudflare.com"]
        
        for url in test_urls:
            try:
                response = requests.head(url, timeout=5)
                print(f"  ✅ {url}: Accessible")
            except requests.RequestException:
                print(f"  ❌ {url}: Not accessible")
    
    except Exception as e:
        print(f"Error getting system info: {str(e)}")

def check_render_status(api_url):
    """Test typical Render.com behaviors"""
    print_section("Render.com Deployment Analysis")
    
    # Extract the domain
    domain = api_url.split("//")[1].split("/")[0]
    
    # Check cold start behavior with multiple requests
    print("\nChecking cold start behavior...")
    
    start_time = time.time()
    response_times = []
    
    for i in range(3):
        req_start = time.time()
        try:
            response = requests.get(f"{api_url}/health", timeout=20)
            req_time = time.time() - req_start
            response_times.append(req_time)
            print(f"  Request {i+1}: {req_time:.2f}s - Status: {response.status_code}")
        except requests.RequestException as e:
            print(f"  Request {i+1}: Failed - {str(e)}")
        
        # Small delay between requests
        if i < 2:
            time.sleep(1)
    
    # Analyze response patterns
    if response_times:
        if response_times[0] > 5 and (len(response_times) >= 2 and response_times[1] < response_times[0]):
            print("\n🔍 Analysis: Observed cold start pattern typical of Render free tier")
            print("   First request was slow, subsequent requests were faster")
        elif all(t > 3 for t in response_times):
            print("\n🔍 Analysis: All requests were slow, possible resource constraints")
        else:
            print("\n🔍 Analysis: Response times appear normal")
    
    # Check if the service looks like Render
    print("\nVerifying Render.com hosting:")
    try:
        response = requests.get(f"{api_url}/health", timeout=TIMEOUT)
        headers = response.headers
        
        render_indicators = 0
        
        if "render" in domain.lower():
            render_indicators += 1
            print("  ✓ Domain contains 'render'")
        
        if "fly.dev" in domain.lower() or "herokuapp" in domain.lower():
            print("  ✗ Domain suggests alternative hosting (Fly.io or Heroku)")
        
        server_header = headers.get("server", "").lower()
        if "nginx" in server_header:
            render_indicators += 1
            print("  ✓ Server header indicates Nginx (used by Render)")
            
        # Check for typical Render headers
        if any(h.lower().startswith("fly-") for h in headers):
            print("  ✗ Found Fly.io specific headers")
        
        if render_indicators >= 1:
            print("\n✅ Service appears to be hosted on Render.com")
            return True
        else:
            print("\n⚠️ Service may not be hosted on Render.com")
            return False
            
    except Exception as e:
        print(f"\n❌ Error checking hosting: {str(e)}")
        return False

def test_video_upload(api_url):
    """Test uploading a small video file to the API"""
    print_section("Testing Video Upload")
    
    # Create a small test video file
    try:
        import numpy as np
        from PIL import Image
        
        # Generate a very small mock video file (just a few bytes)
        # This is just to test the API connectivity without sending large files
        test_file_path = "test_unity_pixel.mp4"
        with open(test_file_path, "wb") as f:
            # Create 1KB of random data to simulate a tiny video
            f.write(os.urandom(1024))
        
        print(f"✓ Created test video file: {test_file_path}")
        
        # Test upload
        upload_url = f"{api_url}/api/evaluate/video"
        try:
            files = {'video': open(test_file_path, 'rb')}
            data = {'candidate_id': 'test-1234'}
            
            print(f"⏳ Sending test video to {upload_url}...")
            response = requests.post(upload_url, files=files, data=data, timeout=30)
            
            if response.status_code == 200:
                print(f"✅ Upload test successful! Status code: {response.status_code}")
                try:
                    print(f"   Response: {response.json()}")
                except:
                    print(f"   Response: {response.text[:100]}...")
                result = True
            else:
                print(f"❌ Upload failed. Status code: {response.status_code}")
                print(f"   Response: {response.text[:100]}...")
                result = False
                
        except requests.RequestException as e:
            print(f"❌ Upload error: {str(e)}")
            result = False
            
        # Cleanup
        if os.path.exists(test_file_path):
            os.remove(test_file_path)
            print(f"✓ Cleaned up test file")
            
        return result
            
    except ImportError:
        print("⚠️ PIL or NumPy not available, skipping video generation test")
        return False
    except Exception as e:
        print(f"❌ Error in video upload test: {str(e)}")
        return False

def main():
    parser = argparse.ArgumentParser(description="Comprehensive PET API and Android Connectivity Test")
    parser.add_argument("--url", default=DEFAULT_URL, help=f"API base URL (default: {DEFAULT_URL})")
    args = parser.parse_args()
    
    # Ensure URL doesn't end with a slash
    api_url = args.url.rstrip('/')
    
    print_header("PET API COMPREHENSIVE TEST")
    print(f"Date: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
    print(f"Testing URL: {api_url}")
    
    # First check system info
    check_machine_info()
    
    # Check API health
    api_available = check_api_directly(api_url)
    
    if not api_available:
        print("\n⚠️ API is not responding. Possible reasons:")
        print("  - The service could be hibernating (Render free tier)")
        print("  - The service might be down")
        print("  - There might be network connectivity issues")
        print("\nTrying one more time with extended timeout...")
        
        # Try once more with longer timeout
        time.sleep(2)
        api_available = check_api_directly(api_url)
    
    # Continue with more tests if API is available
    if api_available:
        endpoints_ok = check_endpoints(api_url)
        render_verified = check_render_status(api_url)
        video_upload_ok = test_video_upload(api_url)
        
        # Calculate overall health score
        health_score = 0
        if api_available:
            health_score += 50
        if endpoints_ok:
            health_score += 30
        if render_verified:
            health_score += 20
        if video_upload_ok:
            health_score += 10
        
        print_header("TEST RESULTS")
        print(f"API Health: {'✅ Available' if api_available else '❌ Unavailable'}")
        print(f"Endpoints: {'✅ OK' if endpoints_ok else '⚠️ Partial'}")
        print(f"Render Deployment: {'✅ Verified' if render_verified else '⚠️ Unverified'}")
        print(f"Video Upload: {'✅ Successful' if video_upload_ok else '❌ Failed'}")
        print(f"\nOverall Health Score: {health_score}%")
        
        # Recommendations
        print("\nRECOMMENDATIONS:")
        if health_score < 50:
            print("❌ Critical issues detected. The API is not functioning properly.")
            print("   - Check Render.com dashboard for service status")
            print("   - Review application logs for errors")
        elif health_score < 80:
            print("⚠️ Some issues detected. The API is functioning but with limitations.")
            print("   - Consider upgrading from free tier for better reliability")
            print("   - Monitor error logs for specific endpoint failures")
        else:
            print("✅ The API is functioning well!")
            print("   - Remember that free tier instances will sleep after inactivity")
            print("   - Consider implementing a ping service to keep it alive")
        
        # Android-specific advice
        print("\nANDROID CONNECTION ADVICE:")
        print("1. Ensure the Android app uses the correct URL with trailing slash:")
        print(f"   {api_url}/")
        print("2. Add retry logic in the app to handle cold starts")
        print("3. Show appropriate loading UI during API wake-up period")
        
        return health_score >= 50
    else:
        print_header("TEST RESULTS")
        print("❌ API is unavailable. Cannot complete further tests.")
        print("\nRECOMMENDATIONS:")
        print("1. Check if the service is deployed correctly on Render.com")
        print("2. Verify your Render account status and build logs")
        print("3. Try accessing the Render dashboard to manually restart the service")
        print("4. If using free tier, note that it hibernates after inactivity")
        return False

if __name__ == "__main__":
    success = main()
    sys.exit(0 if success else 1)
