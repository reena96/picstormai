# RapidPhoto AWS Deployment Guide

Complete guide for deploying RapidPhoto to AWS.

## Prerequisites

- AWS CLI installed and configured with credentials
- AWS CDK CLI installed globally (`npm install -g aws-cdk`)
- Node.js and npm installed
- Java 21 installed (for building backend)
- `jq` command-line JSON processor installed

## Quick Start

### 1. Deploy Infrastructure (One Time)

```bash
cd infrastructure/cdk
npm install
npm run deploy:dev
```

This creates:
- RDS PostgreSQL database
- ElastiCache Redis cluster
- S3 buckets (photos + thumbnails)
- Application Load Balancer
- Auto Scaling Group with EC2 instances
- CloudFront CDN
- VPC, Security Groups, IAM Roles

**Deployment Time:** ~13 minutes
**Cost:** ~$113/month for dev environment

### 2. Setup Environment Variables

```bash
cd deployment
./setup-environment.sh
```

This automatically:
- Fetches database credentials from AWS Secrets Manager
- Retrieves all AWS resource endpoints
- Generates JWT secret
- Creates `.env` file with all configuration

### 3. Deploy Backend Application

```bash
./deploy-to-ec2.sh
```

This automatically:
- Builds the Spring Boot JAR
- Uploads to S3
- Deploys to all EC2 instances via AWS Systems Manager
- Configures systemd service
- Verifies health checks via ALB

### 4. Deploy Frontend

```bash
./deploy-frontend.sh
```

This automatically:
- Builds the React web application
- Uploads to S3
- Invalidates CloudFront cache
- Updates frontend with correct API endpoint

---

## Infrastructure Details

### AWS Resources Created

Run `./setup-environment.sh` to see your specific resource endpoints.

```
CloudFront CDN:       <from CloudFormation outputs>
Load Balancer:        <from CloudFormation outputs>
Photo Bucket:         <from CloudFormation outputs>
Thumbnail Bucket:     <from CloudFormation outputs>
VPC ID:               <from CloudFormation outputs>

Database (RDS PostgreSQL):
  Endpoint:           <from Secrets Manager>
  Port:               5432
  Database:           rapidphoto

Redis (ElastiCache):
  Endpoint:           <from CloudFormation outputs>
  Port:               6379
```

### Security

All secrets are managed securely:
- **Database credentials** → AWS Secrets Manager
- **JWT secret** → Generated locally, stored in `.jwt-secret` (gitignored)
- **Environment variables** → Set on EC2 via deployment script
- **IAM roles** → EC2 instances have S3/RDS/Redis access automatically

**No secrets are committed to git.**

---

## Deployment Scripts

### `setup-environment.sh`

Fetches all configuration from AWS and prepares environment variables.

**What it does:**
1. Retrieves CloudFormation stack outputs
2. Fetches database credentials from Secrets Manager
3. Generates JWT secret (if not exists)
4. Creates `.env` file for local reference

**Output:**
- `deployment/.env` - Environment variables (gitignored)
- `deployment/.jwt-secret` - JWT secret (gitignored)

### `deploy-to-ec2.sh`

Fully automated backend deployment to EC2.

**What it does:**
1. Builds Spring Boot JAR (`./gradlew clean build`)
2. Creates S3 deployment bucket
3. Uploads JAR to S3
4. Finds all healthy EC2 instances in Auto Scaling Group
5. For each instance:
   - Installs Java 21 (if needed)
   - Downloads JAR from S3
   - Creates systemd service
   - Starts application
   - Verifies service is running
6. Waits for ALB health checks to pass

**Requirements:**
- AWS Systems Manager (SSM) agent on EC2 (pre-installed in CDK)
- IAM permissions for SSM, S3, Auto Scaling

**Usage:**
```bash
cd deployment
./deploy-to-ec2.sh
```

### `deploy-frontend.sh`

Fully automated frontend deployment to S3/CloudFront.

**What it does:**
1. Builds React web app (`npm run build:web`)
2. Updates API endpoint to use ALB
3. Uploads static files to S3
4. Configures S3 bucket for static website hosting
5. Invalidates CloudFront cache for instant updates

