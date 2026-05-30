# Model Files Directory

This directory contains pre-trained models for the advanced photo extraction pipeline.

## Required Files:
- `photo_detector.onnx` - YOLOv5n model for photo region detection (~4MB)
- `face_detection_yunet_2023mar.onnx` - YuNet face detector
- `deploy.prototxt` + `res10_300x300_ssd_iter_140000.caffemodel` - OpenCV DNN face detector

## Setup:
Large model files are **not** committed to version control. Run the download script to fetch them:

```bash
cd python-ocr-service
python download_face_models.py
```

For the custom `photo_detector.onnx`, obtain it from your team artifact store or build it from the training pipeline.

## Model Info:
- **photo_detector.onnx**: YOLOv5n trained on COCO dataset, detects persons/objects
- **Size**: ~4MB
- **Performance**: Fast inference, good for real-time extraction
- **Fallback**: If YOLO fails, system falls back to face detection + heuristics

## Future Enhancement:
You can replace `photo_detector.onnx` with a custom-trained model specific to voter card layouts for even better accuracy.