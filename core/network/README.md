# Network Module

This module handles all network-related operations for the application.

## Contents

- **API Services**: Retrofit interfaces for API endpoints
- **Interceptors**: OkHttp interceptors for authentication, logging, etc.
- **DTOs**: Data Transfer Objects for network responses and requests
- **Error Handling**: Centralized error handling for network requests
- **Connection Monitoring**: Network connection status monitoring
- **DI**: Dependency injection modules for network components

## Architecture

This module should:
- Provide a clean API for making network requests
- Handle common concerns like authentication, retries, and caching
- Isolate network-specific logic from the rest of the application

## Dependencies

- `:core:common` - For utilities and extensions