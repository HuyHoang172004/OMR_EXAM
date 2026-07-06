# Changelog

All notable changes to the OMR-Exam research project are documented here.

## [3.0 - Research Version] - 2026

### Research Contributions

#### Algorithm Innovations
- **Container Suppression Algorithm**: Box-in-box filtering for noisy detection elimination
- **Adaptive Grid Sorting (Interpolation Grid)**: Dynamic Y-axis grid alignment based on median bubble height
- **Square Padding Augmentation**: ROI preprocessing for consistent bubble representation

#### Data Augmentation
- **DCGAN Synthesis**: Generate 1,000 synthetic samples for crossed-out class
  - Addresses data imbalance (202 original → 3,890 with traditional augmentation → 4,890 with DCGAN)
  - Simulates diverse human erasure patterns
  - Acts as strong regularization mechanism

#### Experimental Results
- **F1-Score Improvement**: 87.02% (baseline) → **89.79%** (with DCGAN)  
- **Sheet Accuracy Improvement**: 86.06% (baseline) → **88.98%** (with DCGAN)
- **5-Fold Cross-Validation**: Robust evaluation on 735 exam sheets
- **Low Variance**: ±2.77% F1-Score, ±3.92% Sheet Accuracy
- **Mobile Deployment**: Quantized int8 models (4x size reduction)

### Technical Implementation

#### Detection Pipeline (YOLO26)
- Eliminates per-bubble manual coordinate configuration
- Automatic template interpretation in real-time
- Robust to perspective distortion and image skew

#### Classification Pipeline (EfficientNet-B0)
- 3-class bubble state classification (confirmed/crossed-out/empty)
- Lightweight architecture (4M parameters) for mobile deployment
- Square padding augmentation improves edge case handling

#### System Integration
- ORB + Homography for sheet registration
- Adaptive row thresholding for grid alignment
- Dynamic grading engine supporting variable question structures

### Performance Characteristics

| Metric | Value |
|--------|-------|
| Detection Precision | High (Container Suppression) |
| Classification Accuracy | 89.79% ± 2.77% |
| Sheet Accuracy | 88.98% ± 3.92% |
| Processing Speed | 80-120 ms/sheet (GPU) |
| Model Size | 120 MB (full) / 30 MB (quantized) |

### Datasets
- **exam6**: 735 sheets (scanned documents + mobile camera captures)
- **5-Fold Splits**: Cross-validation on all subsets
- **DCGAN Synthetic**: 1,000 generated samples

### Research Work
- Authors: Le Duc Thuan, Nguyen Thi Hong Ngan, Nguyen Huy Hoang
- Affiliation: Faculty of Information Technology, Academy of Cryptography Techniques, Hanoi, Vietnam

---

## [2.0 - Development Release] - 2023

### Initial Implementation
- YOLO26 bubble detection
- EfficientNet-B0 classification
- ORB feature alignment
- CSV export for grading results
- Evaluation framework for accuracy measurement

### Known Limitations (v2.0)
- Manual template coordinate configuration required
- Static row thresholding (limited on skewed sheets)
- No data augmentation for imbalanced classes
- No quantized model support

---

## [1.0 - Research Prototype] - 2023

### Core Concepts
- Initial YOLO-based bubble detection
- CNN-based bubble classification
- Homography-based alignment
- Basic grading pipeline

---

## Version Comparison

| Feature | v1.0 | v2.0 | v3.0 |
|---------|------|------|------|
| Automatic Bubble Detection | ✓ | ✓ | ✓✓ |
| Manual Template Config | ✓✓ | ✓✓ | ✗ (Automatic) |
| Container Suppression | ✗ | ✗ | ✓ |
| Adaptive Grid Sorting | ✗ | ✗ | ✓ |
| DCGAN Augmentation | ✗ | ✗ | ✓ |
| Classification Accuracy | ~85% | ~87% | **89.79%** |
| Mobile Deployment | ✗ | ✗ | ✓ (TFLite) |
| F1-Score (5-Fold CV) | - | - | **89.79% ± 2.77%** |

---

## Future Research Directions (v4.0+)

### Planned Enhancements
- [ ] Handwritten digit recognition support
- [ ] Automated template detection (no manual specification)
- [ ] Real-time camera feed processing
- [ ] Barcode/QR integration for student ID
- [ ] Web-based grading management system
- [ ] Server API for enterprise deployment
- [ ] Performance analytics dashboard

### Research Opportunities
- Exploring attention mechanisms for bubble detection
- Multi-task learning for joint detection & classification
- Domain adaptation for cross-institutional datasets
- Federated learning for privacy-preserving grading
- Transformer-based architectures for sequence prediction

---

## Reference

```bibtex
@article{thuan2024omr,
  title={OMR-Exam: An Efficient Lightweight Framework for Automated Exam Scoring via YOLO26, EfficientNet, and DCGAN-Enhanced Recognition},
  author={Thuan, Le Duc and Ngan, Nguyen Thi Hong and Hoang, Nguyen Huy},
  year={2024},
  note={Research code repository}
}
```

---

**Last Updated**: 2026-07-06  
**Code Repository**: v3.0
