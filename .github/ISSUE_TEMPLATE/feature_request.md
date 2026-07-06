name: Feature Request
description: Suggest a new feature or enhancement
title: "[FEATURE] "
labels: ["enhancement"]

body:
  - type: markdown
    attributes:
      value: |
        Thanks for suggesting a feature! Please provide details about your idea.

  - type: textarea
    id: motivation
    attributes:
      label: Motivation
      description: Why do we need this feature? What problem does it solve?
      placeholder: "Currently, the system... would be better if..."
    validations:
      required: true

  - type: textarea
    id: description
    attributes:
      label: Feature Description
      description: Detailed description of the proposed feature
      placeholder: "This feature would..."
    validations:
      required: true

  - type: textarea
    id: use_case
    attributes:
      label: Use Case
      description: How would this feature be used?
      placeholder: |
        Example: 
        1. User loads...
        2. Selects...
        3. System outputs...
    validations:
      required: true

  - type: textarea
    id: alternatives
    attributes:
      label: Alternatives Considered
      description: Any alternative approaches?
      placeholder: "Other approaches could include..."

  - type: textarea
    id: additional
    attributes:
      label: Additional Context
      description: Links to research papers, similar projects, designs, etc.
      placeholder: "Related: https://..."

  - type: checkboxes
    id: checklist
    attributes:
      label: Checklist
      options:
        - label: I've searched existing issues for similar requests
          required: true
        - label: I've provided clear use cases
          required: true
        - label: This aligns with project goals
          required: true
