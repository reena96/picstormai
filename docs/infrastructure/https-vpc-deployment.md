# HTTPS/VPC Deployment Requirements - Security Remediation

**Status**: CRITICAL - BREAKING CHANGES REQUIRED
**Created**: 2025-11-09
**Story**: 0.1 (AWS Infrastructure Setup)
**Priority**: P0 - Must Fix Before Production

## Executive Summary

This document addresses two critical security issues identified in code review of the RapidPhotoAI infrastructure:

1. **HTTP-Only ALB**: Application Load Balancer currently uses HTTP only (port 80) - needs HTTPS (port 443) with ACM certificate
2. **Public Subnet Security Risk**: RDS and Redis are deployed in public subnets - should be in private subnets for defense-in-depth

**Impact**: These issues pose significant security risks and MUST be addressed before production deployment.

**Effort Estimate**: 4-6 hours for implementation, 1-2 hours for testing and validation

---

## Table of Contents

1. [Critical Issue #1: HTTPS/TLS Configuration](#critical-issue-1-httpstls-configuration)
2. [Critical Issue #2: VPC Subnet Architecture](#critical-issue-2-vpc-subnet-architecture)
3. [Implementation Guide](#implementation-guide)
4. [Deployment Checklist](#deployment-checklist)
5. [Cost Implications](#cost-implications)
6. [Testing and Validation](#testing-and-validation)
7. [Rollback Plan](#rollback-plan)
8. [References](#references)

---

## Critical Issue #1: HTTPS/TLS Configuration

### Current State (INSECURE)

```typescript
// File: infrastructure/cdk/lib/rapidphoto-stack.ts (Lines 192-200)
// PROBLEM: HTTP listener on port 80 only, no HTTPS listener

const httpListener = alb.addListener('HTTPListener', {
  port: 80,
  open: true,
  defaultAction: elbv2.ListenerAction.fixedResponse(200, {
    contentType: 'text/plain',
    messageBody: 'RapidPhotoUpload API - Use HTTPS',
  }),
});
```

### Why This Is Critical

- **Data Exposure**: Credentials, session tokens, and user data transmitted in plaintext
- **MITM Attacks**: Vulnerable to man-in-the-middle attacks
- **Compliance**: Violates GDPR, HIPAA, PCI-DSS, SOC 2 requirements
- **SEO Impact**: Search engines penalize non-HTTPS sites
- **Browser Warnings**: Modern browsers display "Not Secure" warnings

### Required Changes: HTTPS Setup

#### Step 1: Request ACM Certificate

**Option A: Domain Validation via DNS (Recommended)**

```bash
# Request certificate via AWS Console or CLI
aws acm request-certificate \
  --domain-name api.rapidphoto.com \
  --subject-alternative-names "*.rapidphoto.com" \
  --validation-method DNS \
  --region us-east-1

# Response includes CertificateArn and validation DNS records
```

**Important Notes**:
- ACM certificates are **FREE** (no additional cost)
- Must be in **us-east-1** region for CloudFront
- Can be in any region for ALB (use same region as ALB)
- DNS validation requires adding CNAME records to Route53 or your DNS provider
- Certificate typically validates within 30 minutes

**Option B: Email Validation**

- AWS sends validation email to domain owner
- Less preferred (manual process, requires email access)

#### Step 2: Validate Domain Ownership

```bash
# Add DNS validation record to Route53
aws route53 change-resource-record-sets \
  --hosted-zone-id Z1234567890ABC \
  --change-batch '{
    "Changes": [{
      "Action": "CREATE",
      "ResourceRecordSet": {
        "Name": "_abc123.api.rapidphoto.com",
        "Type": "CNAME",
        "TTL": 300,
        "ResourceRecords": [{"Value": "_xyz789.acm-validations.aws."}]
      }
    }]
  }'

# Check certificate status
aws acm describe-certificate --certificate-arn arn:aws:acm:us-east-1:123456789012:certificate/abc-123

# Wait for status: ISSUED
```

#### Step 3: Update CDK Stack for HTTPS

**File**: `infrastructure/cdk/lib/rapidphoto-stack.ts`

**BEFORE (Lines 183-222)**:
```typescript
const alb = new elbv2.ApplicationLoadBalancer(this, 'ALB', {
  vpc,
  internetFacing: true,
  loadBalancerName: `rapidphoto-alb-${environment}`,
  vpcSubnets: {
    subnetType: ec2.SubnetType.PUBLIC,
  },
});

// HTTP listener (redirect to HTTPS in production)
const httpListener = alb.addListener('HTTPListener', {
  port: 80,
  open: true,
  defaultAction: elbv2.ListenerAction.fixedResponse(200, {
    contentType: 'text/plain',
    messageBody: 'RapidPhotoUpload API - Use HTTPS',
  }),
});

// Target group for Spring Boot instances
const targetGroup = new elbv2.ApplicationTargetGroup(this, 'TargetGroup', {
  vpc,
  port: 8080, // Spring Boot default port
  protocol: elbv2.ApplicationProtocol.HTTP,
  targetType: elbv2.TargetType.INSTANCE,
  healthCheck: {
    enabled: true,
    path: '/actuator/health',
    interval: cdk.Duration.seconds(30),
    timeout: cdk.Duration.seconds(5),
    healthyThresholdCount: 2,
    unhealthyThresholdCount: 3,
    healthyHttpCodes: '200',
  },
  deregistrationDelay: cdk.Duration.seconds(30),
});

httpListener.addTargetGroups('DefaultTarget', {
  targetGroups: [targetGroup],
});
```

**AFTER (Secure HTTPS Configuration)**:
```typescript
import * as certificatemanager from 'aws-cdk-lib/aws-certificatemanager';

// Add to RapidPhotoStackProps interface
interface RapidPhotoStackProps extends cdk.StackProps {
  environment: 'dev' | 'staging' | 'prod';
  frontendDomain?: string;
  certificateArn?: string; // NEW: ACM certificate ARN for HTTPS
}

// Inside RapidPhotoStack constructor:

const alb = new elbv2.ApplicationLoadBalancer(this, 'ALB', {
  vpc,
  internetFacing: true,
  loadBalancerName: `rapidphoto-alb-${environment}`,
  vpcSubnets: {
    subnetType: ec2.SubnetType.PUBLIC,
  },
});

// Target group for Spring Boot instances (unchanged)
const targetGroup = new elbv2.ApplicationTargetGroup(this, 'TargetGroup', {
  vpc,
  port: 8080,
  protocol: elbv2.ApplicationProtocol.HTTP,
  targetType: elbv2.TargetType.INSTANCE,
  healthCheck: {
    enabled: true,
    path: '/actuator/health',
    interval: cdk.Duration.seconds(30),
    timeout: cdk.Duration.seconds(5),
    healthyThresholdCount: 2,
    unhealthyThresholdCount: 3,
    healthyHttpCodes: '200',
  },
  deregistrationDelay: cdk.Duration.seconds(30),
});

// HTTPS Listener (Primary) - Port 443
if (props.certificateArn) {
  const certificate = certificatemanager.Certificate.fromCertificateArn(
    this,
    'Certificate',
    props.certificateArn
  );

  const httpsListener = alb.addListener('HTTPSListener', {
    port: 443,
    protocol: elbv2.ApplicationProtocol.HTTPS,
    certificates: [certificate],
    sslPolicy: elbv2.SslPolicy.TLS12_EXT, // Enforce TLS 1.2 or higher
    open: true,
    defaultAction: elbv2.ListenerAction.forward([targetGroup]),
  });

  // HTTP Listener (Redirect to HTTPS) - Port 80
  alb.addListener('HTTPListener', {
    port: 80,
    open: true,
    defaultAction: elbv2.ListenerAction.redirect({
      protocol: 'HTTPS',
      port: '443',
      permanent: true, // 301 redirect
    }),
  });
} else {
  // Fallback for dev/staging without certificate (temporary)
  const httpListener = alb.addListener('HTTPListener', {
    port: 80,
    open: true,
    defaultAction: elbv2.ListenerAction.forward([targetGroup]),
  });

  // WARNING: HTTP-only mode - for development only
  console.warn('⚠️  WARNING: ALB configured for HTTP only. Add certificateArn for HTTPS.');
}
```

#### Step 4: Update CDK Deployment Configuration

**File**: `infrastructure/cdk/bin/app.ts`

**BEFORE**:
```typescript
new RapidPhotoStack(app, 'RapidPhotoStackDev', {
  environment: 'dev',
  env: {
    account: process.env.CDK_DEFAULT_ACCOUNT,
    region: process.env.CDK_DEFAULT_REGION || 'us-east-1',
  },
});
```

**AFTER**:
```typescript
new RapidPhotoStack(app, 'RapidPhotoStackDev', {
  environment: 'dev',
  certificateArn: process.env.ACM_CERTIFICATE_ARN, // Set via environment variable
  frontendDomain: 'dev.rapidphoto.com',
  env: {
    account: process.env.CDK_DEFAULT_ACCOUNT,
    region: process.env.CDK_DEFAULT_REGION || 'us-east-1',
  },
});

new RapidPhotoStack(app, 'RapidPhotoStackProd', {
  environment: 'prod',
  certificateArn: process.env.PROD_ACM_CERTIFICATE_ARN, // Different cert for prod
  frontendDomain: 'app.rapidphoto.com',
  env: {
    account: process.env.CDK_DEFAULT_ACCOUNT,
    region: process.env.CDK_DEFAULT_REGION || 'us-east-1',
  },
});
```

#### Step 5: Update CloudFront to Use HTTPS Origin

**File**: `infrastructure/cdk/lib/rapidphoto-stack.ts` (Lines 386-406)

**BEFORE**:
```typescript
additionalBehaviors: {
  '/api/*': {
    origin: new origins.LoadBalancerV2Origin(alb, {
      protocolPolicy: cloudfront.OriginProtocolPolicy.HTTP_ONLY, // INSECURE
    }),
    viewerProtocolPolicy: cloudfront.ViewerProtocolPolicy.REDIRECT_TO_HTTPS,
    allowedMethods: cloudfront.AllowedMethods.ALLOW_ALL,
    cachePolicy: cloudfront.CachePolicy.CACHING_DISABLED,
    originRequestPolicy: cloudfront.OriginRequestPolicy.ALL_VIEWER,
  },
},
```

**AFTER**:
```typescript
additionalBehaviors: {
  '/api/*': {
    origin: new origins.LoadBalancerV2Origin(alb, {
      protocolPolicy: cloudfront.OriginProtocolPolicy.HTTPS_ONLY, // SECURE
      // CloudFront will verify ALB certificate
    }),
    viewerProtocolPolicy: cloudfront.ViewerProtocolPolicy.REDIRECT_TO_HTTPS,
    allowedMethods: cloudfront.AllowedMethods.ALLOW_ALL,
    cachePolicy: cloudfront.CachePolicy.CACHING_DISABLED,
    originRequestPolicy: cloudfront.OriginRequestPolicy.ALL_VIEWER,
  },
},
```

---

## Critical Issue #2: VPC Subnet Architecture

### Current State (INSECURE)

```typescript
// File: infrastructure/cdk/lib/rapidphoto-stack.ts (Lines 31-33, 126-128, 161)

// Using default VPC (only has public subnets)
const vpc = ec2.Vpc.fromLookup(this, 'DefaultVPC', {
  isDefault: true,
});

// RDS in PUBLIC subnet - SECURITY RISK
const database = new rds.DatabaseInstance(this, 'PostgresDB', {
  // ...
  vpcSubnets: {
    subnetType: ec2.SubnetType.PUBLIC, // PROBLEM: Database accessible from internet
  },
  publiclyAccessible: false, // This helps but defense-in-depth requires private subnet
});

// Redis in PUBLIC subnet - SECURITY RISK
const redisSubnetGroup = new elasticache.CfnSubnetGroup(this, 'RedisSubnetGroup', {
  description: 'Subnet group for RapidPhoto Redis',
  subnetIds: vpc.publicSubnets.map(subnet => subnet.subnetId), // PROBLEM
});
```

### Why This Is Critical

**Defense-in-Depth Principle Violated**:
- **Public Subnet**: Has route to Internet Gateway (0.0.0.0/0 → IGW)
- **Private Subnet**: No direct route to Internet Gateway
- **Isolated Subnet**: No route to Internet, no NAT Gateway

**Current Risk**:
- Even with `publiclyAccessible: false`, databases are in subnets with internet routes
- Security group misconfiguration could expose database to internet
- Compliance frameworks (SOC 2, PCI-DSS) require private subnets for databases

**Attack Surface**:
```
Current (Insecure):
Internet → IGW → Public Subnet → RDS/Redis
         ↑                  ↑
    Route exists      Security Group only defense (single point of failure)

Required (Secure):
Internet → IGW → Public Subnet (ALB only)
                        ↓
                 Private Subnet → RDS/Redis
                        ↑
                   No IGW route (network-level defense)
                        +
                 Security Group (defense-in-depth)
```

### Required Changes: Private Subnet Architecture

#### Option A: Create Custom VPC with Private Subnets (Recommended)

**File**: `infrastructure/cdk/lib/rapidphoto-stack.ts` (Lines 27-33)

**BEFORE**:
```typescript
// ========================================
// VPC - Network Foundation (Using Default VPC)
// ========================================
// Using existing default VPC to avoid hitting AWS VPC limits
const vpc = ec2.Vpc.fromLookup(this, 'DefaultVPC', {
  isDefault: true,
});
```

**AFTER**:
```typescript
// ========================================
// VPC - Network Foundation (Custom VPC with Private Subnets)
// ========================================

const vpc = new ec2.Vpc(this, 'RapidPhotoVPC', {
  vpcName: `rapidphoto-vpc-${environment}`,
  maxAzs: 2, // Multi-AZ for high availability
  natGateways: environment === 'prod' ? 2 : 1, // HA for prod, cost-optimized for dev

  // Subnet Configuration (3-tier architecture)
  subnetConfiguration: [
    {
      // Public Subnets - ALB, NAT Gateway, Bastion (optional)
      name: 'Public',
      subnetType: ec2.SubnetType.PUBLIC,
      cidrMask: 24,
      mapPublicIpOnLaunch: true,
    },
    {
      // Private Subnets - EC2 instances, Lambda functions
      name: 'Private',
      subnetType: ec2.SubnetType.PRIVATE_WITH_EGRESS,
      cidrMask: 24,
      // Has route to NAT Gateway for internet access (software updates, AWS API calls)
    },
    {
      // Isolated Subnets - RDS, Redis (no internet access)
      name: 'Isolated',
      subnetType: ec2.SubnetType.PRIVATE_ISOLATED,
      cidrMask: 24,
      // No route to internet - maximum security for databases
    },
  ],

  // IP Address Range
  ipAddresses: ec2.IpAddresses.cidr('10.0.0.0/16'),

  // Enable DNS
  enableDnsHostnames: true,
  enableDnsSupport: true,
});

// VPC Flow Logs for security monitoring (optional but recommended)
vpc.addFlowLog('FlowLog', {
  destination: ec2.FlowLogDestination.toCloudWatchLogs(),
  trafficType: ec2.FlowLogTrafficType.ALL,
});
```

#### Update RDS to Use Isolated Subnets

**File**: `infrastructure/cdk/lib/rapidphoto-stack.ts` (Lines 116-145)

**BEFORE**:
```typescript
const database = new rds.DatabaseInstance(this, 'PostgresDB', {
  // ...
  vpcSubnets: {
    subnetType: ec2.SubnetType.PUBLIC, // INSECURE
  },
  publiclyAccessible: false,
  // ...
});
```

**AFTER**:
```typescript
const database = new rds.DatabaseInstance(this, 'PostgresDB', {
  // ...
  vpcSubnets: {
    subnetType: ec2.SubnetType.PRIVATE_ISOLATED, // SECURE: No internet route
    // RDS will create subnet group across all isolated subnets (multi-AZ)
  },
  publiclyAccessible: false, // Defense-in-depth: both subnet + setting
  // ...
});
```

#### Update Redis to Use Isolated Subnets

**File**: `infrastructure/cdk/lib/rapidphoto-stack.ts` (Lines 158-176)

**BEFORE**:
```typescript
const redisSubnetGroup = new elasticache.CfnSubnetGroup(this, 'RedisSubnetGroup', {
  description: 'Subnet group for RapidPhoto Redis',
  subnetIds: vpc.publicSubnets.map(subnet => subnet.subnetId), // INSECURE
  cacheSubnetGroupName: `rapidphoto-redis-subnet-${environment}`,
});
```

**AFTER**:
```typescript
const redisSubnetGroup = new elasticache.CfnSubnetGroup(this, 'RedisSubnetGroup', {
  description: 'Subnet group for RapidPhoto Redis',
  subnetIds: vpc.isolatedSubnets.map(subnet => subnet.subnetId), // SECURE
  cacheSubnetGroupName: `rapidphoto-redis-subnet-${environment}`,
});
```

#### Update EC2 Instances to Use Private Subnets

**File**: `infrastructure/cdk/lib/rapidphoto-stack.ts` (Lines 346-368)

**BEFORE**:
```typescript
const asg = new autoscaling.AutoScalingGroup(this, 'AutoScalingGroup', {
  vpc,
  // ...
  vpcSubnets: {
    subnetType: ec2.SubnetType.PUBLIC, // Instances get public IPs
  },
  // ...
});
```

**AFTER**:
```typescript
const asg = new autoscaling.AutoScalingGroup(this, 'AutoScalingGroup', {
  vpc,
  // ...
  vpcSubnets: {
    subnetType: ec2.SubnetType.PRIVATE_WITH_EGRESS, // Private subnet with NAT Gateway
    // Instances can access internet (for yum updates, AWS API) but not directly accessible
  },
  // ...
});
```

**Why PRIVATE_WITH_EGRESS for EC2**:
- Instances need internet access for:
  - Software updates (`yum update`)
  - AWS API calls (S3, Secrets Manager, CloudWatch)
  - Downloading Spring Boot JAR from artifact repository
- NAT Gateway provides outbound internet access without exposing instances

#### Option B: Keep Default VPC (NOT RECOMMENDED)

If you must use the default VPC (e.g., VPC quota constraints), implement these mitigations:

```typescript
// WORKAROUND: Add extra security layers with default VPC

// 1. Restrict security group rules to absolute minimum
dbSecurityGroup.addIngressRule(
  ec2SecurityGroup, // Only EC2 instances
  ec2.Port.tcp(5432),
  'Allow Spring Boot to access PostgreSQL'
);

// 2. Enable VPC Flow Logs for monitoring
const defaultVpc = ec2.Vpc.fromLookup(this, 'DefaultVPC', { isDefault: true });
defaultVpc.addFlowLog('FlowLog', {
  destination: ec2.FlowLogDestination.toCloudWatchLogs(),
  trafficType: ec2.FlowLogTrafficType.REJECT, // Log rejected traffic
});

// 3. Add Network ACLs (additional layer beyond security groups)
// ... (advanced configuration)

// 4. Enable GuardDuty for threat detection
// ... (requires separate AWS service)
```

**Important**: This is NOT a substitute for proper subnet architecture. Use only for development/testing.

---

## VPC Subnet Architecture Explained

### Public Subnet
- **Route Table**: Has route to Internet Gateway (0.0.0.0/0 → IGW)
- **Use Case**: Load balancers, NAT Gateways, Bastion hosts
- **Security**: Exposed to internet (firewall via Security Groups required)

### Private Subnet with Egress (PRIVATE_WITH_EGRESS)
- **Route Table**: Has route to NAT Gateway (0.0.0.0/0 → NAT-GW → IGW)
- **Use Case**: Application servers, Lambda functions
- **Security**: Can initiate outbound connections, cannot receive inbound from internet

### Isolated Subnet (PRIVATE_ISOLATED)
- **Route Table**: No route to internet (local VPC routes only)
- **Use Case**: Databases, Redis, sensitive data stores
- **Security**: Completely isolated from internet (highest security)

### Architecture Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                         Internet                             │
└──────────────────────────┬──────────────────────────────────┘
                           │
                   ┌───────▼────────┐
                   │ Internet Gateway│
                   └───────┬────────┘
                           │
        ┌──────────────────┼──────────────────┐
        │                  │                  │
┌───────▼────────┐  ┌──────▼───────┐  ┌──────▼───────┐
│  Public Subnet │  │Public Subnet │  │ Public Subnet│
│  (AZ-1)        │  │  (AZ-2)      │  │  (AZ-3)      │
│  - ALB         │  │  - ALB       │  │  - ALB       │
│  - NAT Gateway │  │  - NAT GW    │  │              │
└───────┬────────┘  └──────┬───────┘  └──────────────┘
        │                  │
        │ (NAT)            │ (NAT)
        │                  │
┌───────▼────────┐  ┌──────▼───────┐  ┌──────────────┐
│ Private Subnet │  │Private Subnet│  │Private Subnet│
│  (AZ-1)        │  │  (AZ-2)      │  │  (AZ-3)      │
│  - EC2 (ASG)   │  │  - EC2 (ASG) │  │  - EC2 (ASG) │
│  - Lambda      │  │  - Lambda    │  │  - Lambda    │
└───────┬────────┘  └──────┬───────┘  └──────┬───────┘
        │                  │                  │
        │ (Local VPC only) │                  │
        │                  │                  │
┌───────▼────────┐  ┌──────▼───────┐  ┌──────▼───────┐
│Isolated Subnet │  │Isolated Sub. │  │Isolated Sub. │
│  (AZ-1)        │  │  (AZ-2)      │  │  (AZ-3)      │
│  - RDS Primary │  │  - RDS Stand.│  │              │
│  - Redis       │  │  - Redis     │  │              │
└────────────────┘  └──────────────┘  └──────────────┘

Legend:
→ : Traffic flow allowed
↛ : Traffic flow blocked
```

---

## Implementation Guide

### Prerequisites

1. **Domain Name**: Registered domain (e.g., rapidphoto.com)
2. **Route53 Hosted Zone**: DNS management (or access to DNS provider)
3. **AWS Account**: With appropriate IAM permissions
4. **CDK Environment**: AWS CDK v2 installed and configured

### Phase 1: HTTPS Setup (2-3 hours)

#### Step 1.1: Request ACM Certificate
```bash
# Request certificate (replace with your domain)
aws acm request-certificate \
  --domain-name api.rapidphoto.com \
  --subject-alternative-names "*.rapidphoto.com" \
  --validation-method DNS \
  --region us-east-1 \
  --tags Key=Environment,Value=prod Key=Project,Value=RapidPhotoAI

# Save the CertificateArn from response
```

#### Step 1.2: Validate Domain (30 minutes wait)
```bash
# Get validation CNAME records
aws acm describe-certificate \
  --certificate-arn arn:aws:acm:us-east-1:ACCOUNT:certificate/CERT-ID

# Add CNAME records to Route53 or DNS provider
# Wait for status to change from PENDING_VALIDATION to ISSUED
```

#### Step 1.3: Update CDK Code
```bash
cd /Users/reena/gauntletai/picstormai/infrastructure/cdk

# Edit lib/rapidphoto-stack.ts - apply HTTPS changes from above
# Edit bin/app.ts - add certificateArn parameter
```

#### Step 1.4: Deploy HTTPS Changes
```bash
# Set environment variable
export ACM_CERTIFICATE_ARN="arn:aws:acm:us-east-1:ACCOUNT:certificate/CERT-ID"

# Diff changes before deployment
npm run cdk diff RapidPhotoStackDev

# Deploy (will update ALB listeners)
npm run cdk deploy RapidPhotoStackDev

# Deployment time: 5-10 minutes (ALB listener update)
```

#### Step 1.5: Test HTTPS
```bash
# Get ALB DNS name
aws elbv2 describe-load-balancers \
  --names rapidphoto-alb-dev \
  --query 'LoadBalancers[0].DNSName' \
  --output text

# Test HTTPS endpoint
curl -v https://<ALB-DNS-NAME>/actuator/health

# Test HTTP redirect
curl -v http://<ALB-DNS-NAME>
# Should return 301 redirect to https://
```

### Phase 2: VPC Migration (2-3 hours)

**WARNING**: This requires destroying and recreating infrastructure. Database data will be lost unless you create snapshots first.

#### Step 2.1: Backup Existing Data
```bash
# Create RDS snapshot before migration
aws rds create-db-snapshot \
  --db-instance-identifier rapidphoto-db-dev \
  --db-snapshot-identifier rapidphoto-migration-backup-$(date +%Y%m%d)

# Wait for snapshot to complete
aws rds wait db-snapshot-completed \
  --db-snapshot-identifier rapidphoto-migration-backup-$(date +%Y%m%d)
```

#### Step 2.2: Update CDK Code
```bash
cd /Users/reena/gauntletai/picstormai/infrastructure/cdk

# Edit lib/rapidphoto-stack.ts - apply VPC changes from above
# - Replace default VPC lookup with new VPC creation
# - Update RDS subnet type to PRIVATE_ISOLATED
# - Update Redis subnet group to use isolated subnets
# - Update EC2 ASG to use PRIVATE_WITH_EGRESS subnets
```

#### Step 2.3: Preview Changes
```bash
# Diff will show VPC replacement (destructive)
npm run cdk diff RapidPhotoStackDev

# Expected changes:
# [-] AWS::EC2::VPC (lookup) → [+] AWS::EC2::VPC (create)
# [~] RDS::DBInstance (replace - data loss!)
# [~] ElastiCache::CacheCluster (replace - cache cleared)
```

#### Step 2.4: Deploy VPC Changes
```bash
# This is DESTRUCTIVE - data loss expected
npm run cdk deploy RapidPhotoStackDev --require-approval never

# Deployment time: 15-25 minutes
# - VPC creation: 2 minutes
# - NAT Gateway creation: 2 minutes
# - RDS creation: 10-15 minutes
# - ElastiCache creation: 5 minutes
```

#### Step 2.5: Restore Data (if needed)
```bash
# Restore RDS from snapshot (manual - requires console or AWS CLI)
# Note: Snapshot restore creates new DB instance, update connection strings

# Or run database migrations to recreate schema
cd /Users/reena/gauntletai/picstormai/backend
./gradlew flywayMigrate
```

---

## Deployment Checklist

### Pre-Deployment

- [ ] **Domain Setup**
  - [ ] Domain registered and accessible
  - [ ] Route53 hosted zone created (or DNS provider configured)
  - [ ] DNS propagation verified (`dig api.rapidphoto.com`)

- [ ] **ACM Certificate**
  - [ ] Certificate requested via ACM
  - [ ] DNS validation CNAME records added
  - [ ] Certificate status: ISSUED (verified in ACM console)
  - [ ] Certificate ARN saved for CDK deployment

- [ ] **Backup Strategy**
  - [ ] RDS snapshot created (if migrating existing database)
  - [ ] Redis data backed up (if critical cache data exists)
  - [ ] S3 buckets verified (versioning ensures protection)

- [ ] **Testing Environment**
  - [ ] Deploy changes to dev environment first
  - [ ] Staging environment available for validation
  - [ ] Production deployment scheduled during maintenance window

### Infrastructure Changes

- [ ] **Code Changes**
  - [ ] HTTPS listener configuration added to CDK stack
  - [ ] HTTP to HTTPS redirect configured
  - [ ] VPC creation with public/private/isolated subnets
  - [ ] RDS subnet changed to PRIVATE_ISOLATED
  - [ ] Redis subnet changed to PRIVATE_ISOLATED
  - [ ] EC2 ASG subnet changed to PRIVATE_WITH_EGRESS
  - [ ] CloudFront origin protocol changed to HTTPS_ONLY
  - [ ] Certificate ARN parameterized (not hardcoded)

- [ ] **CDK Validation**
  - [ ] `npm run build` succeeds
  - [ ] `npm run cdk synth` generates valid CloudFormation
  - [ ] `npm run cdk diff` reviewed (destructive changes identified)
  - [ ] No TypeScript errors or warnings

### Security Configuration

- [ ] **Security Groups**
  - [ ] ALB security group allows 443 (HTTPS) inbound
  - [ ] ALB security group allows 80 (HTTP) for redirect
  - [ ] EC2 security group allows ALB on port 8080
  - [ ] RDS security group allows EC2 on port 5432 only
  - [ ] Redis security group allows EC2 on port 6379 only
  - [ ] No security group allows 0.0.0.0/0 except ALB ports 80/443

- [ ] **Network ACLs** (optional but recommended)
  - [ ] Default VPC NACLs reviewed
  - [ ] Custom NACLs configured for isolated subnets

- [ ] **IAM Permissions**
  - [ ] EC2 instance role has S3 access
  - [ ] EC2 instance role has Secrets Manager access
  - [ ] EC2 instance role has CloudWatch Logs access
  - [ ] No overly permissive policies (e.g., `*` actions)

### Testing and Validation

- [ ] **HTTPS Validation**
  - [ ] HTTPS endpoint accessible: `curl https://api.rapidphoto.com/actuator/health`
  - [ ] HTTP redirects to HTTPS: `curl -I http://api.rapidphoto.com`
  - [ ] SSL Labs grade A or higher: https://www.ssllabs.com/ssltest/
  - [ ] Certificate matches domain (no browser warnings)
  - [ ] TLS 1.2+ enforced (TLS 1.0/1.1 disabled)

- [ ] **Network Security**
  - [ ] RDS not accessible from internet (test from external IP)
  - [ ] Redis not accessible from internet
  - [ ] EC2 instances not directly accessible (no public IPs)
  - [ ] ALB health checks passing (target group healthy)
  - [ ] EC2 can reach RDS: `psql -h <rds-endpoint> -U rapidphoto_admin`
  - [ ] EC2 can reach Redis: `redis-cli -h <redis-endpoint> PING`

- [ ] **Application Functionality**
  - [ ] Spring Boot application starts successfully
  - [ ] Database migrations applied
  - [ ] API endpoints responding correctly
  - [ ] File uploads to S3 working
  - [ ] CloudFront serving content correctly

- [ ] **Monitoring**
  - [ ] CloudWatch alarms configured and active
  - [ ] VPC Flow Logs enabled (verify logs in CloudWatch)
  - [ ] ALB access logs enabled (optional)
  - [ ] RDS enhanced monitoring enabled

### Post-Deployment

- [ ] **DNS Configuration**
  - [ ] Route53 A record created: api.rapidphoto.com → ALB
  - [ ] Route53 CNAME created: www.rapidphoto.com → CloudFront
  - [ ] DNS propagation verified: `dig api.rapidphoto.com`

- [ ] **Documentation**
  - [ ] Infrastructure diagram updated
  - [ ] Runbook updated with new endpoints
  - [ ] Certificate renewal process documented (auto-renewed by ACM)
  - [ ] Disaster recovery plan updated

- [ ] **Compliance**
  - [ ] Security audit completed
  - [ ] Compliance checklist verified (SOC 2, GDPR, etc.)
  - [ ] Penetration testing scheduled (if required)

---

## Cost Implications

### HTTPS/TLS Costs

| Component | Cost | Notes |
|-----------|------|-------|
| **ACM Certificate** | **$0/month** | FREE for certificates used with AWS services (ALB, CloudFront) |
| **ALB Data Processing** | $0.008/GB | Same cost for HTTP and HTTPS (no additional charge) |
| **ALB Listener Rules** | $0.008/rule/month | Minimal cost for redirect rule |

**Total Additional Cost for HTTPS**: **~$0/month** (essentially free)

### VPC Architecture Costs

| Component | Dev (1 NAT GW) | Prod (2 NAT GW) | Notes |
|-----------|----------------|-----------------|-------|
| **NAT Gateway** | $32.40/month | $64.80/month | $0.045/hour per NAT Gateway |
| **NAT Gateway Data** | $0.045/GB | $0.045/GB | Outbound data processing |
| **VPC (itself)** | $0/month | $0/month | No charge for VPC creation |
| **Elastic IPs** | $0/month | $0/month | Free when attached to NAT Gateway |

**Estimated Monthly Cost**:
- **Dev Environment**: +$32.40/month (1 NAT Gateway)
- **Prod Environment**: +$64.80/month (2 NAT Gateways for HA)

**Data Transfer Example** (Dev):
- 100 GB outbound per month: 100 × $0.045 = $4.50
- **Total**: $32.40 + $4.50 = **$36.90/month**

### Cost Optimization Strategies

#### Option 1: Single NAT Gateway (Dev Only)
```typescript
const vpc = new ec2.Vpc(this, 'RapidPhotoVPC', {
  maxAzs: 2,
  natGateways: 1, // Single NAT Gateway shared across AZs
  // Cost: $32.40/month (saves $32.40/month in dev)
  // Risk: Single point of failure (acceptable for dev)
});
```

#### Option 2: NAT Instance Instead of NAT Gateway (Dev Only)
```typescript
// Use t4g.nano NAT instance (~$3/month) instead of NAT Gateway ($32/month)
// Requires custom configuration - not recommended for production
// Complexity vs. cost trade-off
```

#### Option 3: VPC Endpoints for AWS Services
```typescript
// Add VPC endpoints to avoid NAT Gateway for AWS API calls
const vpc = new ec2.Vpc(this, 'RapidPhotoVPC', {
  // ...
});

// S3 Gateway Endpoint (FREE - no additional cost)
vpc.addGatewayEndpoint('S3Endpoint', {
  service: ec2.GatewayVpcEndpointAwsService.S3,
  subnets: [{ subnetType: ec2.SubnetType.PRIVATE_WITH_EGRESS }],
});

// DynamoDB Gateway Endpoint (FREE)
vpc.addGatewayEndpoint('DynamoDBEndpoint', {
  service: ec2.GatewayVpcEndpointAwsService.DYNAMODB,
});

// Secrets Manager Interface Endpoint ($7.20/month per AZ)
vpc.addInterfaceEndpoint('SecretsManagerEndpoint', {
  service: ec2.InterfaceVpcEndpointAwsService.SECRETS_MANAGER,
  subnets: { subnetType: ec2.SubnetType.PRIVATE_WITH_EGRESS },
});

// CloudWatch Logs Interface Endpoint ($7.20/month per AZ)
vpc.addInterfaceEndpoint('CloudWatchLogsEndpoint', {
  service: ec2.InterfaceVpcEndpointAwsService.CLOUDWATCH_LOGS,
});

// Cost: S3/DDB endpoints are FREE
// Cost: Interface endpoints ~$7.20/AZ/month
// Benefit: Reduces NAT Gateway data transfer costs
```

**Recommendation**:
- **Dev**: Use 1 NAT Gateway ($32/month) - simplest approach
- **Prod**: Use 2 NAT Gateways ($65/month) - high availability
- **Future**: Add VPC endpoints for S3 and Secrets Manager to reduce NAT data costs

### Total Cost Summary

| Environment | Current Cost | New Cost | Increase | Notes |
|-------------|--------------|----------|----------|-------|
| **Dev** | $113/month | $149/month | +$36/month | 1 NAT Gateway + data |
| **Prod** | $478/month | $543/month | +$65/month | 2 NAT Gateways (HA) |

**Cost Increase**: ~32% for dev, ~14% for prod

**Security Value**: Priceless - protects against data breaches (avg. cost: $4.45 million per IBM 2023 report)

---

## Rollback Plan

### Rollback HTTPS Changes (Low Risk)

If HTTPS deployment causes issues:

```bash
# Rollback to previous CDK stack version
cd /Users/reena/gauntletai/picstormai/infrastructure/cdk

# Option 1: Revert code changes
git revert <commit-hash>
npm run cdk deploy RapidPhotoStackDev

# Option 2: Remove certificate parameter (fallback to HTTP)
unset ACM_CERTIFICATE_ARN
npm run cdk deploy RapidPhotoStackDev

# Time to rollback: 5-10 minutes (ALB listener update)
```

**Impact**: Low - ALB listener changes are non-destructive

### Rollback VPC Changes (High Risk - Data Loss)

**WARNING**: VPC migration is destructive. Rollback requires restoring from snapshot.

```bash
# Step 1: Identify RDS snapshot created before migration
aws rds describe-db-snapshots \
  --db-instance-identifier rapidphoto-db-dev \
  --query 'DBSnapshots[?SnapshotCreateTime>=`2025-11-09`]'

# Step 2: Destroy new stack
cd /Users/reena/gauntletai/picstormai/infrastructure/cdk
npm run cdk destroy RapidPhotoStackDev

# Step 3: Revert CDK code to use default VPC
git revert <commit-hash>

# Step 4: Redeploy with old VPC configuration
npm run cdk deploy RapidPhotoStackDev

# Step 5: Restore RDS from snapshot (manual process)
# - Use AWS Console or CLI to restore snapshot
# - Update CDK outputs with new RDS endpoint

# Time to rollback: 20-30 minutes
```

**Prevention Strategy**: Test VPC migration in dev environment first, then staging, before production.

---

## Testing and Validation

### HTTPS Testing

#### Test 1: HTTPS Endpoint Accessibility
```bash
# Test HTTPS listener
curl -v https://<ALB-DNS-NAME>/actuator/health

# Expected: 200 OK, valid SSL certificate
# Check for:
# - TLS 1.2 or higher
# - Valid certificate chain
# - No certificate errors
```

#### Test 2: HTTP to HTTPS Redirect
```bash
# Test HTTP redirect
curl -I http://<ALB-DNS-NAME>/actuator/health

# Expected:
# HTTP/1.1 301 Moved Permanently
# Location: https://<ALB-DNS-NAME>/actuator/health
```

#### Test 3: SSL Labs Grade
```bash
# Test with SSL Labs (online tool)
# https://www.ssllabs.com/ssltest/analyze.html?d=<YOUR-DOMAIN>

# Target: Grade A or A+
# Checks:
# - Certificate validity
# - Protocol support (TLS 1.2+)
# - Cipher suites
# - Forward secrecy
# - HSTS header (if configured)
```

#### Test 4: Certificate Validation
```bash
# Verify certificate details
openssl s_client -connect <ALB-DNS-NAME>:443 -servername <ALB-DNS-NAME> < /dev/null 2>&1 | openssl x509 -noout -text

# Check:
# - Issuer: Amazon (ACM)
# - Subject: CN=api.rapidphoto.com
# - Validity dates (10 months for ACM)
# - Subject Alternative Names (SAN)
```

### VPC Security Testing

#### Test 5: Database Isolation (External)
```bash
# Try to connect to RDS from external IP (should fail)
psql -h <RDS-ENDPOINT> -U rapidphoto_admin -d rapidphoto

# Expected: Connection timeout (not reachable from internet)
```

#### Test 6: Database Isolation (Internal)
```bash
# SSH to EC2 instance via SSM Session Manager
aws ssm start-session --target <INSTANCE-ID>

# Test RDS connection from EC2
psql -h <RDS-ENDPOINT> -U rapidphoto_admin -d rapidphoto

# Expected: Connection successful (same VPC)
```

#### Test 7: Redis Isolation (External)
```bash
# Try to connect to Redis from external IP (should fail)
redis-cli -h <REDIS-ENDPOINT> PING

# Expected: Connection timeout
```

#### Test 8: Redis Isolation (Internal)
```bash
# SSH to EC2 instance
aws ssm start-session --target <INSTANCE-ID>

# Install redis-cli
sudo yum install -y redis

# Test Redis connection
redis-cli -h <REDIS-ENDPOINT> PING

# Expected: PONG (connection successful)
```

#### Test 9: EC2 Internet Access (Outbound)
```bash
# SSH to EC2 instance
aws ssm start-session --target <INSTANCE-ID>

# Test outbound internet access via NAT Gateway
curl -I https://www.google.com

# Expected: 200 OK (NAT Gateway working)
```

#### Test 10: EC2 Public Access (Inbound)
```bash
# Try to SSH to EC2 instance directly (should fail)
# Get instance public IP (should not exist)
aws ec2 describe-instances \
  --instance-ids <INSTANCE-ID> \
  --query 'Reservations[0].Instances[0].PublicIpAddress'

# Expected: null (no public IP assigned)
```

### Application Testing

#### Test 11: Spring Boot Application
```bash
# Check application logs
aws logs tail /aws/ec2/rapidphoto --follow

# Test API endpoint
curl https://api.rapidphoto.com/api/v1/albums

# Expected: Valid JSON response
```

#### Test 12: S3 Upload via HTTPS
```bash
# Upload test file via API
curl -X POST https://api.rapidphoto.com/api/v1/photos \
  -H "Authorization: Bearer <TOKEN>" \
  -F "file=@test-image.jpg"

# Expected: 201 Created, photo uploaded to S3
```

#### Test 13: CloudFront HTTPS
```bash
# Test CloudFront distribution
curl -I https://<CLOUDFRONT-DOMAIN>/thumbnails/test.jpg

# Expected: 200 OK, X-Cache: Hit from cloudfront
```

### Monitoring Validation

#### Test 14: VPC Flow Logs
```bash
# Check VPC Flow Logs in CloudWatch
aws logs tail /aws/vpc/flowlogs --follow --filter-pattern "REJECT"

# Expected: Rejected traffic logged (attempted unauthorized access)
```

#### Test 15: CloudWatch Alarms
```bash
# Trigger high CPU alarm (test)
aws ec2 run-instances --instance-type t3.small --image-id ami-12345 --count 10

# Check alarm state
aws cloudwatch describe-alarms --alarm-names "RapidPhoto-DBCPUAlarm"

# Expected: Alarm transitions to ALARM state (SNS notification sent)
```

---

## References

### AWS Documentation

**AWS Certificate Manager (ACM)**:
- ACM User Guide: https://docs.aws.amazon.com/acm/latest/userguide/
- ACM Certificate Validation: https://docs.aws.amazon.com/acm/latest/userguide/dns-validation.html
- ACM Pricing (FREE): https://aws.amazon.com/certificate-manager/pricing/

**VPC and Networking**:
- VPC User Guide: https://docs.aws.amazon.com/vpc/latest/userguide/
- VPC Subnet Types: https://docs.aws.amazon.com/vpc/latest/userguide/configure-subnets.html
- NAT Gateways: https://docs.aws.amazon.com/vpc/latest/userguide/vpc-nat-gateway.html
- VPC Endpoints: https://docs.aws.amazon.com/vpc/latest/privatelink/vpc-endpoints.html
- Security Groups: https://docs.aws.amazon.com/vpc/latest/userguide/vpc-security-groups.html
- Network ACLs: https://docs.aws.amazon.com/vpc/latest/userguide/vpc-network-acls.html

**Application Load Balancer**:
- ALB HTTPS Listeners: https://docs.aws.amazon.com/elasticloadbalancing/latest/application/create-https-listener.html
- ALB Security Policies: https://docs.aws.amazon.com/elasticloadbalancing/latest/application/create-https-listener.html#describe-ssl-policies

**AWS CDK**:
- CDK VPC Construct: https://docs.aws.amazon.com/cdk/api/v2/docs/aws-cdk-lib.aws_ec2.Vpc.html
- CDK ALB Construct: https://docs.aws.amazon.com/cdk/api/v2/docs/aws-cdk-lib.aws_elasticloadbalancingv2-readme.html
- CDK ACM Construct: https://docs.aws.amazon.com/cdk/api/v2/docs/aws-cdk-lib.aws_certificatemanager-readme.html
- CDK RDS Construct: https://docs.aws.amazon.com/cdk/api/v2/docs/aws-cdk-lib.aws_rds-readme.html

**Security Best Practices**:
- AWS Well-Architected Security Pillar: https://docs.aws.amazon.com/wellarchitected/latest/security-pillar/
- AWS Security Best Practices for VPC: https://docs.aws.amazon.com/vpc/latest/userguide/vpc-security-best-practices.html
- OWASP Top 10: https://owasp.org/www-project-top-ten/

### Compliance and Standards

**Industry Standards**:
- PCI DSS Network Segmentation: https://www.pcisecuritystandards.org/
- NIST Cybersecurity Framework: https://www.nist.gov/cyberframework
- SOC 2 Trust Service Criteria: https://www.aicpa.org/soc4so

**Encryption Standards**:
- TLS 1.2+ Requirements: https://datatracker.ietf.org/doc/html/rfc5246
- TLS 1.3 Specification: https://datatracker.ietf.org/doc/html/rfc8446

### Project-Specific References

**Internal Documentation**:
- Infrastructure Code: `/Users/reena/gauntletai/picstormai/infrastructure/cdk/lib/rapidphoto-stack.ts`
- Story Documentation: `/Users/reena/gauntletai/picstormai/docs/stories/0-1-aws-infrastructure-setup.md`
- Architecture Overview: `/Users/reena/gauntletai/picstormai/docs/AWS-INFRASTRUCTURE-SUMMARY.md`
- PRD Security Requirements: `/Users/reena/gauntletai/picstormai/docs/PRD-RapidPhotoUpload.md#8.4-Security-Requirements`

---

## Appendix: Security Group Configuration Reference

### Recommended Security Group Rules

#### ALB Security Group
```typescript
// Inbound
- Port 443 (HTTPS) from 0.0.0.0/0 (internet)
- Port 80 (HTTP) from 0.0.0.0/0 (for redirect only)

// Outbound
- Port 8080 to EC2 Security Group (health checks + traffic)
```

#### EC2 Security Group
```typescript
// Inbound
- Port 8080 from ALB Security Group (Spring Boot app)
- Port 22 (SSH) - REMOVED (use SSM Session Manager instead)

// Outbound
- Port 5432 to RDS Security Group (PostgreSQL)
- Port 6379 to Redis Security Group (Redis)
- Port 443 to 0.0.0.0/0 (HTTPS for AWS APIs, yum updates)
- Port 80 to 0.0.0.0/0 (HTTP for yum updates)
```

#### RDS Security Group
```typescript
// Inbound
- Port 5432 from EC2 Security Group (PostgreSQL)

// Outbound
- None (no outbound required)
```

#### Redis Security Group
```typescript
// Inbound
- Port 6379 from EC2 Security Group (Redis)

// Outbound
- None (no outbound required)
```

---

## Document Change Log

| Date | Version | Author | Changes |
|------|---------|--------|---------|
| 2025-11-09 | 1.0 | Dev Agent | Initial documentation created for Story 0.1 |

---

**IMPORTANT**: This is a BREAKING CHANGE. Coordinate with team before implementing. Test in dev → staging → production.

**Questions?** Contact the DevOps team or refer to the AWS CDK documentation.
