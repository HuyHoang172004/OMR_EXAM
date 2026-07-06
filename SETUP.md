# Setup Guide

Setup instructions for running the OMR-Exam framework locally.

## Prerequisites

- **Python**: 3.8 or higher
- **pip**: Latest version  
- **Virtual Environment**: Recommended (venv or conda)
- **GPU** (Optional but Recommended): CUDA 11.8+ for faster training and inference
- **Storage**: ~5-10 GB for code, datasets, uncompressed images, and model weights

## Quick Setup

### 1. Clone and Navigate

```bash
git clone [https://github.com/HuyHoang172004/OMR_EXAM.git](https://github.com/HuyHoang172004/OMR_EXAM.git)
cd omr_exam
```

### 2. Create Virtual Environment

**Option A: Using venv**
```bash
python -m venv .venv
.\.venv\Scripts\activate  # Windows
source .venv/bin/activate  # Linux/macOS
```

**Option B: Using conda**
```bash
conda create -n omr_exam python=3.10
conda activate omr_exam
```

### 3. Install Dependencies

```bash
pip install --upgrade pip
pip install -r requirements.txt
```

### 4. Optional: GPU Support

```bash
# For CUDA 11.8 (recommended)
pip install torch torchvision torchaudio --index-url https://download.pytorch.org/whl/cu118

# For CPU only
pip install torch torchvision torchaudio
```

Verify installation:
```bash
python -c "import torch; print(torch.cuda.is_available())"
```

### 5. Download Dataset (Optional)

Download the required data archives and pre-trained models from our Google Drive.

Extract the downloaded .zip files directly into your project root directory. Ensure your structure contains:

- `OMR_5Fold_Sheets/` (Original dataset)

- `OMR_5Fold_ROIs_split/` (Pre-processed ROIs for WithGAN/WithoutGAN scenarios)

- `exam6/ & exam6-camera/` (Unseen robustness test data)

- `train-cls/ & train_detect/` (Model weights)

### 6. Start Jupyter

```bash
jupyter lab
# or
jupyter notebook
```

## Troubleshooting

### CUDA Not Detected
```bash
python -c "import torch; print(torch.cuda.is_available())"
# If False, reinstall PyTorch with correct CUDA version
pip install torch torchvision torchaudio --index-url https://download.pytorch.org/whl/cu118
```

### Missing Packages
```bash
pip install -r requirements.txt --upgrade
```

### Virtual Environment Issues
```bash
# Recreate venv
rm -rf .venv
python -m venv .venv
.\.venv\Scripts\activate
pip install -r requirements.txt
```

## Environment Details

Tested on:
- Python 3.8, 3.9, 3.10, 3.11
- PyTorch 1.9+
- CUDA 11.8
- Linux, Windows, macOS

---

For detailed usage, see [README.md](README.md)
