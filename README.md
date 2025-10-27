## 📘 Extended Documentation: CommonRestClient

The `CommonRestClient` class is a lightweight, configurable REST client that provides built-in **retry**, **circuit breaker**, and **request tracing** support for Spring Boot applications.

It supports the following HTTP methods:

* `GET`
* `POST`
* `PUT`
* `DELETE`

All methods share the same resilience and tracing logic, making the client robust and easy to use across projects.

---

## 🧠 Core Features

| Feature                   | Description                                                                                       |
| ------------------------- | ------------------------------------------------------------------------------------------------- |
| **Retry Mechanism**       | Automatically retries failed requests based on configurable retry count, delay, and jitter.       |
| **Circuit Breaker**       | Prevents repeated calls to an unhealthy service by “opening” the circuit after repeated failures. |
| **Tracing (RequestSpan)** | Each request generates a unique span ID (UUID) to track request lifecycle with detailed logs.     |
| **Configurable Timeouts** | Connection and read timeouts are fully configurable.                                              |

---

## ⚙️ Configuration

You can configure the properties in your `application.yml`:

```yaml
common:
  rest:
    connection-timeout: 3000
    read-timeout: 3000

    retry:
      max-attempts: 3
      base-delay-ms: 200
      max-delay-ms: 2000
      jitter-factor: 0.2

    circuit-breaker:
      failure-threshold: 3
      open-state-duration: 5
```

* If `retry` is not configured, retries will be **disabled**.
* If `circuit-breaker` is not configured, the circuit breaker will be **disabled**.

---

## 🚀 Usage Examples

### 1️⃣ GET Request

```java
CommonRestClient client = new CommonRestClient(restClientProperties);
String url = "https://api.example.com/customers/123";

CustomerResponse response = client.get(url, CustomerResponse.class);
```

---

### 2️⃣ POST Request

Sends data to create a new resource.

```java
CustomerRequest request = new CustomerRequest("John", "Doe");
String url = "https://api.example.com/customers";

CustomerResponse response = client.post(url, request, CustomerResponse.class);
```

---

### 3️⃣ PUT Request

Updates an existing resource.

```java
CustomerRequest request = new CustomerRequest("John", "Smith");
String url = "https://api.example.com/customers/123";

CustomerResponse response = client.put(url, request, CustomerResponse.class);
```

---

### 4️⃣ DELETE Request

Deletes a resource by ID.

```java
String url = "https://api.example.com/customers/123";

DeleteResponse response = client.delete(url, DeleteResponse.class);
```

Optional variant if your DELETE endpoint expects a request body:

```java
DeleteRequest request = new DeleteRequest("force");
String url = "https://api.example.com/customers/123";

DeleteResponse response = client.delete(url, request, DeleteResponse.class);
```

---

## 🧾 Exception Mapping

| HTTP Status | Exception Type                 |
| ----------- | ------------------------------ |
| `400`       | `BadRequestException`          |
| `404`       | `NotFoundException`            |
| `500`       | `InternalServerErrorException` |
| Others      | `RemoteServiceException`       |

All exceptions include a `RemoteErrorResponse` object with:

* `statusCode`
* `statusMessage`
* `errorMessage`
* `requestUrl`

---

## 🧩 Logging & Tracing Example

Each request is automatically tagged with a **unique span ID**. Logs will appear as:

```
[SPAN: 1b4f9a2d-...-ac91] Starting request to https://api.example.com/customers/123
[SPAN: 1b4f9a2d-...-ac91] Attempt 1 calling URL: https://api.example.com/customers/123
[SPAN: 1b4f9a2d-...-ac91] Request succeeded for URL: https://api.example.com/customers/123
```

If the call fails and retries are enabled:

```
[SPAN: 1b4f9a2d-...-ac91] Attempt 1 failed, retrying after 200ms
[SPAN: 1b4f9a2d-...-ac91] Attempt 2 calling URL: https://api.example.com/customers/123
```

---

## ✅ Summary

| HTTP Method | Request Body | Response Body | Retries      | Circuit Breaker | Tracing |
| ----------- | ------------ | ------------- | ------------ | --------------- | ------- |
| GET         | ❌            | ✅             | ✅ (optional) | ✅ (optional)    | ✅       |
| POST        | ✅            | ✅             | ✅ (optional) | ✅ (optional)    | ✅       |
| PUT         | ✅            | ✅             | ✅ (optional) | ✅ (optional)    | ✅       |
| DELETE      | Optional     | ✅             | ✅ (optional) | ✅ (optional)    | ✅       |

---

### 💡 Recommendation

For best compatibility:

* Use **Java 17+**
* Use **Spring Boot 3.2.0 or later**

The library is tested on:

* Java **17**
* Spring Boot **3.2.0**
