# 🧰 Common REST Client Library

A lightweight, reusable **Spring Boot library** that provides resilient REST API communication features for microservices.
It centralizes commonly used configurations like **timeouts**, **retries with exponential backoff and jitter**, and **circuit breaker** handling — so that all your microservices can use the same consistent and robust setup.

---

## 🚀 Features

✅ **Centralized Configuration**

* Define timeout, retry, and circuit breaker settings in `application.yml`
* Automatically bound via `@ConfigurationProperties`

✅ **Retry with Exponential Backoff and Jitter**

* Retries failed requests with a growing delay between attempts
* Adds jitter (random variation) to avoid thundering herd issues

✅ **Circuit Breaker**

* Opens the circuit after a configurable number of consecutive failures
* Automatically transitions from `OPEN → HALF_OPEN → CLOSED` based on elapsed time

✅ **Fine-Grained Error Handling**

* Converts remote API error responses into structured `RemoteErrorResponse` objects
* Throws meaningful exceptions like `BadRequestException`, `NotFoundException`, and `InternalServerErrorException`

✅ **Logging**

* Logs all retry attempts, circuit breaker state changes, and remote call failures

---

## 🏗️ Project Structure

```
com/example/commonlib/
 ├─ config/
 │   ├─ RestClientProperties.java
 │   ├─ RetryProperties.java
 │   ├─ CircuitBreakerProperties.java
 │   ├─ CommonRestAutoConfiguration.java
 ├─ client/
 │   ├─ RetryExecutor.java
 │   ├─ CommonRestClient.java
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

Add the following properties to your **microservice’s** `application.yml` (or override them as needed):

```yaml
common:
  rest:
    connection-timeout: 5000      # in milliseconds
    read-timeout: 5000            # in milliseconds

    retry-properties:
      max-attempts: 3             # number of retry attempts
      base-delay-ms: 200          # base delay in milliseconds
      max-delay-ms: 2000          # maximum delay cap in milliseconds
      jitter-factor: 0.2          # ±20% random variation

    circuit-breaker-properties:
      failure-threshold: 5        # failures before circuit opens
      open-state-duration: 30     # open state duration in seconds
```

All values are optional — the library provides sensible defaults.

---

## 🧩 Usage in a Microservice

Once the library is added as a Maven dependency (see below), you can inject and use `CommonRestClient` in any Spring component.

### Example:

```java
@Service
public class ProductService {

    private final CommonRestClient restClient;

    public ProductService(CommonRestClient restClient) {
        this.restClient = restClient;
    }

    public ProductDto getProductById(Long id) {
        String url = "http://inventory-service/api/products/" + id;
        return restClient.get(url, ProductDto.class);
    }
}
```

---

## 🧾 Maven Dependency

Once published to a repository (e.g., Maven Central or GitHub Packages), add the dependency:

```xml
<dependency>
    <groupId>com.example</groupId>
    <artifactId>common-rest-lib</artifactId>
    <version>1.0.0</version>
</dependency>
```

If it’s a local JAR (for now), install it to your local Maven repo:

```bash
mvn clean install
```

Then include it in other projects as a normal dependency.

---

## 🧠 Key Concepts

### RetryExecutor

* Implements **exponential backoff with jitter**
* Ensures requests are retried gracefully without overloading the target service

### CircuitBreaker

* Protects your system from cascading failures
* Uses a **failure threshold** and **open-state duration** to control the flow

### RemoteErrorResponse

* Standardized structure for representing error responses from remote APIs

---

## 🧩 Example RemoteErrorResponse

```json
{
  "timestamp": "2025-10-24T08:32:00Z",
  "status": 404,
  "error": "Not Found",
  "message": "Product not found",
  "path": "/api/products/99"
}
```

---

## 🔮 Future Enhancements

* Add **asynchronous support** using `WebClient`
* Integrate **Micrometer metrics** for monitoring retry and circuit breaker stats
* Optional integration with **Resilience4j** for advanced users
* Add **bulkhead pattern** and **rate limiting**

---

## 🧑‍💻 Contributing

1. Fork this repository
2. Create your feature branch: `git checkout -b feature/awesome-feature`
3. Commit your changes: `git commit -m 'Add awesome feature'`
4. Push to the branch: `git push origin feature/awesome-feature`
5. Create a new Pull Request

---
