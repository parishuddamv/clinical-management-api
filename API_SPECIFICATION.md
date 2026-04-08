# ClinicOS API Specification v1.0

## Base URL

```
Local Development: http://localhost:8080/api/v1
Production: https://clinic-gateway.run.app/api/v1
```

## Authentication

All endpoints (except `/auth/*`) require JWT authentication:

```
Authorization: Bearer <JWT_TOKEN>
```

JWT Token Format:
```json
{
  "sub": "user@clinic.com",
  "clinicId": "CLINIC_001",
  "iat": 1712486400,
  "exp": 1712572800
}
```

## Response Format

All endpoints return standardized JSON response:

```json
{
  "success": true/false,
  "message": "Success message or error description",
  "data": { /* response data */ },
  "timestamp": 1712486400000
}
```

## Error Codes

| Code | Status | Meaning |
|------|--------|---------|
| 200 | OK | Successful GET/PUT request |
| 201 | Created | Successful POST request |
| 400 | Bad Request | Invalid request parameters |
| 401 | Unauthorized | Missing or invalid JWT token |
| 404 | Not Found | Resource not found |
| 409 | Conflict | Duplicate resource |
| 500 | Internal Server Error | Server error |

## Patient Endpoints

### Register Patient

**Endpoint:** `POST /patients`

**Authentication:** Required

**Request:**
```json
{
  "firstName": "Rajesh",
  "lastName": "Kumar",
  "phone": "9876543210",
  "dateOfBirth": "1990-01-15",
  "gender": "MALE",
  "bloodGroup": "O+",
  "address": "123 Main St, Delhi",
  "emergencyContactName": "Priya Kumar",
  "emergencyContactPhone": "9876543211"
}
```

**Validation Rules:**
- firstName: 1-100 characters, required
- lastName: 1-100 characters, required
- phone: Valid 10-digit Indian number (6-9), unique per clinic
- dateOfBirth: Past or present date only
- gender: MALE, FEMALE, or OTHER
- bloodGroup: O+, O-, A+, A-, B+, B-, AB+, AB-
- address: 0-500 characters
- emergencyContactPhone: Valid 10-digit number or empty

**Response (201 Created):**
```json
{
  "success": true,
  "message": "Patient registered successfully",
  "data": {
    "id": 1,
    "firstName": "Rajesh",
    "lastName": "Kumar",
    "phone": "9876543210",
    "dateOfBirth": "1990-01-15",
    "gender": "MALE",
    "bloodGroup": "O+",
    "address": "123 Main St, Delhi",
    "emergencyContactName": "Priya Kumar",
    "emergencyContactPhone": "9876543211",
    "isActive": true,
    "tags": [],
    "createdAt": "2026-04-07T10:30:00",
    "updatedAt": "2026-04-07T10:30:00"
  },
  "timestamp": 1712486400000
}
```

**Error Responses:**

Duplicate Phone (409):
```json
{
  "success": false,
  "message": "Patient with phone '9876543210' already exists",
  "data": null,
  "timestamp": 1712486400000
}
```

Validation Error (400):
```json
{
  "success": false,
  "message": "Validation failed",
  "data": {
    "phone": "Phone must be a valid 10-digit Indian number",
    "dateOfBirth": "Date of birth cannot be in future"
  },
  "timestamp": 1712486400000
}
```

---

### Get Patient by ID

**Endpoint:** `GET /patients/{id}`

**Authentication:** Required

