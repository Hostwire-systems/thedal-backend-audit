#!/usr/bin/env python3
"""
Advanced Photo Extractor

High-accuracy voter photo extraction pipeline targeting >95-98% correct crops.
Key components:
 1. Page deskew & normalization
 2. Table/grid structural segmentation (robust to margin shifts)
 3. Per-card photo candidate proposal (face-first, heuristic second)
 4. Composite scoring & validation (face prob, edge orientation entropy, variance, skin tone, aspect prior)
 5. Fallback cascading & logging for continuous improvement

NOTE: Lightweight DNN face model (OpenCV ResNet SSD) optional.
Place model files in: python-ocr-service/models/
  - deploy.prototxt
  - res10_300x300_ssd_iter_140000.caffemodel

Usage:
    from advanced_photo_extractor import AdvancedPhotoExtractor
    extractor = AdvancedPhotoExtractor()
    results = extractor.extract_photos(pdf_path, page_number=4)

Returned results structure:
{
  'photos': [ { 'card_number': int, 'serial_number': str|"UNK", 'method': str, 'size': 'WxH', 'confidence': float, 'path': str } ],
  'extracted_count': int,
  'success_rate': float,
  'total_cards': 30,
  'debug': { 'deskew_angle': float, 'cards_detected': int }
}
"""

import os
import io
import cv2
import fitz
import numpy as np
from PIL import Image
import logging
from typing import List, Tuple, Optional, Dict, Any

logger = logging.getLogger(__name__)
if not logger.handlers:
    logging.basicConfig(level=logging.INFO)

