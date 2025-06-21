# Data Module

This module is responsible for all data operations in the application, following the repository pattern.

## Contents

- **Repositories**: Implementation of repository interfaces defined in the domain module
- **Data Sources**: 
  - **Local**: Room database, DataStore, SharedPreferences
  - **Remote**: API service implementations
- **Models**: Data models and mappers to convert between data and domain models
- **DI**: Dependency injection modules for data sources and repositories

## Architecture

This module implements the repository pattern and follows these principles:
- Repositories coordinate data from different data sources
- Data sources are responsible for fetching data from a single source
- Models are mapped between layers to ensure separation of concerns

## Dependencies

- `:core:common` - For utilities and extensions
- `:core:network` - For remote API interactions
- `:core:domain` - For repository interfaces and domain models