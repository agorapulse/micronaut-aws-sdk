# Micronaut AWS SDK Development Guide

## Build Commands
- `./gradlew build` - Full build with tests
- `./gradlew test` - Run all tests
- `./gradlew test --tests "com.agorapulse.micronaut.amazon.awssdk.dynamodb.DynamoDBServiceTest"` - Run a specific test class
- `./gradlew check` - Run all verification tasks (tests, checkstyle, codenarc)
- `./gradlew checkstyleMain checkstyleTest` - Run checkstyle for Java files
- `./gradlew codenarcMain codenarcTest` - Run CodeNarc for Groovy files
- `./gradlew clean` - Clean build artifacts
- `./gradlew :subprojects:micronaut-amazon-awssdk-dynamodb:test` - Run tests for a specific subproject
- `./gradlew publishToMavenLocal` - Publish artifacts to local Maven repository

## Code Style Guidelines
- **Java/Groovy**: Follow checkstyle/codenarc rules in config directory
- **Naming**: 
  - CamelCase for classes (e.g., `DynamoDBService`)
  - camelCase for methods/variables (e.g., `findById`)
  - UPPER_CASE for constants (e.g., `DEFAULT_REGION`)
- **Type Parameters**: Use single uppercase letters (e.g., `<T>`, `<E>`)
- **Max Line Length**: 160 characters
- **Imports**: No wildcard imports except for static imports
- **Braces**: Required for all control structures, opening brace on same line
- **Indentation**: Use 4 spaces, not tabs
- **Tests**: 
  - Use Spock framework for Groovy tests, suffix test classes with "Spec"
  - Use JUnit 5 for Java tests, suffix test classes with "Test"
- **CompileStatic**: Apply @CompileStatic to all Groovy classes except tests
- **Error Handling**: Use specific exceptions, avoid empty catch blocks

## Project Structure
- **subprojects/**: Contains all modules including AWS service integrations
  - **micronaut-aws-sdk-***: AWS SDK v1.x modules (legacy)
  - **micronaut-amazon-awssdk-***: AWS SDK v2.x modules
- **platforms/**: Contains BOM and dependency management
- **docs/**: Contains project documentation including guide
- **examples/**: Contains example applications
- **benchmarks/**: Contains performance benchmarks

## AWS Services Integration
This library integrates with multiple AWS services including:
- DynamoDB - Database operations with declarative annotations
- S3 - Object storage operations
- SQS - Message queue operations
- SNS - Notification service operations
- SES - Email service operations
- Kinesis - Data streaming operations
- Lambda - Serverless function invocation
- CloudWatch - Logging and monitoring

## Design Patterns
- **Factory Pattern**: Each service has a factory for creation (e.g., `DynamoDBFactory`)
- **Builder Pattern**: Fluent builders for queries (e.g., DynamoDB query/scan/update)
- **Declarative APIs**: Annotation-based service interfaces (`@Service`, `@LambdaClient`, etc.)
- **Configuration**: YAML-based configuration with Micronaut properties
- **Dependency Injection**: Constructor or field-based using Micronaut DI

## Testing Patterns
- **Spock Framework**: Used for specification-style testing
- **Mocking**: Service interfaces designed for easy mocking
- **LocalStack**: Integration tests with simulated AWS services
- **Playbook Pattern**: Track service lifecycle events in tests
- **Consistent Naming**: Use "Spec" suffix for test classes

## Best Practices
- Use dependency injection via constructor or field annotations
- Follow AWS SDK v1/v2 patterns for service-specific implementations
- Provide both declarative (annotation-based) and imperative APIs
- Include comprehensive unit tests with good coverage
- Use Micronaut configuration properties for configuring services
- Use reactive programming where appropriate (Reactor, Flux, Publisher)