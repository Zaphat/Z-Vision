# App Module

This is the main application module that ties together all the features and core modules.

## Contents

- **Main Activity**: Entry point for the application
- **Application Class**: Application-wide setup and initialization
- **Navigation**: Main navigation graph connecting all features
- **DI**: Main dependency injection modules and component
- **UI**: Main screens like home, settings, and about

## Architecture

This module:
- Orchestrates the navigation between different features
- Handles application-level concerns like permissions and lifecycle
- Manages the global app state and user preferences
- Coordinates feature modules without direct dependencies between them

## Guidelines

- Keep this module lightweight
- Do not put business logic here
- Use navigation component to connect features
- Handle deep links and notifications here

## Dependencies

- All core modules
- All feature modules
- Android framework dependencies