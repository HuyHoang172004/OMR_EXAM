# OMR-Exam: An Efficient Lightweight Framework for Automated Exam Scoring

![Python 3.8+](https://img.shields.io/badge/Python-3.8+-blue)
[![PyTorch](https://img.shields.io/badge/PyTorch-1.9+-red?logo=pytorch)](https://pytorch.org/)

## 📖 About

This is the research repository for **"OMR-Exam: An Efficient Lightweight Framework for Automated Exam Scoring via YOLO26, EfficientNet, and DCGAN-Enhanced Recognition"**.

**OMR-Exam** is a fully automated end-to-end system framework for multiple-choice exam grading on mobile devices. Our approach addresses two key challenges in optical mark recognition:

1. **Elimination of manual coordinate configuration** through YOLO26 detection with Container Suppression and Adaptive Grid Sorting algorithms
2. **Data imbalance mitigation** using DCGAN to synthesize 1,000 artificial samples for the minority crossed-out class

## 🎯 Key Contributions

### 1. Container Suppression Algorithm
- Removes false parent/outer boxes that contain answer bubbles
- Significantly improves detection precision by filtering overlapping detections
- Enables flexible template-free bubble localization

### 2. Adaptive Grid Sorting (Interpolation Grid)
- Dynamic Y-axis grouping based on median bubble height
- Robust to skewed sheets and variable question layouts
- Automatically interprets exam template structure in real-time

### 3. DCGAN-Enhanced Data Augmentation
- Generates 1,000 synthetic crossed-out bubble samples
- Addresses class imbalance to improve classifier robustness
- Simulates diverse human erasure patterns as strong regularization

### 4. EfficientNet-B0 Bubble Classification
- Lightweight architecture optimized for mobile deployment
- 3-class classification: confirmed/crossed-out/empty
- Square padding augmentation for robust ROI processing

## 📊 Performance Results

**Experimental Validation (5-Fold Cross-Validation on 735 Exam Sheets)**

| Metric | Score | Std Dev |
|--------|-------|---------|
| Core Classifier F1-Score | **89.79%** | ±2.77% |
| Sheet-Level Accuracy | **88.98%** | ±3.92% |
| Processing Speed | **0.77s** per sheet | (GPU) |
| Model Size | **120 MB** | (full) / 30 MB (quantized) |

## 🏆 System Architecture

```
Student Answer Sheets
    ↓
[YOLO26 Detect] → Bubble locations (+ Container Suppression)
    ↓
[Adaptive Grid Sort] → Question grid alignment
    ↓
[ORB Alignment] → Sheet registration to template
    ↓
[ROI Extraction] → Individual bubble images (square padded)
    ↓
[EfficientNet-B0] → Bubble classification (confirmed/crossed-out/empty)
    ↓
[Dynamic Grading] → Grade against answer key
    ↓
[Export CSV] → Final grades & report
```

## 🚀 Quick Start

### Prerequisites
- Python 3.8+
- PyTorch 1.9+ (CUDA 11.8+ recommended for GPU)
- Git

### Installation

```bash
# Clone repository
git clone https://github.com/HuyHoang172004/OMR_EXAM.git
cd omr_exam

# Create virtual environment
python -m venv .venv
.\.venv\Scripts\activate  # Windows
# source .venv/bin/activate  # Linux/macOS

# Install dependencies
pip install --upgrade pip
pip install -r requirements.txt

# (Optional) Install PyTorch with GPU support
pip install torch torchvision torchaudio --index-url https://download.pytorch.org/whl/cu118
```

### Run Production Pipeline

```bash
jupyter lab
# Open: product/OMR_exam.ipynb
# Update configuration variables
# Run all cells
```

Output: `ket_qua_cham_thi_hoan_thien.csv` with grading results

See [SETUP.md](SETUP.md) for detailed environment configuration.

## 📁 Repository Structure

```
omr_exam/
├── README.md                          # This file
├── SETUP.md                           # Environment setup guide
├── DEVELOPMENT.md                     # System architecture & development
├── CHANGELOG.md                       # Version history
├── LICENSE                            # MIT License
├── requirements.txt                   # Python dependencies
├── .gitignore                         # Git exclusions
│
├── product/                           # Production pipeline
│   ├── OMR_exam.ipynb                 # Main grading notebook
│   └── exam/                          # Sample exam data
│
├── src/                               # Research & training code
│   ├── fine-tuned-yolo26s-detect/     # YOLO26 detection training
│   │   ├── finetuned_yolo26s_detect.ipynb
│   │   └── create_yolo_dataset.ipynb
│   │
│   ├── fine-tuned-cls/                # Classifier models (3 architectures)
│   │   ├── efficientnetb0/            # EfficientNet-B0 (recommended)
│   │   ├── resnet50/                  # ResNet50
│   │   └── yolo26s-cls/               # YOLOv8-cls
│   │
│   ├── gan/                           # DCGAN data augmentation
│   │   ├── train_DCGAN_5_folds.ipynb
│   │   └── generate_gan_img.ipynb
│   │
│   └── prepare-data/                  # Dataset preprocessing
│       ├── prepare_dataset_5_folds.ipynb
│       ├── dataset_roi_2_scene.ipynb
│       └── crossedout_augmentation_5_folds.ipynb
│
├── eval/                              # Evaluation & validation
│   ├── eval-orginal-datasets/         # Evaluation on original datasets
│   ├── eval-on-new-dataset/           # Evaluation on new data
│   └── draf/                          # Draft evaluation notebooks
│
├── weights/                           # Pre-trained models
│   ├── efficientnetb0/                # Classification weights (.pth, .tflite)
│   └── yolo26s-detect/                # Detection weights (.pt, .tflite)
│
└── android-app/                       # Mobile Android application
    ├── app/src/main/
    ├── build.gradle
    └── settings.gradle
```

## 🧪 Experimental Results

### Dataset
- **735 original exam sheets** (exam6 dataset - scanned documents and mobile camera captures)
- **5-fold cross-validation** protocol for robust evaluation
- **1,000 DCGAN-generated samples** for crossed-out class augmentation

### Baseline vs Proposed (GAN-Enhanced)

| Configuration | F1-Score | Sheet Accuracy | Improvement |
|---------------|----------|----------------|-------------|
| Without DCGAN | 87.02% ± 3.14% | 86.06% ± 4.21% | Baseline |
| With DCGAN | **89.79% ± 2.77%** | **88.98% ± 3.92%** | +2.77% / +2.92% |

### Analysis
- DCGAN synthesis improves minority class (crossed-out) robustness
- Consistent improvements across 5 evaluation folds
- Low standard deviation indicates stable performance
- Reliable deployment on diverse real-world scenarios

## 🔬 Detailed Methodology

### 1. Object Detection (YOLO26)

**Architecture**: YOLO26s-detect

**Processing Pipeline**:
- YOLO inference on full exam sheet
- **Container Suppression**: Filter parent/child box relationships
- **Adaptive Sorting**: Grid arrangement using median bubble height

**Advantages**:
- Eliminates manual coordinate configuration
- Automatically interprets different exam templates
- Real-time processing capability

### 2. Feature Alignment

**Method**: ORB (Oriented FAST and Rotated BRIEF) + Homography

**Purpose**:
- Register student sheets to template coordinate frame
- Compensate for capture angle variations
- Ensure consistent ROI extraction position

### 3. Data Augmentation (DCGAN)
*   **Architecture**: Deep Convolutional GAN
*   **Process**:
    1. Expand the minority crossed-out class (from ~120 real samples per fold to exactly 1,500 samples) via traditional geometric augmentations.
    2. Train DCGAN and generate 1,000 highly diverse synthetic crossed-out samples.
    3. Combine synthetic data with the baseline training set (expanding the class to 2,500 samples per fold).
*   **Impact**: Addresses extreme class imbalance by synthesizing novel geometric erasure structures (outperforming linear interpolation methods like SMOTE), resulting in highly robust minority class recognition.

### 4. Bubble Classification (EfficientNet-B0)
*   **Architecture**: Lightweight CNN (~4M parameters).
*   **Input**: Square-padded ROI images (**128 × 128 RGB**).
*   **Output**: 3-class probability vector (`confirmed`, `crossed-out`, `empty`).
*   **Optimization**: 
    *   **Square padding:** Reduces geometric distortion and edge artifacts during resizing.
    *   **Resolution Optimization:** Standardized to $128 \times 128$ instead of the default $224 \times 224$ ImageNet resolution, effectively cutting down computational overhead and VRAM usage by $\sim$3x while maintaining visual coherence for inference on edge devices.

## 📚 How to Reproduce


### 1. Prepare Dataset & Generate Adversarial Data

```bash
# Extract original dataset from Answersheet.zip (Baseline study by Afifi & Hussain)
# Structure: datasets/Answersheet/ (contains 735 original exam sheets spanning Exam 0 to Exam 5)
# Note: 'exam6' and 'exam6-camera' datasets are isolated in a separate folder strictly for evaluating the end-to-end pipeline on a new dataset (unseen robustness testing).

# Step 1.1: Create 5-fold cross-validation splits for both ROIs and full sheets
# Run: src/prepare-data/prepare_dataset_5_folds.ipynb

# Step 1.2: Expand minority crossed-out class via traditional geometric augmentation (120 -> 1500 samples)
# Run: src/prepare-data/crossedout_augmentation_5_folds.ipynb

# Step 1.3: Train DCGAN and generate 1,000 synthetic crossed-out images
# Run: src/gan/train_DCGAN_5_folds.ipynb
# Run: src/gan/generate_gan_img.ipynb

# Step 1.4: Structure datasets into two isolated scenarios (WithGAN and WithoutGAN)
# Run: src/prepare-data/dataset_roi_2_scene.ipynb
```

### 2. Train Detection Model

```bash
# Step 1: Format data for YOLO
# Run: src/fine-tuned-yolo26s-detect/create_yolo_dataset.ipynb

# Step 2: Train the detection model
# Open: src/fine-tuned-yolo26s-detect/finetuned_yolo26s_detect.ipynb
# Configure dataset paths & Run training
```

### 3. Train Classification Model (with/without DCGAN)

```bash
# Since the datasets are already prepared in Step 1, you just need to select 
# either Scenario 1 (WithoutGAN) or Scenario 2 (WithGAN) within the notebook 
# to fine-tune your desired core classifier.

# To train EfficientNet-B0 (Proposed Architecture):
# Run: src/fine-tuned-cls/efficientnetb0/train_efficientNet_b0.ipynb

# To train ResNet50 (Baseline comparison):
# Run: src/fine-tuned-cls/resnet50/train_resnet50_omr.ipynb

# To train YOLO26s-cls (Baseline comparison):
# Run: src/fine-tuned-cls/yolo26s-cls/finetune_yolo26_cls.ipynb
```

### 4. Evaluate Performance

```bash
# Part 1: Evaluate on Original Dataset (5-fold cross-validation)

# 1.1 Evaluate core classifier on discrete ROI images:
# Run: eval/eval-orginal-datasets/eval_roi.ipynb
# Generates: Per-bubble classification metrics (Accuracy, Macro F1-score) and confusion matrices.

# 1.2 Evaluate grading accuracy using IDEAL coordinates (Scenario A):
# Run: eval/eval-orginal-datasets/omr_eval_ideal_coordinates.ipynb
# Generates: The "upper-bound" Question-based and Sheet-based accuracy using ground-truth bounding boxes.

# 1.3 Evaluate END-TO-END automated pipeline (Scenario B):
# Run: eval/eval-orginal-datasets/eval_omr_pipeline.ipynb
# Generates: Practical grading accuracy using YOLO26s detection and ORB geometric alignment.


# Part 2: Evaluate on Unseen Data (Exam 6 - Cross-Institutional Robustness)

# Run: eval/eval-on-new-dataset/eval_omr_pipeline_on_other_data.ipynb
# Requires: ground_truth.json and templates_info.json
# Generates: Performance metrics on novel templates and mobile camera captures (demonstrating the "GAN Paradox").
```

### 5. Run Production Pipeline

```bash
# Configure: product/OMR_exam.ipynb
#   - YOLO_DETECT_PATH: path to detection weights
#   - EFFICIENTNET_WEIGHTS_PATH: path to classification weights
#   - IMAGE_FOLDER_PATH: student exam sheets
#   - USER_INPUT_METADATA: exam template structure & answers
# Run all cells
# Output: ket_qua_cham_thi_hoan_thien.csv
```

## 🔧 Environment

For detailed setup instructions, see [SETUP.md](SETUP.md)

### Core Dependencies
```
torch >= 1.9.0          # Deep learning framework
torchvision >= 0.10.0   # Computer vision utilities
ultralytics >= 8.0.0    # YOLOv8 implementation
opencv-python >= 4.5.0  # Image processing
numpy, pandas, scipy    # Data manipulation
jupyter                 # Interactive notebooks
```

### Optional for Mobile
```
tensorflow >= 2.6.0     # For TFLite export
onnx, onnxruntime       # Model conversion
```

## 📱 Mobile Deployment

Unquantized models (Float32 TensorFlow Lite) are provided to preserve maximum feature extraction accuracy:
- `weights/efficientnetb0/efficientnet.tflite` (15.4 MB)
- `weights/yolo26s-detect/best_float32.tflite` (36.4 MB)

The `android-app/` folder contains a Gradle-based Android project scaffold for mobile deployment.

## 📄 Reference

This work is based on the research paper:
**"OMR-Exam: An Efficient Lightweight Framework for Automated Exam Scoring via YOLO26, EfficientNet, and DCGAN-Enhanced Recognition"**

Authors: Le Duc Thuan, Nguyen Thi Hong Ngan, Nguyen Huy Hoang
Affiliation: Faculty of Information Technology, Academy of Cryptography Techniques, Hanoi, Vietnam

## 📊 Dataset & Pre-trained Models (Google Drive)

All experimental datasets, pre-trained weights, and metadata are publicly available on [Google Drive](https://drive.google.com/drive/folders/1vKB6uCjVFhwlp6gA2Lk7NUI2OWmxBOc7?usp=sharing). Please download and extract the ZIP files to your working directory. The `OMR-Datasets` Drive repository is organized exactly as follows:

### 1. Data Archives (.zip)
*   **`OMR_5Fold_ROIs.zip`**: The original extracted ROI (Region of Interest) data for all 5 folds.
*   **`OMR_5Fold_ROIs_split.zip`**: The fully pre-processed ROI dataset, already divided into the two main experimental scenarios (Scenario 1: *WithoutGAN* and Scenario 2: *WithGAN*).
*   **`OMR_5Fold_Sheets.zip`**: Original answer sheet 2D scene images (spanning Exam 0 to Exam 5) used for training and cross-validation.
*   **`OMR_5Fold_Sheets_TestOnly.zip`**: Reserved full-sheet images strictly isolated for the end-to-end pipeline evaluation.
*   **`exam6.zip`** & **`exam6-camera.zip`**: The completely unseen dataset (standard scans and mobile camera captures) used exclusively for cross-institutional robustness testing.
*   **`yolo_dataset.zip`**: The formatted bounding box dataset ready for training the YOLO26s detection network.
*   **`ModelAnswer.zip`**: The original ground-truth blank templates and answer key formats.

### 2. Model Weights & Supplementary Folders
*   **`train-cls/`**: Pre-trained weights (`.pth`, `.pt`, `.tflite`) for the core classification models (EfficientNet-B0, ResNet50, YOLO26s-cls) trained under both scenarios.
*   **`train_detect/`**: Pre-trained weights for the YOLO26s object detection model.
*   **`gan/`**: The synthetic adversarial data (fake crossed-out bubbles) generated by the DCGAN model.
*   **`metadata/`**: Crucial configuration files, including template structures (`template_info.json`), ground-truth labels for the new dataset (`grouth_true.json`), and original baseline `.mat` files.
*   **`report/`**: Contains generated evaluation reports.
*   **`ModelAnswer/`**: Extracted folder containing the blank template images.

## 🔍 System Performance Characteristics

| Aspect | Value |
|--------|-------|
| **Detection Precision** | High (Container Suppression filtering) |
| **Classification Accuracy** | 89.79% ± 2.77% (F1-Score) |
| **Sheet-Level Accuracy** | 88.98% ± 3.92% |
| **Inference Time** | 0.77s per sheet (GPU) |
| **Model Parameters** | ~4M (EfficientNet-B0) |
| **Mobile Model Size** | ~52.8 MB (Unquantized Float32) |
| **Data Augmentation Gain** | +2.77% F1-Score (DCGAN) |
| **Deployment** | CPU/GPU/Mobile (TFLite) |

## ⚠️ Limitations & Future Work

### Current Limitations
- **Sheet rotation:** Robust to ±30°, limited beyond.
- **Overlapping bubbles:** May be detected as a single bubble.
- **Very poor image quality:** Preprocessing quality is critical.
- **Cross-Institutional Generalization (The "GAN Paradox"):** Generative augmentation may overfit localized physical traits of the training dataset, requiring fine-tuning for entirely unseen exam templates.

### Future Enhancements (v4.0+)
- Handwritten digit recognition support (HTR).
- Web-based grading management interface.
- Real-time camera feed processing.
- Barcode/QR integration for student ID.
- Server API for enterprise deployment.
- Database integration for grade management.

## 📞 Contact & Support

**Authors**:
- Le Duc Thuan (corresponding author: thuanld@actvn.edu.vn)
- Nguyen Thi Hong Ngan
- Nguyen Huy Hoang

**Affiliation**: Faculty of Information Technology, Academy of Cryptography Techniques, Hanoi, Vietnam

## 📜 License

This project is provided for academic and educational use. 
Please cite the original paper if you use this work.

## 🙏 Acknowledgments

- Academy of Cryptography Techniques for research support.
- Ultralytics for YOLOv8 framework.
- PyTorch & OpenCV communities.

---

**Last Updated**: 2026-07-06  
**Code Version**: 3.0

