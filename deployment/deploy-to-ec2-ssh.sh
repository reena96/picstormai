#!/bin/bash
# Automated EC2 Deployment Script for RapidPhoto (SSH Method)
# This script deploys the Spring Boot application to AWS EC2 instances via SSH

set -e  # Exit on any error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${GREEN}=== RapidPhoto EC2 Deployment Script (SSH) ===${NC}"

# Configuration
AWS_REGION="us-east-1"
S3_DEPLOYMENT_BUCKET="rapidphoto-deployments-$(aws sts get-caller-identity --query Account --output text)"
JAR_NAME="rapidphoto-backend.jar"
SSH_KEY_PATH="${SSH_KEY_PATH:-~/.ssh/rapidphoto-key.pem}"

# Check if SSH key exists
if [ ! -f "$SSH_KEY_PATH" ]; then
    echo -e "${RED}✗ SSH key not found at: ${SSH_KEY_PATH}${NC}"
    echo "Please set SSH_KEY_PATH environment variable or place your key at ~/.ssh/rapidphoto-key.pem"
    exit 1
fi

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

# Step 4: Get EC2 instance IDs and public IPs from Auto Scaling Group
echo -e "\n${YELLOW}Step 4: Finding EC2 instances in Auto Scaling Group...${NC}"
INSTANCE_INFO=$(aws autoscaling describe-auto-scaling-groups \
    --auto-scaling-group-names "${ASG_NAME}" \
    --region "${AWS_REGION}" \
    --query 'AutoScalingGroups[0].Instances[?HealthStatus==`Healthy`].[InstanceId]' \
    --output text)

if [ -z "$INSTANCE_INFO" ]; then
    echo -e "${RED}✗ No healthy EC2 instances found in Auto Scaling Group${NC}"
    exit 1
fi

echo -e "${GREEN}✓ Found healthy instances${NC}"

# Step 5: Deploy to each EC2 instance via SSH
echo -e "\n${YELLOW}Step 5: Deploying to EC2 instances via SSH...${NC}"

for INSTANCE_ID in $INSTANCE_INFO; do
    echo -e "\n${YELLOW}Deploying to ${INSTANCE_ID}...${NC}"

    # Get public IP
    PUBLIC_IP=$(aws ec2 describe-instances \
        --instance-ids "${INSTANCE_ID}" \
        --region "${AWS_REGION}" \
        --query 'Reservations[0].Instances[0].PublicIpAddress' \
        --output text)

    if [ -z "$PUBLIC_IP" ] || [ "$PUBLIC_IP" == "None" ]; then
        echo -e "${RED}✗ No public IP found for ${INSTANCE_ID}${NC}"
        continue
    fi

    echo "  Public IP: ${PUBLIC_IP}"

    # Create deployment script
    cat > /tmp/deploy-remote.sh <<'EOF'
#!/bin/bash
set -e

# Configuration
S3_BUCKET="__S3_BUCKET__"
S3_KEY="__S3_KEY__"
APP_DIR="/opt/rapidphoto"
JAR_NAME="rapidphoto-backend.jar"

# Install Java 21 if not present
if ! java -version 2>&1 | grep -q "21"; then
    echo "Installing Amazon Corretto 21..."
    sudo yum install -y java-21-amazon-corretto-devel
fi

# Create application directory
sudo mkdir -p ${APP_DIR}
cd ${APP_DIR}

# Download JAR from S3
echo "Downloading application from S3..."
aws s3 cp "s3://${S3_BUCKET}/${S3_KEY}" "${APP_DIR}/${JAR_NAME}"
sudo chown ec2-user:ec2-user "${APP_DIR}/${JAR_NAME}"

# Stop existing service if running
if sudo systemctl is-active --quiet rapidphoto 2>/dev/null; then
    echo "Stopping existing service..."
    sudo systemctl stop rapidphoto
fi

# Create systemd service file
sudo tee /etc/systemd/system/rapidphoto.service > /dev/null <<'SERVICE'
[Unit]
Description=RapidPhoto Spring Boot Application
After=network.target

[Service]
Type=simple
User=ec2-user
WorkingDirectory=/opt/rapidphoto
ExecStart=/usr/bin/java -jar \
    -Xms512m -Xmx1024m \
    -Dspring.profiles.active=aws \
    /opt/rapidphoto/rapidphoto-backend.jar
Restart=always
RestartSec=10
StandardOutput=journal
StandardError=journal
SyslogIdentifier=rapidphoto

# Environment variables (AWS credentials from IAM role)
Environment="AWS_REGION=us-east-1"

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

    # Replace placeholders
    sed -i.bak "s|__S3_BUCKET__|${S3_DEPLOYMENT_BUCKET}|g" /tmp/deploy-remote.sh
    sed -i.bak "s|__S3_KEY__|latest/${JAR_NAME}|g" /tmp/deploy-remote.sh

    # Copy script to EC2 and execute
    echo "  Copying deployment script..."
    scp -i "${SSH_KEY_PATH}" -o StrictHostKeyChecking=no /tmp/deploy-remote.sh ec2-user@${PUBLIC_IP}:/tmp/

    echo "  Executing deployment..."
    ssh -i "${SSH_KEY_PATH}" -o StrictHostKeyChecking=no ec2-user@${PUBLIC_IP} "chmod +x /tmp/deploy-remote.sh && /tmp/deploy-remote.sh"

    echo -e "${GREEN}✓ Deployed to ${INSTANCE_ID} (${PUBLIC_IP})${NC}"
done

# Clean up
rm -f /tmp/deploy-remote.sh /tmp/deploy-remote.sh.bak

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
echo -e "  Service Status:  ssh -i ${SSH_KEY_PATH} ec2-user@<instance-ip> 'sudo systemctl status rapidphoto'"
