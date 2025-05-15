#!/usr/bin/env python3
"""
Test utility to verify the PET app's resilience to network issues.
This script can check if the app is using offline fallback correctly.
"""

import argparse
import logging
import os
import socket
import sys
import time
from http.server import BaseHTTPRequestHandler, HTTPServer

# Set up logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
    handlers=[logging.StreamHandler()]
)
logger = logging.getLogger("NetworkResilienceTest")

class DelayedResponseHandler(BaseHTTPRequestHandler):
    """HTTP request handler that simulates slow server responses."""
    
    def __init__(self, *args, delay_seconds=5, **kwargs):
        self.delay_seconds = delay_seconds
        super().__init__(*args, **kwargs)
    
    def do_GET(self):
        """Handle GET requests with a delay."""
        self._handle_request()
    
    def do_POST(self):
        """Handle POST requests with a delay."""
        self._handle_request()
    
    def _handle_request(self):
        """Common handler for all requests."""
        logger.info(f"Received {self.command} request to {self.path}")
        
        # Read request content
        content_length = int(self.headers.get('Content-Length', 0))
        if content_length > 0:
            post_data = self.rfile.read(content_length)
            logger.info(f"Request payload size: {len(post_data)} bytes")
        
        # Simulate processing delay
        logger.info(f"Simulating delay of {self.delay_seconds} seconds...")
        time.sleep(self.delay_seconds)
        
        # Send response
        self.send_response(200)
        self.send_header('Content-type', 'application/json')
        self.end_headers()
        
        # Send a simple mock response
        mock_response = '{"success": true, "message": "This is a delayed response from the test server"}'
        self.wfile.write(mock_response.encode('utf-8'))
        logger.info("Response sent after delay")


def check_port_availability(port):
    """Check if the port is available for use."""
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
        try:
            s.bind(('localhost', port))
            return True
        except OSError:
            return False


def run_slow_server(port=5000, delay=10):
    """Run a server that responds slowly to simulate timeouts."""
    if not check_port_availability(port):
        logger.error(f"Port {port} is already in use. Please choose a different port.")
        return False
    
    handler = lambda *args, **kwargs: DelayedResponseHandler(*args, delay_seconds=delay, **kwargs)
    
    server_address = ('', port)
    httpd = HTTPServer(server_address, handler)
    logger.info(f"Starting delayed response server on port {port} with {delay}s delay")
    logger.info("Press Ctrl+C to stop the server")
    
    try:
        httpd.serve_forever()
    except KeyboardInterrupt:
        logger.info("Shutting down server...")
        httpd.server_close()
    
    logger.info("Server stopped")
    return True


def main():
    """Main entry point."""
    parser = argparse.ArgumentParser(description='Test PET app resilience to network issues')
    parser.add_argument('--port', type=int, default=5000, help='Port to run the test server on (default: 5000)')
    parser.add_argument('--delay', type=float, default=10.0, help='Delay in seconds for responses (default: 10)')
    args = parser.parse_args()
    
    logger.info("PET Network Resilience Test")
    logger.info("==========================")
    logger.info("This utility helps test the PET app's ability to handle network issues.")
    logger.info(f"It will run a test server on port {args.port} that delays responses by {args.delay} seconds.")
    logger.info("This should trigger timeout handling in the app.")
    logger.info("")
    logger.info("Instructions:")
    logger.info("1. Run this script")
    logger.info("2. Launch the PET app in the Android emulator")
    logger.info("3. Try to upload a resume or video")
    logger.info("4. The app should detect the timeout and fall back to offline mode")
    logger.info("5. Check that offline results are displayed correctly")
    logger.info("")
    
    run_slow_server(port=args.port, delay=args.delay)


if __name__ == "__main__":
    main()
