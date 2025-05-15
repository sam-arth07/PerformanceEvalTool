#!/usr/bin/env python3
import logging
import os
import sys

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

# Add the current directory to the path so Python can find the modules
current_dir = os.path.dirname(os.path.abspath(__file__))
parent_dir = os.path.dirname(current_dir)

logger.info(f"Adding to sys.path: {current_dir}")
logger.info(f"Adding to sys.path: {parent_dir}")

sys.path.insert(0, current_dir)
sys.path.insert(0, parent_dir)

try:
    # Import the Flask app
    logger.info("Importing Flask app from api.py")
    from api import app
    logger.info("Successfully imported Flask app")
except Exception as e:
    logger.error(f"Error importing Flask app: {str(e)}")
    raise

# For Gunicorn
if __name__ == "__main__":
    port = int(os.environ.get("PORT", 5000))
    logger.info(f"Starting Flask app on port {port}")
    app.run(host='0.0.0.0', port=port)
