# Setup Guide

## Prerequisites
- Java 21
- Maven 3.9+
- MongoDB Atlas account
- Docker Desktop
- Git

## MongoDB Atlas Setup
1. Go to [mongodb.com/cloud/atlas](https://mongodb.com/cloud/atlas)
2. Create a free cluster
3. Create a database user
4. Whitelist your IP (0.0.0.0/0 for development)
5. Get your connection string

## Local Development Setup

### 1. Clone the repo
```bash
git clone https://github.com/RajsProjects/society-management.git
cd society-management
```

### 2. Configure application
```bash
cp src/main/resources/application-template.yml src/main/resources/application.yml
```

Edit `application.yml`:
```yaml
spring:
  data:
    mongodb:
      uri: YOUR_MONGODB_URI
jwt:
  secret: YOUR_JWT_SECRET_MIN_32_CHARS
```

### 3. Run the app
```bash
mvn spring-boot:run
```

### 4. Verify
Visit `http://localhost:8080/swagger-ui.html`

## Docker Setup

### 1. Configure environment
```bash
cp .env.example .env
# Edit .env with your values
```

### 2. Run with Docker
```bash
docker build -t society-management:latest .
docker run -p 8080:8080 --env-file .env society-management:latest
```

### 3. Run with Docker Compose (includes Prometheus + Grafana)
```bash
docker-compose up -d
```

Services available:
- App: http://localhost:8080
- Swagger: http://localhost:8080/swagger-ui.html
- Prometheus: http://localhost:9090
- Grafana: http://localhost:3000 (admin/admin123)

## Default Admin Account
The app auto-creates an admin on first run: