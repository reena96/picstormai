#!/bin/bash
# Automated EC2 Deployment Script for RapidPhoto
# This script deploys the Spring Boot application to AWS EC2 instances

set -e  # Exit on any error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${GREEN}=== RapidPhoto EC2 Deployment Script ===${NC}"

# Configuration
AWS_REGION="us-east-1"
S3_DEPLOYMENT_BUCKET="rapidphoto-deployments-$(aws sts get-caller-identity --query Account --output text)"
JAR_NAME="rapidphoto-backend.jar"

# Find the RapidPhoto Auto Scaling Group (CloudFormation adds a suffix to the name)
ASG_NAME=$(aws autoscaling describe-auto-scaling-groups \
    --region "${AWS_REGION}" \
    --query 'AutoScalingGroups[?starts_with(AutoScalingGroupName, `RapidPhotoStack-Dev-AutoScalingGroupASG`)].AutoScalingGroupName' \
    --output text)

if [ -z "$ASG_NAME" ]; then
    echo -e "${RED}✗ Could not find RapidPhoto Auto Scaling Group${NC}"
    echo "Available ASGs:"
    aws autoscaling describe-auto-scaling-groups --region "${AWS_REGION}" --query 'AutoScalingGroups[*].AutoScalingGroupName' --output text
    exit 1
fi

echo -e "${GREEN}✓ Found Auto Scaling Group: ${ASG_NAME}${NC}"

# Step 1: Build the application
echo -e "\n${YELLOW}Step 1: Building application JAR...${NC}"
cd "$(dirname "$0")/../backend"
./gradlew clean build -x test
echo -e "${GREEN}✓ Build complete${NC}"

# Step 2: Create deployment bucket if it doesn't exist
echo -e "\n${YELLOW}Step 2: Setting up S3 deployment bucket...${NC}"
if ! aws s3 ls "s3://${S3_DEPLOYMENT_BUCKET}" 2>/dev/null; then
    aws s3 mb "s3://${S3_DEPLOYMENT_BUCKET}" --region "${AWS_REGION}"
    echo -e "${GREEN}✓ Created deployment bucket: ${S3_DEPLOYMENT_BUCKET}${NC}"
else
    echo -e "${GREEN}✓ Deployment bucket already exists${NC}"
fi

# Step 3: Upload JAR to S3
echo -e "\n${YELLOW}Step 3: Uploading JAR to S3...${NC}"
JAR_PATH="build/libs/rapidphoto-backend-0.0.1-SNAPSHOT.jar"
TIMESTAMP=$(date +%Y%m%d-%H%M%S)
S3_KEY="releases/${TIMESTAMP}/${JAR_NAME}"

aws s3 cp "${JAR_PATH}" "s3://${S3_DEPLOYMENT_BUCKET}/${S3_KEY}"
aws s3 cp "${JAR_PATH}" "s3://${S3_DEPLOYMENT_BUCKET}/latest/${JAR_NAME}"  # Also upload as "latest"
echo -e "${GREEN}✓ Uploaded to s3://${S3_DEPLOYMENT_BUCKET}/${S3_KEY}${NC}"

# Step 4: Get EC2 instance IDs from Auto Scaling Group
echo -e "\n${YELLOW}Step 4: Finding EC2 instances in Auto Scaling Group...${NC}"
INSTANCE_IDS=$(aws autoscaling describe-auto-scaling-groups \
    --auto-scaling-group-names "${ASG_NAME}" \
    --region "${AWS_REGION}" \
    --query 'AutoScalingGroups[0].Instances[?HealthStatus==`Healthy`].InstanceId' \
    --output text)

if [ -z "$INSTANCE_IDS" ]; then
    echo -e "${RED}✗ No healthy EC2 instances found in Auto Scaling Group${NC}"
    exit 1
fi

echo -e "${GREEN}✓ Found instances: ${INSTANCE_IDS}${NC}"

# Step 5: Deploy to each EC2 instance
echo -e "\n${YELLOW}Step 5: Deploying to EC2 instances...${NC}"