**Path Parameters:**
- `id` (Long, required): Patient ID

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Success",
  "data": {
    "id": 1,
    "firstName": "Rajesh",
    "lastName": "Kumar",
    "phone": "9876543210",
    "dateOfBirth": "1990-01-15",
    "gender": "MALE",
    "bloodGroup": "O+",
    "address": "123 Main St, Delhi",
    "emergencyContactName": "Priya Kumar",
    "emergencyContactPhone": "9876543211",
    "isActive": true,
    "tags": ["diabetic", "hypertensive"],
    "createdAt": "2026-04-07T10:30:00",
    "updatedAt": "2026-04-07T10:30:00"
  },
  "timestamp": 1712486400000
}
```

**Error Response (404):**
```json
{
  "success": false,
  "message": "Patient not found: ID: 1",
  "data": null,
  "timestamp": 1712486400000
}
```

---

### Update Patient

**Endpoint:** `PUT /patients/{id}`

**Authentication:** Required

**Path Parameters:**
- `id` (Long, required): Patient ID

**Request:** (All fields optional for partial updates)
```json
{
  "firstName": "Rajesh",
  "lastName": "Kumar",
  "phone": "9876543210",
  "dateOfBirth": "1990-01-15",
  "gender": "MALE",
  "bloodGroup": "O+",
  "address": "123 Main St, Bangalore",
  "emergencyContactName": "Priya Kumar",
  "emergencyContactPhone": "9876543211",
  "isActive": true
}
```

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Patient updated successfully",
  "data": { /* updated patient data */ },
  "timestamp": 1712486400000
}
```

---

### Delete Patient

**Endpoint:** `DELETE /patients/{id}`

**Authentication:** Required

**Path Parameters:**
- `id` (Long, required): Patient ID

**Description:** Soft delete - sets isActive to false

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Patient deleted successfully",
  "data": null,
  "timestamp": 1712486400000
}
```

---

### Search Patients

**Endpoint:** `GET /patients/search`

**Authentication:** Required

**Query Parameters:**
- `q` (String, required): Search query (name or phone)
- `page` (Integer, default: 0): Page number (0-indexed)
- `size` (Integer, default: 20): Results per page

**Example Request:**
```
GET /patients/search?q=rajesh&page=0&size=20
```

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Success",
  "data": {
    "content": [
      {
        "id": 1,
        "firstName": "Rajesh",
        "lastName": "Kumar",
        "phone": "9876543210",
        "gender": "MALE",
        "isActive": true,
        "createdAt": "2026-04-07T10:30:00"
      },
      {
        "id": 2,
        "firstName": "Rajeshwar",
        "lastName": "Singh",
        "phone": "9876543211",
        "gender": "MALE",
        "isActive": true,
        "createdAt": "2026-04-07T10:31:00"
      }
    ],
    "pageable": {
      "pageNumber": 0,
      "pageSize": 20,
      "totalElements": 2,
      "totalPages": 1
    },
    "last": true,
    "first": true
  },
  "timestamp": 1712486400000
}
```

---

### Add Patient Tag

**Endpoint:** `POST /patients/{id}/tags`

**Authentication:** Required

**Path Parameters:**
- `id` (Long, required): Patient ID

**Request:**
```json
{
  "tag": "diabetic"
}
```

**Valid Tags:**
- `diabetic` - Patient has diabetes
- `hypertensive` - Patient has hypertension
- `cardiac` - Patient has cardiac condition
- `asthmatic` - Patient has asthma
- `regular` - Regular/frequent patient
- `vip` - VIP patient
- Custom tags allowed

**Response (201 Created):**
```json
{
  "success": true,
  "message": "Tag added successfully",
  "data": null,
  "timestamp": 1712486400000
}
```

**Error Response (409):**
```json
{
  "success": false,
  "message": "Tag already exists for this patient",
  "data": null,
  "timestamp": 1712486400000
}
```

---

### Remove Patient Tag

**Endpoint:** `DELETE /patients/{id}/tags/{tag}`

**Authentication:** Required

**Path Parameters:**
- `id` (Long, required): Patient ID
- `tag` (String, required): Tag name to remove

