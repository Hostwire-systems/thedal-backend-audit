#!/usr/bin/env python3
"""Train reranker model from dataset CSV produced by build_reranker_dataset.py.

Usage:
  python scripts/train_reranker.py --data reranker_dataset.csv --out models/photo_reranker.joblib

Options:
  --algo hgb        Use HistGradientBoostingClassifier (default)
  --algo rf         RandomForestClassifier
  --algo xgb        Requires xgboost installed
  --test-split 0.2  Fraction for test split (per page grouping)

The script groups by page to avoid leakage and reports per-card top1 accuracy.
"""
import argparse
import csv
import os
from pathlib import Path
import joblib
import numpy as np
from collections import defaultdict
from sklearn.ensemble import HistGradientBoostingClassifier, RandomForestClassifier
from sklearn.metrics import roc_auc_score

try:
    import xgboost as xgb  # optional
    HAVE_XGB = True
except Exception:
    HAVE_XGB = False

FEATURES = ['variance','orientation_entropy','skin_ratio','aspect','contrast','edge_density','small_components','line_peaks','text_penalty','score']


def load_dataset(path: Path):
    rows = []
    with open(path,'r',encoding='utf-8') as f:
        r = csv.DictReader(f)
        for row in r:
            try:
                rows.append(row)
            except Exception:
                continue
    return rows

def group_by_page(rows):
    pages = defaultdict(list)
    for r in rows:
        pages[r['page']].append(r)
    return pages

def build_matrices(rows):
    X=[]; y=[]
    for r in rows:
        feats = []
        for f in FEATURES:
            try:
                feats.append(float(r.get(f,0.0)))
            except Exception:
                feats.append(0.0)
        X.append(feats)
        y.append(int(r.get('label',0)))
    return np.array(X,dtype=float), np.array(y,dtype=int)

def top1_accuracy(model, rows):
    # Evaluate per (page, card): check if highest predicted prob among that card's candidates is the labeled=1
    groups = defaultdict(list)
    for r in rows:
        groups[(r['page'], r['card'])].append(r)
    correct = 0
    total = 0
    for key, cand_list in groups.items():
        Xc, yc = build_matrices(cand_list)
        if hasattr(model, 'predict_proba'):
            probs = model.predict_proba(Xc)[:,1]
        else:
            preds = model.predict(Xc)
            # fallback: treat predictions as probabilities 0/1
            probs = preds.astype(float)
        # find true positive index
        true_index = None
        for i, r in enumerate(cand_list):
            if int(r.get('label',0)) == 1:
                true_index = i
                break
        if true_index is None:
            continue
        best_index = int(np.argmax(probs))
        if best_index == true_index:
            correct += 1
        total += 1
    return correct / total if total else 0.0

def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--data', required=True)
    ap.add_argument('--out', required=True)
    ap.add_argument('--algo', choices=['hgb','rf','xgb'], default='hgb')
    ap.add_argument('--test-split', type=float, default=0.2)
    args = ap.parse_args()

    data_path = Path(args.data)
    rows = load_dataset(data_path)
    if not rows:
        raise SystemExit('No rows loaded')

    pages = group_by_page(rows)
    page_keys = list(pages.keys())
    rng = np.random.default_rng(42)
    rng.shuffle(page_keys)
    test_size = max(1, int(len(page_keys)*args.test_split))
    test_pages = set(page_keys[:test_size])
    train_rows = [r for r in rows if r['page'] not in test_pages]
    test_rows = [r for r in rows if r['page'] in test_pages]

    X_train, y_train = build_matrices(train_rows)
    X_test, y_test = build_matrices(test_rows)

    if args.algo == 'hgb':
        model = HistGradientBoostingClassifier(max_depth=6, learning_rate=0.12, max_iter=240)
    elif args.algo == 'rf':
        model = RandomForestClassifier(n_estimators=300, max_depth=14, n_jobs=-1)
    else:
        if not HAVE_XGB:
            raise SystemExit('xgboost not installed')
        model = xgb.XGBClassifier(n_estimators=300, max_depth=6, eta=0.12, subsample=0.9, colsample_bytree=0.9, objective='binary:logistic')

    model.fit(X_train, y_train)

    if hasattr(model, 'predict_proba'):
        train_probs = model.predict_proba(X_train)[:,1]
        test_probs = model.predict_proba(X_test)[:,1]
        auc_train = roc_auc_score(y_train, train_probs)
        auc_test = roc_auc_score(y_test, test_probs)
    else:
        train_preds = model.predict(X_train)
        test_preds = model.predict(X_test)
        from sklearn.metrics import accuracy_score
        auc_train = accuracy_score(y_train, train_preds)
        auc_test = accuracy_score(y_test, test_preds)

    top1_train = top1_accuracy(model, train_rows)
    top1_test = top1_accuracy(model, test_rows)

    out_path = Path(args.out)
    out_path.parent.mkdir(parents=True, exist_ok=True)
    joblib.dump(model, out_path)

    print(f"Model saved to {out_path}")
    print(f"Train AUC: {auc_train:.4f}  Test AUC: {auc_test:.4f}")
    print(f"Train Top1: {top1_train:.4f}  Test Top1: {top1_test:.4f}")

if __name__ == '__main__':
    main()
