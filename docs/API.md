# API Reference

## Base URL

## Roles
| Role | Description |
|------|-------------|
| ADMIN | Full access to all endpoints |
| RESIDENT | Limited to own data and community features |

## Modules

### Auth
| Method | Endpoint | Role | Description |
|--------|----------|------|-------------|
| POST | `/auth/signup` | Public | Register new resident |
| POST | `/auth/login` | Public | Login and get JWT token |

### User Management
| Method | Endpoint | Role | Description |
|--------|----------|------|-------------|
| GET | `/users` | ADMIN | List all users with filters |
| PATCH | `/users/{id}/status` | ADMIN | Approve or block a resident |

### Issues
| Method | Endpoint | Role | Description |
|--------|----------|------|-------------|
| POST | `/issues` | ALL | Report a new issue |
| GET | `/issues` | ALL | List all issues |
| PATCH | `/issues/{id}/status` | ADMIN | Update issue status |
| PATCH | `/issues/{id}/priority` | ADMIN | Set issue priority |
| POST | `/issues/{id}/votes` | ALL | Vote on an issue |
| DELETE | `/issues/{id}/votes` | ALL | Remove vote |

### Finance
| Method | Endpoint | Role | Description |
|--------|----------|------|-------------|
| POST | `/finance/bills` | ADMIN | Generate maintenance bill |
| GET | `/finance/bills` | ALL | View bills |
| POST | `/finance/bills/{id}/pay` | RESIDENT | Pay a bill |

### Announcements
| Method | Endpoint | Role | Description |
|--------|----------|------|-------------|
| POST | `/announcements` | ADMIN | Create announcement |
| GET | `/announcements` | ALL | List announcements |
| GET | `/announcements/{id}` | ALL | Get announcement |
| PUT | `/announcements/{id}` | ADMIN | Update announcement |
| DELETE | `/announcements/{id}` | ADMIN | Delete announcement |

## Error Response Format
```json
{
  "timestamp": "2026-05-07T10:00:00Z",
  "status": 404,
  "error": "User not found",
  "path": "/api/v1/users/abc",
  "traceId": "a3f2b1c4"
}
```

## Status Codes
| Code | Meaning |
|------|---------|
| 200 | Success |
| 201 | Created |
| 204 | No Content |
| 400 | Bad Request |
| 401 | Unauthorized |
| 403 | Forbidden |
| 404 | Not Found |
| 409 | Conflict |
| 500 | Server Error |