# Development & Research

This document describes the technical architecture and research methodology of OMR-Exam.

## System Architecture

### Pipeline Overview

```
Input: Exam Sheet Image
    ↓
[1] YOLO26 Object Detection
    → Detects answer bubble bounding boxes
    → Container Suppression Algorithm (filters parent boxes)
    → Adaptive Grid Sorting (groups into questions)
    ↓
[2] Image Alignment (ORB + Homography)
    → Registers student sheet to template
    → Compensates for rotation/skew
    ↓
[3] ROI Extraction
    → Crops individual bubbles from aligned sheet
    → Square Padding Augmentation (normalization)
    ↓
[4] EfficientNet-B0 Classification
    → Predicts bubble state: confirmed/crossed-out/empty
    → Outputs: per-bubble prediction + confidence scores
    ↓
[5] Dynamic Grading
    → Maps predictions to answer key
    → Outputs: question scores + sheet total
    ↓
Output: Grading Report (CSV)
```

## Key Algorithms

### 1. Container Suppression Algorithm

**Problem**: YOLO detects overlapping boxes (e.g., parent box containing child answer bubble)

**Solution**:
```python
# Filter out parent/outer boxes
# Keep only genuine answer bubbles
# Uses IOU (Intersection over Union) analysis
```

**Impact**: Eliminates noisy detections → improves precision

### 2. Adaptive Grid Sorting

**Problem**: Arrange detected bubbles into question grid (rows/columns)

**Solution**:
```python
# Calculate median bubble height
# Use 60% median height as Y-axis threshold
# Group bubbles into question rows
# Adaptively handle different layouts
```

**Impact**: Works on variable exam templates without manual configuration

### 3. DCGAN Data Augmentation

**Problem**: Extreme dataset imbalance - crossed-out class is heavily underrepresented (~120 original samples per fold).

**Solution**:
```python
# Step 1: Expand crossed-out class to 1,500 samples via geometric augmentation
# Step 2: Train DCGAN to learn the erasure distribution
# Step 3: Generate 1,000 diverse synthetic erasure samples
```

**Impact**: +2.77% F1-Score improvement, better minority class recognition

### 4. Square Padding Augmentation

**Problem**: ROI images have variable aspect ratios and high-resolution inputs consume excessive VRAM.

**Solution**:
```python
# Add white borders to make ROIs perfectly square
# Resize to an optimized 128x128 resolution (instead of standard 224x224)
```

**Impact**: Reduces geometric edge artifacts, normalizes input, and cuts computational overhead by ~3x while maintaining visual coherence.

## Experimental Setup

### Dataset
- **Baseline Training Sets (Exam 0-5)**: 735 original scanned sheets.
- **Unseen Test Sets (Exam 6)**: Standard scans and mobile camera captures reserved strictly for cross-institutional robustness testing.
- **Annotations**: Hand-labeled ground truth for 3 bubble states (confirmed, crossed-out, empty).

### Evaluation Protocol
- **5-Fold Cross-Validation**: Data split into 5 folds
- **Metrics**:
  - F1-Score (classification level)
  - Sheet Accuracy (end-to-end level)
  - Confusion Matrix (per-class analysis)

### Models Tested
| Model | Type | F1-Score | Sheet Acc |
|-------|------|----------|-----------|
| Baseline (EfficientNet-B0) | No GAN | 87.02% | 86.06% |
| **Proposed (EfficientNet-B0 + DCGAN)** | **With GAN** | **89.79%** | **88.98%** |

## Training Procedures

### Detection Model (YOLO26)

**Notebook**: `src/fine-tuned-yolo26s-detect/finetuned_yolo26s_detect.ipynb`

**Steps**:
1. Prepare dataset in YOLO format (images + .txt annotations)
2. Create `data.yaml` with class definition
3. Fine-tune YOLOv8-small on bubble detection task
4. Export best checkpoint → `weights/yolo26s-detect/best.pt`

**Hyperparameters**:
```
epochs: 50
imgsz: 960
device: 0 (GPU)
```

### Classification Model (EfficientNet-B0)

**Notebook**: `src/fine-tuned-cls/efficientnetb0/train_efficientNet_b0.ipynb`

**Steps**:
1. Prepare ImageFolder structure: `data/train/confirmed/, data/train/crossed_out/, data/train/empty/`
2. Load pretrained EfficientNet-B0 from torchvision
3. Fine-tune classifier head for 3-class task
4. Apply data augmentation (rotation, flip, color jitter)
5. Save best weights → `weights/efficientnetb0/efficientnetb0_fold5_gan.pth`

