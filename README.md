# Cerbos Spring Boot Demo

This project shows how to wire the [Cerbos Java SDK](https://github.com/cerbos/cerbos-sdk-java) into a Spring Boot
application so every protected HTTP request is authorized by a Cerbos Policy Decision Point (PDP).

## What the sample contains

- **Cerbos client configuration** — `CerbosClientConfiguration` bootstraps a `CerbosBlockingClient` from typed
  properties (`cerbos.pdp.*`). It supports plaintext, insecure TLS, custom timeouts and Cerbos Playground targets.
- **HTTP request to Cerbos mapping** — `CerbosAuthorizationService` converts Spring Security `Authentication` and the
  current `HttpServletRequest` into Cerbos principals/resources, attaches rich attributes (roles, headers, path
  segments, etc.), calls the Cerbos PDP and translates the decision back to Spring.
- **Custom AuthorizationManager** — `CerbosAuthorizationManager` plugs Cerbos into Spring Security’s authorization
  pipeline so all protected routes defer to the PDP.
- **Security setup** — `SecurityConfiguration` protects every endpoint except `/public/**` and `/actuator/health`, keeps
  an in-memory user store (`alice`/`password`, `bob`/`password`) and enables both form login and HTTP Basic.
- **REST controllers** — `DocumentController` (protected) and `PublicController` (public) provide concrete URLs you can
  exercise.
- **Tests** — focused unit tests for the authorization service/manager plus a MockMvc integration test that demonstrates
  allow/deny flows with mocked Cerbos responses.

## Authorization scenarios demonstrated

1. **Public resources** (`GET /public/info`) bypass Cerbos and are always allowed.
2. **Protected read** (`GET /documents/{id}`) — Cerbos evaluates whether the caller can perform a `read` action on the
   HTTP resource identified by the request URI.
3. **Protected create** (`POST /documents`) — Cerbos checks the `create` action before the request body is processed.
4. **Cerbos allow** — when the PDP grants the action, the controller executes and returns business data.
5. **Cerbos deny** — the authorization manager converts a PDP denial into HTTP 403 without entering the controller.
6. **Cerbos failure** — network/runtime errors from the PDP surface as 403 responses via `AccessDeniedException`, keeping
   the app secure by default.

## Running the application

1. Make sure a Cerbos PDP is reachable at the address in `src/main/resources/application.yml` (default
   `localhost:3593`). You can swap in a Playground endpoint or change the host/port via
   `--cerbos.pdp.target=<host:port>`.
2. Start the app:
   ```bash
   mvn spring-boot:run
   ```
3. Test endpoints:
   ```bash
   curl -i http://localhost:8080/public/info
   curl -i -u alice:password http://localhost:8080/documents/alpha
   ```
   Expect `200 OK` for public routes. Protected routes return `200 OK` when Cerbos allows the action or `403 Forbidden`
   otherwise.

## Customising

- Update `application.yml` to change PDP target, resource kind and HTTP method→action mapping.
- Replace the in-memory users in `SecurityConfiguration` with your own `UserDetailsService`.
- Extend `CerbosAuthorizationService` if you need additional principal/resource attributes (e.g. organisation IDs,
  feature flags, request body fields).
- For batch or plan queries, call `client.batch(...)`/`client.plan(...)` inside the authorization service the same way.

## Running tests

The test suite uses Mockito to mock the Cerbos client and MockMvc for security flows:
```bash
mvn test
```
(First run requires internet access so Maven can download dependencies.)

## License

This example is provided without an explicit license; adapt it to your needs.
=======
