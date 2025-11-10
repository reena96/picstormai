#!/bin/bash

# PicStormAI Startup Script
# This script helps you run the complete application stack

set -e

echo "🚀 PicStormAI Startup Script"
echo "=============================="
echo ""

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Check prerequisites
echo "Checking prerequisites..."

# Check if Docker is available
if command -v docker &> /dev/null; then
    echo -e "${GREEN}✓${NC} Docker is installed"
    DOCKER_AVAILABLE=true
else
    echo -e "${RED}✗${NC} Docker is not installed"
    echo -e "${YELLOW}  → Install Docker Desktop from: https://www.docker.com/products/docker-desktop${NC}"
    DOCKER_AVAILABLE=false
fi

# Check if Node.js is available
if command -v node &> /dev/null; then
    echo -e "${GREEN}✓${NC} Node.js is installed ($(node --version))"
    NODE_AVAILABLE=true
else
    echo -e "${RED}✗${NC} Node.js is not installed"
    NODE_AVAILABLE=false
fi

# Check if Java is available
if command -v java &> /dev/null; then
    echo -e "${GREEN}✓${NC} Java is installed ($(java -version 2>&1 | head -n 1))"
    JAVA_AVAILABLE=true
else
    echo -e "${RED}✗${NC} Java is not installed"
    JAVA_AVAILABLE=false
fi

echo ""

# If Docker is available, start services
if [ "$DOCKER_AVAILABLE" = true ]; then
    echo "Starting infrastructure services with Docker..."
    echo -e "${BLUE}→${NC} Starting PostgreSQL, Redis, and LocalStack..."

    docker-compose up -d

    echo -e "${GREEN}✓${NC} Infrastructure services started"
    echo ""
    echo "Waiting for services to be ready..."
    sleep 5

    # Check service health
    docker-compose ps
    echo ""
else
    echo -e "${YELLOW}⚠${NC}  Cannot start infrastructure services without Docker"
    echo -e "${YELLOW}   You'll need to install and start PostgreSQL, Redis, and S3 manually${NC}"
    echo ""
fi

# Start frontend if Node is available
if [ "$NODE_AVAILABLE" = true ]; then
    echo "Starting frontend development server..."
    echo -e "${BLUE}→${NC} Running npm install (if needed)..."

    cd frontend

    if [ ! -d "node_modules" ]; then
        npm install --legacy-peer-deps
    fi

    echo -e "${GREEN}✓${NC} Frontend dependencies ready"
    echo -e "${BLUE}→${NC} Starting webpack dev server on http://localhost:8081"

    npm run web &
    FRONTEND_PID=$!

    cd ..
    echo ""
else
    echo -e "${YELLOW}⚠${NC}  Cannot start frontend without Node.js"
    echo ""
fi

# Start backend if Java is available and Docker services are running
if [ "$JAVA_AVAILABLE" = true ] && [ "$DOCKER_AVAILABLE" = true ]; then
    echo "Starting backend Spring Boot application..."
    echo -e "${BLUE}→${NC} Running Gradle bootRun..."

    cd backend

    export DB_HOST=localhost
    export DB_PORT=5432
    export DB_NAME=rapidphoto
    export DB_USERNAME=rapidphoto_admin
    export DB_PASSWORD=password
    export REDIS_HOST=localhost
    export REDIS_PORT=6379
    export AWS_S3_ENDPOINT=http://localhost:4566
    export S3_BUCKET_NAME=rapidphoto-uploads

    echo -e "${BLUE}→${NC} Backend will run on http://localhost:8080"
    echo -e "${BLUE}→${NC} Actuator health: http://localhost:8080/actuator/health"

    ./gradlew bootRun &
    BACKEND_PID=$!

    cd ..
    echo ""
else
    if [ "$JAVA_AVAILABLE" = false ]; then
        echo -e "${YELLOW}⚠${NC}  Cannot start backend without Java"
    elif [ "$DOCKER_AVAILABLE" = false ]; then
        echo -e "${YELLOW}⚠${NC}  Cannot start backend without infrastructure services"
    fi
    echo ""
fi

echo "=============================="
echo -e "${GREEN}✓ Startup complete!${NC}"
echo ""
echo "Access the application:"
echo -e "  ${BLUE}Frontend:${NC} http://localhost:8081"
echo -e "  ${BLUE}Backend API:${NC} http://localhost:8080"
echo -e "  ${BLUE}Health Check:${NC} http://localhost:8080/actuator/health"
echo -e "  ${BLUE}Storybook:${NC} npm run storybook (in frontend/)"
echo ""
echo "To stop services:"
echo "  ${YELLOW}docker-compose down${NC} (stops infrastructure)"
echo "  ${YELLOW}Ctrl+C${NC} then kill processes"
echo ""

# Keep script running
wait
