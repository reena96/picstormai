# RapidPhotoUpload - AWS Infrastructure (CDK)

Complete AWS infrastructure as code for RapidPhotoUpload using AWS CDK (TypeScript).

**One command deploys:**
- ✅ VPC with 3 availability zones
- ✅ S3 buckets (photos + thumbnails)
- ✅ RDS PostgreSQL database (with secrets)
- ✅ ElastiCache Redis cluster
- ✅ Application Load Balancer
- ✅ Auto Scaling Group (for Spring Boot)
- ✅ CloudFront CDN
- ✅ Security groups, IAM roles, CloudWatch alarms

---

## Prerequisites

### 1. AWS Account
- Create an AWS account: https://aws.amazon.com
- You'll need an IAM user with AdministratorAccess (for CDK deployment)

### 2. Install Required Tools

**Install Node.js (if not already installed):**
```bash
# Check if you have Node.js
node --version  # Should be v18+

# If not installed, download from: https://nodejs.org
```

**Install AWS CLI:**
```bash
# macOS (you're on macOS)
brew install awscli

# Verify installation
aws --version
```

**Install AWS CDK CLI:**
```bash
npm install -g aws-cdk

# Verify installation
cdk --version  # Should be 2.100.0+
```

---

## Setup Instructions (10 minutes)

### Step 1: Configure AWS Credentials

```bash
# Run AWS configuration wizard
aws configure

# Enter your credentials:
# AWS Access Key ID: <your-access-key>
# AWS Secret Access Key: <your-secret-key>
# Default region name: us-east-1
# Default output format: json
```

**How to get AWS credentials:**
1. Log into AWS Console: https://console.aws.amazon.com
2. Go to IAM → Users → Your User → Security Credentials
3. Click "Create Access Key"
4. Copy Access Key ID and Secret Access Key

### Step 2: Install Dependencies

```bash
cd infrastructure/cdk
npm install
```

This installs all AWS CDK libraries and TypeScript.

### Step 3: Bootstrap CDK (One-time Setup)

```bash
# Bootstrap your AWS account for CDK
cdk bootstrap

# This creates a CloudFormation stack with:
# - S3 bucket for CDK assets
# - IAM roles for deployments
# Takes ~2 minutes
```

**You only need to run this ONCE per AWS account/region.**

### Step 4: Review Infrastructure

```bash
# Compile TypeScript
npm run build

# Preview what will be created (optional)
cdk synth

# Show differences (what will change)
cdk diff
```

### Step 5: Deploy Infrastructure 🚀

```bash
# Deploy development environment
npm run deploy:dev

# OR use cdk directly
cdk deploy RapidPhotoStack-Dev

# Confirm deployment when prompted:
# "Do you wish to deploy these changes (y/n)?" → y
```

**Deployment takes ~15-20 minutes** (creating RDS takes the longest).

You'll see progress in the terminal:
```
RapidPhotoStack-Dev: creating CloudFormation changeset...
 ✅  RapidPhotoStack-Dev.VPCId = vpc-xxxxx
 ✅  RapidPhotoStack-Dev.PhotoBucketName = rapidphoto-uploads-dev-xxxxx
 ✅  RapidPhotoStack-Dev.DatabaseEndpoint = xxxxx.rds.amazonaws.com
 ...
```

---

## What Gets Created

### Networking
- **VPC** with public/private/isolated subnets across 3 AZs
- **Internet Gateway** for public internet access
- **NAT Gateways** for private subnet outbound traffic
- **Security Groups** for EC2, RDS, Redis, ALB

### Storage
- **S3 Photo Bucket** - Primary photo storage
  - Versioning enabled
  - CORS configured for direct uploads
  - Lifecycle policy: Incomplete uploads deleted after 7 days
- **S3 Thumbnail Bucket** - Generated thumbnails for CDN

### Database
- **RDS PostgreSQL 15.4**
  - Multi-AZ (production) or single AZ (dev)
  - Automated backups (7-30 days retention)
  - Encrypted at rest
  - CloudWatch logs exported
  - Credentials stored in Secrets Manager

### Cache
- **ElastiCache Redis 7.0**
  - For session state and rate limiting
  - Automated minor version upgrades

