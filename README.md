# CipherBank Backend

Enterprise-grade bank statement processing service built for The PayTrix financial ecosystem.

---

## Table of Contents

- [Overview](#overview)
- [Technology Stack](#technology-stack)
- [Key Features](#key-features)
- [Prerequisites](#prerequisites)
- [Installation](#installation)
- [Configuration](#configuration)
- [Running the Application](#running-the-application)
- [API Documentation](#api-documentation)
- [Security](#security)
- [Deployment](#deployment)
- [Project Structure](#project-structure)
- [Contributing](#contributing)

---

## Overview

CipherBank is a secure bank statement parser and processing service designed to handle sensitive financial data with enterprise-level security standards. The service provides RESTful APIs for bank statement ingestion, processing, and retrieval with comprehensive authentication and authorization mechanisms.

**Company:** The PayTrix  
**Environment:** Production-Ready  
**Architecture:** Hexagonal Architecture (Ports & Adapters)

---

## Technology Stack

| Component | Technology | Version |
|-----------|-----------|---------|
| Runtime | Java | 17 |
| Framework | Spring Boot | 3.5.6 |
| Build Tool | Maven | 3.x |
| Database | MySQL | 8.x |
| Security | Spring Security + JWT | - |
| ORM | Spring Data JPA / Hibernate | - |
| API Documentation | SpringDoc OpenAPI | 2.8.13 |
| Utilities | Lombok, MapStruct | - |

---

## Key Features

- **Secure Authentication**: JWT-based authentication with configurable token expiration
- **Role-Based Access Control**: Admin and user role segregation
- **IP Whitelisting**: Optional IP-based access control for enhanced security
- **Bank Statement Processing**: Automated parsing and storage of bank statements
- **File Upload Support**: Multi-part file upload with 10MB size limit
- **Health Monitoring**: Spring Boot Actuator endpoints for operational insights
- **API Documentation**: Interactive Swagger UI for API exploration
- **Blue-Green Deployment**: Zero-downtime deployment strategy with automated rollback

---

## Prerequisites

Before setting up the project, ensure you have:

- **Java Development Kit (JDK) 17** or higher
- **Maven 3.6+** for dependency management and builds
- **MySQL 8.x** database server
- **Git** for version control
- IDE with Spring Boot support (IntelliJ IDEA, Eclipse, or VS Code recommended)

---

## Installation

### 1. Clone the Repository

```bash
git clone https://github.com/YourOrganization/CipherBank.git
cd CipherBank
```

### 2. Database Setup

Create the MySQL database:

```sql
CREATE DATABASE cipherbank;
CREATE USER 'cipherbank_user'@'localhost' IDENTIFIED BY 'your_secure_password';
GRANT ALL PRIVILEGES ON cipherbank.* TO 'cipherbank_user'@'localhost';
FLUSH PRIVILEGES;
```

### 3. Install Dependencies

```bash
mvn clean install
```

---

## Configuration

### Application Configuration

Update `src/main/resources/application.yml` with your environment-specific settings:

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/cipherbank
    username: cipherbank_user
    password: your_secure_password

jwt:
  secret: your-jwt-secret-key-min-32-characters
  encryption-secret: your-32-character-hex-encryption-key
  expiration: 900000  # 15 minutes

security:
  ip-whitelist:
    enabled: false  # Set to true in production with configured IPs
    debug: false
```

### Environment Variables (Recommended for Production)

Instead of hardcoding sensitive data, use environment variables:

```bash
export DB_URL=jdbc:mysql://your-db-host:3306/cipherbank
export DB_USERNAME=cipherbank_user
export DB_PASSWORD=your_secure_password
export JWT_SECRET=your-jwt-secret-key
export JWT_ENCRYPTION_SECRET=your-encryption-key
```

---

## Running the Application

### Development Mode

```bash
mvn spring-boot:run
```

The application will start on `http://localhost:8080` (or the configured port).

### Production Build

```bash
mvn clean package -DskipTests
java -jar target/cipherbank-0.0.1-SNAPSHOT.jar
```

### Verify Application Health

```bash
curl http://localhost:8080/actuator/health
```

Expected response:
```json
{"status":"UP"}
```

---

## API Documentation

### Swagger UI

Once the application is running, access the interactive API documentation:

```
http://localhost:8080/swagger-ui.html
```

### Key Endpoints

| Endpoint | Method | Description | Authentication |
|----------|--------|-------------|----------------|
| `/api/auth/login` | POST | User authentication | Public |
| `/api/auth/register` | POST | User registration | Admin Only |
| `/api/auth/change-password` | POST | Change user password | Authenticated |
| `/api/statements/**` | Various | Bank statement operations | Public/Authenticated |
| `/actuator/health` | GET | Application health check | Public |

### Authentication Flow

1. **Login**: POST to `/api/auth/login` with credentials
2. **Receive JWT**: Extract the token from the response
3. **Use Token**: Include in subsequent requests as `Authorization: Bearer <token>`
4. **Token Expiry**: Tokens expire after 15 minutes (configurable)

Example login request:
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"user","password":"password"}'
```

---

## Security

### Authentication & Authorization

- **JWT Tokens**: Secure token-based authentication with HS256 signing
- **Role-Based Access**: `ADMIN` and `USER` roles with method-level security
- **Password Encryption**: BCrypt password hashing
- **Session Management**: Stateless (no server-side sessions)

### IP Whitelisting

Optional IP-based access control can be enabled in production:

```yaml
security:
  ip-whitelist:
    enabled: true
    debug: true  # Enable for troubleshooting proxy/load balancer issues
```

Configure allowed IP addresses in the `IpWhitelistFilter` class.

### File Upload Security

- Maximum file size: 10MB
- Allowed file types: Configurable per controller
- Virus scanning: Recommended for production (not included in base setup)

---

## Deployment

### CRITICAL: Automated Deployment Warning

**WARNING: DO NOT PUSH TO THE `release` BRANCH UNLESS YOU INTEND TO DEPLOY TO PRODUCTION**

Pushing to the `release` branch automatically triggers the CI/CD pipeline and deploys the application to the production server using blue-green deployment strategy.

**Before Pushing to Release:**
1. Ensure all tests pass locally
2. Code has been reviewed and approved
3. Changes have been tested in a staging environment
4. Database migrations (if any) are backward compatible
5. You have communicated the deployment to the team

### Blue-Green Deployment Architecture

The application uses an automated blue-green deployment strategy:

- **Blue Environment**: Runs on port 8083
- **Green Environment**: Runs on port 8084
- **Zero Downtime**: Traffic switches only after health checks pass
- **Automatic Rollback**: Deployment reverts on failure
- **Health Monitoring**: Validates `/actuator/health` endpoint
- **Notifications**: Telegram notifications for deployment status

### Deployment Process

1. Push to `release` branch triggers GitHub Actions workflow
2. Application is built with Maven
3. JAR is deployed to inactive environment (blue or green)
4. Systemd service starts the new version
5. Health checks verify application startup
6. Nginx configuration updates to route traffic
7. Old version is gracefully stopped
8. Telegram notification confirms success or failure

### Manual Deployment

If you need to deploy manually:

```bash
# Build the application
mvn clean package -DskipTests

# Transfer JAR to server
scp target/cipherbank-0.0.1-SNAPSHOT.jar user@server:/opt/cipherbank/

# SSH to server and restart service
ssh user@server
sudo systemctl restart cipherbank-blue  # or cipherbank-green
```

### Monitoring Deployment

- Check GitHub Actions for build status
- Monitor Telegram channel for deployment notifications
- Verify health endpoint: `https://cipher.thepaytrix.com/api/actuator/health`
- Check application logs: `sudo journalctl -u cipherbank-blue -f`

---

## Project Structure

```
cipherbank/
├── src/
│   ├── main/
│   │   ├── java/com/paytrix/cipherbank/
│   │   │   ├── application/          # Application layer (use cases)
│   │   │   ├── domain/               # Domain models and business logic
│   │   │   └── infrastructure/       # Infrastructure layer
│   │   │       ├── adapter/          # Adapters (REST controllers, repositories)
│   │   │       ├── config/           # Spring configuration
│   │   │       ├── security/         # Security filters and utilities
│   │   │       └── util/             # Utility classes
│   │   └── resources/
│   │       ├── application.yml       # Application configuration
│   │       └── application-prod.yml  # Production-specific config
│   └── test/                         # Test suites
├── .github/
│   └── workflows/
│       └── deploy.yml               # CI/CD pipeline configuration
├── pom.xml                          # Maven dependencies
└── README.md                        # This file
```

### Architecture Principles

The project follows Hexagonal Architecture (Ports & Adapters):

- **Domain Layer**: Core business logic, independent of frameworks
- **Application Layer**: Use cases and application services
- **Infrastructure Layer**: Technical implementations (REST, JPA, Security)
- **Adapters**: Interface implementations for external systems

---

## Contributing

### Development Workflow

1. Create a feature branch from `main`:
   ```bash
   git checkout -b feature/your-feature-name
   ```

2. Make your changes and commit:
   ```bash
   git add .
   git commit -m "feat: add your feature description"
   ```

3. Push to your branch:
   ```bash
   git push origin feature/your-feature-name
   ```

4. Create a Pull Request to `main` (NOT `release`)

5. After code review and approval, merge to `main`

6. When ready to deploy, create a PR from `main` to `release`

### Code Standards

- Follow Java naming conventions
- Write unit tests for business logic
- Document public APIs with Javadoc
- Use Lombok annotations to reduce boilerplate
- Maintain consistent code formatting

### Commit Message Convention

Follow conventional commits:
- `feat:` New features
- `fix:` Bug fixes
- `docs:` Documentation changes
- `refactor:` Code refactoring
- `test:` Test additions or modifications
- `chore:` Build process or auxiliary tool changes

---

## Support

For issues, questions, or feature requests:

1. Check existing GitHub issues
2. Contact the development team lead
3. Refer to internal documentation wiki
4. Escalate to The PayTrix technical leadership

---

## License

Proprietary and confidential. Copyright (c) 2024 The PayTrix. All rights reserved.

This software is the property of The PayTrix and may not be distributed, modified, or used outside of authorized company operations without explicit written permission.

---

**Last Updated:** December 2024  
**Maintained By:** The PayTrix Engineering Team