# Deployment Guide - OCR Service with Local Models

## ✅ Current Setup (Recommended)
All required models are now stored locally in the repository:
- `python-ocr-service/models/photo_detector.onnx` (YOLOv5n)
- `python-ocr-service/models/face_detection_yunet_2023mar.onnx`
- `python-ocr-service/models/deploy.prototxt` + `res10_300x300_ssd_iter_140000.caffemodel`

## 🚀 Deployment Steps

### 1. Environment Configuration
Copy the `.env` file contents or set these environment variables in Coolify:

```bash
# Core service
FLASK_ENV=production
HOST=0.0.0.0
PORT=5000

# Photo detector (local model)
PHOTO_DETECTOR=onnx
AUTO_DOWNLOAD_MODELS=0

# Face detection
ENABLE_DNN_FACE=1
FACE_DETECTOR=yunet

# Logging & diagnostics
LOG_CANDIDATES=1
CANDIDATE_LOG_DIR=/app/candidate_logs
SMART_SWAP=1

# Output settings
RESULTS_DIR=/app/extraction_results
UPLOAD_DIR=/app/temp_uploads
JPEG_QUALITY=92

# Cleanup (hours)
TEMP_FILE_MAX_AGE_HOURS=12
RESULTS_MAX_AGE_HOURS=72
```

### 2. Docker Build
The Dockerfile should include:
```dockerfile
# Copy models into the container
COPY python-ocr-service/models/ /app/python-ocr-service/models/

# Install Python dependencies
RUN pip install -r python-ocr-service/requirements.txt
```

### 3. Volume Mounts (Optional but Recommended)
- `/app/extraction_results` - Persistent photo storage
- `/app/candidate_logs` - ML training data collection

### 4. Health Check
```bash
curl http://your-domain/health
```

Should return status "healthy" with configuration details.

## 🔧 Advantages of Local Models

### ✅ Benefits:
- **Zero external dependencies** - No network calls during startup
- **Predictable deployment** - Models always available
- **Version control** - Model changes tracked in Git
- **Faster startup** - No download delays
- **Offline capable** - Works without internet

### 📁 Repository Structure:
```
python-ocr-service/
├── models/
│   ├── photo_detector.onnx          # 4MB - YOLO photo detector
│   ├── face_detection_yunet_2023mar.onnx  # Face detector
│   ├── deploy.prototxt              # DNN config
│   ├── res10_300x300_ssd_iter_140000.caffemodel  # Face model
│   └── README.md                    # Model documentation
├── flask_ocr_service.py             # Main service
├── advanced_photo_extractor.py      # ML pipeline
└── requirements.txt                 # Dependencies
```

## 🎯 Extraction Pipeline Order:
1. **YOLO Detection** → Photo region detection (if available)
2. **Face Detection** → YuNet + OpenCV DNN fallback  
3. **Heuristic Scoring** → Variance, edges, skin tone analysis
4. **Structural Locator** → Grid-based photo positioning
5. **Contour Fallback** → Shape-based extraction
6. **Recovery Pass** → Second attempt for failed cards

## 🔍 Monitoring & Logs
- Service logs show model initialization status
- Candidate logs (if enabled) useful for ML model training
- Memory usage tracking for cloud deployment optimization

## 📈 Future Enhancements
- Train custom YOLO model on voter card data for higher accuracy
- Implement reranker model using collected candidate logs
- Add model versioning and A/B testing capabilities

## 🛠 Troubleshooting
- **Model not found**: Ensure models directory copied to container
- **Memory issues**: Adjust container memory limits (recommend 2GB+)
- **Slow startup**: First extraction may take extra time for model loading