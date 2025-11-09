# AWS CDK Quick Start - 5 Minute Setup

## TL;DR - Get Infrastructure Running

**Total time: ~25 minutes (5 min setup + 20 min deployment)**

### 1. Install Prerequisites (2 minutes)

```bash
# Install AWS CLI (if not already installed)
brew install awscli

# Install AWS CDK globally
npm install -g aws-cdk

# Verify installations
aws --version   # Should show version
cdk --version   # Should show 2.100.0+
```

### 2. Configure AWS Credentials (2 minutes)

```bash
# Run AWS configuration
aws configure

# Enter when prompted:
# AWS Access Key ID: <get from AWS Console → IAM → Users → Security Credentials>
# AWS Secret Access Key: <from same place>
# Default region: us-east-1
# Default output format: json
```

**Get AWS credentials:**
1. Go to https://console.aws.amazon.com
2. IAM → Users → Your User → Security Credentials
3. Click "Create Access Key"
4. Copy and paste into `aws configure`

### 3. Bootstrap CDK (1 minute, one-time only)

```bash
cd infrastructure/cdk

# Install dependencies
npm install

# Bootstrap your AWS account
cdk bootstrap

# This creates CDK staging bucket (takes ~2 minutes)
```

### 4. Deploy Infrastructure (20 minutes automated)

```bash
# Deploy development environment
npm run deploy:dev

# Confirm when prompted: y

# ☕ Grab coffee - this takes 15-20 minutes
# RDS database creation is the longest step
```

### 5. Save Outputs

When deployment completes, you'll see:

```
✅ RapidPhotoStack-Dev

Outputs:
RapidPhotoStack-Dev.PhotoBucketName = rapidphoto-uploads-dev-123456
RapidPhotoStack-Dev.DatabaseEndpoint = rapidphoto-xxxxx.rds.amazonaws.com
RapidPhotoStack-Dev.LoadBalancerDNS = rapidphoto-alb-xxxxx.elb.amazonaws.com
...
```

**Copy these values** - you'll need them for Spring Boot configuration!

---

## ✅ Done!

Your AWS infrastructure is now running:
- VPC with 3 availability zones
- S3 buckets for photo storage
- PostgreSQL database (RDS)
- Redis cache (ElastiCache)
- Load balancer (ALB)
- Auto-scaling EC2 instances (ready for Spring Boot)
- CloudFront CDN

**Next step:** Deploy Spring Boot application (Claude will help you!)

---

## Common Issues

**"Command not found: cdk"**
```bash
npm install -g aws-cdk
```

**"Unable to resolve AWS account"**
```bash
aws configure  # Enter your credentials again
aws sts get-caller-identity  # Verify it works
```

**"Need to bootstrap"**
```bash
cdk bootstrap
```

**Deployment fails midway**
- Check CloudFormation console for detailed error
- Usually an IAM permissions issue
- Ensure your IAM user has AdministratorAccess

---

## Destroy Infrastructure (When Done Testing)

```bash
npm run destroy

# Or:
cdk destroy RapidPhotoStack-Dev
```

**⚠️ This deletes everything!** (Some resources like RDS may create snapshots first)

---

**Need help?** Read the full [README.md](./README.md) for detailed documentation.
