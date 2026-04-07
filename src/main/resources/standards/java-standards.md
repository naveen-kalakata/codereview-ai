## Dependency Injection
Always use constructor injection, never field injection with @Autowired. Constructor injection makes dependencies explicit, supports immutability with final fields, and makes unit testing easier since you can pass mocks directly.

## Null Handling
Never return null from a method. Use Optional<T> for methods that may not return a value. This forces the caller to handle the absent case explicitly instead of risking NullPointerException.

## Exception Handling
Never catch generic Exception. Catch specific exception types. Use @ControllerAdvice for global exception handling in REST APIs. Always log the exception before rethrowing or returning an error response.

## REST API Design
All REST controller methods must return ResponseEntity<T> to allow explicit control over HTTP status codes. Use proper HTTP methods: GET for reads, POST for creates, PUT for full updates, PATCH for partial updates, DELETE for removals.

## Logging
Use SLF4J with parameterized messages. Never concatenate strings in log statements. Use log.info("User {} logged in", userId) instead of log.info("User " + userId + " logged in"). The parameterized version avoids string concatenation when the log level is disabled.

## Security
Never hardcode passwords, API keys, or secrets in source code. Use environment variables or external configuration (application.properties, Vault, AWS Secrets Manager). Never log sensitive data like passwords, tokens, or personal information.

## Testing
Every service class must have corresponding unit tests. Use Mockito to mock dependencies. Test both happy path and error scenarios. Aim for 80% code coverage minimum.

## Code Style
Keep methods under 30 lines. If a method exceeds this, extract helper methods. Each method should do one thing. Use meaningful variable names — avoid single letters except in loops (i, j, k).

## Database
Always use parameterized queries. Never concatenate user input into SQL strings. Use Spring Data JPA Specifications for dynamic queries instead of building SQL with string concatenation.

## Collections
Prefer List.of(), Map.of(), Set.of() for immutable collections. Use Stream API for transformations but avoid overly complex stream chains — if it takes more than 3 operations, extract to a method with a descriptive name.
