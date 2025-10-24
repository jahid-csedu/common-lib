# Common REST Client Library

A reusable Spring Boot library that simplifies calling remote REST services with **configurable resilience features** — **retry** and **circuit breaker**.
Both are **optional**, meaning users can enable either, both, or none depending on their needs.

---

## 🧱 Project Structure

```
com/example/commonlib/
 ├─ config/
 │   ├─ RestClientProperties.java
 │   ├─ RetryProperties.java
 │   ├─ CircuitBreakerProperties.java
 │   ├─ CommonRestAutoConfiguration.java
 ├─ client/
 │   ├─ RetryExecutor.java
 │   ├─ CircuitBreaker.java
 │   └─ CommonRestClient.java
 ├─ exception/
 │   ├─ RemoteServiceException.java
 │   ├─ BadRequestException.java
 │   ├─ NotFoundException.java
 │   ├─ InternalServerErrorException.java
 └─ model/
     └─ RemoteErrorResponse.java
```

---

## ⚙️ Configuration

All configuration is provided via Spring Boot’s `application.yml` (or `.properties`) under the prefix `common.rest`.

### Example Configuration

```yaml
rest:
  client:
    connection-timeout: 2000   # Connection timeout in milliseconds
    read-timeout: 2000         # Read timeout in milliseconds

    # Optional Retry Configuration
    retry:
      max-attempts: 3
      base-delay-ms: 200
      max-delay-ms: 2000
      jitter-factor: 0.2

    # Optional Circuit Breaker Configuration
    circuit-breaker:
      failure-threshold: 5
      open-duration-ms: 30000
```

### Configuration Notes

| Property             | Description                                                          | Default             |
| -------------------- | -------------------------------------------------------------------- | ------------------- |
| `connection-timeout` | Connection timeout in ms                                             | 5000                |
| `read-timeout`       | Read timeout in ms                                                   | 5000                |
| `retry-properties`   | Optional retry config (enables retry if present)                     | Disabled if not set |
| `circuit-breaker`    | Optional circuit breaker config (enables circuit breaker if present) | Disabled if not set |

---

## 🪄 Behavior Matrix

| Configured Components   | Behavior                                                        |
| ----------------------- | --------------------------------------------------------------- |
| Only `retry-properties` | Retries failed requests with exponential backoff                |
| Only `circuit-breaker`  | Skips requests if the circuit is open                           |
| Both                    | Retries requests, and trips circuit breaker on repeated failure |
| None                    | Performs a direct REST call with no resilience logic            |

---

## 🧩 Usage Example

```java
import com.example.commonlib.client.CommonRestClient;
import org.springframework.stereotype.Service;

@Service
public class ExampleService {

    private final CommonRestClient restClient;

    public ExampleService(CommonRestClient restClient) {
        this.restClient = restClient;
    }

    public MyResponse getUserData(String userId) throws Exception {
        String url = "https://api.example.com/users/" + userId;
        return restClient.get(url, MyResponse.class);
    }
}
```

When used:

* Retries and circuit breaking apply **automatically** based on your configuration.
* If both are disabled, it performs a simple HTTP GET call.

---

## 🚨 Exception Handling

All remote call failures are wrapped into one of the following custom exceptions:

* `BadRequestException` → HTTP 400
* `NotFoundException` → HTTP 404
* `InternalServerErrorException` → HTTP 500
* `RemoteServiceException` → Other unexpected or network-related errors

Each exception contains a `RemoteErrorResponse` with details:

```json
{
  "status": 500,
  "error": "Remote Service Error",
  "message": "Timeout occurred",
  "path": "https://api.example.com/users/123"
}
```

---

## 🧠 Design Highlights

* Clean separation of **configuration**, **resilience**, and **exception mapping**
* Compatible with Spring Boot’s `@ConfigurationProperties`
* Supports **pluggable** retry and circuit breaker logic
* Production-ready logging and error model

---

## 🧰 Future Enhancements

* Add metrics integration (Micrometer)
* Support for POST, PUT, DELETE with the same resilience layer
* Option to configure time-based or error-rate-based circuit breaking

---

## 🏁 Summary

This library provides a **plug-and-play**, **configurable**, and **safe** way to call remote REST APIs with optional resilience.
Simply include it as a dependency, define your YAML properties, and inject `CommonRestClient` — it handles the rest!