**Usage:**
```bash
cd deployment
./deploy-frontend.sh
```

---

## Monitoring & Operations

### View Application Logs

```bash
# On EC2 instance
sudo journalctl -u rapidphoto -f

# Or via CloudWatch Logs
aws logs tail /aws/ec2/rapidphoto --follow
```

### Check Service Status

```bash
# SSH into EC2 instance
aws ssm start-session --target <instance-id>

# Check service status
sudo systemctl status rapidphoto

# Restart service
sudo systemctl restart rapidphoto
```

### Health Check Endpoints

```bash
# ALB Health Check (get URL from .env file)
curl http://$LOAD_BALANCER_DNS/actuator/health

# Direct Instance Health Check (from inside VPC)
curl http://<instance-private-ip>:8080/actuator/health
```

### CloudWatch Alarms

Pre-configured alarms:
- **Database CPU > 80%** → SNS notification
- **ALB Response Time > 1 second** → SNS notification

View alarms:
```bash
aws cloudwatch describe-alarms --region us-east-1
```

---

## Troubleshooting

### Backend won't start

1. Check logs:
   ```bash
   sudo journalctl -u rapidphoto -n 100
   ```

2. Verify environment variables:
   ```bash
   sudo systemctl show rapidphoto | grep Environment
   ```

3. Test database connection:
   ```bash
   psql -h <RDS_ENDPOINT> -U rapidphoto_admin -d rapidphoto
   ```

### Frontend shows 404

1. Verify S3 bucket website configuration:
   ```bash
   aws s3 website s3://rapidphoto-frontend-dev --index-document index.html
   ```

2. Check CloudFront distribution status:
   ```bash
   aws cloudfront get-distribution --id <distribution-id>
   ```

### Health checks failing

1. Check security groups allow ALB → EC2 on port 8080
2. Verify application is running on port 8080
3. Ensure `/actuator/health` endpoint returns 200 OK

---

## Cost Breakdown

### Development Environment (~$113/month)

- RDS db.t3.small: $37/month
- ElastiCache cache.t3.micro: $15/month
- EC2 t3.small (2 instances): $30/month
- ALB: $20/month
- S3 + Data Transfer: $5/month
- CloudFront: $1/month
- Secrets Manager: $0.40/month
- CloudWatch: $5/month

### Production Environment (~$478/month)

- RDS db.r5.large: $200/month
- ElastiCache cache.r5.large: $120/month
- EC2 t3.medium (4 instances): $100/month
- ALB: $20/month
- S3 + Data Transfer: $20/month
- CloudFront: $10/month
- Secrets Manager: $0.40/month
- CloudWatch: $8/month

---

## Cleanup

To destroy all AWS resources:

```bash
cd infrastructure/cdk
npm run destroy:dev
```

**Warning:** This will delete:
- All databases (data will be lost)
- All S3 buckets and uploaded photos
- All infrastructure

Make sure to backup any important data first!

---

## CI/CD Integration

### GitHub Actions Example

```yaml
name: Deploy to AWS

on:
  push:
    branches: [main]

jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3

      - name: Configure AWS credentials
        uses: aws-actions/configure-aws-credentials@v2
        with:
          aws-access-key-id: ${{ secrets.AWS_ACCESS_KEY_ID }}
          aws-secret-access-key: ${{ secrets.AWS_SECRET_ACCESS_KEY }}
          aws-region: us-east-1

      - name: Setup environment
        run: |
          cd deployment
          ./setup-environment.sh

      - name: Deploy backend
        run: |
          cd deployment
          ./deploy-to-ec2.sh

      - name: Deploy frontend
        run: |
          cd deployment
          ./deploy-frontend.sh
```

---

## Support

For issues or questions:
- Check logs: `sudo journalctl -u rapidphoto -f`
- View CloudWatch metrics in AWS Console
- Check Auto Scaling Group status
- Verify ALB target health

## Next Steps

1. Set up custom domain with Route 53
2. Configure SSL certificate with ACM
3. Enable CloudWatch detailed monitoring
4. Set up automated backups (RDS snapshots)
5. Configure CloudWatch Logs retention
6. Add monitoring dashboards
7. Set up SNS notifications for alarms
