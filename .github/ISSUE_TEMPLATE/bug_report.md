name: Bug Report
description: Report a bug or issue with the system
title: "[BUG] "
labels: ["bug"]

body:
  - type: markdown
    attributes:
      value: |
        Thanks for reporting a bug! Please provide detailed information to help us resolve it quickly.

  - type: textarea
    id: description
    attributes:
      label: Bug Description
      description: Clear description of the bug
      placeholder: "Explain what went wrong..."
    validations:
      required: true

  - type: textarea
    id: reproduction
    attributes:
      label: Steps to Reproduce
      description: Step-by-step instructions to reproduce the bug
      placeholder: |
        1. Open product/OMR_exam.ipynb
        2. Configure paths to...
        3. Run cell X
        4. Observe error...
    validations:
      required: true

  - type: textarea
    id: expected
    attributes:
      label: Expected Behavior
      description: What should happen instead?
      placeholder: "The system should..."
    validations:
      required: true

  - type: textarea
    id: actual
    attributes:
      label: Actual Behavior
      description: What actually happened?
      placeholder: "Instead, it..."
    validations:
      required: true

  - type: textarea
    id: context
    attributes:
      label: Additional Context
      description: Screenshots, error messages, logs, etc.
      placeholder: "Paste error messages or screenshots here..."

  - type: input
    id: os
    attributes:
      label: Operating System
      placeholder: "e.g., Windows 10, Ubuntu 20.04, macOS 12.0"
    validations:
      required: true

  - type: input
    id: python
    attributes:
      label: Python Version
      placeholder: "e.g., 3.9.0"
    validations:
      required: true

  - type: input
    id: packages
    attributes:
      label: Relevant Package Versions
      placeholder: "e.g., torch==1.9.0, opencv-python==4.5.0"

  - type: checkboxes
    id: checklist
    attributes:
      label: Checklist
      options:
        - label: I've searched existing issues for duplicates
          required: true
        - label: I've provided clear reproduction steps
          required: true
        - label: I've included environment details
          required: true
