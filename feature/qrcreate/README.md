# QR Create Feature Module

This module handles the QR code creation functionality of the application.

## Contents

- **UI**:
  - Screens for creating and customizing QR codes
  - ViewModels for managing UI state
  - State holders for UI state representation
- **Navigation**: Navigation components for this feature
- **DI**: Dependency injection modules for this feature
- **Utils**: QR code generation specific utilities

## Features

- Create QR codes for different content types:
  - URLs
  - Text
  - Contact information
  - Wi-Fi credentials
  - etc.
- Customize QR code appearance (colors, size, error correction level)
- Save generated QR codes to the device
- Share generated QR codes

## Architecture

This module follows MVVM architecture:
- UI layer with Compose UI components
- ViewModel layer for business logic and state management
- Domain layer interactions through use cases

## Dependencies

- `:core:common` - For utilities and extensions
- `:core:design` - For UI components and theming
- `:core:domain` - For use cases and domain models
- ZXing or other QR code generation library