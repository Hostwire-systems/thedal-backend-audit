#!/usr/bin/env python3
"""YOLO Photo Detector Scaffold.

This module provides a thin wrapper to load an ONNX YOLO model (e.g., custom-trained
on card photo regions) and run detection on a card ROI to extract the best photo box.

Integration steps after you have a trained model:
 1. Export YOLO model to ONNX (e.g., yolov8n -> photo_detector.onnx).
 2. Place file at python-ocr-service/models/photo_detector.onnx
 3. Set env PHOTO_DETECTOR=onnx to enable.
 4. In advanced_photo_extractor, add call before heuristic candidates.

The YOLO ONNX is expected to output (common Ultralytics format) either:
  - A single output with shape (batch, num_boxes, 6) => [x, y, w, h, conf, cls]
  - Or two outputs (boxes, scores). Adjust parse if your export differs.

NOTE: This scaffold does not depend on ultralytics at runtime, only on onnxruntime.
Install onnxruntime:
  pip install onnxruntime==1.18.0

You will need to add onnxruntime to requirements.txt when you integrate for production.
"""
from __future__ import annotations
import os
import numpy as np
import cv2
from typing import Optional, Tuple

try:
    import onnxruntime as ort
    HAVE_ORT = True
except Exception:
    HAVE_ORT = False

class YoloPhotoDetector:
    def __init__(self, model_path: str, conf_threshold: float = 0.40, iou_threshold: float = 0.50):
        if not HAVE_ORT:
            raise RuntimeError("onnxruntime not installed. Install onnxruntime to use YOLO detector.")
        if not os.path.exists(model_path):
            raise FileNotFoundError(model_path)
        self.model_path = model_path
        self.conf_threshold = conf_threshold
        self.iou_threshold = iou_threshold
        self.session = ort.InferenceSession(model_path, providers=["CPUExecutionProvider"])  # CPU first; can add CUDA if available
        self.input_name = self.session.get_inputs()[0].name
        input_info = self.session.get_inputs()[0]
        shp = input_info.shape
        # Expect [batch, channels, h, w]
        self.in_h = shp[2] if isinstance(shp[2], int) else 640
        self.in_w = shp[3] if isinstance(shp[3], int) else 640
        # Log model input requirements for debugging
        import logging
        logger = logging.getLogger(__name__)
        logger.info(f"YOLO model input: {input_info.name}, shape: {shp}, type: {input_info.type}")

    def _preprocess(self, img):
        h, w = img.shape[:2]
        scale = min(self.in_w / w, self.in_h / h)
        nw, nh = int(w * scale), int(h * scale)
        resized = cv2.resize(img, (nw, nh))
        canvas = np.zeros((self.in_h, self.in_w, 3), dtype=np.uint8)
        canvas[:nh, :nw] = resized
        blob = canvas[:, :, ::-1].transpose(2, 0, 1).astype(np.float32) / 255.0
        
        # Check if model expects float16 and convert if needed
        input_dtype = self.session.get_inputs()[0].type
        if 'float16' in input_dtype or 'half' in input_dtype:
            blob = blob.astype(np.float16)
            
        return blob, scale, nw, nh

    def _nms(self, boxes, scores):
        idxs = cv2.dnn.NMSBoxes(boxes, scores, self.conf_threshold, self.iou_threshold)
        if len(idxs) == 0:
            return []
        return [boxes[i[0]] + [scores[i[0]]] for i in idxs]

    def detect(self, card_roi) -> Optional[Tuple[int,int,int,int,float]]:
        try:
            blob, scale, nw, nh = self._preprocess(card_roi)
            inp = blob[np.newaxis, :]
            outputs = self.session.run(None, {self.input_name: inp})
            
            # Debug output structure
            import logging
            logger = logging.getLogger(__name__)
            logger.debug(f"YOLO outputs: {len(outputs)} tensors, shapes: {[o.shape for o in outputs]}")
            
            # Try to parse standard YOLO export
            preds = outputs[0]
            logger.debug(f"First output shape: {preds.shape}, dtype: {preds.dtype}")
            
            # Handle different YOLO output formats
            if preds.ndim == 3 and preds.shape[0] == 1:
                # Shape: (1, num_detections, 6) -> squeeze batch dim
                preds = preds[0]
            elif preds.ndim == 2:
                # Shape: (num_detections, 6) - already correct
                pass
            elif preds.ndim == 3 and preds.shape[-1] in [4, 5, 6]:
                # Some models: (num_detections, 1, 6) -> squeeze middle
                preds = preds.squeeze()
            else:
                logger.warning(f"Unexpected YOLO output shape: {preds.shape}")
                return None
                
            logger.debug(f"After reshape: {preds.shape}")
            
            if preds.size == 0:
                return None
                
            # Handle case where no detections (empty array)
            if len(preds.shape) == 1:
                if len(preds) < 6:
                    return None
                preds = preds.reshape(1, -1)
                
            # Each row: x y w h conf cls (or similar)
            boxes = []
            scores = []
            for i, row in enumerate(preds):
                if len(row) < 5:  # At least need x,y,w,h,conf
                    continue
                x, y, w, h, conf = row[:5]
                cls = row[5] if len(row) > 5 else 0
                if conf < self.conf_threshold:
                    continue
                
                # Convert center coordinates to top-left coordinates
                x1 = int((x - w/2) / scale)
                y1 = int((y - h/2) / scale) 
                bw = int(w / scale)
                bh = int(h / scale)
                
                # Clamp to image bounds
                x1 = max(0, x1)
                y1 = max(0, y1)
                
                boxes.append([x1, y1, bw, bh])
                scores.append(float(conf))
                
            if not boxes:
                return None
                
            kept = self._nms(boxes, scores)
            if not kept:
                return None
                
            # Keep highest score
            best = max(kept, key=lambda b: b[4])
            x,y,w,h,conf = best
            
            # Clamp inside image bounds
            H,W = card_roi.shape[:2]
            x = max(0, min(W-1, x))
            y = max(0, min(H-1, y))
            w = max(1, min(W-x, w))
            h = max(1, min(H-y, h))
            
            return x, y, w, h, conf
            
        except Exception as e:
            import logging
            logger = logging.getLogger(__name__)
            logger.warning(f"YOLO detection failed: {e}")
            return None

# End scaffold
