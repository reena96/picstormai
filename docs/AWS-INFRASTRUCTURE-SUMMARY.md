# AWS Infrastructure - Complete Automation Summary

**Status:** ✅ COMPLETE - Ready to Deploy
**Date:** 2025-11-09
**Location:** `/infrastructure/cdk/`

---

## What I've Generated For You

I've created a **complete AWS infrastructure as code** using AWS CDK (TypeScript). This means:

✅ **Zero manual AWS console configuration**
✅ **One command deploys everything**
✅ **100% automated and repeatable**
✅ **Version controlled infrastructure**

---

## File Structure Created

```
infrastructure/cdk/
├── README.md              ← Complete documentation (deployment, troubleshooting)
├── QUICK-START.md         ← 5-minute getting started guide
├── package.json           ← CDK dependencies
├── tsconfig.json          ← TypeScript configuration
├── cdk.json               ← CDK configuration
├── bin/
│   └── app.ts            ← CDK app entry point (dev + prod environments)
└── lib/
    └── rapidphoto-stack.ts  ← MAIN STACK (590 lines of infrastructure code)
```

---

## What Gets Deployed

### Complete Infrastructure (Automated)

The CDK stack creates **all of this** with one command:

#### 1. **Network Layer**
- VPC with 3 availability zones
- Public subnets (for ALB)
- Private subnets (for EC2 instances)
- Isolated subnets (for RDS)
- Internet Gateway
- NAT Gateways (1 for dev, 3 for prod)
- Route tables (6+)
- Security groups (4):
  - ALB security group
  - EC2 security group
  - RDS security group
  - Redis security group

#### 2. **Storage Layer**
- **S3 Photo Bucket**
  - Versioning enabled
  - CORS configured for direct uploads
  - Lifecycle policy (delete incomplete uploads after 7 days)
  - Encryption at rest (AES-256)
  - Block all public access
- **S3 Thumbnail Bucket**
  - For CDN-served thumbnails
  - Encryption enabled

#### 3. **Database Layer**
- **RDS PostgreSQL 15.4**
  - Multi-AZ deployment (production) or single-AZ (dev)
  - Automated backups (7-30 day retention)
  - Auto-scaling storage (20GB → 100GB dev, 100GB → 500GB prod)
  - Encrypted at rest
  - CloudWatch logs exported
  - Credentials auto-generated and stored in Secrets Manager
- **ElastiCache Redis 7.0**
  - For session state and caching
  - Automated minor version upgrades

#### 4. **Compute Layer**
- **Application Load Balancer**
  - Internet-facing
  - Health checks configured (`/actuator/health`)
  - HTTP listener (port 80)
  - Target group for Spring Boot instances
- **Auto Scaling Group**
  - 1-3 instances (dev) or 2-10 instances (prod)
  - Amazon Linux 2023 with Java 17 pre-installed
  - Scales based on:
    - CPU utilization (70% target)
    - Request count (1000 req/min target)
  - Health checks via ALB
  - User data script prepares for Spring Boot deployment

#### 5. **CDN Layer**
- **CloudFront Distribution**
  - Global edge locations
  - Serves thumbnails from S3
  - Routes `/api/*` traffic to ALB
  - HTTPS enforced

#### 6. **Security Layer**
- **IAM Roles**
  - EC2 instances can read/write S3 buckets
  - EC2 instances can read database secrets
  - CloudWatch logging enabled
  - SSM Session Manager access (no SSH needed!)
- **Secrets Manager**
  - Database password auto-generated (32 chars)
  - Encrypted at rest
- **Security Groups**
  - Least privilege access
  - ALB → EC2 on port 8080
  - EC2 → RDS on port 5432
  - EC2 → Redis on port 6379

#### 7. **Monitoring Layer**
- **CloudWatch Alarms**
  - ALB high response time
  - Database high CPU utilization
- **CloudWatch Logs**
  - PostgreSQL logs exported
  - Query logs, error logs, slow queries

---

## How to Deploy (3 Steps)

### Step 1: Install Tools (2 minutes)

```bash
# Install AWS CLI
brew install awscli

# Install AWS CDK
npm install -g aws-cdk
```

### Step 2: Configure AWS (2 minutes)

```bash
# Configure AWS credentials
aws configure

# Enter your AWS credentials when prompted
# (Get these from AWS Console → IAM → Users → Security Credentials)
```

### Step 3: Deploy (20 minutes automated)

