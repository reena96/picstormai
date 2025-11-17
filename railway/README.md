# Railway Deployment Guide

## Prerequisites

1. GitHub account
2. Railway account (sign up at https://railway.app)
3. AWS S3 bucket for photo storage
4. Node.js and npm installed locally

## Quick Setup (5 minutes)

### 1. Install Railway CLI

```bash
npm install -g @railway/cli
```

### 2. Run Setup Script

```bash
cd railway
./setup-railway.sh
```

This script will:
- ✅ Login to Railway
- ✅ Create a new project
- ✅ Link GitHub repository
- ✅ Create backend and frontend services
- ✅ Add PostgreSQL and Redis
- ✅ Set all environment variables
- ✅ Generate public domains

### 3. Deploy

```bash
git push origin main
```

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

```bash
git push origin main
```

### Manual Deployment

```bash
railway up --service backend
railway up --service frontend
```

## Monitoring

### View Logs

```bash
# Backend logs
railway logs --service backend

# Frontend logs
railway logs --service frontend
```

### Check Status

```bash
railway status
```

### Health Check

```bash
# Backend health
curl https://<backend-url>/actuator/health

# Frontend
curl https://<frontend-url>
```

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

```bash
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
```

## Migration from AWS

### Data Migration

**PostgreSQL:**
```bash
# Export from AWS RDS
pg_dump -h <aws-rds-endpoint> -U postgres -d rapidphoto > dump.sql

# Import to Railway
railway connect postgresql
psql < dump.sql
```

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
