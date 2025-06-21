# OCR and Translation Feature Module

This module handles the Optical Character Recognition (OCR) and text translation functionality of the application.

## Contents

- **UI**:
  - Screens for capturing text from camera and images
  - Text translation interface
  - ViewModels for managing UI state
  - State holders for UI state representation
- **Navigation**: Navigation components for this feature
- **DI**: Dependency injection modules for this feature
- **Utils**: OCR and translation specific utilities

## Features

- Capture text from:
  - Camera (real-time)
  - Existing images
- Recognize text in multiple languages
- Translate recognized text between different languages
- Copy, share, and save recognized/translated text
- History of scanned and translated text

## Architecture

This module follows MVVM architecture:
- UI layer with Compose UI components
- ViewModel layer for business logic and state management
- Domain layer interactions through use cases

## Dependencies

- `:core:common` - For utilities and extensions
- `:core:design` - For UI components and theming
- `:core:domain` - For use cases and domain models
- `:core:network` - For translation API calls
- ML Kit OCR or Tesseract - For text recognition
- Translation API integration (Google Translate, DeepL, etc.)