#!/bin/bash
# Deploy JAR to S3 only
# Manual step: SSH or SSM into EC2 to download and run

set -e

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo -e "${GREEN}=== Build and Upload JAR to S3 ===${NC}"

# Configuration
AWS_REGION="us-east-1"
S3_DEPLOYMENT_BUCKET="rapidphoto-deployments-$(aws sts get-caller-identity --query Account --output text)"
JAR_NAME="rapidphoto-backend.jar"

# Build
echo -e "\n${YELLOW}Building JAR...${NC}"
cd "$(dirname "$0")/../backend"
./gradlew clean build -x test
echo -e "${GREEN}✓ Build complete${NC}"

# Create bucket if needed
if ! aws s3 ls "s3://${S3_DEPLOYMENT_BUCKET}" 2>/dev/null; then
    aws s3 mb "s3://${S3_DEPLOYMENT_BUCKET}" --region "${AWS_REGION}"
fi

# Upload
echo -e "\n${YELLOW}Uploading JAR to S3...${NC}"
JAR_PATH="build/libs/rapidphoto-backend-0.0.1-SNAPSHOT.jar"
TIMESTAMP=$(date +%Y%m%d-%H%M%S)

aws s3 cp "${JAR_PATH}" "s3://${S3_DEPLOYMENT_BUCKET}/releases/${TIMESTAMP}/${JAR_NAME}"
aws s3 cp "${JAR_PATH}" "s3://${S3_DEPLOYMENT_BUCKET}/latest/${JAR_NAME}"

echo -e "\n${GREEN}✓ JAR uploaded to S3${NC}"
echo -e "\nS3 Location: s3://${S3_DEPLOYMENT_BUCKET}/latest/${JAR_NAME}"
echo -e "\n${YELLOW}=== Manual Deployment Steps ===${NC}"
echo -e "\nOption 1 - Via EC2 Instance Connect (if available):"
echo -e "  1. Go to AWS Console → EC2 → Instances"
echo -e "  2. Select instance i-0e1def26b2f1fed6c"
echo -e "  3. Click 'Connect' → 'EC2 Instance Connect'"
echo -e "  4. Run these commands:"
echo -e ""
echo -e "     sudo yum install -y java-21-amazon-corretto-devel"
echo -e "     sudo mkdir -p /opt/rapidphoto"
echo -e "     aws s3 cp s3://${S3_DEPLOYMENT_BUCKET}/latest/${JAR_NAME} /opt/rapidphoto/rapidphoto-backend.jar"
echo -e "     sudo chown ec2-user:ec2-user /opt/rapidphoto/rapidphoto-backend.jar"
echo -e "     cd /opt/rapidphoto"
echo -e "     java -jar -Dspring.profiles.active=aws rapidphoto-backend.jar"
echo -e ""
echo -e "\nOption 2 - Test locally first:"
echo -e "  cd backend"
echo -e "  ./gradlew bootRun"
echo -e ""
echo -e "\nOption 3 - Wait for SSM agent to register (may take time)"
echo -e "  Then run: ./deploy-to-ec2.sh"
