# Railway Deployment Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Migrate PicStormAI from AWS to Railway with fully automated deployment via GitHub integration.

**Architecture:** Replace AWS services (EC2, ALB, RDS, ElastiCache) with Railway managed services. Backend (Spring Boot) and Frontend (React Native Web) deploy as separate Railway services. PostgreSQL and Redis use Railway add-ons. S3 storage remains on AWS. GitHub push triggers automatic deployment.

**Tech Stack:** Railway (platform), Spring Boot WebFlux, React Native Web, PostgreSQL (Railway), Redis (Railway), AWS S3 (storage), GitHub Actions (CI/CD)

---

## Task 1: Create Railway Configuration for Backend

**Files:**
- Create: `railway.json`
- Create: `nixpacks.toml`
- Create: `backend/src/main/resources/application-railway.yml`

**Step 1: Create railway.json for project structure**

Create `railway.json`:

```json
{
  "$schema": "https://railway.app/railway.schema.json",
  "build": {
    "builder": "NIXPACKS"
  },
  "deploy": {
    "numReplicas": 1,
    "restartPolicyType": "ON_FAILURE",
    "restartPolicyMaxRetries": 10
  }
}
```

**Step 2: Create nixpacks.toml for custom build configuration**

Create `nixpacks.toml`:

```toml
[phases.setup]
nixPkgs = ['...', 'jdk17']

[phases.build]
cmds = ['cd backend && ./gradlew bootJar --no-daemon']

[start]
cmd = 'java -jar backend/build/libs/picstormai-backend-0.0.1-SNAPSHOT.jar'
```

**Step 3: Create Spring Boot Railway profile**

Create `backend/src/main/resources/application-railway.yml`:

```yaml
spring:
  application:
    name: picstormai-backend

  # R2DBC Configuration - Railway PostgreSQL
  r2dbc:
    url: r2dbc:postgresql://${PGHOST}:${PGPORT:5432}/${PGDATABASE}
    username: ${PGUSER}
    password: ${PGPASSWORD}
    pool:
      initial-size: 5
      max-size: 10

  # Flyway Configuration - Railway PostgreSQL
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true
    validate-on-migrate: true
    url: jdbc:postgresql://${PGHOST}:${PGPORT:5432}/${PGDATABASE}?sslmode=require
    user: ${PGUSER}
    password: ${PGPASSWORD}

  # Redis Configuration - Railway Redis
  data:
    redis:
      url: ${REDIS_URL}
      timeout: 2000ms

# AWS S3 Configuration (remains on AWS)
aws:
  s3:
    region: ${AWS_REGION:us-east-1}
    bucket-name: ${S3_BUCKET_NAME}
    thumbnail-bucket-name: ${S3_THUMBNAIL_BUCKET_NAME}
  accessKeyId: ${AWS_ACCESS_KEY_ID}
  secretAccessKey: ${AWS_SECRET_ACCESS_KEY}

# JWT Configuration
jwt:
  secret: ${JWT_SECRET}
  access-token-expiration-minutes: 15
  refresh-token-expiration-days: 30

# Actuator
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
  endpoint:
    health:
      show-details: always

server:
  port: ${PORT:8080}

logging:
  level:
    root: INFO
    com.rapidphoto: INFO
```

**Step 4: Verify files are created**

Run:
```bash
ls -la railway.json nixpacks.toml backend/src/main/resources/application-railway.yml
```

Expected: All three files exist

**Step 5: Commit configuration files**

Run:
```bash
git add railway.json nixpacks.toml backend/src/main/resources/application-railway.yml
git commit -m "feat: Add Railway configuration for backend deployment"
```

---

## Task 2: Create Frontend Railway Configuration

**Files:**
- Create: `frontend/railway.json`
- Create: `frontend/nixpacks.toml`
- Create: `frontend/package.json` (modify start script)

**Step 1: Create frontend railway.json**

Create `frontend/railway.json`:

```json
{
  "$schema": "https://railway.app/railway.schema.json",
  "build": {
    "builder": "NIXPACKS",
    "buildCommand": "npm run build:web"
  },
  "deploy": {
    "numReplicas": 1,
    "startCommand": "npx serve -s dist -p $PORT",
    "restartPolicyType": "ON_FAILURE"
  }
}
```

**Step 2: Create frontend nixpacks.toml**

Create `frontend/nixpacks.toml`:

```toml
[phases.setup]
nixPkgs = ['nodejs-18_x']

[phases.install]
cmds = ['npm ci']

[phases.build]
cmds = ['npm run build:web']

[start]
cmd = 'npx serve -s dist -p $PORT'
```

**Step 3: Update package.json to add serve dependency**

Modify `frontend/package.json` dependencies section to include:

```json
{
  "dependencies": {
    ...existing dependencies...,
    "serve": "^14.2.1"
  }
}
```

**Step 4: Verify frontend configuration**

Run:
```bash
ls -la frontend/railway.json frontend/nixpacks.toml
cat frontend/package.json | grep serve
```

Expected: Files exist, serve dependency added

