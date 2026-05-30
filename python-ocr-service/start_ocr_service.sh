#!/bin/bash

# Flask OCR Service Startup Script
# This script starts the Flask OCR service for photo extraction

echo "==============================================="
echo "Starting Thedal OCR Photo Extraction Service"
echo "==============================================="

# Set environment variables
export FLASK_APP=flask_ocr_service.py
export FLASK_ENV=production
export PYTHONPATH="${PYTHONPATH}:$(pwd)"

# Check if Python is available
if ! command -v python3 &> /dev/null; then
    echo "Error: Python 3 is not installed or not in PATH"
    exit 1
fi

# Check if required files exist
if [ ! -f "flask_ocr_service.py" ]; then
    echo "Error: flask_ocr_service.py not found in current directory"
    exit 1
fi

if [ ! -f "enhanced_precise_extractor.py" ]; then
    echo "Error: enhanced_precise_extractor.py not found in current directory"
    exit 1
fi

# Create required directories
echo "Creating required directories..."
mkdir -p temp_uploads
mkdir -p extraction_results
mkdir -p logs

# Install dependencies
echo "Installing/updating dependencies..."
pip3 install --upgrade pip

if [ -f "flask_requirements.txt" ]; then
    pip3 install -r flask_requirements.txt
else
    echo "Warning: flask_requirements.txt not found, installing basic dependencies..."
    pip3 install flask flask-cors opencv-python numpy pillow PyMuPDF scikit-learn werkzeug
fi

# Check if all dependencies are installed
echo "Verifying installations..."
python3 -c "
import flask
import cv2
import numpy
import PIL
import fitz
import sklearn
print('✓ All dependencies verified')
"

if [ $? -ne 0 ]; then
    echo "Error: Some dependencies failed to install properly"
    exit 1
fi

# Test the enhanced extractor
echo "Testing enhanced precise extractor..."
python3 -c "
from enhanced_precise_extractor import EnhancedPreciseExtractor
extractor = EnhancedPreciseExtractor()
print('✓ Enhanced precise extractor loaded successfully')
"

if [ $? -ne 0 ]; then
    echo "Error: Enhanced precise extractor failed to load"
    exit 1
fi

echo ""
echo "==============================================="
echo "Starting Flask OCR Service on port 5000..."
echo "Health check: http://localhost:5000/health"
echo "API endpoint: http://localhost:5000/extract-photos"
echo "==============================================="
echo ""

# Start the Flask application
python3 flask_ocr_service.py
