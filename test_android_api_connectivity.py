#!/usr/bin/env python3
"""
Android-API connection test script
Tests the connection between an Android device/emulator and the ML API
"""

import argparse
import json
import os
import subprocess
import sys
import time

import requests

# Set default API URL 
DEFAULT_URL = "https://pet-ml-api-sqwo.onrender.com"

def is_android_device_connected():
    """Check if an Android device or emulator is connected"""
    try:
        result = subprocess.run(
            ["adb", "devices"], 
            capture_output=True, 
            text=True, 
            check=True
        )
        lines = result.stdout.strip().split('\n')
        # Filter out empty lines and header
        devices = [line for line in lines if line and not line.startswith('List')]
        return len(devices) > 0
    except (subprocess.SubprocessError, FileNotFoundError):
        return False

def test_api_from_android(api_url):
    """Test API connection from an Android device/emulator"""
    if not is_android_device_connected():
        print("❌ No Android device or emulator detected")
        print("   Please connect a device or start an emulator")
        return False
    
    try:
        # First, get the correct local IP to use from Android device perspective
        network_output = subprocess.run(
            ["ipconfig"], 
            capture_output=True, 
            text=True
        )
        
        print("\n🔍 Testing API connection from Android device...")
        
        # Use ADB to perform a request from the Android device/emulator
        cmd = f"adb shell curl -s {api_url}/health"
        result = subprocess.run(cmd, shell=True, capture_output=True, text=True)
        
        # Check if we got a valid JSON response
        try:
            response = json.loads(result.stdout)
            if 'status' in response and response['status'] == 'healthy':
                print(f"✅ API connection from Android successful!")
                print(f"   Response: {response}")
                return True
            else:
                print(f"❌ API returned unexpected response: {response}")
        except json.JSONDecodeError:
            print(f"❌ API returned invalid JSON: {result.stdout}")
            if result.stderr:
                print(f"   Error: {result.stderr}")
    except subprocess.SubprocessError as e:
        print(f"❌ Error executing ADB command: {str(e)}")
    
    return False

def check_api_directly(api_url):
    """Check if the API is accessible directly"""
    health_url = f"{api_url}/health"
    print(f"\n🔍 Checking API directly at: {health_url}")
    
    try:
        response = requests.get(health_url, timeout=10)
        if response.status_code == 200:
            health_data = response.json()
            print(f"✅ API is accessible! Status: {health_data.get('status')}, Version: {health_data.get('version')}")
            return True
        else:
            print(f"❌ API returned status code {response.status_code}")
    except requests.RequestException as e:
        print(f"❌ Connection error: {str(e)}")
    
    return False

def main():
    parser = argparse.ArgumentParser(description="Test PET API connectivity from Android")
    parser.add_argument("--url", default=DEFAULT_URL, help=f"API base URL (default: {DEFAULT_URL})")
    args = parser.parse_args()
    
    # Ensure URL doesn't end with a slash
    api_url = args.url.rstrip('/')
    
    print(f"\n🔍 Testing PET API connectivity with {api_url}")
    
    # First check direct API access
    direct_success = check_api_directly(api_url)
    
    if not direct_success:
        print("\n⚠️ Could not access the API directly.")
        print("  - Check if the API is deployed correctly")
        print("  - Verify network connectivity to the API server")
    
    # Then check from Android
    android_success = test_api_from_android(api_url)
    
    if not android_success:
        print("\n⚠️ Android couldn't connect to the API.")
        print("  - Check Android network permissions")
        print("  - Verify 'android.permission.INTERNET' is in AndroidManifest.xml")
        print("  - For API > 28, ensure network_security_config.xml allows cleartext traffic")
    
    if direct_success and android_success:
        print("\n✅ Connectivity test successful! The system is properly configured.")
        sys.exit(0)
    else:
        print("\n❌ Connectivity test failed. Please address the issues above.")
        sys.exit(1)

if __name__ == "__main__":
    main()
