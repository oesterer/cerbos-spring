# Cerbos Spring Boot Demo

This project shows how to wire the [Cerbos Java SDK](https://github.com/cerbos/cerbos-sdk-java) into a Spring Boot
application so both HTTP requests and business-layer methods are authorised by a Cerbos Policy Decision Point (PDP).

## Architecture

- **Cerbos client configuration** – `CerbosClientConfiguration` bootstraps a `CerbosBlockingClient` using typed
  properties (`cerbos.pdp.*`). Plaintext and insecure TLS modes plus custom timeouts and Playground targets are
  supported out of the box.
- **HTTP-level guard** – `CerbosAuthorizationService` still maps `Authentication` + `HttpServletRequest` data to Cerbos
  principals/resources so the custom `CerbosAuthorizationManager` can protect the servlet layer.
- **Business services** – `DocumentService` encapsulates domain logic for reading and creating documents, delegating to
  a simple in-memory `DocumentRepository`.
- **Method security via Cerbos** – `CerbosMethodAuthorizer` exposes bean methods that invoke Cerbos; `DocumentService`
  uses `@PreAuthorize` and `@PostAuthorize` to call those methods, guaranteeing Cerbos signs off either before or after
  the business logic executes.
- **REST controllers** – `DocumentController` delegates to the service layer, while `PublicController` exposes a Cerbos-
  free endpoint for comparison.
- **Tests** – Mockito-based unit tests exercise the HTTP authorisation components, and `SecurityIntegrationTest` drives
  MockMvc through allow/deny flows covering both HTTP-level and method-level Cerbos checks.

## Authorisation scenarios demonstrated

1. **Public resource** – `GET /public/info` bypasses Cerbos and always succeeds.
2. **HTTP-level allow/deny** – `CerbosAuthorizationManager` queries Cerbos before the request reaches the controller.
3. **Method-level pre-check** – `@PreAuthorize` on `DocumentService#createDocument` calls Cerbos to vet create
   operations before any data is persisted.
4. **Method-level post-check** – `@PostAuthorize` on `DocumentService#readDocument` asks Cerbos to approve the returned
   document, enabling row-level decisions once business attributes are known.
5. **Failure handling** – network/runtime errors from the PDP are surfaced as `AccessDeniedException`, keeping responses
   secure-by-default.

## Running the application

1. Ensure a Cerbos PDP is reachable at `cerbos.pdp.target` (defaults to `localhost:3593`). A Playground endpoint can be
   used instead by pointing the property at the generated host:port.
2. Start the app:
   ```bash
   mvn spring-boot:run
   ```
3. Exercise the endpoints:
   ```bash
   curl -i http://localhost:8080/public/info
   curl -i -u alice:password http://localhost:8080/documents/alpha
   curl -i -u alice:password -H 'Content-Type: application/json' \
     -d '{"documentId":"proposal","content":{"title":"Proposal"}}' \
     http://localhost:8080/documents
   ```
   Successful Cerbos evaluations return `200 OK` for reads or `201 Created` for writes; denied actions result in `403 Forbidden`.

## Customising

- Tweak `application.yml` for PDP settings, HTTP method→action mapping, or to point at a different Cerbos instance.
- Replace the in-memory users in `SecurityConfiguration` with your own `UserDetailsService` implementation.
- Extend `CerbosMethodAuthorizer` to add more domain-specific checks (e.g., update/delete actions, additional resource
  attributes).
- Swap the in-memory `DocumentRepository` for a database-backed implementation; the method security stays unchanged.

## Running tests

```bash
mvn test
```
(First run requires Internet access so Maven can download dependencies from Maven Central.)

## License

This example is provided without an explicit license; adapt it to your needs.
