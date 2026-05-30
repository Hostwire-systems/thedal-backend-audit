# Photo Extraction Model Pipeline

This document explains how to move from the heuristic + face pipeline to a nearly 99%+ accurate ML-driven system.

## Components
1. Candidate Logging (already integrated)  
   Enabled via environment variable: `LOG_CANDIDATES=1` (optional `LOG_CANDIDATES_SAMPLE_RATE=0.5` to subsample).  
   Output: JSONL lines in `candidate_logs/` containing feature metrics per candidate.

2. Reranker Dataset Builder  
   Script: `scripts/build_reranker_dataset.py`  
   Produces CSV with features + label derived from accepted candidate per card.

3. Reranker Trainer  
   Script: `scripts/train_reranker.py`  
   Trains a classifier (default: HistGradientBoosting) to predict probability a candidate is the correct crop.

4. YOLO Photo Detector (Scaffold)  
   File: `yolo_photo_detector.py`  
   Provides ONNX inference wrapper for a custom YOLO model that directly predicts photo bounding boxes inside card ROIs.

## Workflow
### Step 1: Collect Logs
Run extraction over as many PDFs as possible:
```
SET LOG_CANDIDATES=1
python flask_ocr_service.py  # or batch evaluation script
```
Verify logs appear in `candidate_logs/`.

### Step 2: Build Reranker Dataset
```
python scripts/build_reranker_dataset.py --logs candidate_logs --out reranker_dataset.csv
```
Inspect the CSV; optionally filter obviously incorrect positives.

### Step 3: Train Reranker
```
python scripts/train_reranker.py --data reranker_dataset.csv --out models/photo_reranker.joblib
```
After training, set:
```
SET PHOTO_RERANKER_MODEL=python-ocr-service/models/photo_reranker.joblib
```
Re-run extraction. You should see improved selection (higher mean confidence / fewer mis-crops).

### Step 4: Prepare YOLO Detector Data
- Extract individual card images (already produced during extraction; if not, add a small export hook).  
- Annotate each card image with a single bounding box around the photo using LabelImg/RoboFlow/CVAT.  
- Export YOLO format annotations.

### Step 5: Train YOLO Model
Recommended starting point: ultralytics (yolov8n) or newer lightweight variant.
```
pip install ultralytics
ultralytics task=detect mode=train model=yolov8n.pt data=photo.yaml epochs=80 imgsz=416
```
Export to ONNX:
```
ultralytics mode=export model=runs/detect/train/weights/best.pt format=onnx imgsz=416
```
Place the exported file at: `python-ocr-service/models/photo_detector.onnx`.

### Step 6: Integrate YOLO Detector
Set environment variable:
```
SET PHOTO_DETECTOR=onnx
```
(Integration code path to call `YoloPhotoDetector` should be added—currently scaffold only.)
If YOLO detects a box with confidence above threshold (e.g., 0.45), use that crop. If not, fallback to face + heuristic pipeline.

### Step 7: Hybrid Improvement Loop
1. Keep logging enabled even with reranker & detector (to refine future versions).  
2. Periodically rebuild dataset including new mistakes; retrain reranker.  
3. Optionally calibrate reranker probabilities with isotonic regression if deploying thresholds.

## Feature Glossary
| Feature | Description |
|---------|-------------|
| variance | Grayscale variance (texture richness) |
| orientation_entropy | Normalized entropy of dominant edge orientations |
| skin_ratio | Fraction of pixels matching coarse skin HSV filter |
| aspect | Width/Height ratio |
| contrast | Max - min grayscale intensity |
| edge_density | Mean of Canny edge map (0–1 scaled) |
| small_components | Count of small connected components (proxy for text) |
| line_peaks | Number of strong horizontal projection peaks |
| text_penalty | Aggregated penalty heuristic for text-like structure |
| score | Heuristic composite score pre-reranker |

## Troubleshooting
- No logs produced: ensure `LOG_CANDIDATES=1` is set in same shell/process that starts the extractor.
- Reranker not loading: confirm `PHOTO_RERANKER_MODEL` points to an existing joblib file.
- ONNX runtime error: install `onnxruntime` and verify model opset version (prefer opset 13+).
- Detector picks text region: lower `conf_threshold` might not be the issue—improve training data or add reranker blending.

## Future Enhancements
- Add face embedding verification (discard crops with face similarity below threshold).  
- Serial alignment check: crop vertical alignment vs serial number region.  
- Outlier area re-evaluation: replace extremely small or large candidate with 2nd best.

---
This pipeline sets the stage to push remaining error rate down by combining a layout-aware detector with learned ranking.