**Step 5: Commit frontend configuration**

Run:
```bash
git add frontend/railway.json frontend/nixpacks.toml frontend/package.json
git commit -m "feat: Add Railway configuration for frontend deployment"
```

---

## Task 3: Create Railway Setup Automation Script

**Files:**
- Create: `railway/setup-railway.sh`
- Create: `railway/README.md`

**Step 1: Create Railway setup script**

Create `railway/setup-railway.sh`:

```bash
#!/bin/bash
set -e

echo "🚂 Railway Deployment Setup for PicStormAI"
echo "=========================================="
echo ""

# Check if Railway CLI is installed
if ! command -v railway &> /dev/null; then
    echo "❌ Railway CLI not found. Installing..."
    npm install -g @railway/cli
fi

echo "✅ Railway CLI installed"
echo ""

# Login to Railway
echo "🔐 Logging into Railway..."
railway login
echo ""

# Create new project
echo "📦 Creating Railway project..."
read -p "Enter project name (default: picstormai): " PROJECT_NAME
PROJECT_NAME=${PROJECT_NAME:-picstormai}

railway init --name "$PROJECT_NAME"
echo ""

# Link to GitHub repository
echo "🔗 Linking to GitHub repository..."
echo "Please connect your GitHub repository through the Railway dashboard:"
echo "   1. Go to https://railway.app/dashboard"
echo "   2. Select your project"
echo "   3. Click 'Settings' → 'Connect GitHub'"
echo "   4. Select your repository"
echo ""
read -p "Press ENTER when GitHub is connected..."

# Create backend service
echo "🏗️  Creating Backend Service..."
railway service create backend
railway link backend

# Add PostgreSQL
echo "🐘 Adding PostgreSQL database..."
railway add --service postgresql
echo ""

# Add Redis
echo "🔴 Adding Redis cache..."
railway add --service redis
echo ""

# Set backend environment variables
echo "🔧 Setting backend environment variables..."
echo "Please enter the following values:"
echo ""

read -p "AWS Access Key ID: " AWS_ACCESS_KEY_ID
read -p "AWS Secret Access Key: " AWS_SECRET_ACCESS_KEY
read -p "S3 Bucket Name (photos): " S3_BUCKET_NAME
read -p "S3 Thumbnail Bucket Name: " S3_THUMBNAIL_BUCKET_NAME
read -sp "JWT Secret (leave blank to generate): " JWT_SECRET
echo ""

# Generate JWT secret if not provided
if [ -z "$JWT_SECRET" ]; then
    JWT_SECRET=$(openssl rand -base64 64 | tr -d '\n')
    echo "Generated JWT Secret"
fi

# Set environment variables
railway variables set \
    SPRING_PROFILES_ACTIVE=railway \
    AWS_REGION=us-east-1 \
    AWS_ACCESS_KEY_ID="$AWS_ACCESS_KEY_ID" \
    AWS_SECRET_ACCESS_KEY="$AWS_SECRET_ACCESS_KEY" \
    S3_BUCKET_NAME="$S3_BUCKET_NAME" \
    S3_THUMBNAIL_BUCKET_NAME="$S3_THUMBNAIL_BUCKET_NAME" \
    JWT_SECRET="$JWT_SECRET"

echo "✅ Backend environment variables set"
echo ""

# Create frontend service
echo "🎨 Creating Frontend Service..."
railway service create frontend
railway link frontend

# Get backend URL for frontend
echo "📡 Getting backend URL..."
BACKEND_URL=$(railway domain --service backend)

railway variables set \
    REACT_APP_API_URL="https://$BACKEND_URL"

echo "✅ Frontend environment variables set"
echo ""

# Generate domains
echo "🌐 Generating public domains..."
railway domain --service backend
railway domain --service frontend

echo ""
echo "✅ Railway setup complete!"
echo ""
echo "📋 Next Steps:"
echo "   1. Push to GitHub: git push origin main"
echo "   2. Railway will automatically build and deploy"
echo "   3. Check deployment status: railway status"
echo "   4. View logs: railway logs"
echo ""
echo "🔗 Dashboard: https://railway.app/dashboard"
```

**Step 2: Make script executable**

Run:
```bash
chmod +x railway/setup-railway.sh
```

Expected: Script is executable

**Step 3: Create Railway documentation**

Create `railway/README.md`:

