@echo off
REM Flask OCR Service Startup Script for Windows
REM This script starts the Flask OCR service for photo extraction

echo ===============================================
echo Starting Thedal OCR Photo Extraction Service
echo ===============================================

REM Set environment variables
set FLASK_APP=flask_ocr_service.py
set FLASK_ENV=production
set PYTHONPATH=%PYTHONPATH%;%CD%

REM Check if Python is available
python --version >nul 2>&1
if %errorlevel% neq 0 (
    echo Error: Python is not installed or not in PATH
    pause
    exit /b 1
)

REM Check if required files exist
if not exist "flask_ocr_service.py" (
    echo Error: flask_ocr_service.py not found in current directory
    pause
    exit /b 1
)

if not exist "enhanced_precise_extractor.py" (
    echo Error: enhanced_precise_extractor.py not found in current directory
    pause
    exit /b 1
)

REM Create required directories
echo Creating required directories...
if not exist "temp_uploads" mkdir temp_uploads
if not exist "extraction_results" mkdir extraction_results
if not exist "logs" mkdir logs

REM Install/upgrade pip
echo Updating pip...
python -m pip install --upgrade pip

REM Install dependencies
echo Installing/updating dependencies...
if exist "flask_requirements.txt" (
    python -m pip install -r flask_requirements.txt
) else (
    echo Warning: flask_requirements.txt not found, installing basic dependencies...
    python -m pip install flask flask-cors opencv-python numpy pillow PyMuPDF scikit-learn werkzeug
)

REM Check if all dependencies are installed
echo Verifying installations...
python -c "import flask; import cv2; import numpy; import PIL; import fitz; import sklearn; print('✓ All dependencies verified')"
if %errorlevel% neq 0 (
    echo Error: Some dependencies failed to install properly
    pause
    exit /b 1
)

REM Test the enhanced extractor
echo Testing enhanced precise extractor...
python -c "from enhanced_precise_extractor import EnhancedPreciseExtractor; extractor = EnhancedPreciseExtractor(); print('✓ Enhanced precise extractor loaded successfully')"
if %errorlevel% neq 0 (
    echo Error: Enhanced precise extractor failed to load
    pause
    exit /b 1
)

echo.
echo ===============================================
echo Starting Flask OCR Service on port 5000...
echo Health check: http://localhost:5000/health
echo API endpoint: http://localhost:5000/extract-photos
echo ===============================================
echo.

REM Start the Flask application
python flask_ocr_service.py

echo.
echo ===============================================
echo OCR Service stopped
echo ===============================================
pause
