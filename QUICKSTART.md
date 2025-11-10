# PicStormAI Quick Start Guide

## Prerequisites

### Required
- **Node.js** 18+ (for frontend) - [Download](https://nodejs.org/)
- **Java** 17+ (for backend) - [Download](https://adoptium.net/)
- **Docker Desktop** (for PostgreSQL, Redis, S3) - [Download](https://www.docker.com/products/docker-desktop)

### Optional
- **AWS Account** (for deploying infrastructure)
- **AWS CLI** (for managing resources)

## Quick Start (Complete Stack)

### 1. Start All Services (Easy Mode)
```bash
./run.sh
```

This script will:
- ✅ Start PostgreSQL (port 5432)
- ✅ Start Redis (port 6379)
- ✅ Start LocalStack S3 (port 4566)
- ✅ Start Backend API (port 8080)
- ✅ Start Frontend Dev Server (port 8081)

### 2. Access the Application
- **Frontend**: http://localhost:8081
- **Backend API**: http://localhost:8080
- **Health Check**: http://localhost:8080/actuator/health
- **Storybook**: `cd frontend && npm run storybook` → http://localhost:6006

## Manual Start (Step by Step)

### Step 1: Start Infrastructure Services
```bash
docker-compose up -d
```

This starts PostgreSQL, Redis, and LocalStack (S3).

### Step 2: Start Backend
```bash
cd backend
./gradlew bootRun
```

Backend runs on http://localhost:8080

### Step 3: Start Frontend
```bash
cd frontend
npm install --legacy-peer-deps  # First time only
npm run web
```

Frontend runs on http://localhost:8081

## Development Commands

### Frontend
```bash
cd frontend

# Development server
npm run web

# Run tests
npm test

# Type checking
npm run type-check

# Linting
npm run lint

# Storybook (component library)
npm run storybook

# Build for production
npm run build:web
```

### Backend
```bash
cd backend

# Run application
./gradlew bootRun

# Run tests
./gradlew test

# Build JAR
./gradlew build

# Clean build
./gradlew clean build
```

### Infrastructure
```bash
# Start all services
docker-compose up -d

# View logs
docker-compose logs -f

# Stop all services
docker-compose down

# Stop and remove volumes (clean slate)
docker-compose down -v
```

## Troubleshooting

### Port Already in Use
If you get "port already in use" errors:

```bash
# Find process using port 8080 (backend)
lsof -i :8080

# Find process using port 8081 (frontend)
lsof -i :8081

# Kill process
kill -9 <PID>
```

### Database Connection Issues
```bash
# Check if PostgreSQL is running
docker-compose ps

# View PostgreSQL logs
docker-compose logs postgres

# Connect to database
docker exec -it picstormai-postgres psql -U rapidphoto_admin -d rapidphoto
```

### Frontend Build Errors
```bash
cd frontend

# Clean install
rm -rf node_modules package-lock.json
npm install --legacy-peer-deps

# Clear webpack cache
rm -rf .webpack_cache
```

### Backend Build Errors
```bash
cd backend

# Clean build
./gradlew clean build

# Skip tests
./gradlew build -x test
```

## Project Structure

```
picstormai/
├── frontend/                # React Native Web (TypeScript)
│   ├── src/
│   │   ├── components/     # UI components (atoms, molecules, organisms)
│   │   ├── contexts/       # React contexts (Theme, etc.)
│   │   ├── hooks/          # Custom React hooks
│   │   ├── styles/         # Design tokens and themes
│   │   └── types/          # TypeScript type definitions
│   ├── package.json
│   └── webpack.config.js
│
├── backend/                 # Spring Boot (Java 17)
│   ├── src/main/java/com/rapidphoto/
│   │   ├── config/         # Spring configuration
│   │   ├── cqrs/           # Command/Query handlers
│   │   ├── domain/         # Domain models (DDD)
│   │   └── health/         # Health indicators
│   ├── src/main/resources/
│   │   ├── db/migration/   # Flyway SQL migrations
│   │   └── application.yml # Configuration
│   └── build.gradle
│
├── infrastructure/          # AWS CDK (TypeScript)
│   └── cdk/
│       └── lib/rapidphoto-stack.ts
│
├── docs/                    # Documentation
│   ├── epics/              # Epic documentation
│   ├── stories/            # User story documentation
│   └── infrastructure/     # Infrastructure guides
│
├── docker-compose.yml      # Local development services
├── run.sh                  # Quick start script
└── QUICKSTART.md          # This file
```

## Environment Variables

### Backend (application.yml)
```yaml
DB_HOST=localhost          # PostgreSQL host
DB_PORT=5432              # PostgreSQL port
DB_NAME=rapidphoto        # Database name
DB_USERNAME=rapidphoto_admin
DB_PASSWORD=password

REDIS_HOST=localhost      # Redis host
REDIS_PORT=6379          # Redis port

AWS_S3_ENDPOINT=http://localhost:4566  # LocalStack S3
AWS_REGION=us-east-1
S3_BUCKET_NAME=rapidphoto-uploads
```

### Frontend (webpack)
Frontend uses environment-specific builds. No environment variables needed for development.

## Testing

### Run All Tests
```bash
# Frontend tests
cd frontend && npm test

# Backend tests
cd backend && ./gradlew test

# Integration tests (backend)
cd backend && ./gradlew test --tests "*IntegrationTest"
```

### Test Coverage
```bash
# Frontend coverage
cd frontend && npm run test:coverage

# Backend coverage
cd backend && ./gradlew test jacocoTestReport
# View: backend/build/reports/jacoco/test/html/index.html
```

## Deployment

See detailed deployment guide: `docs/infrastructure/https-vpc-deployment.md`

### Deploy to AWS
```bash
cd infrastructure/cdk

# Install dependencies
npm install

# Bootstrap CDK (first time only)
cdk bootstrap

# Deploy dev environment
cdk deploy RapidPhotoStack-Dev

# Deploy production
cdk deploy RapidPhotoStack-Prod
```

## Getting Help

- **Frontend Issues**: Check `frontend/README.md`
- **Backend Issues**: Check `backend/README.md`
- **Infrastructure**: Check `docs/infrastructure/`
- **Stories**: Check `docs/stories/` for feature documentation
- **Epics**: Check `docs/epics/` for architectural overview

## Next Steps

1. ✅ Start the application with `./run.sh`
2. 📖 Read Epic 0 documentation: `docs/epics/epic-0-foundation-infrastructure.md`
3. 🎨 View Storybook component library: `cd frontend && npm run storybook`
4. 🧪 Run tests to ensure everything works
5. 🚀 Start building Epic 1 (Authentication)!

---

**Status**: Epic 0 (Foundation & Infrastructure) ✅ COMPLETE

All blocking issues resolved. Ready for Epic 1 (Authentication) development!