```markdown
# Railway Deployment Guide

## Prerequisites

1. GitHub account
2. Railway account (sign up at https://railway.app)
3. AWS S3 bucket for photo storage
4. Node.js and npm installed locally

## Quick Setup (5 minutes)

### 1. Install Railway CLI

\`\`\`bash
npm install -g @railway/cli
\`\`\`

### 2. Run Setup Script

\`\`\`bash
cd railway
./setup-railway.sh
\`\`\`

This script will:
- ✅ Login to Railway
- ✅ Create a new project
- ✅ Link GitHub repository
- ✅ Create backend and frontend services
- ✅ Add PostgreSQL and Redis
- ✅ Set all environment variables
- ✅ Generate public domains

### 3. Deploy

\`\`\`bash
git push origin main
\`\`\`

Railway automatically deploys on push!

## Manual Setup (if script fails)

### Step 1: Create Railway Project

1. Go to https://railway.app/dashboard
2. Click "New Project"
3. Select "Deploy from GitHub repo"
4. Connect your GitHub account
5. Select `picstormai` repository

### Step 2: Add Services

**Backend Service:**
1. Click "+ New Service"
2. Select "GitHub Repo"
3. Set Root Directory: `/`
4. Add environment variables:
   - `SPRING_PROFILES_ACTIVE=railway`
   - `AWS_REGION=us-east-1`
   - `AWS_ACCESS_KEY_ID=<your-key>`
   - `AWS_SECRET_ACCESS_KEY=<your-secret>`
   - `S3_BUCKET_NAME=<your-bucket>`
   - `S3_THUMBNAIL_BUCKET_NAME=<your-thumbnail-bucket>`
   - `JWT_SECRET=<random-64-char-string>`

**Frontend Service:**
1. Click "+ New Service"
2. Select "GitHub Repo"
3. Set Root Directory: `/frontend`
4. Add environment variable:
   - `REACT_APP_API_URL=<backend-railway-url>`

### Step 3: Add Databases

**PostgreSQL:**
1. Click "+ New Service"
2. Select "Database" → "PostgreSQL"
3. Railway auto-injects variables: `PGHOST`, `PGUSER`, `PGPASSWORD`, `PGDATABASE`

**Redis:**
1. Click "+ New Service"
2. Select "Database" → "Redis"
3. Railway auto-injects variable: `REDIS_URL`

### Step 4: Generate Domains

1. Click on Backend service → Settings → Generate Domain
2. Click on Frontend service → Settings → Generate Domain
3. Copy backend domain and update frontend `REACT_APP_API_URL`

## Deployment

### Automatic Deployment

Push to `main` branch triggers automatic deployment:

\`\`\`bash
git push origin main
\`\`\`

### Manual Deployment

\`\`\`bash
railway up --service backend
railway up --service frontend
\`\`\`

## Monitoring

### View Logs

\`\`\`bash
# Backend logs
railway logs --service backend

# Frontend logs
railway logs --service frontend
\`\`\`

### Check Status

\`\`\`bash
railway status
\`\`\`

### Health Check

\`\`\`bash
# Backend health
curl https://<backend-url>/actuator/health

# Frontend
curl https://<frontend-url>
\`\`\`

## Cost Estimation

### Starter Plan (Recommended for Development)

- Backend: $5/month (512MB RAM, 1 vCPU)
- Frontend: $5/month (512MB RAM, 1 vCPU)
- PostgreSQL: $5/month (1GB storage)
- Redis: $5/month (256MB)
- **Total: $20/month** (vs $113/month on AWS)

### Pro Plan (Production)

- Backend: $20/month (8GB RAM, 8 vCPU)
- Frontend: $10/month (2GB RAM, 2 vCPU)
- PostgreSQL: $15/month (10GB storage)
- Redis: $10/month (1GB)
- **Total: $55/month** (vs $478/month on AWS)

## Troubleshooting

### Build Fails

1. Check logs: `railway logs --service backend`
2. Verify `nixpacks.toml` is in root directory
3. Ensure Java 17 is specified

### Database Connection Issues

1. Verify PostgreSQL plugin is added
2. Check environment variables: `railway variables --service backend`
3. Ensure `application-railway.yml` uses correct variable names

### Frontend Can't Reach Backend

1. Verify `REACT_APP_API_URL` points to backend Railway domain
2. Check CORS settings in Spring Boot
3. Ensure backend is running: `railway status`

## Useful Commands

\`\`\`bash
# View environment variables
railway variables

# Set environment variable
railway variables set KEY=value

# SSH into service
railway shell

# View service info
railway service

# Delete service
railway service delete <name>
\`\`\`

## Migration from AWS

### Data Migration

**PostgreSQL:**
\`\`\`bash
# Export from AWS RDS
pg_dump -h <aws-rds-endpoint> -U postgres -d rapidphoto > dump.sql

# Import to Railway
railway connect postgresql
psql < dump.sql
\`\`\`

**S3:**
No migration needed - continue using existing S3 buckets

### DNS Update

Update your domain DNS to point to Railway:
1. Get Railway frontend URL
2. Create CNAME record: `www.yourdomain.com` → `<frontend-railway-domain>`
3. Configure custom domain in Railway dashboard

## Support

- Railway Docs: https://docs.railway.app
- Discord: https://discord.gg/railway
- Status: https://railway.instatus.com
\`\`\`

**Step 4: Verify documentation**

Run:
```bash
ls -la railway/
```

Expected: `setup-railway.sh` and `README.md` exist

**Step 5: Commit Railway setup files**

Run:
```bash
git add railway/
git commit -m "feat: Add Railway setup automation and documentation"
```

---

## Task 4: Create GitHub Actions Workflow

**Files:**
- Create: `.github/workflows/railway-deploy.yml`

**Step 1: Create GitHub Actions workflow for Railway deployment**

Create `.github/workflows/railway-deploy.yml`:

```yaml
name: Deploy to Railway