### Compute
- **Application Load Balancer** (ALB)
  - Internet-facing
  - Health checks on `/actuator/health`
  - HTTP listener (port 80)
- **Auto Scaling Group**
  - 1-3 instances (dev) or 2-10 instances (prod)
  - EC2 instances with Amazon Linux 2023
  - Java 17 pre-installed
  - Scales on CPU (70%) and request count
  - User data script prepares for Spring Boot deployment

### CDN
- **CloudFront Distribution**
  - Global edge locations for thumbnails
  - API traffic routed to ALB
  - HTTPS enforced

### Monitoring
- **CloudWatch Alarms**
  - ALB high response time
  - Database high CPU
- **CloudWatch Logs** - Database logs exported

### Security
- **IAM Roles** - EC2 instances can access S3, Secrets Manager
- **Secrets Manager** - Database credentials (auto-generated)
- **Security Groups** - Network isolation (least privilege)

---

## Outputs (After Deployment)

CDK will display important values:

```
Outputs:
RapidPhotoStack-Dev.PhotoBucketName = rapidphoto-uploads-dev-123456789
RapidPhotoStack-Dev.DatabaseEndpoint = rapidphoto-db.xxxxx.us-east-1.rds.amazonaws.com
RapidPhotoStack-Dev.DatabaseSecretArn = arn:aws:secretsmanager:...
RapidPhotoStack-Dev.RedisEndpoint = rapidphoto-redis.xxxxx.cache.amazonaws.com
RapidPhotoStack-Dev.LoadBalancerDNS = rapidphoto-alb-xxxxx.us-east-1.elb.amazonaws.com
RapidPhotoStack-Dev.CloudFrontDomain = xxxxx.cloudfront.net
```

**Save these values** - you'll need them for Spring Boot configuration!

---

## Next Steps: Deploy Spring Boot

After infrastructure is created, deploy your Spring Boot application:

### 1. Get Database Password

```bash
# Get the secret ARN from outputs
aws secretsmanager get-secret-value \
  --secret-id <DatabaseSecretArn from outputs> \
  --query SecretString \
  --output text | jq -r .password
```

### 2. Configure Spring Boot

Update `application.yml` with the output values:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://<DatabaseEndpoint>:5432/rapidphoto
    username: rapidphoto_admin
    password: <get-from-secrets-manager>
  data:
    redis:
      host: <RedisEndpoint>
      port: 6379

cloud:
  aws:
    s3:
      bucket: <PhotoBucketName>
      thumbnail-bucket: <ThumbnailBucketName>
    region:
      static: us-east-1
```

### 3. Build Spring Boot JAR

```bash
cd ../../backend  # Navigate to Spring Boot project
./gradlew bootJar

# JAR will be at: build/libs/rapidphoto-backend-0.0.1-SNAPSHOT.jar
```

### 4. Deploy to EC2

**Option A: Upload to S3, EC2 pulls on boot**
```bash
# Create S3 bucket for artifacts
aws s3 mb s3://rapidphoto-artifacts-123456789

# Upload JAR
aws s3 cp build/libs/rapidphoto-backend-0.0.1-SNAPSHOT.jar \
  s3://rapidphoto-artifacts-123456789/rapidphoto-backend.jar

# Update user data script in CDK to download from S3
# Then redeploy: cdk deploy
```

**Option B: Manual upload to EC2 (for testing)**
```bash
# Get EC2 instance ID from AWS Console
# Use Session Manager (no SSH needed!)
aws ssm start-session --target <instance-id>

# Once connected, upload JAR (from your local machine):
aws s3 cp rapidphoto-backend.jar s3://temp-bucket/
# Then on EC2: aws s3 cp s3://temp-bucket/rapidphoto-backend.jar /opt/rapidphoto/app.jar

