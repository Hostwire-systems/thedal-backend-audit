#!/usr/bin/env python3
"""Build reranker training dataset from candidate JSONL logs.

Usage:
  python scripts/build_reranker_dataset.py --logs candidate_logs --outputs-dir page4_advanced_photos --out reranker_dataset.csv

Process:
  1. Read all *.jsonl under --logs.
  2. Each line: {page, card, type, accepted, score, features:{...}}
  3. Determine positive label per (page, card): the heuristic_selected/face/struct/contour with accepted True and highest score.
  4. Mark that candidate label=1, others label=0.
  5. Output CSV with selected feature columns + label.

Notes:
  - If multiple accepted types exist for same (page,card), highest score wins.
  - Rows missing required fields are skipped.
  - Add --append to extend an existing CSV.
"""
import argparse
import csv
import json
import os
from pathlib import Path
from collections import defaultdict

FEATURE_ORDER = [
    'variance','orientation_entropy','skin_ratio','aspect','contrast',
    'edge_density','small_components','line_peaks','text_penalty','score'
]

def load_logs(log_dir: Path):
    records = []
    for path in sorted(log_dir.glob('*.jsonl')):
        with open(path,'r',encoding='utf-8') as f:
            for line in f:
                line = line.strip()
                if not line:
                    continue
                try:
                    obj = json.loads(line)
                    records.append(obj)
                except Exception:
                    continue
    return records

def group_and_label(records):
    groups = defaultdict(list)
    for r in records:
        page = r.get('page')
        card = r.get('card')
        feats = r.get('features') or {}
        if page is None or card is None or not feats:
            continue
        groups[(page, card)].append(r)
    labeled = []
    for key, cand_list in groups.items():
        # find winner
        best = None
        best_score = -1
        for c in cand_list:
            if c.get('accepted'):
                s = float(c.get('score',0.0))
                if s > best_score:
                    best = c; best_score = s
        # assign labels
        for c in cand_list:
            feats = c.get('features') or {}
            row = {k: feats.get(k,'') for k in FEATURE_ORDER}
            row['page'] = key[0]
            row['card'] = key[1]
            row['ctype'] = c.get('type')
            row['accepted'] = int(bool(c.get('accepted')))
            row['label'] = 1 if best is c else 0
            labeled.append(row)
    return labeled

def write_csv(rows, out_path: Path, append: bool):
    header = ['page','card','ctype','accepted'] + FEATURE_ORDER + ['label']
    mode = 'a' if append and out_path.exists() else 'w'
    with open(out_path, mode, newline='', encoding='utf-8') as f:
        w = csv.DictWriter(f, fieldnames=header)
        if mode == 'w':
            w.writeheader()
        for r in rows:
            w.writerow({h: r.get(h,'') for h in header})


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--logs', required=True, help='Directory with candidate jsonl logs')
    ap.add_argument('--out', required=True, help='Output CSV path')
    ap.add_argument('--append', action='store_true', help='Append to existing CSV')
    args = ap.parse_args()
    log_dir = Path(args.logs)
    if not log_dir.exists():
        raise SystemExit(f"Log dir {log_dir} not found")
    records = load_logs(log_dir)
    if not records:
        raise SystemExit("No log records found")
    labeled = group_and_label(records)
    out_path = Path(args.out)
    write_csv(labeled, out_path, args.append)
    print(f"Wrote {len(labeled)} rows to {out_path}")

if __name__ == '__main__':
    main()