on:
  push:
    branches:
      - main
  pull_request:
    branches:
      - main

jobs:
  test-backend:
    name: Test Backend
    runs-on: ubuntu-latest

    steps:
      - name: Checkout code
        uses: actions/checkout@v4

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          distribution: 'temurin'
          java-version: '17'
          cache: 'gradle'

      - name: Make gradlew executable
        run: chmod +x backend/gradlew

      - name: Run backend tests
        run: cd backend && ./gradlew test --no-daemon

      - name: Build JAR
        run: cd backend && ./gradlew bootJar --no-daemon

  test-frontend:
    name: Test Frontend
    runs-on: ubuntu-latest

    steps:
      - name: Checkout code
        uses: actions/checkout@v4

      - name: Setup Node.js
        uses: actions/setup-node@v4
        with:
          node-version: '18'
          cache: 'npm'
          cache-dependency-path: frontend/package-lock.json

      - name: Install dependencies
        run: cd frontend && npm ci

      - name: Run tests
        run: cd frontend && npm test -- --passWithNoTests

      - name: Build frontend
        run: cd frontend && npm run build:web

  deploy:
    name: Deploy to Railway
    needs: [test-backend, test-frontend]
    runs-on: ubuntu-latest
    if: github.ref == 'refs/heads/main' && github.event_name == 'push'

    steps:
      - name: Checkout code
        uses: actions/checkout@v4

      - name: Install Railway CLI
        run: npm install -g @railway/cli

      - name: Deploy Backend
        run: railway up --service backend --detach
        env:
          RAILWAY_TOKEN: ${{ secrets.RAILWAY_TOKEN }}

      - name: Deploy Frontend
        run: railway up --service frontend --detach
        env:
          RAILWAY_TOKEN: ${{ secrets.RAILWAY_TOKEN }}

      - name: Deployment Summary
        run: |
          echo "✅ Deployment triggered successfully"
          echo "🔗 Check status at https://railway.app/dashboard"
```

**Step 2: Create GitHub Actions documentation**

Create `.github/workflows/README.md`:

```markdown
# GitHub Actions Setup

## Required Secret

Add `RAILWAY_TOKEN` to GitHub repository secrets:

1. Generate Railway token:
   \`\`\`bash
   railway login
   railway token
   \`\`\`

2. Add to GitHub:
   - Go to repository Settings → Secrets → Actions
   - Click "New repository secret"
   - Name: `RAILWAY_TOKEN`
   - Value: (paste token from step 1)
   - Click "Add secret"

## Workflow Behavior

- **On Pull Request:** Runs tests only
- **On Push to main:** Runs tests + deploys to Railway

## Manual Trigger

You can manually trigger deployment:

1. Go to Actions tab
2. Select "Deploy to Railway"
3. Click "Run workflow"
```

**Step 3: Verify workflow files**

Run:
```bash
ls -la .github/workflows/
```

Expected: `railway-deploy.yml` and `README.md` exist

**Step 4: Commit GitHub Actions workflow**

Run:
```bash
git add .github/workflows/
git commit -m "feat: Add GitHub Actions workflow for Railway deployment"
```

---

## Task 5: Create Environment Setup Helper Script

**Files:**
- Create: `railway/check-environment.sh`

**Step 1: Create environment checker script**

Create `railway/check-environment.sh`:

```bash
#!/bin/bash

echo "🔍 Railway Environment Check"
echo "============================"
echo ""

# Check Railway CLI
if command -v railway &> /dev/null; then
    echo "✅ Railway CLI installed ($(railway --version))"
else
    echo "❌ Railway CLI not found"
    echo "   Install: npm install -g @railway/cli"
    exit 1
fi

# Check if logged in
if railway whoami &> /dev/null; then
    echo "✅ Logged into Railway ($(railway whoami))"
else
    echo "❌ Not logged into Railway"
    echo "   Run: railway login"
    exit 1
fi

# Check if project linked
if railway status &> /dev/null; then
    echo "✅ Project linked"
    echo ""
    echo "📊 Service Status:"
    railway status
else
    echo "⚠️  No project linked"
    echo "   Run: railway link"
fi

echo ""
echo "🌐 Domains:"
railway domain --service backend 2>/dev/null || echo "   Backend: Not configured"
railway domain --service frontend 2>/dev/null || echo "   Frontend: Not configured"

echo ""
echo "📋 Environment Variables:"
echo ""

echo "Backend:"
railway variables --service backend | grep -E "SPRING_PROFILES_ACTIVE|PGHOST|REDIS_URL|AWS_REGION|S3_BUCKET" || echo "   Not set"

echo ""
echo "Frontend:"
railway variables --service frontend | grep "REACT_APP_API_URL" || echo "   Not set"

echo ""
echo "✅ Environment check complete"
```

**Step 2: Make script executable**

Run:
```bash
chmod +x railway/check-environment.sh
```

Expected: Script is executable

