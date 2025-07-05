# Testing Module

This module contains testing utilities, fakes, and test doubles for the application.

## Contents

- **Fakes**: Fake implementations of repositories and data sources
- **Test Doubles**: Mock and stub implementations for testing
- **Test Rules**: JUnit rules for testing
- **Test Data**: Sample data for tests
- **Test Utils**: Utilities for writing and running tests

## Usage

This module should be used as a dependency for the `test` or `androidTest` configurations of other modules. It provides common testing infrastructure for consistent testing across modules.

## Guidelines

- Create fake implementations that don't depend on external services
- Provide test data factories for generating test data
- Create test rules for common test setup and teardown

## Dependencies

- `:core:common` - For utilities and extensions
- `:core:domain` - For domain models and interfaces