for INSTANCE_ID in $INSTANCE_IDS; do
    echo -e "\n${YELLOW}Deploying to ${INSTANCE_ID}...${NC}"

    # Create deployment script that will run on EC2
    DEPLOY_SCRIPT=$(cat <<'EOF'
#!/bin/bash
set -e

# Configuration
S3_BUCKET="__S3_BUCKET__"
S3_KEY="__S3_KEY__"
APP_DIR="/opt/rapidphoto"
JAR_NAME="rapidphoto-backend.jar"
AWS_REGION="us-east-1"
DB_SECRET_ARN="__DB_SECRET_ARN__"

# Install Java 21 and jq if not present
if ! java -version 2>&1 | grep -q "21"; then
    echo "Installing Amazon Corretto 21..."
    sudo yum install -y java-21-amazon-corretto-devel
fi

if ! command -v jq &> /dev/null; then
    echo "Installing jq..."
    sudo yum install -y jq
fi

# Fetch database credentials from Secrets Manager
echo "Fetching database credentials from Secrets Manager..."
DB_SECRET=$(aws secretsmanager get-secret-value \
    --secret-id "${DB_SECRET_ARN}" \
    --region "${AWS_REGION}" \
    --query 'SecretString' \
    --output text)

DB_USERNAME=$(echo "$DB_SECRET" | jq -r '.username')
DB_PASSWORD=$(echo "$DB_SECRET" | jq -r '.password')

# Get configuration from CloudFormation outputs (passed as placeholders)
DB_HOST="__DB_HOST__"
REDIS_HOST="__REDIS_HOST__"
S3_PHOTO_BUCKET="__S3_PHOTO_BUCKET__"
S3_THUMBNAIL_BUCKET="__S3_THUMBNAIL_BUCKET__"

# Generate JWT secret if not provided
JWT_SECRET="__JWT_SECRET__"
if [ "$JWT_SECRET" = "__JWT_SECRET__" ]; then
    JWT_SECRET=$(openssl rand -base64 64 | tr -d '\n')
fi

# Create application directory
sudo mkdir -p ${APP_DIR}
cd ${APP_DIR}

# Download JAR from S3
echo "Downloading application from S3..."
aws s3 cp "s3://${S3_BUCKET}/${S3_KEY}" "${APP_DIR}/${JAR_NAME}"
sudo chown ec2-user:ec2-user "${APP_DIR}/${JAR_NAME}"

# Stop existing service if running
if sudo systemctl is-active --quiet rapidphoto; then
    echo "Stopping existing service..."
    sudo systemctl stop rapidphoto
fi

# Create systemd service file with environment variables
echo "Creating systemd service with environment configuration..."
sudo tee /etc/systemd/system/rapidphoto.service > /dev/null <<SERVICE
[Unit]
Description=RapidPhoto Spring Boot Application
After=network.target

[Service]
Type=simple
User=ec2-user
WorkingDirectory=/opt/rapidphoto
ExecStart=/usr/bin/java -jar \\
    -Xms512m -Xmx1024m \\
    -Dspring.profiles.active=aws \\
    /opt/rapidphoto/rapidphoto-backend.jar
Restart=always
RestartSec=10
StandardOutput=journal
StandardError=journal
SyslogIdentifier=rapidphoto

# Environment variables
Environment="AWS_REGION=${AWS_REGION}"
Environment="DB_HOST=${DB_HOST}"
Environment="DB_PORT=5432"
Environment="DB_NAME=rapidphoto"
Environment="DB_USERNAME=${DB_USERNAME}"
Environment="DB_PASSWORD=${DB_PASSWORD}"
Environment="REDIS_HOST=${REDIS_HOST}"
Environment="REDIS_PORT=6379"
Environment="S3_BUCKET=${S3_PHOTO_BUCKET}"
Environment="S3_THUMBNAIL_BUCKET=${S3_THUMBNAIL_BUCKET}"
Environment="JWT_SECRET=${JWT_SECRET}"

[Install]
WantedBy=multi-user.target
SERVICE

# Reload systemd and start service
echo "Starting service..."
sudo systemctl daemon-reload
sudo systemctl enable rapidphoto
sudo systemctl start rapidphoto

# Wait for service to start
sleep 10

# Check service status
if sudo systemctl is-active --quiet rapidphoto; then
    echo "✓ Service started successfully"
    sudo systemctl status rapidphoto --no-pager
else
    echo "✗ Service failed to start"
    sudo journalctl -u rapidphoto -n 50 --no-pager
    exit 1
fi
EOF
)

    # Get CloudFormation outputs for environment variables
    DB_HOST=$(aws cloudformation describe-stacks \
        --stack-name RapidPhotoStack-Dev \
        --region "${AWS_REGION}" \
        --query 'Stacks[0].Outputs[?OutputKey==`DatabaseEndpoint`].OutputValue' \
        --output text)

    REDIS_HOST=$(aws cloudformation describe-stacks \
        --stack-name RapidPhotoStack-Dev \
        --region "${AWS_REGION}" \
        --query 'Stacks[0].Outputs[?OutputKey==`RedisEndpoint`].OutputValue' \
        --output text)

    S3_PHOTO_BUCKET=$(aws cloudformation describe-stacks \
        --stack-name RapidPhotoStack-Dev \
        --region "${AWS_REGION}" \
        --query 'Stacks[0].Outputs[?OutputKey==`PhotoBucketName`].OutputValue' \
        --output text)

    S3_THUMBNAIL_BUCKET=$(aws cloudformation describe-stacks \
        --stack-name RapidPhotoStack-Dev \
        --region "${AWS_REGION}" \
        --query 'Stacks[0].Outputs[?OutputKey==`ThumbnailBucketName`].OutputValue' \
        --output text)

    DB_SECRET_ARN=$(aws cloudformation describe-stacks \
        --stack-name RapidPhotoStack-Dev \
        --region "${AWS_REGION}" \
        --query 'Stacks[0].Outputs[?OutputKey==`DatabaseSecretArn`].OutputValue' \
        --output text)

    # Generate a secure JWT secret (or use existing one from deployment/.jwt-secret if it exists)
    if [ -f "$(dirname "$0")/.jwt-secret" ]; then
        JWT_SECRET=$(cat "$(dirname "$0")/.jwt-secret")
    else
        JWT_SECRET=$(openssl rand -base64 64 | tr -d '\n')
        echo "$JWT_SECRET" > "$(dirname "$0")/.jwt-secret"
        echo -e "${GREEN}✓ Generated new JWT secret and saved to deployment/.jwt-secret${NC}"
    fi

    # Replace placeholders
    DEPLOY_SCRIPT="${DEPLOY_SCRIPT//__S3_BUCKET__/$S3_DEPLOYMENT_BUCKET}"
    DEPLOY_SCRIPT="${DEPLOY_SCRIPT//__S3_KEY__/latest/$JAR_NAME}"
    DEPLOY_SCRIPT="${DEPLOY_SCRIPT//__DB_SECRET_ARN__/$DB_SECRET_ARN}"
    DEPLOY_SCRIPT="${DEPLOY_SCRIPT//__DB_HOST__/$DB_HOST}"
    DEPLOY_SCRIPT="${DEPLOY_SCRIPT//__REDIS_HOST__/$REDIS_HOST}"
    DEPLOY_SCRIPT="${DEPLOY_SCRIPT//__S3_PHOTO_BUCKET__/$S3_PHOTO_BUCKET}"
    DEPLOY_SCRIPT="${DEPLOY_SCRIPT//__S3_THUMBNAIL_BUCKET__/$S3_THUMBNAIL_BUCKET}"
    DEPLOY_SCRIPT="${DEPLOY_SCRIPT//__JWT_SECRET__/$JWT_SECRET}"

    # Upload deployment script to S3 to avoid JSON escaping issues
    SCRIPT_S3_KEY="scripts/deploy-${TIMESTAMP}.sh"
    TEMP_SCRIPT=$(mktemp)
    echo "$DEPLOY_SCRIPT" > "$TEMP_SCRIPT"
    aws s3 cp "$TEMP_SCRIPT" "s3://${S3_DEPLOYMENT_BUCKET}/${SCRIPT_S3_KEY}"

    # Execute deployment via SSM by downloading and running script from S3
    COMMAND_ID=$(aws ssm send-command \
        --instance-ids "${INSTANCE_ID}" \
        --document-name "AWS-RunShellScript" \
        --parameters commands="aws s3 cp s3://${S3_DEPLOYMENT_BUCKET}/${SCRIPT_S3_KEY} /tmp/deploy.sh && chmod +x /tmp/deploy.sh && /tmp/deploy.sh" \
        --region "${AWS_REGION}" \
        --query 'Command.CommandId' \
        --output text)

    # Clean up temp file
    rm -f "$TEMP_SCRIPT"

    echo "  Waiting for deployment to complete..."
    aws ssm wait command-executed \
        --command-id "${COMMAND_ID}" \
        --instance-id "${INSTANCE_ID}" \
        --region "${AWS_REGION}"

    # Get command output
    OUTPUT=$(aws ssm get-command-invocation \
        --command-id "${COMMAND_ID}" \
        --instance-id "${INSTANCE_ID}" \
        --region "${AWS_REGION}" \
        --query 'StandardOutputContent' \
        --output text)

    echo "${OUTPUT}"
    echo -e "${GREEN}✓ Deployed to ${INSTANCE_ID}${NC}"
