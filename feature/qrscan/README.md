# QR Scan Feature Module

This module handles the QR code scanning functionality of the application.

## Contents

- **UI**: 
  - Screens for camera scanning and image selection
  - ViewModels for managing UI state
  - State holders for UI state representation
- **Navigation**: Navigation components for this feature
- **DI**: Dependency injection modules for this feature
- **Utils**: QR code scanning specific utilities

## Features

- Scan QR codes using the device camera
- Import and scan QR codes from images in the gallery
- History of scanned QR codes
- Process and validate different QR code formats

## Architecture

This module follows MVVM architecture:
- UI layer with Compose UI components
- ViewModel layer for business logic and state management
- Domain layer interactions through use cases

## Dependencies

- `:core:common` - For utilities and extensions
- `:core:design` - For UI components and theming
- `:core:domain` - For use cases and domain models
- CameraX - For camera functionality
- ML Kit or ZXing - For QR code processing