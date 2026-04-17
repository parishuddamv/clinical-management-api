# Frontend Auth Integration (Bearer Token)

Use this when calling protected APIs such as `/api/v1/patients/**` through the gateway.

## 1) Store token after login

```javascript
// Example: after successful auth response
localStorage.setItem("authToken", token);
```

## 2) Fetch wrapper (recommended)

```javascript
const API_BASE = "http://localhost:8080";

function withAuthHeaders(headers = {}) {
  const token = localStorage.getItem("authToken");
  if (!token) {
    return { ...headers };
  }
  return {
    ...headers,
    Authorization: `Bearer ${token}`,
  };
}

export async function apiFetch(path, options = {}) {
  const isProtected = path.startsWith("/api/v1/patients/");

  const requestOptions = {
    ...options,
    headers: isProtected
      ? withAuthHeaders(options.headers || {})
      : (options.headers || {}),
  };

  const response = await fetch(`${API_BASE}${path}`, requestOptions);

  if (response.status === 401) {
    // Token missing/expired/invalid: clear and redirect to login
    localStorage.removeItem("authToken");
    window.location.href = "/login";
    throw new Error("Unauthorized");
  }

  return response;
}
```

## 3) Usage example

```javascript
import { apiFetch } from "./apiClient";

async function searchPatients() {
  const response = await apiFetch("/api/v1/patients/search?q=a&page=0&size=20", {
    method: "GET",
  });

  const data = await response.json();
  return data;
}
```

## 4) Axios interceptor option

```javascript
import axios from "axios";

const api = axios.create({
  baseURL: "http://localhost:8080",
});

api.interceptors.request.use((config) => {
  const path = config.url || "";
  const isProtected = path.startsWith("/api/v1/patients/");
  if (isProtected) {
    const token = localStorage.getItem("authToken");
    if (token) {
      config.headers = {
        ...(config.headers || {}),
        Authorization: `Bearer ${token}`,
      };
    }
  }
  return config;
});

api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error?.response?.status === 401) {
      localStorage.removeItem("authToken");
      window.location.href = "/login";
    }
    return Promise.reject(error);
  }
);

export default api;
```

## Notes

- `GET /api/v1/patients/search` requires `q` query param.
- Without a token, protected routes correctly return `401`.
- With token but missing `q`, the endpoint should return `400`.