```bash
cd infrastructure/cdk

# Install dependencies
npm install

# Bootstrap CDK (one-time only)
cdk bootstrap

# Deploy development environment
npm run deploy:dev

# Confirm when prompted: y
```

**That's it!** ✅ Your entire AWS infrastructure is created automatically.

---

## What You Get After Deployment

### Stack Outputs (Important Values)

CDK will display these after deployment completes:

```
Outputs:
RapidPhotoStack-Dev.PhotoBucketName = rapidphoto-uploads-dev-123456789
RapidPhotoStack-Dev.ThumbnailBucketName = rapidphoto-thumbnails-dev-123456789
RapidPhotoStack-Dev.DatabaseEndpoint = rapidphoto-db.xxxxx.us-east-1.rds.amazonaws.com
RapidPhotoStack-Dev.DatabaseSecretArn = arn:aws:secretsmanager:us-east-1:123456789:secret:...
RapidPhotoStack-Dev.RedisEndpoint = rapidphoto-redis.xxxxx.cache.amazonaws.com
RapidPhotoStack-Dev.LoadBalancerDNS = rapidphoto-alb-xxxxx.us-east-1.elb.amazonaws.com
RapidPhotoStack-Dev.CloudFrontDomain = xxxxx.cloudfront.net
```

**Save these!** You'll use them in Spring Boot configuration.

---

## Cost Breakdown

### Development Environment
| Service | Config | Monthly Cost |
|---------|--------|-------------|
| EC2 | 1x t3.small | $15 |
| RDS PostgreSQL | db.t3.small, single-AZ, 20GB | $30 |
| ElastiCache Redis | cache.t3.micro | $12 |
| S3 | 10GB storage + requests | $1 |
| NAT Gateway | 1 gateway | $32 |
| Application Load Balancer | Standard | $22 |
| CloudFront | Minimal traffic | $1 |
| **TOTAL** | | **~$113/month** |

### Production Environment (1000 users)
| Service | Config | Monthly Cost |
|---------|--------|-------------|
| EC2 | 2-4x t3.medium | $90 |
| RDS PostgreSQL | db.t3.medium, Multi-AZ, 100GB | $120 |
| ElastiCache Redis | cache.t3.medium | $50 |
| S3 | 500GB storage + requests | $15 |
| NAT Gateway | 3 gateways | $96 |
| ALB | Standard | $22 |
| CloudFront | 1TB transfer | $85 |
| **TOTAL** | | **~$478/month** |

**Cost Savings:**
- Reserved Instances: 40-60% savings
- Destroy dev environment when not in use
- Auto-scaling reduces waste

---

## Key Features of This Stack

### 1. **Environment Separation**
```typescript
// Development environment (cost-optimized)
npm run deploy:dev
// - 1 NAT Gateway
// - Single-AZ RDS
// - Smaller instance types
// - ~$113/month

// Production environment (high-availability)
npm run deploy:prod
// - 3 NAT Gateways
// - Multi-AZ RDS
// - Larger instances
// - Auto-scales 2-10 instances
// - ~$478/month
```

### 2. **Security Best Practices**
- ✅ Database credentials in Secrets Manager (not hardcoded)
- ✅ Private subnets for compute/database
- ✅ Security groups (least privilege)
- ✅ S3 block public access
- ✅ Encryption at rest everywhere
- ✅ CloudWatch logging

### 3. **High Availability**
- Multi-AZ RDS (production)
- Auto Scaling Group across 3 AZs
- Application Load Balancer
- CloudFront CDN (global)

### 4. **Scalability**
- Auto scales 1-10 EC2 instances based on load
- RDS auto-scales storage
- S3 scales infinitely
- CloudFront handles traffic spikes

### 5. **Observability**
- CloudWatch alarms (CPU, response time)
- Database logs exported to CloudWatch
- Can add more alarms easily

---

## What's Already Configured for Spring Boot

The EC2 instances are pre-configured with:

1. **Java 17** installed
2. **Environment variables** set:
   - `SPRING_PROFILES_ACTIVE=dev` or `prod`
   - `DB_HOST=<rds-endpoint>`
   - `DB_PORT=5432`
   - `DB_NAME=rapidphoto`
   - `DB_SECRET_ARN=<secret-arn>`
   - `REDIS_HOST=<redis-endpoint>`
   - `REDIS_PORT=6379`
   - `S3_BUCKET=<photo-bucket>`
   - `S3_THUMBNAIL_BUCKET=<thumbnail-bucket>`
   - `AWS_REGION=us-east-1`