class AdvancedPhotoExtractor:
    def __init__(self, enable_dnn: bool = True, photo_detector_mode: str | None = None):
        self.enable_dnn = enable_dnn
        self._init_face_models()
        # Tunable parameters
        self.min_card_area = 4000
        self.expected_rows = 10
        self.expected_cols = 3
        self.photo_left_max_ratio = 0.45  # photos expected left portion
        self.min_photo_area_ratio = 0.035
        self.max_photo_area_ratio = 0.20
        # tighter upper bound for candidate heuristic stage to avoid large text blocks
        self.max_photo_area_ratio_candidate = 0.16
        self.candidate_step = 6
        self.min_candidate_size = 40
        self.aspect_min = 0.65
        self.aspect_max = 1.55
        # Structural photo locator parameters
        self.struct_left_ratio = 0.55
        self.struct_min_area_ratio = 0.025
        self.struct_max_area_ratio = 0.22
        self.struct_min_fill = 0.32
        # Logging & ML reranker placeholders
        self.log_candidates = os.environ.get('LOG_CANDIDATES','0') in ('1','true','TRUE','yes','Y')
        self.log_sample_rate = float(os.environ.get('LOG_CANDIDATES_SAMPLE_RATE','1.0'))
        self.candidate_log_dir = os.environ.get('CANDIDATE_LOG_DIR','candidate_logs')
        if self.log_candidates:
            os.makedirs(self.candidate_log_dir, exist_ok=True)
        self.reranker = None
        reranker_path = os.environ.get('PHOTO_RERANKER_MODEL','')
        if reranker_path and os.path.exists(reranker_path):
            try:
                import joblib
                self.reranker = joblib.load(reranker_path)
                logger.info(f"Loaded photo reranker model: {reranker_path}")
            except Exception as e:
                logger.warning(f"Failed loading reranker model {reranker_path}: {e}")
        # Context placeholders for logging
        self._current_card_index = None
        self._current_page_number = None
        self.smart_swap = os.environ.get('SMART_SWAP','0') in ('1','true','TRUE','yes','Y')
        # Record desired photo detector mode (explicit argument overrides env)
        self.photo_detector_mode = (photo_detector_mode or os.environ.get('PHOTO_DETECTOR','')).lower().strip()
        # Optional YOLO detector
        self.photo_detector = None
        if self.photo_detector_mode == 'onnx':
            try:
                from .yolo_photo_detector import YoloPhotoDetector  # relative import
            except Exception:
                try:
                    from yolo_photo_detector import YoloPhotoDetector  # fallback
                except Exception as e:
                    YoloPhotoDetector = None
            model_dir = os.path.join(os.path.dirname(__file__), 'models')
            yolo_path = os.path.join(model_dir, 'photo_detector.onnx')
            # Auto-download if requested and missing
            if not os.path.exists(yolo_path):
                auto_dl = os.environ.get('AUTO_DOWNLOAD_MODELS','0') in ('1','true','TRUE','yes','Y')
                if auto_dl:
                    os.makedirs(model_dir, exist_ok=True)
                    url = os.environ.get('PHOTO_DETECTOR_URL', '').strip() or 'https://example.com/path/to/photo_detector.onnx'
                    try:
                        import urllib.request
                        logger.info(f"Downloading YOLO photo detector from {url} ...")
                        urllib.request.urlretrieve(url, yolo_path)
                        logger.info("YOLO photo detector downloaded")
                    except Exception as e:
                        logger.warning(f"Failed to download YOLO detector model: {e}")
            if 'YoloPhotoDetector' in locals() and YoloPhotoDetector and os.path.exists(yolo_path):
                try:
                    self.photo_detector = YoloPhotoDetector(yolo_path, conf_threshold=0.40, iou_threshold=0.50)
                    logger.info("YOLO photo detector initialized")
                except Exception as e:
                    logger.warning(f"Failed to init YOLO detector: {e}")
            else:
                logger.warning("PHOTO_DETECTOR=onnx set but photo_detector.onnx not present or import failed")

    def _init_face_models(self):
        self.haar = cv2.CascadeClassifier(cv2.data.haarcascades + 'haarcascade_frontalface_default.xml')
        self.dnn_net = None
        self.use_dnn = False
        self.yunet = None
        self.use_yunet = False
        if not self.enable_dnn:
            return
        model_dir = os.path.join(os.path.dirname(__file__), 'models')
        proto = os.path.join(model_dir, 'deploy.prototxt')
        weights = os.path.join(model_dir, 'res10_300x300_ssd_iter_140000.caffemodel')
        # Optional auto-download
        if (not os.path.exists(proto) or not os.path.exists(weights)) and os.environ.get('AUTO_DOWNLOAD_MODELS','0') == '1':
            try:
                import subprocess, sys as _sys
                logger.info("Attempting automatic download of face models (AUTO_DOWNLOAD_MODELS=1)")
                subprocess.run([_sys.executable, os.path.join(os.path.dirname(__file__), 'download_face_models.py'), '--only', 'dnn'], check=False, timeout=180)
            except Exception as e:
                logger.warning(f"Auto-download attempt failed: {e}")
        if os.path.exists(proto) and os.path.exists(weights):
            try:
                self.dnn_net = cv2.dnn.readNetFromCaffe(proto, weights)
                self.use_dnn = True
                logger.info("DNN face detector loaded")
            except Exception as e:
                logger.warning(f"Failed to load DNN face model: {e}")
        else:
            logger.warning("DNN face model files not found in models/. Falling back to Haar only.")

        # Optional YuNet (OpenCV face detection network) if available
        # Expect model file 'face_detection_yunet_2023mar.onnx' (or env FACE_YUNET_MODEL) in models/
        try:
            detector_choice = os.environ.get('FACE_DETECTOR', '').lower()  # yunet|dnn|haar|auto
            yunet_model = os.environ.get('FACE_YUNET_MODEL', 'face_detection_yunet_2023mar.onnx')
            yunet_path = os.path.join(model_dir, yunet_model)
            if (detector_choice in ('yunet','auto')) and not os.path.exists(yunet_path) and os.environ.get('AUTO_DOWNLOAD_MODELS','0') == '1':
                try:
                    import subprocess, sys as _sys
                    logger.info("Attempting automatic download of YuNet model")
                    subprocess.run([_sys.executable, os.path.join(os.path.dirname(__file__), 'download_face_models.py'), '--only', 'yunet'], check=False, timeout=120)
                except Exception as de:
                    logger.warning(f"Auto-download YuNet failed: {de}")
            if detector_choice in ('yunet','auto') and os.path.exists(yunet_path):
                # Dynamically access FaceDetectorYN if present
                if hasattr(cv2, 'FaceDetectorYN_create'):
                    self.yunet = cv2.FaceDetectorYN_create(yunet_path, "", (320,320), 0.9, 0.3, 5000)
                    self.use_yunet = True
                    logger.info("YuNet face detector loaded")
                else:
                    logger.warning("OpenCV build lacks FaceDetectorYN_create; cannot use YuNet")
        except Exception as e:
            logger.warning(f"Failed to initialize YuNet detector: {e}")

    # ---------------- Public API -----------------
    def extract_photos(self, pdf_path: str, page_number: int = 4) -> Dict[str, Any]:
        self._current_page_number = page_number
        doc = fitz.open(pdf_path)
        if page_number < 1 or page_number > len(doc):
            doc.close()
            raise ValueError(f"Invalid page number {page_number}")
        page = doc[page_number - 1]
        mat = fitz.Matrix(3.0, 3.0)
        pix = page.get_pixmap(matrix=mat)
        img_data = pix.tobytes("ppm")
        pil_image = Image.open(io.BytesIO(img_data))
        page_image = cv2.cvtColor(np.array(pil_image), cv2.COLOR_RGB2BGR)
        doc.close()

        deskew_angle, deskewed = self._deskew(page_image)
        seg_cards = self._segment_cards(deskewed)
        logger.info(f"Segmented {len(seg_cards)} candidate card regions (target {self.expected_rows * self.expected_cols})")

        # Always build grid and merge for stable ordering & full coverage
        grid_cards = self._fallback_grid_cards(deskewed)
        cards = self._merge_cards(seg_cards, grid_cards)
        if len(cards) < 30:
            logger.warning(f"After merge only {len(cards)} cards; padding with extra grid boxes")
            added = 0
            existing_set = {(x,y,w,h) for (x,y,w,h) in cards}
            for g in grid_cards:
                if len(cards) >= 30:
                    break
                if g in existing_set:
                    continue
                cards.append(g)
                added += 1
            logger.info(f"Padded with {added} grid boxes => total {len(cards)}")

        extracted = []
        low_confidence_slots = []
        output_dir = f"page{page_number}_advanced_photos"
        os.makedirs(output_dir, exist_ok=True)

        for idx, (x,y,w,h) in enumerate(cards[:30]):
            card_num = idx + 1
            self._current_card_index = card_num
            card_roi = deskewed[y:y+h, x:x+w]
            photo_crop, method, confidence = self._extract_card_photo(card_roi, second_pass=False)
            serial = self._extract_serial_number(card_roi)
            if photo_crop is not None:
                if confidence < 0.40 or method == 'heuristic':
                    low_confidence_slots.append((card_num, card_roi, confidence))
                filename = f"card{card_num:02d}_serial{serial}_{method}.jpg"
                path = os.path.join(output_dir, filename)
                try:
                    cv2.imwrite(path, photo_crop)
                except Exception as e:
                    logger.error(f"Failed saving {filename}: {e}")
                    path = None
                extracted.append({
                    'card_number': card_num,
                    'serial_number': serial,
                    'method': method,
                    'size': f"{photo_crop.shape[1]}x{photo_crop.shape[0]}",
                    'confidence': float(f"{confidence:.3f}"),
                    'path': path
                })

        # Recovery pass for low-confidence crops
        if low_confidence_slots:
            logger.info(f"Recovery pass attempting improvements on {len(low_confidence_slots)} cards")
            for card_num, card_roi, prev_conf in low_confidence_slots:
                self._current_card_index = card_num
                improved_crop, improved_method, improved_conf = self._extract_card_photo(card_roi, second_pass=True)
                if improved_crop is not None and improved_conf > prev_conf + 0.08:
                    filename = f"card{card_num:02d}_serialUNK_{improved_method}.jpg"
                    path = os.path.join(output_dir, filename)
                    try:
                        cv2.imwrite(path, improved_crop)
                    except Exception as e:
                        logger.error(f"Failed saving improved {filename}: {e}")
                        path = None
                    for entry in extracted:
                        if entry['card_number'] == card_num:
                            entry.update({
                                'method': improved_method,
                                'size': f"{improved_crop.shape[1]}x{improved_crop.shape[0]}",
                                'confidence': float(f"{improved_conf:.3f}"),
                                'path': path
                            })
                            break

        return {
            'photos': extracted,
            'extracted_count': len(extracted),
            'success_rate': (len(extracted) / 30) * 100,
            'total_cards': 30,
            'output_dir': output_dir,
            'debug': {
                'deskew_angle': deskew_angle,
                'cards_detected': len(cards)
            }
        }

    # --------------- Deskew ---------------------
    def _deskew(self, image):
        gray = cv2.cvtColor(image, cv2.COLOR_BGR2GRAY)
        edges = cv2.Canny(gray, 50, 150)
        lines = cv2.HoughLinesP(edges, 1, np.pi/180, threshold=120, minLineLength=200, maxLineGap=10)
        angle = 0.0
        if lines is not None and len(lines) > 0:
            angles = []
            for l in lines[:100]:
                x1,y1,x2,y2 = l[0]
                if abs(x2 - x1) < 5:  # vertical lines -> skip to avoid instability
                    continue
                theta = np.degrees(np.arctan2(y2 - y1, x2 - x1))
                if -15 < theta < 15:
                    angles.append(theta)
            if angles:
                angle = float(np.median(angles))
        if abs(angle) < 0.2:
            return angle, image
        (h,w) = image.shape[:2]
        M = cv2.getRotationMatrix2D((w//2, h//2), angle, 1.0)
        rotated = cv2.warpAffine(image, M, (w,h), flags=cv2.INTER_LINEAR, borderMode=cv2.BORDER_REPLICATE)
        return angle, rotated

    # --------------- Card Segmentation ---------
    def _segment_cards(self, image) -> List[Tuple[int,int,int,int]]:
        gray = cv2.cvtColor(image, cv2.COLOR_BGR2GRAY)
        blur = cv2.GaussianBlur(gray, (3,3), 0)
        bw = cv2.adaptiveThreshold(blur,255,cv2.ADAPTIVE_THRESH_MEAN_C,cv2.THRESH_BINARY_INV,31,9)
        horiz_kernel = cv2.getStructuringElement(cv2.MORPH_RECT,(40,1))
        vert_kernel = cv2.getStructuringElement(cv2.MORPH_RECT,(1,40))
        horiz = cv2.dilate(cv2.erode(bw, horiz_kernel, 1), horiz_kernel, 1)
        vert = cv2.dilate(cv2.erode(bw, vert_kernel, 1), vert_kernel, 1)
        table = cv2.add(horiz, vert)
        contours, _ = cv2.findContours(table, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)
        candidates = []
        h_img, w_img = image.shape[:2]
        for c in contours:
            x,y,w,h = cv2.boundingRect(c)
            area = w*h
            if area < self.min_card_area or h < 60 or w < 60:
                continue
            if y < h_img * 0.04:  # skip top header lines
                continue
            aspect = w / float(h)
            if aspect < 0.4 or aspect > 2.8:
                continue
            candidates.append((y,x,w,h))
        # Group by rows via y clustering
        candidates.sort()
        if not candidates:
            return []
        rows = []
        current_row = [candidates[0]]
        row_tol = 18
        for c in candidates[1:]:
            if abs(c[0] - current_row[-1][0]) <= row_tol:
                current_row.append(c)
            else:
                rows.append(current_row)
                current_row = [c]
        rows.append(current_row)
        # Sort each row by x, flatten preserving order
        ordered = []
        for r in rows:
            r_sorted = sorted(r, key=lambda t: t[1])
            for item in r_sorted:
                y,x,w,h = item
                ordered.append((x,y,w,h))
        return ordered

    def _fallback_grid_cards(self, image):
        h, w = image.shape[:2]
        rows = self.expected_rows
        cols = self.expected_cols
        margin_top = int(h * 0.07)
        margin_bottom = int(h * 0.03)
        margin_left = int(w * 0.02)
        margin_right = int(w * 0.02)
        usable_h = h - margin_top - margin_bottom
        usable_w = w - margin_left - margin_right
        card_h = usable_h // rows
        card_w = usable_w // cols
        boxes = []
        for r in range(rows):
            for c in range(cols):
                x = margin_left + c * card_w
                y = margin_top + r * card_h
                boxes.append((x,y,card_w,card_h))
        return boxes

    def _merge_cards(self, segmented, grid):
        """Merge segmented and grid ROIs using IoU > 0.35 preference for segmented.

        Grid ordering (row-major) is canonical to ensure deterministic mapping card1..card30.
        """
        if not grid:
            return segmented
        merged = []
        for gx,gy,gw,gh in grid:
            best = None
            best_iou = 0.0
            for sx,sy,sw,sh in segmented:
                iou = self._iou((gx,gy,gw,gh), (sx,sy,sw,sh))
                if iou > 0.35 and iou > best_iou:
                    best_iou = iou
                    best = (sx,sy,sw,sh)
            merged.append(best if best else (gx,gy,gw,gh))
        return merged

    def _iou(self, a, b):
        ax,ay,aw,ah = a
        bx,by,bw,bh = b
        ax2 = ax + aw; ay2 = ay + ah
        bx2 = bx + bw; by2 = by + bh
        ix1 = max(ax, bx); iy1 = max(ay, by)
        ix2 = min(ax2, bx2); iy2 = min(ay2, by2)
        if ix2 <= ix1 or iy2 <= iy1:
            return 0.0
        inter = (ix2 - ix1) * (iy2 - iy1)
        union = aw*ah + bw*bh - inter
        return inter / union if union > 0 else 0.0

    # ------------- Per-card Extraction ----------
    def _extract_card_photo(self, card_roi, second_pass: bool = False):
        # 0. YOLO direct photo detect (if enabled)
        if self.photo_detector is not None and not second_pass:
            try:
                det = self.photo_detector.detect(card_roi)
                if det:
                    x,y,w,h,conf = det
                    crop = card_roi[y:y+h, x:x+w]
                    if crop.size > 0 and crop.shape[0] >= self.min_candidate_size and crop.shape[1] >= self.min_candidate_size:
                        self._log_candidate('yolo', crop, conf, accepted=True)
                        return crop, 'yolo', float(conf)
                else:
                    logger.debug("YOLO produced no detection, falling back to face/heuristics")
            except Exception as e:
                logger.warning(f"YOLO detection failed: {e}")
        # 1. Try face first
        face_crop, conf = self._detect_face(card_roi, second_pass=second_pass)
        if face_crop is not None:
            self._log_candidate('face', face_crop, conf, accepted=True)
            return face_crop, 'face', conf
        # 2. Structural locator
        struct_crop = self._photo_region_from_structure(card_roi)
        if struct_crop is not None:
            s_score = self._score_region(struct_crop)
            if s_score >= (0.60 if not second_pass else 0.55):
                self._log_candidate('struct', struct_crop, s_score, accepted=True)
                return struct_crop, 'struct', s_score
        # 3. Candidate windows
        candidates = self._generate_candidates(card_roi, widen=second_pass)
        best = None
        best_score = 0
        top_candidates = []  # (score, crop, feats)
        for (x,y,w,h) in candidates:
            crop = card_roi[y:y+h, x:x+w]
            score, feats = self._compute_region_features(crop)
            # store top N for possible smart swap
            if len(top_candidates) < 5:
                top_candidates.append((score, crop, feats))
                top_candidates.sort(key=lambda t: t[0], reverse=True)
            else:
                if score > top_candidates[-1][0]:
                    top_candidates[-1] = (score, crop, feats)
                    top_candidates.sort(key=lambda t: t[0], reverse=True)
            score_only = score if isinstance(score, (int,float)) else float(score)
            self._log_candidate('heuristic', crop, score, accepted=False)
            if score_only > best_score:
                best_score = score_only
                best = crop
        thresh = 0.60 if not second_pass else 0.54
        if best is not None and best_score >= thresh:
            # optional smart swap
            if self.smart_swap and top_candidates:
                swapped = self._smart_swap(top_candidates, best, best_score)
                if swapped:
                    best, best_score = swapped
            final_score = self._apply_reranker(best, best_score)
            self._log_candidate('heuristic_selected', best, final_score, accepted=True)
            return best, 'heuristic', final_score
        # 4. Contour fallback
        contour_crop, cscore = self._contour_fallback(card_roi)
        if contour_crop is not None:
            final_c = self._apply_reranker(contour_crop, cscore)
            self._log_candidate('contour', contour_crop, final_c, accepted=True if final_c>=cscore else False)
            return contour_crop, 'contour', final_c
        return None, 'none', 0.0

    def _detect_face(self, img, second_pass: bool = False):
        h, w = img.shape[:2]
        # Priority: YuNet -> DNN -> Haar
        faces = []
        # YuNet
        if self.use_yunet:
            try:
                # YuNet requires setting input size to image size
                if hasattr(self.yunet, 'setInputSize'):
                    self.yunet.setInputSize((w,h))
                yunet_dets, _ = self.yunet.detect(img)
                # Fix: Check if yunet_dets is actually an array/list before calling len()
                if yunet_dets is not None and hasattr(yunet_dets, '__len__') and len(yunet_dets) > 0:
                    for det in yunet_dets:
                        x, y, fw, fh, score = det[:5]
                        if score < (0.65 if not second_pass else 0.55):
                            continue
                        if fw < 24 or fh < 24:
                            continue
                        faces.append((int(x), int(y), int(fw), int(fh), float(score)))
                elif yunet_dets is not None and not hasattr(yunet_dets, '__len__'):
                    logger.debug(f"YuNet returned non-array result: {type(yunet_dets)} = {yunet_dets}")
            except Exception as e:
                logger.warning(f"YuNet detection failed: {e}")
        if self.use_dnn:
            blob = cv2.dnn.blobFromImage(img, 1.0, (300, 300),(104.0,177.0,123.0), swapRB=False, crop=False)
            self.dnn_net.setInput(blob)
            detections = self.dnn_net.forward()
            for i in range(detections.shape[2]):
                conf = detections[0,0,i,2]
                cutoff = 0.55 if not second_pass else 0.45
                if conf < cutoff: continue
                box = detections[0,0,i,3:7] * np.array([w,h,w,h])
                x1,y1,x2,y2 = box.astype(int)
                x1,y1 = max(0,x1), max(0,y1)
                x2,y2 = min(w-1,x2), min(h-1,y2)
                if (x2-x1) < 28 or (y2-y1) < 28: continue
                faces.append((x1,y1,x2-x1,y2-y1,conf))
        # Haar fallback
        if not faces:
            gray = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)
            variants = [gray]
            variants.append(cv2.createCLAHE(clipLimit=2.0, tileGridSize=(8,8)).apply(gray))
            for gamma in (0.8, 1.2):
                lut = np.array([((i/255.0) ** gamma) * 255 for i in range(256)]).astype('uint8')
                variants.append(cv2.LUT(gray, lut))
            params = [ (1.05,3,(20,20)), (1.08,2,(18,18)), (1.12,2,(16,16)) ]
            for eg in variants:
                for (sc, neigh, ms) in params:
                    hf = self.haar.detectMultiScale(eg, scaleFactor=sc, minNeighbors=neigh, minSize=ms)
                    for (x,y,fw,fh) in hf:
                        faces.append((x,y,fw,fh,0.38))
        if not faces:
            return None, 0.0
        best = max(faces, key=lambda f: f[4]*f[2]*f[3])
        x,y,fw,fh,conf = best
        pad = int(0.28 * max(fw, fh))  # slightly tighter
        x1 = max(0, x - pad)
        y1 = max(0, y - pad)
        x2 = min(w, x + fw + pad)
        y2 = min(h, y + fh + pad)
        crop = img[y1:y2, x1:x2]
        if crop.shape[0] < self.min_candidate_size or crop.shape[1] < self.min_candidate_size:
            return None, 0.0
        return crop, float(conf)

    def _generate_candidates(self, img, widen: bool = False):
        h,w = img.shape[:2]
        ratio = self.photo_left_max_ratio if not widen else min(0.60, self.photo_left_max_ratio + 0.12)
        max_x = int(w * ratio)
        candidates = []
        stride = max(4, self.candidate_step - 2)
        side_list = (50, 60, 70, 80, 90, 100, 110, 120, 130)
        rect_sizes = [(sw, sh) for sw in side_list for sh in side_list if 0.75 <= sw/float(sh) <= 1.4]
        # Precompute gray & quick variance map patch to prune low-information zones
        gray = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)
        # Simple integral image for fast local variance approximation
        mean = cv2.blur(gray, (9,9))
        sqmean = cv2.blur(gray.astype(np.float32)**2, (9,9))
        var_map = np.maximum(sqmean - mean.astype(np.float32)**2, 0)
        var_norm = var_map / (var_map.max() + 1e-6)
        max_candidates = 1800  # hard cap to keep runtime bounded
        for y in range(0, h - self.min_candidate_size, stride):
            for x in range(0, max_x - self.min_candidate_size, stride):
                # Skip zones with very low local variance (likely blank/text margin)
                if var_norm[y, x] < 0.08:
                    continue
                for (sw, sh) in rect_sizes:
                    if x + sw > max_x or y + sh > h:
                        continue
                    area_ratio = (sw * sh) / (w * h)
                    upper = self.max_photo_area_ratio_candidate if not widen else self.max_photo_area_ratio
                    if not (self.min_photo_area_ratio <= area_ratio <= upper):
                        continue
                    candidates.append((x, y, sw, sh))
                    if len(candidates) >= max_candidates:
                        return candidates
        return candidates

    # ------------- Structural Photo Region (pre-candidate) ---------
    def _photo_region_from_structure(self, img):
        """Attempt to isolate photo rectangle via frequency + morphology before exhaustive candidates.

        Strategy:
          1. High-pass emphasize internal face/photo texture vs text lines (use Laplacian variance + band-pass).
          2. Adaptive threshold to binary; remove tiny specks.
          3. Connected components restricted to left portion; evaluate fill ratio and aspect.
        Returns crop ndarray or None.
        """
        h, w = img.shape[:2]
        left_limit = int(w * self.struct_left_ratio)
        work = img[:, :left_limit]
        gray = cv2.cvtColor(work, cv2.COLOR_BGR2GRAY)
        # Band-pass: blur then subtract heavier blur
        blur_small = cv2.GaussianBlur(gray, (3,3), 0)
        blur_large = cv2.GaussianBlur(gray, (11,11), 0)
        band = cv2.subtract(blur_small, blur_large)
        # Normalize & threshold
        norm = cv2.normalize(band, None, 0, 255, cv2.NORM_MINMAX)
        th = cv2.adaptiveThreshold(norm,255,cv2.ADAPTIVE_THRESH_MEAN_C,cv2.THRESH_BINARY,31,5)
        # Invert so photo (texture) tends to white blobs consolidated
        th = 255 - th
        # Open to remove thin text lines still present
        th = cv2.morphologyEx(th, cv2.MORPH_OPEN, np.ones((3,3),np.uint8), iterations=1)
        # Closing to fill small gaps
        th = cv2.morphologyEx(th, cv2.MORPH_CLOSE, np.ones((5,5),np.uint8), iterations=1)
        contours,_ = cv2.findContours(th, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)
        best = None
        best_score = 0
        total_area = w * h
        for c in contours:
            x,y,rw,rh = cv2.boundingRect(c)
            area = rw * rh
            area_ratio = area / total_area
            if not (self.struct_min_area_ratio <= area_ratio <= self.struct_max_area_ratio):
                continue
            ar = rw / float(rh)
            if ar < self.aspect_min or ar > self.aspect_max:
                continue
            mask = np.zeros((rh,rw), dtype=np.uint8)
            cv2.drawContours(mask, [c - [x,y]], -1, 255, -1)
            fill_ratio = mask.mean()/255.0
            if fill_ratio < self.struct_min_fill:
                continue
            # internal variance
            crop = work[y:y+rh, x:x+rw]
            g2 = cv2.cvtColor(crop, cv2.COLOR_BGR2GRAY)
            var = np.var(g2)
            score = var * fill_ratio
            if score > best_score:
                best_score = score
                best = (x,y,rw,rh)
        if best is None:
            return None
        x,y,rw,rh = best
        return work[y:y+rh, x:x+rw]

    # -------- Text Block Filter ---------
    def _is_text_block(self, region):
        if region.size == 0:
            return False
        gray = cv2.cvtColor(region, cv2.COLOR_BGR2GRAY)
        h,w = gray.shape[:2]
        if h < 28 or w < 28:
            return False
        # Binarize
        bw = cv2.adaptiveThreshold(gray,255,cv2.ADAPTIVE_THRESH_MEAN_C,cv2.THRESH_BINARY_INV,25,7)
        # Run-length coherence (horizontal)
        runs = []
        for row in bw:
            count = 0; prev = 0
            for px in row:
                if px == 255 and prev == 255:
                    count += 1
                elif px == 255:
                    count = 1
                else:
                    if count>0:
                        runs.append(count)
                    count = 0
                prev = px
            if count>0:
                runs.append(count)
        if not runs:
            return False
        runs_sorted = sorted(runs, reverse=True)
        topk = runs_sorted[: min(12, len(runs_sorted))]
        coherence = sum(topk)/ (w * len(topk))
        # Component density
        cc = cv2.morphologyEx(bw, cv2.MORPH_OPEN, np.ones((2,2),np.uint8))
        contours,_ = cv2.findContours(cc, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)
        small = 0
        for c in contours:
            x,y,cw,ch = cv2.boundingRect(c)
            area = cw*ch
            if 20 <= area <= 180 and cw > 3 and ch > 3:
                small += 1
        small_density = small / ( (w*h)/900.0 + 1e-6 )  # normalized by notional cell count
        # Orientation entropy reuse
        sobelx = cv2.Sobel(gray, cv2.CV_32F,1,0,ksize=3)
        sobely = cv2.Sobel(gray, cv2.CV_32F,0,1,ksize=3)
        angles = np.arctan2(sobely, sobelx)
        mag = np.sqrt(sobelx**2 + sobely**2)
        mag_mask = mag > np.percentile(mag, 75)
        angle_hist,_ = np.histogram(angles[mag_mask], bins=12, range=(-np.pi,np.pi))
        probs = angle_hist / (angle_hist.sum() + 1e-6)
        orient_entropy = -(probs * np.log(probs + 1e-6)).sum() / np.log(len(angle_hist))
        # Decision
        if (small >= 28 and orient_entropy < 0.60) or (coherence > 0.11 and orient_entropy < 0.58) or (small_density > 5.5 and orient_entropy < 0.63):
            return True
        return False

    def _score_region(self, region):
        score, feats = self._compute_region_features(region)
        return score

    def _compute_region_features(self, region):
        if region.size == 0:
            return 0.0, {}
        gray = cv2.cvtColor(region, cv2.COLOR_BGR2GRAY)
        h_reg, w_reg = gray.shape[:2]
        variance = float(np.var(gray))
        var_score = min(variance / 1200.0, 1.0)
        edges = cv2.Canny(gray, 50, 150)
        sobelx = cv2.Sobel(gray, cv2.CV_32F, 1,0,ksize=3)
        sobely = cv2.Sobel(gray, cv2.CV_32F, 0,1,ksize=3)
        angles = np.arctan2(sobely, sobelx)
        mag = np.sqrt(sobelx**2 + sobely**2)
        mag_mask = mag > np.percentile(mag, 75)
        angle_hist, _ = np.histogram(angles[mag_mask], bins=12, range=(-np.pi, np.pi))
        angle_probs = angle_hist / (angle_hist.sum() + 1e-6)
        orientation_entropy = -(angle_probs * np.log(angle_probs + 1e-6)).sum() / np.log(len(angle_hist))
        hsv = cv2.cvtColor(region, cv2.COLOR_BGR2HSV)
        h_chan,s_chan,v_chan = cv2.split(hsv)
        skin_mask = (((h_chan < 25) | (h_chan > 160)) & (s_chan > 25) & (s_chan < 180) & (v_chan > 40) & (v_chan < 250)).astype(np.uint8)
        skin_ratio = float(skin_mask.mean())
        skin_score = min(skin_ratio * 2.0, 1.0)
        ar = w_reg/float(h_reg)
        if ar < self.aspect_min or ar > self.aspect_max:
            return 0.0, {'reject_aspect': True}
        aspect_score = 1.0 - min(abs(ar - 1.0), 0.7)
        contrast = float(gray.max() - gray.min())
        contrast_score = min(contrast / 180.0, 1.0)
        edge_density = float(edges.mean())
        bw = cv2.adaptiveThreshold(gray,255,cv2.ADAPTIVE_THRESH_MEAN_C,cv2.THRESH_BINARY_INV,25,7)
        cc = cv2.morphologyEx(bw, cv2.MORPH_OPEN, np.ones((2,2),np.uint8))
        contours,_ = cv2.findContours(cc, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)
        small_components = 0
        for c in contours:
            x,y,wc,hc = cv2.boundingRect(c)
            area = wc*hc
            if area < 18:
                continue
            if area < 160 and (wc > 3 and hc > 3):
                small_components += 1
        proj = bw.sum(axis=1) / 255.0
        peaks = int(np.sum(proj > (0.55 * proj.max() if proj.max()>0 else 9999)))
        text_penalty = 0.0
        if edge_density > 0.22 and orientation_entropy < 0.55:
            text_penalty += 0.22
        if small_components > 34:
            return 0.0, {'reject_extreme_text': True, 'small_components': small_components}
        if small_components > 22:
            text_penalty += min(0.20 + 0.01*(small_components-22), 0.35)
        if peaks >= 10 and orientation_entropy < 0.60:
            return 0.0, {'reject_line_stack': True, 'peaks': peaks}
        if peaks >= 5 and orientation_entropy < 0.62:
            text_penalty += 0.18
        text_penalty = min(text_penalty, 0.65)
        score = (0.20*var_score + 0.18*orientation_entropy + 0.17*skin_score + 0.12*aspect_score + 0.18*contrast_score + 0.15*(1.0 - text_penalty))
        feats = {
            'variance': variance,
            'orientation_entropy': float(orientation_entropy),
            'skin_ratio': skin_ratio,
            'aspect': ar,
            'contrast': contrast,
            'edge_density': edge_density,
            'small_components': small_components,
            'line_peaks': peaks,
            'text_penalty': text_penalty,
            'score': float(score)
        }
        return float(score), feats

    # ------------- Reranker Integration -------------
    def _apply_reranker(self, crop, base_score):
        if self.reranker is None:
            return base_score
        try:
            _, feats = self._compute_region_features(crop)
            feature_vector = [
                feats.get('variance',0.0),
                feats.get('orientation_entropy',0.0),
                feats.get('skin_ratio',0.0),
                feats.get('aspect',0.0),
                feats.get('contrast',0.0),
                feats.get('edge_density',0.0),
                feats.get('small_components',0.0),
                feats.get('line_peaks',0.0),
                feats.get('text_penalty',0.0),
                base_score
            ]
            import numpy as _np
            arr = _np.array(feature_vector, dtype=float).reshape(1,-1)
            pred = self.reranker.predict_proba(arr)
            # Assume binary classifier; take probability of class 1 as refined score if available
            if hasattr(self.reranker, 'classes_') and len(self.reranker.classes_) == 2:
                class_index = list(self.reranker.classes_).index(1)
                return float(pred[0][class_index])
            return float(pred[0].max())
        except Exception:
            return base_score

    # ------------- Candidate Logging -------------
    def _log_candidate(self, ctype, crop, score, accepted: bool):
        if not self.log_candidates:
            return
        import random, time, json
        if self.log_sample_rate < 1.0 and random.random() > self.log_sample_rate:
            return
        try:
            _, feats = self._compute_region_features(crop)
            record = {
                't': time.time(),
                'type': ctype,
                'accepted': accepted,
                'score': score,
                'h': int(crop.shape[0]),
                'w': int(crop.shape[1]),
                'page': self._current_page_number,
                'card': self._current_card_index,
                'features': feats
            }
            day = time.strftime('%Y%m%d')
            pid = os.getpid()
            path = os.path.join(self.candidate_log_dir, f'candidates_{day}_{pid}.jsonl')
            with open(path,'a',encoding='utf-8') as f:
                f.write(json.dumps(record)+'\n')
        except Exception as e:
            if random.random() < 0.01:
                logger.warning(f"Candidate logging failed: {e}")

    # ------------- Smart Swap (Heuristic Refinement) -------------
    def _smart_swap(self, top_candidates, current_best, current_score):
        """Evaluate top candidates and optionally replace current best to reduce text false positives.

        Strategy:
          - If best has extremely low skin_ratio and another candidate has higher skin_ratio with comparable score.
          - If best has high small_components & low orientation_entropy suggesting text block.
        Returns (new_crop, new_score) or None.
        """
        try:
            # Find features for current best
            def find_feats(crop):
                for sc, cr, ft in top_candidates:
                    if cr is crop:
                        return sc, ft
                sc, ft = self._compute_region_features(current_best)
                return sc, ft
            best_s, best_feats = find_feats(current_best)
            skin = best_feats.get('skin_ratio',0.0)
            small_comp = best_feats.get('small_components',0)
            orient = best_feats.get('orientation_entropy',1.0)
            text_pen = best_feats.get('text_penalty',0.0)
            # Candidate alternatives
            for sc, crop, feats in top_candidates:
                if crop is current_best:
                    continue
                # Skin-based swap
                if skin < 0.02 and feats.get('skin_ratio',0.0) > 0.05 and feats.get('variance',0.0) >= best_feats.get('variance',0.0)*0.85 and sc >= best_s - 0.08:
                    return crop, sc
                # Text penalty swap
                if (small_comp > 24 and orient < 0.60 and text_pen > 0.15) and sc >= best_s - 0.05 and feats.get('small_components',0) < small_comp - 4:
                    return crop, sc
            return None
        except Exception:
            return None

    def _contour_fallback(self, img):
        gray = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)
        th = cv2.adaptiveThreshold(gray,255,cv2.ADAPTIVE_THRESH_MEAN_C,cv2.THRESH_BINARY,31,7)
        th = 255 - th
        contours,_ = cv2.findContours(th, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)
        h_img,w_img = img.shape[:2]
        best=None
        best_score=0
        for c in contours:
            x,y,w,h = cv2.boundingRect(c)
            area = w*h
            if area < 900: continue
            area_ratio = area / (w_img*h_img)
            if not (self.min_photo_area_ratio <= area_ratio <= self.max_photo_area_ratio):
                continue
            ar = w/float(h)
            if ar < self.aspect_min or ar > self.aspect_max:
                continue
            crop = img[y:y+h,x:x+w]
            score = self._score_region(crop)
            if score > best_score:
                best_score = score
                best = crop
        if best is not None and best_score >= 0.5:
            return best, best_score
        return None, 0.0

    # ------------- Serial Number Extraction (reuse heuristic) ---------
    def _extract_serial_number(self, card_roi):
        # Placeholder; can integrate OCR similar to legacy extractor
        return "UNK"

# End of AdvancedPhotoExtractor