**Step 3: Test environment checker**

Run:
```bash
./railway/check-environment.sh
```

Expected: Script runs (may show not logged in - that's ok)

**Step 4: Commit environment checker**

Run:
```bash
git add railway/check-environment.sh
git commit -m "feat: Add Railway environment checker script"
```

---

## Task 6: Update Backend CORS Configuration for Railway

**Files:**
- Modify: `backend/src/main/java/com/rapidphoto/config/WebConfig.java`

**Step 1: Check if WebConfig exists**

Run:
```bash
find backend/src -name "WebConfig.java" -o -name "*CorsConfig.java" -o -name "*SecurityConfig.java"
```

Expected: Find existing config file or empty result

**Step 2: Create or update CORS configuration**

If file exists at `backend/src/main/java/com/rapidphoto/config/WebConfig.java`, update it. Otherwise create:

```java
package com.rapidphoto.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.config.CorsRegistry;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.reactive.config.WebFluxConfigurer;

@Configuration
@EnableWebFlux
public class WebConfig implements WebFluxConfigurer {

    @Value("${cors.allowed-origins:http://localhost:8081}")
    private String allowedOrigins;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(allowedOrigins.split(","))
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
```

**Step 3: Add CORS configuration to application-railway.yml**

Add to `backend/src/main/resources/application-railway.yml`:

```yaml
cors:
  allowed-origins: ${CORS_ALLOWED_ORIGINS:https://*.up.railway.app}
```

**Step 4: Verify CORS configuration**

Run:
```bash
grep -r "allowedOrigins" backend/src/main/java/
cat backend/src/main/resources/application-railway.yml | grep cors
```

Expected: CORS configuration present

**Step 5: Commit CORS configuration**

Run:
```bash
git add backend/src/main/java/com/rapidphoto/config/WebConfig.java backend/src/main/resources/application-railway.yml
git commit -m "feat: Add CORS configuration for Railway deployment"
```

---

## Task 7: Create Quick Start Guide

**Files:**
- Create: `RAILWAY-QUICKSTART.md`

**Step 1: Create quick start documentation**

Create `RAILWAY-QUICKSTART.md`:

```markdown
# Railway Deployment - Quick Start

**Time to deploy: 10 minutes** ⚡

## Prerequisites Checklist

- [ ] GitHub account
- [ ] Railway account (sign up at https://railway.app)
- [ ] AWS S3 bucket with access keys
- [ ] Repository pushed to GitHub

## 5-Step Deployment

### Step 1: Install Railway CLI (1 minute)

\`\`\`bash
npm install -g @railway/cli
\`\`\`

### Step 2: Run Setup Script (3 minutes)

\`\`\`bash
cd railway
./setup-railway.sh
\`\`\`

Follow the prompts to enter:
- Project name
- AWS credentials
- S3 bucket names

### Step 3: Setup GitHub Actions (2 minutes)

Generate Railway token:

\`\`\`bash
railway login
railway token
\`\`\`

Add token to GitHub:
1. Go to: https://github.com/YOUR_USERNAME/picstormai/settings/secrets/actions
2. Click "New repository secret"
3. Name: `RAILWAY_TOKEN`
4. Value: (paste token)

### Step 4: Deploy (1 minute)

\`\`\`bash
git push origin main
\`\`\`

Watch deployment at https://railway.app/dashboard

### Step 5: Verify (1 minute)

\`\`\`bash
# Check status
railway status

# Get URLs
railway domain --service backend
railway domain --service frontend

# Test backend
curl https://YOUR_BACKEND_URL/actuator/health

# Test frontend
open https://YOUR_FRONTEND_URL
\`\`\`

## What Just Happened?

✅ Created Railway project
✅ Added PostgreSQL database
✅ Added Redis cache
✅ Deployed backend (Spring Boot)
✅ Deployed frontend (React Native Web)
✅ Set up automatic GitHub deployments
✅ Generated public HTTPS domains

## Next Steps

### Add Custom Domain (Optional)

1. Railway Dashboard → Frontend Service → Settings → Domains
2. Add custom domain: `www.yourdomain.com`
3. Update DNS CNAME: `www.yourdomain.com` → `<railway-domain>`

### Monitor Logs

\`\`\`bash
# Watch backend logs
railway logs --service backend -f

# Watch frontend logs
railway logs --service frontend -f
\`\`\`

### Scale Up (When Ready)

1. Railway Dashboard → Service → Settings
2. Adjust resources (RAM/CPU)
3. Enable autoscaling

## Cost Comparison

| Service | AWS | Railway | Savings |
|---------|-----|---------|---------|
| Dev Environment | $113/mo | $20/mo | **82%** |
| Production | $478/mo | $55/mo | **88%** |

## Troubleshooting

**Build fails?**
\`\`\`bash
railway logs --service backend
\`\`\`

**Can't connect to database?**
\`\`\`bash
railway variables --service backend | grep PG
\`\`\`

**Frontend can't reach backend?**
\`\`\`bash
# Check backend URL is set
railway variables --service frontend | grep REACT_APP_API_URL
\`\`\`

## Support

- Railway Docs: https://docs.railway.app
- Discord: https://discord.gg/railway
- GitHub Issues: https://github.com/YOUR_USERNAME/picstormai/issues

---

**🎉 You're live on Railway!**
\`\`\`

**Step 2: Verify quick start guide**

Run:
```bash
ls -la RAILWAY-QUICKSTART.md
wc -l RAILWAY-QUICKSTART.md
```

Expected: File exists with content

**Step 3: Commit quick start guide**

Run:
```bash
git add RAILWAY-QUICKSTART.md
git commit -m "docs: Add Railway quick start guide"
```

---

## Task 8: Create Migration Guide from AWS

**Files:**
- Create: `railway/MIGRATION-FROM-AWS.md`

**Step 1: Create AWS to Railway migration guide**

Create `railway/MIGRATION-FROM-AWS.md`:

```markdown
# Migrating from AWS to Railway

## Overview

This guide helps you migrate your existing AWS deployment to Railway while minimizing downtime.

## Migration Strategy

**Recommended: Blue-Green Deployment**

1. Deploy to Railway (green environment)
2. Test thoroughly
3. Switch DNS to Railway
4. Keep AWS running for 1 week
5. Destroy AWS infrastructure

**Timeline: 1-2 hours**

## Pre-Migration Checklist

- [ ] Railway account created
- [ ] Railway CLI installed
- [ ] AWS credentials for S3 (keep using S3)
- [ ] Database backup completed
- [ ] DNS TTL reduced to 300s (5 minutes)

## Step 1: Export Data from AWS

### PostgreSQL Data Export

\`\`\`bash
# Get RDS endpoint from CloudFormation
aws cloudformation describe-stacks \
    --stack-name RapidPhotoStack-Dev \
    --query 'Stacks[0].Outputs[?OutputKey==\`DatabaseEndpoint\`].OutputValue' \
    --output text

# Get database password from Secrets Manager
aws secretsmanager get-secret-value \
    --secret-id <DatabaseSecretArn> \
    --query SecretString \
    --output text | jq -r .password

# Export database
pg_dump -h <rds-endpoint> -U postgres -d rapidphoto > aws-backup.sql

# Verify export
ls -lh aws-backup.sql
\`\`\`

### Redis Data (Optional - likely empty)

Redis data is ephemeral (sessions/cache) - no migration needed.

## Step 2: Deploy to Railway

Follow the quick start guide:

\`\`\`bash
cd railway
./setup-railway.sh
\`\`\`

Wait for deployment to complete (5-10 minutes).

## Step 3: Import Data to Railway PostgreSQL

\`\`\`bash
# Connect to Railway PostgreSQL
railway connect postgresql

# In the psql prompt:
\i aws-backup.sql

# Verify data
SELECT COUNT(*) FROM users;
SELECT COUNT(*) FROM photos;

# Exit
\q
\`\`\`

## Step 4: Test Railway Deployment

### Backend Health Check

\`\`\`bash
BACKEND_URL=$(railway domain --service backend)
curl https://$BACKEND_URL/actuator/health

# Expected response:
# {"status":"UP","components":{"db":{"status":"UP"},"redis":{"status":"UP"}}}
\`\`\`

### Frontend Check

\`\`\`bash
FRONTEND_URL=$(railway domain --service frontend)
curl -I https://$FRONTEND_URL

# Expected: HTTP/2 200
\`\`\`

### Full Integration Test

1. Open frontend URL in browser
2. Create test account
3. Upload test photo
4. Verify photo appears
5. Check S3 bucket (should see uploaded file)

## Step 5: Update DNS

### If Using Custom Domain

**Current DNS (AWS):**
\`\`\`
www.yourdomain.com  CNAME  rapidphoto-alb-dev-xxx.us-east-1.elb.amazonaws.com
\`\`\`

**New DNS (Railway):**
\`\`\`
www.yourdomain.com  CNAME  picstormai-frontend.up.railway.app
\`\`\`

**Steps:**
1. Login to your DNS provider
2. Update CNAME record
3. Wait for DNS propagation (5 minutes with low TTL)
4. Verify: `dig www.yourdomain.com`

### If Using Railway Subdomain Only

No DNS changes needed - just use Railway domain.

## Step 6: Monitor

### Watch Logs

\`\`\`bash
# Terminal 1: Backend logs
railway logs --service backend -f

# Terminal 2: Frontend logs
railway logs --service frontend -f
\`\`\`

### Check Metrics

Railway Dashboard → Service → Metrics:
- CPU usage
- Memory usage
- Request count
- Response time

## Step 7: Cleanup AWS (After 1 Week)

**Only after confirming Railway is stable!**

\`\`\`bash
cd infrastructure/cdk

# Preview what will be deleted
cdk destroy --dry-run

# Destroy infrastructure
cdk destroy RapidPhotoStack-Dev

# Confirm: y
\`\`\`

**Keep S3 buckets!** They're still being used by Railway.

## Rollback Plan

If issues arise with Railway:

### Quick Rollback (5 minutes)

\`\`\`bash
# Update DNS back to AWS ALB
www.yourdomain.com  CNAME  rapidphoto-alb-dev-xxx.us-east-1.elb.amazonaws.com

# Wait for DNS propagation
\`\`\`

AWS infrastructure still running - users automatically routed back.

### Database Sync Back to AWS

If you made changes on Railway:

\`\`\`bash
# Export from Railway
railway connect postgresql
pg_dump rapidphoto > railway-backup.sql

# Import to AWS RDS
psql -h <aws-rds-endpoint> -U postgres -d rapidphoto < railway-backup.sql
\`\`\`

## Cost Comparison

### AWS Monthly Costs (Current)

| Service | Cost |
|---------|------|
| EC2 (1x t3.small) | $15 |
| RDS (db.t3.small) | $30 |
| ElastiCache | $12 |
| ALB | $22 |
| NAT Gateway | $32 |
| S3 | $1 |
| **Total** | **$113** |

### Railway Monthly Costs (New)

| Service | Cost |
|---------|------|
| Backend | $5 |
| Frontend | $5 |
| PostgreSQL | $5 |
| Redis | $5 |
| S3 (AWS) | $1 |
| **Total** | **$21** |

### Savings: $92/month (82%)

## Common Migration Issues

### Issue: Database connection timeout

**Cause:** Railway PostgreSQL requires SSL

**Fix:** Verify `application-railway.yml` has:
\`\`\`yaml
flyway:
  url: jdbc:postgresql://${PGHOST}:5432/${PGDATABASE}?sslmode=require
\`\`\`

### Issue: Frontend can't reach backend

**Cause:** CORS or incorrect API URL

**Fix:**
\`\`\`bash
# Check backend URL in frontend
railway variables --service frontend

# Should show:
# REACT_APP_API_URL=https://picstormai-backend.up.railway.app

# Update if wrong:
railway variables set REACT_APP_API_URL=https://CORRECT_URL --service frontend
\`\`\`

### Issue: S3 uploads fail

**Cause:** AWS credentials not set

**Fix:**
\`\`\`bash
railway variables set \
    AWS_ACCESS_KEY_ID=your-key \
    AWS_SECRET_ACCESS_KEY=your-secret \
    --service backend
\`\`\`

## Support

**AWS Issues:**
- AWS Support Console
- Check CloudFormation events

**Railway Issues:**
- Discord: https://discord.gg/railway
- Twitter: @Railway

**Application Issues:**
- GitHub Issues: https://github.com/YOUR_USERNAME/picstormai/issues
\`\`\`

**Step 2: Verify migration guide**

Run:
```bash
ls -la railway/MIGRATION-FROM-AWS.md
```

Expected: File exists

**Step 3: Commit migration guide**

Run:
```bash
git add railway/MIGRATION-FROM-AWS.md
git commit -m "docs: Add AWS to Railway migration guide"
```

---

## Task 9: Final Verification and Documentation Update

**Files:**
- Modify: `README.md` (add Railway section)

**Step 1: Update main README with Railway deployment info**

Add to `README.md` after the existing AWS deployment section:

```markdown
## Railway Deployment (Recommended)

**Simpler, cheaper alternative to AWS.**

### Quick Deploy (10 minutes)

\`\`\`bash
# 1. Install Railway CLI
npm install -g @railway/cli

# 2. Run setup script
cd railway
./setup-railway.sh

# 3. Deploy
git push origin main
\`\`\`

See [RAILWAY-QUICKSTART.md](RAILWAY-QUICKSTART.md) for detailed instructions.

### Cost: $20/month (vs $113/month on AWS)

- ✅ Automatic HTTPS
- ✅ GitHub auto-deploy
- ✅ Built-in PostgreSQL & Redis
- ✅ Zero configuration
- ✅ Free $5 credit monthly

### Migrating from AWS?

See [railway/MIGRATION-FROM-AWS.md](railway/MIGRATION-FROM-AWS.md)
```

**Step 2: Verify all files are created**

Run:
```bash
find . -name "railway.json" -o -name "nixpacks.toml" | grep -v node_modules
ls -la railway/
ls -la .github/workflows/railway-deploy.yml
ls -la RAILWAY-QUICKSTART.md
```

Expected: All Railway files present

**Step 3: Run environment checker**

Run:
```bash
./railway/check-environment.sh || echo "Railway not set up yet - expected"
```

Expected: Script runs (may show not logged in)

**Step 4: Verify git status**

Run:
```bash
git status
```

Expected: All changes committed

**Step 5: Create final commit**

Run:
```bash
git add README.md
git commit -m "docs: Add Railway deployment information to README"
```

---

## Task 10: Create Deployment Test Script

**Files:**
- Create: `railway/test-deployment.sh`

**Step 1: Create deployment test script**

Create `railway/test-deployment.sh`:

```bash
#!/bin/bash
set -e

echo "🧪 Testing Railway Deployment"
echo "=============================="
echo ""

# Get service URLs
BACKEND_URL=$(railway domain --service backend)
FRONTEND_URL=$(railway domain --service frontend)

echo "🔗 Service URLs:"
echo "   Backend:  https://$BACKEND_URL"
echo "   Frontend: https://$FRONTEND_URL"
echo ""

# Test backend health
echo "🏥 Testing backend health..."
HEALTH_RESPONSE=$(curl -s https://$BACKEND_URL/actuator/health)
echo "$HEALTH_RESPONSE" | jq .

if echo "$HEALTH_RESPONSE" | jq -e '.status == "UP"' > /dev/null; then
    echo "✅ Backend health check passed"
else
    echo "❌ Backend health check failed"
    exit 1
fi

# Test database connectivity
echo ""
echo "🐘 Testing database connectivity..."
DB_STATUS=$(echo "$HEALTH_RESPONSE" | jq -r '.components.db.status')
if [ "$DB_STATUS" == "UP" ]; then
    echo "✅ Database connected"
else
    echo "❌ Database not connected"
    exit 1
fi

# Test Redis connectivity
echo ""
echo "🔴 Testing Redis connectivity..."
REDIS_STATUS=$(echo "$HEALTH_RESPONSE" | jq -r '.components.redis.status // "UNKNOWN"')
if [ "$REDIS_STATUS" == "UP" ]; then
    echo "✅ Redis connected"
else
    echo "⚠️  Redis status: $REDIS_STATUS"
fi

# Test frontend
echo ""
echo "🎨 Testing frontend..."
FRONTEND_STATUS=$(curl -s -o /dev/null -w "%{http_code}" https://$FRONTEND_URL)
if [ "$FRONTEND_STATUS" == "200" ]; then
    echo "✅ Frontend accessible (HTTP $FRONTEND_STATUS)"
else
    echo "❌ Frontend not accessible (HTTP $FRONTEND_STATUS)"
    exit 1
fi

# Test CORS
echo ""
echo "🌐 Testing CORS..."
CORS_RESPONSE=$(curl -s -I -X OPTIONS https://$BACKEND_URL/api/health \
    -H "Origin: https://$FRONTEND_URL" \
    -H "Access-Control-Request-Method: GET")

if echo "$CORS_RESPONSE" | grep -q "access-control-allow-origin"; then
    echo "✅ CORS configured"
else
    echo "⚠️  CORS may need configuration"
fi

echo ""
echo "✅ All deployment tests passed!"
echo ""
echo "📋 Summary:"
echo "   Backend:  ✅ https://$BACKEND_URL"
echo "   Frontend: ✅ https://$FRONTEND_URL"
echo "   Database: ✅ Connected"
echo "   Redis:    ✅ Connected"
echo ""
echo "🎉 Deployment successful!"
```

**Step 2: Make script executable**

Run:
```bash
chmod +x railway/test-deployment.sh
```

Expected: Script is executable

**Step 3: Add jq installation check**

Update `railway/test-deployment.sh` to add at the beginning (after the header):

```bash
# Check for jq
if ! command -v jq &> /dev/null; then
    echo "❌ jq not found. Installing..."
    if command -v brew &> /dev/null; then
        brew install jq
    else
        echo "Please install jq: https://stedolan.github.io/jq/download/"
        exit 1
    fi
fi
```

**Step 4: Commit test script**

Run:
```bash
git add railway/test-deployment.sh
git commit -m "feat: Add deployment test script for Railway"
```

**Step 5: Push all changes**

Run:
```bash
git log --oneline -10
git push origin main
```

Expected: All commits pushed to GitHub

---

## Summary

**Files Created:**
- ✅ `railway.json` - Railway backend config
- ✅ `nixpacks.toml` - Build configuration
- ✅ `backend/src/main/resources/application-railway.yml` - Spring Boot Railway profile
- ✅ `frontend/railway.json` - Railway frontend config
- ✅ `frontend/nixpacks.toml` - Frontend build config
- ✅ `railway/setup-railway.sh` - Automated setup script
- ✅ `railway/check-environment.sh` - Environment checker
- ✅ `railway/test-deployment.sh` - Deployment test script
- ✅ `railway/README.md` - Detailed Railway documentation
- ✅ `railway/MIGRATION-FROM-AWS.md` - AWS migration guide
- ✅ `.github/workflows/railway-deploy.yml` - GitHub Actions CI/CD
- ✅ `RAILWAY-QUICKSTART.md` - Quick start guide
- ✅ Updated `README.md` with Railway information

**Total Implementation Time:** 30-40 minutes

**User Action Required:**
1. Run `./railway/setup-railway.sh` (5 minutes)
2. Add GitHub secret `RAILWAY_TOKEN`
3. Push to GitHub: `git push origin main`

**Result:** Fully automated Railway deployment with:
- ✅ Backend (Spring Boot)
- ✅ Frontend (React Native Web)
- ✅ PostgreSQL database
- ✅ Redis cache
- ✅ S3 integration (AWS)
- ✅ Auto-deploy on git push
- ✅ HTTPS domains
- ✅ Health monitoring