**Hyperparameters**:
```
optimizer: AdamW
learning_rate: 1e-4
batch_size: 32
epochs: 50
augmentation: RandomRotation, RandomFlip, ColorJitter
```

### DCGAN Training

**Notebook**: `src/gan/train_DCGAN_5_folds.ipynb`

**Architecture**:
- Generator: 4 deconv layers
- Discriminator: 4 conv layers
- Loss: Binary Cross-Entropy (BCELoss)

**Training Steps**:
1. Use augmented crossed-out dataset (3,890 samples)
2. Train G and D adversarially
3. Monitor generated image quality
4. Save generator checkpoint

**Synthetic Sample Generation**:
```
src/gan/generate_gan_img.ipynb
→ Generates 1,000 synthetic crossed-out bubble images
→ Saved to augmentation dataset
→ Combined with original training set
```

## Evaluation Methodology

### Metrics

**Bubble-Level Classification**:
- **F1-Score**: Harmonic mean of precision and recall
- **Accuracy**: Percentage correct
- **Confusion Matrix**: Per-class performance

**Sheet-Level Grading**:
- **Sheet Accuracy**: % of sheets with perfect score
- **Per-Question Accuracy**: Question-by-question correctness

### Evaluation Notebooks

**1. Evaluation on Original Dataset**:
```
eval/eval-orginal-datasets/eval_omr_pipeline.ipynb
- Load model weights
- Run inference on test fold
- Compute metrics
- Generate confusion matrix
```

**2. Evaluation on New Dataset**:
```
eval/eval-on-new-dataset/eval_omr_pipeline_on_other_data.ipynb
- Test generalization on unseen data
- Validate robustness
```

## Results & Analysis

### Performance Summary

**5-Fold Cross-Validation Results**:

| Model | F1-Score | Sheet Acc | Fold Std |
|-------|----------|-----------|----------|
| Baseline | 87.02% | 86.06% | 3.14% |
| Proposed | **89.79%** | **88.98%** | 2.77% |
| **Improvement** | **+2.77%** | **+2.92%** | Lower variance |

### Key Findings

1. **DCGAN Effectiveness**: Synthetic data improves minority class robustness
2. **Stability**: Low variance across folds (±2.77%) indicates reliable performance
3. **Cross-Dataset**: Good generalization from exam6 to exam6-camera

## Reproducibility

### Code Organization

```
src/
├── fine-tuned-yolo26s-detect/   # Detection training
├── fine-tuned-cls/               # 3 classification architectures
├── gan/                          # DCGAN augmentation
└── prepare-data/                 # Data preprocessing

product/
└── OMR_exam.ipynb                # Inference pipeline

eval/
├── eval-orginal-datasets/        # Evaluation on original data
└── eval-on-new-dataset/          # Evaluation on new data
```

### Configuration Files

All hyperparameters specified in notebook configuration sections:
- `DATA_PATH`: Dataset locations
- `MODEL_PATH`: Weight file locations
- `HYPERPARAMS`: Training parameters
- `AUGMENTATION_CONFIG`: Preprocessing settings

### Reproducibility Steps

1. Download dataset: [Google Drive](https://drive.google.com/drive/folders/1pyeTVVInFBqBJGe_LFwhmLm8iNxZRV-4?usp=drive_link)
2. Run training notebooks in order
3. Run evaluation notebooks
4. Compare results with published metrics

## References

**Related Work**:
- YOLO: https://docs.ultralytics.com
- EfficientNet: https://arxiv.org/abs/1905.11946
- DCGAN: https://arxiv.org/abs/1511.06434
- OMR Literature: Academic papers in object detection and image classification

## Performance Characteristics

| Aspect | Value |
|--------|-------|
| Inference Time | 80-120 ms/sheet (GPU) |
| Model Size | 120 MB (full), 30 MB (quantized) |
| Memory Usage | ~2 GB (inference) |
| FLOPs | ~10B (detection + classification) |

---

For detailed usage instructions, see [README.md](README.md) and [SETUP.md](SETUP.md)

**Last Updated**: 2026-07-06
**Authors**: Le Duc Thuan, Nguyen Thi Hong Ngan, Nguyen Huy Hoang
