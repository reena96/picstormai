# PicStormAI - Project Overview

**Version:** 1.0
**Last Updated:** 2025-11-11
**Status:** Epic 1 (Authentication & Onboarding) Complete

---

## What is PicStormAI?

PicStormAI (formerly RapidPhotoUpload) is a high-performance, AI-assisted photo upload and management system designed to handle **100 concurrent media uploads** with exceptional reliability and user experience. Built for professional photographers, content creators, and enterprises, the platform combines modern reactive architecture with a research-driven design system to deliver industry-leading upload performance.

## Core Value Proposition

- **Performance**: Upload 100 photos (2MB each) in under 90 seconds with non-blocking concurrency
- **Reliability**: Automatic network resilience with 100% upload resume capability
- **Cross-Platform**: Single React Native codebase deployed to web, iOS, and Android
- **Modern Architecture**: Reactive backend with DDD + CQRS + Vertical Slice Architecture

## Key Features

### Completed (Epic 0 & 1)
- **Foundation Infrastructure**: PostgreSQL, Redis, AWS S3, Docker-based local development
- **JWT Authentication**: Secure token-based authentication with refresh capabilities
- **User Registration**: Email verification and account activation workflows
- **Login/Logout**: Web and mobile-responsive authentication UI
- **User Settings Panel**: Profile management and preferences
- **Onboarding Tutorial**: First-time user guidance system

### Planned (Epic 2 & 3)
- **Concurrent Upload Engine**: 100 simultaneous uploads with queue management
- **Upload Progress Tracking**: Real-time progress indicators and status updates
- **Network Resilience**: Auto-retry, pause/resume, and offline handling
- **Photo Gallery**: Grid/list views with infinite scroll and lazy loading
- **Photo Tagging**: AI-assisted tagging and organization
- **Bulk Operations**: Download, delete, and share multiple photos

## Technology Stack

### Backend
- **Framework**: Spring Boot 3.2 with WebFlux (Reactive)
- **Language**: Java 17
- **Database**: PostgreSQL with R2DBC (reactive driver)
- **Cache**: Redis (reactive Lettuce client)
- **Storage**: AWS S3 for media files
- **Migrations**: Flyway database versioning
- **Security**: Spring Security with JWT authentication
- **Testing**: JUnit 5, Reactor Test, Testcontainers

### Frontend
- **Framework**: React Native 0.73 with React Native Web
- **Language**: TypeScript 5.3
- **State Management**: React Context API
- **Navigation**: React Navigation 6
- **Icons**: Lucide React Native
- **Testing**: Jest, React Testing Library
- **Build**: Webpack 5

### Infrastructure
- **Orchestration**: Docker Compose (local), AWS CDK (cloud)
- **CI/CD**: Gradle (backend), npm (frontend)
- **Monitoring**: Spring Boot Actuator for health checks

## Architecture Highlights

**Reactive Programming**: End-to-end non-blocking I/O using Spring WebFlux and R2DBC for handling high-concurrency photo uploads efficiently.

**Domain-Driven Design**: Clear domain boundaries with CQRS pattern separating read/write operations for optimized performance.

**Vertical Slice Architecture**: Features organized by capability (authentication, uploads, gallery) rather than technical layers, improving maintainability.

**Security-First**: JWT-based stateless authentication, password hashing with BCrypt, and secure token refresh mechanisms.

## Project Structure

```
picstormai/
├── backend/          # Spring Boot WebFlux API (Java 17)
├── frontend/         # React Native Web UI (TypeScript)
├── infrastructure/   # AWS CDK deployment configs
├── docs/            # PRD, epics, stories, and architecture docs
└── docker-compose.yml # Local development stack
```

## Development Approach

PicStormAI leverages **AI-assisted development** with Claude and Context7 MCP, enabling:
- 3-4x faster development velocity
- Reactive pattern generation for complex async flows
- Comprehensive test coverage with automated test generation
- Single-codebase strategy optimized for AI code generation

## Current Status

- **Epic 0**: Foundation & Infrastructure ✅ **COMPLETE**
- **Epic 1**: Authentication & Onboarding ✅ **COMPLETE**
- **Epic 2**: Core Upload Experience 🚧 **NEXT**
- **Epic 3**: Photo Gallery & Tagging 📋 **PLANNED**

## Quick Start

```bash
# Start all services (PostgreSQL, Redis, S3, Backend, Frontend)
./run.sh

# Access the application
Frontend: http://localhost:8081
Backend API: http://localhost:8080
Health Check: http://localhost:8080/actuator/health
```

## Documentation

- **PRD**: `docs/PRD-RapidPhotoUpload.md` - Complete product requirements
- **Architecture**: `docs/ARCHITECTURE-BMAD.md` - Technical architecture details
- **Epics**: `docs/epics/` - Feature implementation guides
- **Stories**: `docs/stories/` - User story specifications
- **Quick Start**: `QUICKSTART.md` - Development setup guide

---

**Next Milestone**: Epic 2 (Core Upload Experience) - Implementing concurrent upload engine with queue management and progress tracking.