done

# Step 6: Verify health checks
echo -e "\n${YELLOW}Step 6: Verifying ALB health checks...${NC}"
ALB_ARN=$(aws elbv2 describe-load-balancers \
    --names "rapidphoto-alb-dev" \
    --region "${AWS_REGION}" \
    --query 'LoadBalancers[0].LoadBalancerArn' \
    --output text 2>/dev/null || echo "")

if [ -n "$ALB_ARN" ]; then
    TARGET_GROUP_ARN=$(aws elbv2 describe-target-groups \
        --load-balancer-arn "${ALB_ARN}" \
        --region "${AWS_REGION}" \
        --query 'TargetGroups[0].TargetGroupArn' \
        --output text)

    echo "Waiting for targets to become healthy (this may take 1-2 minutes)..."
    sleep 60

    HEALTH_STATUS=$(aws elbv2 describe-target-health \
        --target-group-arn "${TARGET_GROUP_ARN}" \
        --region "${AWS_REGION}" \
        --query 'TargetHealthDescriptions[*].[Target.Id,TargetHealth.State]' \
        --output table)

    echo "${HEALTH_STATUS}"
else
    echo -e "${YELLOW}⚠ Could not find ALB for health check verification${NC}"
fi

echo -e "\n${GREEN}=== Deployment Complete ===${NC}"
echo -e "\nApplication endpoints:"
echo -e "  Load Balancer: http://rapidphoto-alb-dev-534104150.us-east-1.elb.amazonaws.com"
echo -e "  Health Check:  http://rapidphoto-alb-dev-534104150.us-east-1.elb.amazonaws.com/actuator/health"
echo -e "\nMonitoring:"
echo -e "  CloudWatch Logs: aws logs tail /aws/ec2/rapidphoto --follow"
echo -e "  Service Status:  ssh to EC2 and run 'sudo systemctl status rapidphoto'"
