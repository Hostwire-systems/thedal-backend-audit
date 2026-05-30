#!/usr/bin/env python3
"""
Configuration settings for Python OCR Service
Optimized for cloud deployment with memory management
"""

import os

# Processing Configuration
CHUNK_SIZE = int(os.environ.get('OCR_CHUNK_SIZE', 3))  # Process 3 pages at a time (reduced from 5)
SAVE_BATCH_SIZE = int(os.environ.get('OCR_SAVE_BATCH_SIZE', 15))  # Save 15 photos at a time (reduced from 20)
MEMORY_CLEANUP_FREQUENCY = int(os.environ.get('OCR_MEMORY_CLEANUP_FREQ', 1))  # Cleanup after every page

# File Configuration
MAX_FILE_SIZE = int(os.environ.get('OCR_MAX_FILE_SIZE', 50 * 1024 * 1024))  # 50MB
TEMP_FILE_MAX_AGE_HOURS = int(os.environ.get('OCR_TEMP_FILE_MAX_AGE_HOURS', 2))  # 2 hours
RESULTS_MAX_AGE_HOURS = int(os.environ.get('OCR_RESULTS_MAX_AGE_HOURS', 24))  # 24 hours

# Gunicorn Configuration
WORKER_COUNT = int(os.environ.get('OCR_WORKER_COUNT', 2))  # Reduced for memory efficiency
WORKER_TIMEOUT = int(os.environ.get('OCR_WORKER_TIMEOUT', 1200))  # 20 minutes (increased from 10)
MAX_REQUESTS = int(os.environ.get('OCR_MAX_REQUESTS', 500))  # Restart workers after 500 requests

# Image Processing Configuration
JPEG_QUALITY = int(os.environ.get('OCR_JPEG_QUALITY', 95))
ENABLE_DEBUG_IMAGES = os.environ.get('OCR_ENABLE_DEBUG_IMAGES', 'true').lower() == 'true'

# Logging Configuration
LOG_LEVEL = os.environ.get('OCR_LOG_LEVEL', 'INFO')
ENABLE_PERFORMANCE_LOGS = os.environ.get('OCR_ENABLE_PERFORMANCE_LOGS', 'true').lower() == 'true'

def get_config_summary():
    """Return a summary of current configuration."""
    return {
        'chunk_size': CHUNK_SIZE,
        'save_batch_size': SAVE_BATCH_SIZE,
        'worker_count': WORKER_COUNT,
        'worker_timeout': WORKER_TIMEOUT,
        'max_file_size_mb': MAX_FILE_SIZE // (1024 * 1024),
        'memory_cleanup_enabled': True,
        'chunked_processing_enabled': True
    }