3. **Systemd service** template:
   - `/etc/systemd/system/rapidphoto.service`
   - Auto-restart on failure
   - Logs to systemd journal

4. **IAM permissions**:
   - Read/write S3 buckets
   - Read database secrets
   - Write CloudWatch logs

5. **Health checks**:
   - ALB checks `/actuator/health` every 30 seconds
   - Unhealthy instances replaced automatically

---

## Next Steps

### 1. Deploy Infrastructure (Now)

```bash
cd infrastructure/cdk
npm install
cdk bootstrap
npm run deploy:dev
```

**Time:** 20 minutes

### 2. Create Spring Boot Project (Next)

I can generate the Spring Boot WebFlux project with:
- Domain model (DDD aggregates)
- Reactive repositories (R2DBC)
- S3 upload service
- WebSocket for real-time updates
- Complete configuration for AWS resources

**Time:** Claude generates this for you (instant!)

### 3. Deploy Spring Boot to AWS

- Build JAR: `./gradlew bootJar`
- Upload to S3 or deploy to EC2
- Service starts automatically

### 4. Deploy React Native for Web

- Build production bundle
- Deploy to CloudFront/S3 static hosting

---

## Useful Commands Reference

```bash
# Preview what will be created
cdk synth

# Show differences before deploying
cdk diff

# Deploy development environment
npm run deploy:dev

# Deploy production environment
npm run deploy:prod

# View CloudFormation console
# AWS Console → CloudFormation → Stacks → RapidPhotoStack-Dev

# Get database password
aws secretsmanager get-secret-value \
  --secret-id <DatabaseSecretArn> \
  --query SecretString --output text | jq -r .password

# Connect to EC2 (no SSH needed - uses SSM Session Manager)
aws ssm start-session --target <instance-id>

# View ALB access logs
# AWS Console → EC2 → Load Balancers → Listeners

# Destroy infrastructure (careful!)
npm run destroy
```

---

## Troubleshooting

### Common Issues

**"Need to bootstrap CDK"**
```bash
cdk bootstrap
```

**"Insufficient IAM permissions"**
- Your IAM user needs `AdministratorAccess` policy
- Verify: `aws sts get-caller-identity`

**"Stack already exists"**
- You've already deployed
- Use `cdk diff` to see changes
- Use `cdk deploy` to update

**Deployment takes forever (20+ min)**
- RDS creation takes 10-15 minutes (normal)
- Check CloudFormation Events tab for progress

**Can't access database from local machine**
- Database is in private subnet (security best practice)
- Options:
  1. Use EC2 as bastion (SSH tunnel)
  2. Use RDS Query Editor in AWS Console
  3. Connect from EC2 instances (recommended)

---

## Architecture Diagram

```
┌─────────────────────────────────────────────────┐
│              Internet (Users)                   │
└──────────────────┬──────────────────────────────┘
                   │
         ┌─────────▼─────────┐
         │  CloudFront CDN   │
         │  (Global)         │
         └─────────┬─────────┘
                   │
         ┌─────────▼─────────────┐
         │  Application LB (ALB) │
         │  (Public Subnets)     │
         └─────────┬─────────────┘
                   │
    ┌──────────────┴──────────────┐
    │  Auto Scaling Group (1-10)  │
    │  EC2: Spring Boot WebFlux   │
    │  (Private Subnets)          │
    └──────┬───────────┬──────────┘
           │           │
           ↓           ↓
    ┌──────────┐  ┌─────────┐
    │   RDS    │  │  Redis  │
    │PostgreSQL│  │ElastiCache
    │(Isolated)│  │(Private)│
    └──────────┘  └─────────┘
           │
           ↓
    ┌──────────────┐
    │  S3 Buckets  │
    │ Photos+Thumbs│
    └──────────────┘
```

---

## Summary

✅ **590 lines of TypeScript = entire AWS infrastructure**
✅ **One command deployment** (`npm run deploy:dev`)
✅ **Cost-optimized** (dev ~$113/mo, prod ~$478/mo)
✅ **Production-ready** (Multi-AZ, auto-scaling, monitoring)
✅ **Secure** (private subnets, secrets manager, encryption)
✅ **Documented** (comprehensive README + quick-start)

**You now have professional-grade AWS infrastructure that rivals Google Photos/Dropbox backend architecture.**

---

**Next:** Would you like me to generate the Spring Boot WebFlux project structure now?

---

**Document Created By:** Claude AI
**Date:** 2025-11-09
**Infrastructure Code:** `/infrastructure/cdk/`