# Start the service
sudo systemctl start rapidphoto
sudo systemctl status rapidphoto
```

---

## Cost Estimate

### Development Environment (1-3 users, testing)
| Service | Cost/Month |
|---------|-----------|
| EC2 (1x t3.small) | $15 |
| RDS (db.t3.small, single AZ) | $30 |
| ElastiCache (cache.t3.micro) | $12 |
| S3 (10GB storage + requests) | $1 |
| NAT Gateway | $32 |
| ALB | $22 |
| **Total** | **~$112/month** |

### Production Environment (1000 users, 100K photos/month)
| Service | Cost/Month |
|---------|-----------|
| EC2 (2-4x t3.medium) | $90 |
| RDS (db.t3.medium, Multi-AZ) | $120 |
| ElastiCache (cache.t3.medium) | $50 |
| S3 (500GB storage + requests) | $15 |
| CloudFront (1TB transfer) | $85 |
| NAT Gateway (3x) | $96 |
| ALB | $22 |
| **Total** | **~$478/month** |

**Cost Optimization Tips:**
- Use Reserved Instances (40-60% savings on EC2/RDS)
- Delete unused resources when not testing
- Use `cdk destroy` to tear down dev environment

---

## Useful Commands

```bash
# Build TypeScript
npm run build

# Watch mode (auto-compile on changes)
npm run watch

# Preview CloudFormation template
cdk synth

# Show differences (what will change)
cdk diff

# Deploy all stacks
npm run deploy

# Deploy specific environment
npm run deploy:dev
npm run deploy:prod

# Destroy infrastructure (careful!)
npm run destroy

# List all stacks
cdk list

# View stack outputs
aws cloudformation describe-stacks \
  --stack-name RapidPhotoStack-Dev \
  --query 'Stacks[0].Outputs'
```

---

## Troubleshooting

### "CDK not bootstrapped"
```bash
cdk bootstrap
```

### "Insufficient permissions"
- Your IAM user needs `AdministratorAccess` policy
- Check with: `aws sts get-caller-identity`

### Deployment stuck on RDS creation
- RDS takes 10-15 minutes to create (this is normal)
- Check progress: AWS Console → CloudFormation → Stacks → RapidPhotoStack-Dev

### Need to change configuration
1. Edit `lib/rapidphoto-stack.ts`
2. Run `npm run build`
3. Run `cdk diff` to see changes
4. Run `cdk deploy` to apply

### Access database from local machine
```bash
# Create SSH tunnel through EC2 (bastion host)
# Or use RDS Query Editor in AWS Console
```

---

## Production Deployment

When ready for production:

1. **Uncomment production stack** in `bin/app.ts`:
```typescript
new RapidPhotoStack(app, 'RapidPhotoStack-Prod', {
  environment: 'prod',
  // ...
});
```

2. **Deploy production:**
```bash
npm run deploy:prod
```

3. **Configure domain:**
   - Register domain in Route 53
   - Request ACM certificate
   - Add HTTPS listener to ALB
   - Update CloudFront to use custom domain

---

## Security Best Practices

✅ **Implemented:**
- Database credentials in Secrets Manager
- Private subnets for compute/database
- Security groups (least privilege)
- S3 block public access
- Encryption at rest (S3, RDS)
- CloudWatch logging

⚠️ **TODO before production:**
- [ ] Restrict S3 CORS to your domain (not `*`)
- [ ] Add WAF (Web Application Firewall) to ALB
- [ ] Enable MFA delete on S3 buckets
- [ ] Set up AWS Config for compliance monitoring
- [ ] Enable VPC Flow Logs
- [ ] Configure CloudTrail for audit logging
- [ ] Add HTTPS listener to ALB (need ACM certificate)

---

## Support

**Generated by:** Claude AI (RapidPhotoUpload Infrastructure)
**Date:** 2025-11-09
**CDK Version:** 2.100.0+

**Need help?**
- AWS CDK Documentation: https://docs.aws.amazon.com/cdk/
- AWS CDK Workshop: https://cdkworkshop.com/
- Claude can help you modify this stack!

---

## Clean Up (Destroy Infrastructure)

**⚠️ WARNING: This deletes all resources!**

```bash
# Destroy development environment
cdk destroy RapidPhotoStack-Dev

# You'll be prompted to confirm
# This takes ~10 minutes
```

**Note:** Some resources are retained by default (RDS snapshots, S3 buckets with data). You may need to manually delete these from AWS Console.

---

## What's Next?

1. ✅ Infrastructure deployed
2. ⏭️ Deploy Spring Boot application
3. ⏭️ Deploy React Native for Web frontend
4. ⏭️ Configure domain and HTTPS
5. ⏭️ Set up CI/CD pipeline

**Ready to start building the Spring Boot backend!** 🚀
