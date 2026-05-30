#!/usr/bin/env python3
"""Download face detection models (SSD ResNet Caffe + YuNet ONNX) into models/.

Usage:
  python download_face_models.py            # downloads both if missing
  python download_face_models.py --only dnn # only SSD Caffe
  python download_face_models.py --only yunet

Environment variable override:
  MODEL_DIR: target directory (default: ./models)

Exits 0 on success, non-zero on any unrecovered failure.

Notes:
- YuNet license: OpenCV (Apache 2.0). Provided via official OpenCV release URL.
- SSD Caffe model: OpenCV provided standard face detector.
"""
import argparse
import os
import sys
import hashlib
import requests
from pathlib import Path

# Updated URLs: the caffemodel was moved to a tag-specific directory in opencv_3rdparty.
# We relax SHA256 enforcement (set None) because upstream occasionally republishes.
DNN_FILES = [
    ("deploy.prototxt", "https://raw.githubusercontent.com/opencv/opencv/master/samples/dnn/face_detector/deploy.prototxt", None),
]

# Candidate URLs for the caffemodel (try in order)
CAFFE_MODEL_CANDIDATES = [
    "https://raw.githubusercontent.com/opencv/opencv_3rdparty/dnn_samples_face_detector_20170830/res10_300x300_ssd_iter_140000.caffemodel",
    "https://github.com/opencv/opencv_3rdparty/raw/dnn_samples_face_detector_20170830/res10_300x300_ssd_iter_140000.caffemodel",
    # Fallback mirror (if any future relocation occurs add here)
]

YUNET_FILES = [
    ("face_detection_yunet_2023mar.onnx", "https://github.com/opencv/opencv_zoo/raw/main/models/face_detection_yunet/face_detection_yunet_2023mar.onnx", None)
]

CHUNK = 8192

def sha256_file(path: Path) -> str:
    h = hashlib.sha256()
    with open(path, 'rb') as f:
        for chunk in iter(lambda: f.read(CHUNK), b''):
            h.update(chunk)
    return h.hexdigest()

def download(url: str, dest: Path, expected_sha256: str | None) -> bool:
    if dest.exists():
        if expected_sha256:
            current = sha256_file(dest)
            if current.lower() == expected_sha256.lower():
                print(f"[skip] {dest.name} already present and hash OK")
                return True
            else:
                print(f"[warn] Hash mismatch for {dest.name}, re-downloading")
        else:
            print(f"[skip] {dest.name} already exists (no hash to verify)")
            return True
    try:
        resp = requests.get(url, stream=True, timeout=60)
        resp.raise_for_status()
        size = 0
        with open(dest, 'wb') as f:
            for chunk in resp.iter_content(CHUNK):
                if chunk:
                    f.write(chunk)
                    size += len(chunk)
        print(f"[ok] Downloaded {dest.name} ({size/1024:.1f} KB)")
        if expected_sha256:
            h = sha256_file(dest)
            if h.lower() != expected_sha256.lower():
                print(f"[warn] SHA256 mismatch for {dest.name} (expected {expected_sha256[:8]}..., got {h[:8]}...). Continuing.")
        return True
    except Exception as e:
        print(f"[error] Failed to download {url}: {e}")
        return False

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--only', choices=['dnn','yunet'], help='Download only a specific model family')
    parser.add_argument('--force', action='store_true', help='Force re-download even if file exists')
    args = parser.parse_args()

    model_dir = Path(os.environ.get('MODEL_DIR', 'models'))
    model_dir.mkdir(parents=True, exist_ok=True)

    tasks = []
    want_dnn = (args.only is None or args.only == 'dnn')
    if want_dnn:
        tasks.extend(DNN_FILES)
    if args.only is None or args.only == 'yunet':
        tasks.extend(YUNET_FILES)

    failures = 0
    for fname, url, sha256 in tasks:
        dest = model_dir / fname
        if args.force and dest.exists():
            dest.unlink()
        if not download(url, dest, sha256):
            failures += 1

    # Handle caffemodel separately (multi-URL attempts)
    if want_dnn:
        caffe_dest = model_dir / "res10_300x300_ssd_iter_140000.caffemodel"
        need_caffe = args.force or (not caffe_dest.exists())
        if need_caffe:
            got = False
            for curl in CAFFE_MODEL_CANDIDATES:
                print(f"Attempting {curl}")
                if download(curl, caffe_dest, None):
                    got = True
                    break
            if not got:
                print("[error] Could not download res10_300x300_ssd_iter_140000.caffemodel from any known URL")
                failures += 1
        else:
            print("[skip] res10_300x300_ssd_iter_140000.caffemodel already present")

    if failures:
        print(f"Completed with {failures} failure(s)")
        sys.exit(1)
    print("All requested model files are present.")

if __name__ == '__main__':
    main()
