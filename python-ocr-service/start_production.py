#!/usr/bin/env python3
"""
Production startup script for Python OCR Service
Uses Gunicorn for better production performance with memory optimization
"""

import os
import multiprocessing
from flask_ocr_service import app
from config import WORKER_COUNT, WORKER_TIMEOUT, MAX_REQUESTS, get_config_summary

if __name__ == "__main__":
    # Get port from environment variable or default to 5000
    port = int(os.environ.get("PORT", 5000))
    
    # Use configuration-based worker count
    workers = min(WORKER_COUNT, multiprocessing.cpu_count())
    
    print(f"Starting OCR service with optimized configuration:")
    print(f"  Workers: {workers}")
    print(f"  Port: {port}")
    print(f"  Timeout: {WORKER_TIMEOUT}s")
    print(f"  Max requests per worker: {MAX_REQUESTS}")
    print(f"  Configuration: {get_config_summary()}")
    
    # Start with Gunicorn for production with optimized settings
    gunicorn_cmd = (
        f"gunicorn --workers {workers} "
        f"--bind 0.0.0.0:{port} "
        f"--timeout {WORKER_TIMEOUT} "
        f"--max-requests {MAX_REQUESTS} "
        f"--max-requests-jitter {MAX_REQUESTS // 10} "
        f"--worker-class sync "
        f"--preload "
        f"--access-logfile - "
        f"--error-logfile - "
        f"flask_ocr_service:app"
    )
    
    os.system(gunicorn_cmd)
