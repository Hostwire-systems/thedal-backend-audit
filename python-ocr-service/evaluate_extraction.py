#!/usr/bin/env python3
"""
Evaluation harness for photo extraction.

Usage (basic):
    python evaluate_extraction.py --pdf 169.pdf --pages 4 --advanced 1 --out eval_results.csv

You can supply a directory of manually curated ground-truth crops to compute precision:
    python evaluate_extraction.py --pdf 169.pdf --pages 4 --advanced 1 --gt-dir ground_truth/page4

Ground truth file naming convention:
   voter_<serial>.jpg  OR  card<NN>.jpg
We attempt to match by serial first, then by card index ordering.

Outputs:
  CSV with columns: page,card_number,serial_pred,method,confidence,width,height,deskew_angle,cards_detected
  Summary metrics printed at end.
"""

import argparse
import os
import csv
import cv2
from statistics import mean
from advanced_photo_extractor import AdvancedPhotoExtractor
from extract_page4 import Page4PhotoExtractor


def load_ground_truth(gt_dir):
    if not gt_dir or not os.path.isdir(gt_dir):
        return {}
    mapping = {}
    for f in os.listdir(gt_dir):
        lower = f.lower()
        if lower.endswith(('.jpg','.jpeg','.png')):
            serial = None
            if 'voter_' in lower:
                # voter_001.jpg style
                try:
                    base = os.path.splitext(lower)[0]
                    serial = int(base.split('_')[1])
                except Exception:
                    pass
            if serial is None and lower.startswith('card'):
                try:
                    num = int(lower[4:6])
                    serial = num  # fallback approximate
                except Exception:
                    continue
            if serial is not None:
                mapping[serial] = os.path.join(gt_dir, f)
    return mapping


def compare_images(img_path_a, img_path_b):
    try:
        a = cv2.imread(img_path_a)
        b = cv2.imread(img_path_b)
        if a is None or b is None:
            return 0.0
        a = cv2.resize(a, (128,128))
        b = cv2.resize(b, (128,128))
        diff = cv2.absdiff(a,b)
        score = 1.0 - (diff.mean() / 255.0)
        return score
    except Exception:
        return 0.0


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--pdf', required=True)
    ap.add_argument('--pages', type=int, nargs='+', default=[4])
    ap.add_argument('--advanced', type=int, default=1)
    ap.add_argument('--gt-dir', default=None)
    ap.add_argument('--out', default='extraction_eval.csv')
    args = ap.parse_args()

    if not os.path.exists(args.pdf):
        raise FileNotFoundError(args.pdf)

    if args.advanced:
        extractor = AdvancedPhotoExtractor(enable_dnn=True)
    else:
        extractor = Page4PhotoExtractor()

    gt = load_ground_truth(args.gt_dir)

    rows = []
    all_conf = []
    matched=0

    for page in args.pages:
        results = extractor.extract_photos(args.pdf, page) if args.advanced else extractor.extract_page4_photos(args.pdf, page)
        deskew_angle = results.get('debug', {}).get('deskew_angle', 0.0)
        cards_detected = results.get('debug', {}).get('cards_detected', 0)
        for photo in results['photos']:
            serial_pred = photo.get('serial_number')
            conf = photo.get('confidence', 0.0)
            size = photo['size']
            w,h = map(int, size.split('x')) if 'x' in size else (0,0)
            gt_img_path = gt.get(serial_pred)
            similarity = ''
            if gt_img_path and photo.get('path'):
                similarity = f"{compare_images(gt_img_path, photo['path']):.3f}"
                if float(similarity) > 0.80:
                    matched += 1
            rows.append([
                page,
                photo['card_number'],
                serial_pred,
                photo['method'],
                f"{conf:.3f}",
                w,
                h,
                deskew_angle,
                cards_detected,
                similarity
            ])
            all_conf.append(conf)

    with open(args.out, 'w', newline='') as f:
        writer = csv.writer(f)
        writer.writerow(['page','card_number','serial_pred','method','confidence','width','height','deskew_angle','cards_detected','similarity'])
        writer.writerows(rows)

    print(f"Wrote {len(rows)} rows to {args.out}")
    if all_conf:
        print(f"Mean confidence: {mean(all_conf):.3f}")
    if gt:
        print(f"Matched {matched}/{len(gt)} ground-truth (similarity>0.80)")

if __name__ == '__main__':
    main()
