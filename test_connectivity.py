#!/usr/bin/env python
"""
PET Application Server Connectivity Test

This script helps verify that:
1. The backend server is running and accessible
2. Android devices can connect to the server
3. All API endpoints are responding correctly
"""

import json
import os
import socket
import sys
import time
from subprocess import PIPE, Popen

import requests

# Colors for terminal output
GREEN = '\033[92m'
YELLOW = '\033[93m'
RED = '\033[91m'
RESET = '\033[0m'

# Base URL for API server
BASE_URL = "http://localhost:5000"

def get_local_ip():
    """Get the local IP address of this machine"""
    s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    try:
        # Doesn't need to be reachable
        s.connect(('10.255.255.255', 1))
        local_ip = s.getsockname()[0]
    except Exception:
        local_ip = '127.0.0.1'
    finally:
        s.close()
    return local_ip

def check_server_running():
    """Check if the server is running"""
    print(f"Checking if API server is running on {BASE_URL}...")
    try:
        response = requests.get(f"{BASE_URL}/health", timeout=5)
        if response.status_code == 200:
            print(f"{GREEN}✓ Server is running{RESET}")
            return True
        else:
            print(f"{RED}✗ Server returned status code {response.status_code}{RESET}")
            return False
    except requests.exceptions.ConnectionError:
        print(f"{RED}✗ Cannot connect to server at {BASE_URL}{RESET}")
        return False
    except Exception as e:
        print(f"{RED}✗ Error checking server: {str(e)}{RESET}")
        return False

def check_android_connectivity():
    """Check if Android emulator can connect to the server"""
    print("\nChecking Android emulator connectivity...")
    
    # Check if adb is available
    try:
        process = Popen(["adb", "devices"], stdout=PIPE, stderr=PIPE)
        stdout, stderr = process.communicate()
        if process.returncode != 0:
            print(f"{RED}✗ ADB not found or not working{RESET}")
            return
            
        # Check for connected devices
        devices = stdout.decode().strip().split('\n')[1:]
        if not devices or all(device.strip().endswith('offline') for device in devices if device.strip()):
            print(f"{YELLOW}! No online Android devices detected{RESET}")
            print(f"{YELLOW}! Skip Android connectivity test{RESET}")
            return
            
        print(f"{GREEN}✓ Android device connected{RESET}")
        
        # Get local IP for connection from emulator
        local_ip = get_local_ip()
        emulator_url = f"http://10.0.2.2:5000" if "emulator" in stdout.decode() else f"http://{local_ip}:5000"
        print(f"Android devices should connect to: {emulator_url}")
        
        # Test connection from emulator
        print("\nTesting connection from emulator to server...")
        cmd = f"adb shell wget -qO- {emulator_url}/health"
        process = Popen(cmd, shell=True, stdout=PIPE, stderr=PIPE)
        stdout, stderr = process.communicate()
        
        if process.returncode == 0 and b"healthy" in stdout:
            print(f"{GREEN}✓ Emulator can connect to server{RESET}")
        else:
            print(f"{RED}✗ Emulator cannot connect to server{RESET}")
            print(f"{YELLOW}! Make sure the server allows connections from external IPs{RESET}")
            print(f"{YELLOW}! You may need to run the server with: python ml_models/api.py --host 0.0.0.0{RESET}")
            
    except Exception as e:
        print(f"{RED}✗ Error checking Android connectivity: {str(e)}{RESET}")

def check_api_endpoints():
    """Check all API endpoints are working"""
    print("\nChecking API endpoints...")
    endpoints = {
        "/health": "GET",
        "/api/analyze_resume": "POST",
        "/api/analyze_video": "POST",
        "/api/evaluate": "POST"
    }
    
    all_working = True
    for endpoint, method in endpoints.items():
        print(f"Testing endpoint: {endpoint} ({method})...")
        try:
            if method == "GET":
                response = requests.get(f"{BASE_URL}{endpoint}", timeout=5)
                if response.status_code == 200:
                    print(f"{GREEN}✓ Endpoint {endpoint} is working{RESET}")
                else:
                    print(f"{RED}✗ Endpoint {endpoint} returned status {response.status_code}{RESET}")
                    all_working = False
            else:
                # For POST endpoints, we just check if they're reachable (405 Method Not Allowed is OK)
                # since we don't want to actually upload files in this test
                response = requests.get(f"{BASE_URL}{endpoint}", timeout=5)
                if response.status_code in [405, 400, 200]:
                    print(f"{GREEN}✓ Endpoint {endpoint} is reachable{RESET}")
                else:
                    print(f"{RED}✗ Endpoint {endpoint} returned unexpected status {response.status_code}{RESET}")
                    all_working = False
        except requests.exceptions.ConnectionError:
            print(f"{RED}✗ Cannot connect to endpoint {endpoint}{RESET}")
            all_working = False
        except Exception as e:
            print(f"{RED}✗ Error checking endpoint {endpoint}: {str(e)}{RESET}")
            all_working = False
    
    if all_working:
        print(f"\n{GREEN}All API endpoints are working correctly!{RESET}")
    else:
        print(f"\n{YELLOW}Some API endpoints have issues. Check the server logs for more details.{RESET}")

def main():
    print("=" * 60)
    print("PET Application Server Connectivity Test")
    print("=" * 60)
    
    if check_server_running():
        check_android_connectivity()
        check_api_endpoints()
        
        print("\n" + "=" * 60)
        print(f"{GREEN}Server connectivity test completed!{RESET}")
        print("=" * 60)
    else:        
        print(f"\n{RED}Server is not running. Please start the server with:{RESET}")
        print(f"cd /c/Users/Samarth/Desktop/PET && python ml_models/api.py")
        print("=" * 60)
        sys.exit(1)

if __name__ == "__main__":
    main()
