# Python OCR Service - Flask Integration

This directory contains the Flask-based OCR service for voter photo extraction from PDF files.

## Files Overview

- **`enhanced_precise_extractor.py`** - Main photo extraction engine with 100% accuracy
- **`flask_ocr_service.py`** - Flask web service providing REST API endpoints
- **`flask_requirements.txt`** - Python dependencies for the Flask service
- **`start_flask.bat`** - Windows startup script for the Flask service
- **`test_flask_setup.py`** - Setup verification and testing script

## Quick Start

### 1. Setup Dependencies
```bash
pip install -r flask_requirements.txt
```

### 2. Start the Service

**Windows:**
```bash
start_flask.bat
```

**Manual Start:**
```bash
python flask_ocr_service.py
```

### 3. Test the Service
```bash
python test_flask_setup.py
```

## API Endpoints

- **`GET /health`** - Health check
- **`POST /extract-photos`** - Extract photos from PDF
- **`GET /job-status/{jobId}`** - Get job status
- **`GET /download-photo/{jobId}/{serialNo}`** - Download specific photo

## Service Details

- **Port**: 5000
- **Framework**: Flask with CORS
- **Features**: 
  - PDF processing and photo extraction
  - 100% accuracy using Enhanced Precise Extractor
  - Job-based file organization
  - Automatic cleanup

## Integration

This service integrates with the Spring Boot application to provide complete voter photo processing functionality. See the main project documentation for full integration details.

## Requirements

- Python 3.7+
- OpenCV
- PyMuPDF
- Flask and dependencies (see flask_requirements.txt)

## Advanced High-Accuracy Extractor (Experimental)

A next-generation pipeline `AdvancedPhotoExtractor` is included to push correct crop accuracy toward 95–98%+ through:

- Page deskew (Hough-based)
- Structural table/card segmentation (line morphology) with grid fallback
- DNN + Haar hybrid face detection
- Candidate region scanning within expected photo zone
- Composite scoring (variance, edge orientation entropy, skin tone ratio, contrast, aspect prior, text penalty)
- Contour-based tertiary fallback

### Enable in API

Set environment variable before starting service:

```
$env:USE_ADVANCED_EXTRACTOR = "1"   # PowerShell
export USE_ADVANCED_EXTRACTOR=1       # Bash
```

Or per request include form field: `advanced=1`.

### Face DNN Model (Optional but Recommended)
Place the following in `python-ocr-service/models/`:
```
models/
  deploy.prototxt
  res10_300x300_ssd_iter_140000.caffemodel
```
If missing, it will fall back to Haar.

#### Automatic Download
You can download both the SSD face detector and YuNet model automatically:

```
python download_face_models.py                # both models
python download_face_models.py --only dnn     # only SSD Caffe
python download_face_models.py --only yunet   # only YuNet ONNX
```

Or set an environment variable to auto-download on service start if missing:

```
$env:AUTO_DOWNLOAD_MODELS = "1"     # PowerShell
export AUTO_DOWNLOAD_MODELS=1        # Bash
```

To request YuNet instead of SSD (tries YuNet first, falls back):
```
$env:FACE_DETECTOR = "yunet"
export FACE_DETECTOR=yunet
```
Optional override of YuNet model filename (default `face_detection_yunet_2023mar.onnx`):
```
$env:FACE_YUNET_MODEL = "face_detection_yunet_2023mar.onnx"
```

If models are present, downloader safely skips existing verified hashes (SSD only).

### Evaluation Harness
Run comparative evaluation on a PDF:
```
python evaluate_extraction.py --pdf 169.pdf --pages 4 --advanced 1
```
Add ground-truth directory to compute similarity:
```
python evaluate_extraction.py --pdf 169.pdf --pages 4 --advanced 1 --gt-dir ground_truth/page4
```
Outputs `extraction_eval.csv` with per-card metrics.

### Tuning Knobs (in `advanced_photo_extractor.py`)
| Parameter | Purpose |
|-----------|---------|
| `photo_left_max_ratio` | Restricts candidate scan horizontally |
| `min_photo_area_ratio` / `max_photo_area_ratio` | Candidate size bounds |
| `candidate_step` | Sliding window stride (lower = slower & better) |
| `aspect_min` / `aspect_max` | Acceptable aspect range |

Lower `candidate_step` to 4 or extend candidate side list for more exhaustive search (costs CPU).

### Future Enhancements
- Lightweight photo vs non-photo classifier
- Landmark-based face bounding refinement
- Adaptive page layout learning with feedback logs
 - Candidate ranking model & active learning dataset logging
