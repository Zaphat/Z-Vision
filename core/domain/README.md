# Domain Module

This module contains the business logic of the application, independent of the UI or data sources.

## Contents

- **Models**: Domain models representing business entities
- **Repository Interfaces**: Contracts for data operations
- **Use Cases**: Interactors that encapsulate business logic
- **Exceptions**: Custom exceptions for domain-specific errors
- **Validation**: Business rules for input validation

## Architecture

This module follows Clean Architecture principles:
- It has no dependencies on other modules except `:core:common`
- It defines interfaces that are implemented by other modules
- It contains pure Kotlin code with no Android framework dependencies

## Use Cases

Create use cases that represent single operations, such as:
- `ScanQrCodeUseCase`
- `CreateQrCodeUseCase`
- `RecognizeTextUseCase`
- `TranslateTextUseCase`

## Dependencies

- `:core:common` - For utilities and extensions