**Example Request:**
```
DELETE /patients/1/tags/diabetic
```

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Tag removed successfully",
  "data": null,
  "timestamp": 1712486400000
}
```

---

## Appointment Endpoints

### Create Appointment

**Endpoint:** `POST /appointments`

**Authentication:** Required

**Request:**
```json
{
  "patientId": 1,
  "appointmentDateTime": "2026-04-15T14:30:00",
  "doctorName": "Dr. Sharma",
  "notes": "Regular checkup"
}
```

**Response (201 Created):**
```json
{
  "success": true,
  "message": "Appointment created successfully",
  "data": {
    "id": 1,
    "patientId": 1,
    "appointmentDateTime": "2026-04-15T14:30:00",
    "status": "SCHEDULED",
    "doctorName": "Dr. Sharma",
    "notes": "Regular checkup",
    "createdAt": "2026-04-07T10:30:00"
  },
  "timestamp": 1712486400000
}
```

---

## Health Check

**Endpoint:** `GET /health`

**Authentication:** Not required

**Response:**
```json
{
  "status": "UP",
  "components": {
    "db": {
      "status": "UP"
    }
  }
}
```

---

## Rate Limiting

Requests are rate-limited at gateway level:

- **Default:** 100 requests per minute per clinic
- **Header:** `X-RateLimit-Remaining` shows remaining requests
- **Response (429):** Too Many Requests

```json
{
  "success": false,
  "message": "Rate limit exceeded",
  "data": null,
  "timestamp": 1712486400000
}
```

---

## Pagination

All list endpoints support pagination:

**Request:**
```
GET /patients/search?page=0&size=20&sort=createdAt,desc
```

**Response:**
```json
{
  "content": [ /* items */ ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 20,
    "totalElements": 100,
    "totalPages": 5
  },
  "last": false,
  "first": true,
  "empty": false
}
```

---

## Sorting

Supported sort fields depend on entity. Example:

```
GET /patients/search?sort=createdAt,desc&sort=lastName,asc
```

---

## Date & Time Format

All timestamps in ISO 8601 format:

```
2026-04-07T10:30:00
2026-04-07T10:30:00.123Z
```

---

## API Versioning

Current version: **v1**

Endpoints follow pattern:
```
/api/v1/patients
/api/v2/patients (future)
```

---

## SDK Examples

### cURL

```bash
# Register patient
curl -X POST http://localhost:8080/api/v1/patients \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "Rajesh",
    "lastName": "Kumar",
    "phone": "9876543210",
    "gender": "MALE"
  }'

# Get patient
curl -X GET http://localhost:8080/api/v1/patients/1 \
  -H "Authorization: Bearer YOUR_TOKEN"

# Search patients
curl -X GET "http://localhost:8080/api/v1/patients/search?q=rajesh" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

### JavaScript/TypeScript

```typescript
const token = "YOUR_JWT_TOKEN";

// Register patient
const registerPatient = async (patient) => {
  const response = await fetch("http://localhost:8080/api/v1/patients", {
    method: "POST",
    headers: {
      "Authorization": `Bearer ${token}`,
      "Content-Type": "application/json"
    },
    body: JSON.stringify(patient)
  });
  return response.json();
};

// Get patient
const getPatient = async (id) => {
  const response = await fetch(`http://localhost:8080/api/v1/patients/${id}`, {
    headers: { "Authorization": `Bearer ${token}` }
  });
  return response.json();
};
```

### Java

```java
RestTemplate restTemplate = new RestTemplate();

HttpHeaders headers = new HttpHeaders();
headers.set("Authorization", "Bearer " + token);
headers.setContentType(MediaType.APPLICATION_JSON);

HttpEntity<RegisterPatientRequest> request = 
    new HttpEntity<>(patientRequest, headers);

ResponseEntity<ApiResponse<PatientResponse>> response = 
    restTemplate.postForEntity(
        "http://localhost:8080/api/v1/patients",
        request,
        new ParameterizedTypeReference<ApiResponse<PatientResponse>>() {}
    );
```

---

## Webhooks (Future)

Planned for v2:
- Patient registration events
- Appointment reminders
- Payment notifications
- Follow-up alerts

---

For complete specification updates, see:
- [README.md](README.md)
- [ARCHITECTURE.md](ARCHITECTURE.md)

