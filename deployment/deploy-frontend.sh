#!/bin/bash
# Automated Frontend Deployment Script for RapidPhoto
# Deploys React web app to S3/CloudFront

set -e  # Exit on any error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${GREEN}=== RapidPhoto Frontend Deployment Script ===${NC}"

# Check if .env exists
if [ ! -f ".env" ]; then
    echo -e "${RED}✗ .env file not found${NC}"
    echo "Please run ./setup-environment.sh first"
    exit 1
fi

# Load environment variables
source .env

# Configuration
AWS_REGION="${AWS_REGION:-us-east-1}"
FRONTEND_DIR="../frontend"

# Step 1: Get CloudFormation outputs
echo -e "\n${YELLOW}Step 1: Fetching CloudFormation outputs...${NC}"
OUTPUTS=$(aws cloudformation describe-stacks \
    --stack-name RapidPhotoStack-Dev \
    --region "${AWS_REGION}" \
    --query 'Stacks[0].Outputs' \
    --output json)

CLOUDFRONT_DOMAIN=$(echo "$OUTPUTS" | jq -r '.[] | select(.OutputKey=="CloudFrontDomain") | .OutputValue')
LOAD_BALANCER_DNS=$(echo "$OUTPUTS" | jq -r '.[] | select(.OutputKey=="LoadBalancerDNS") | .OutputValue')
S3_FRONTEND_BUCKET="rapidphoto-frontend-dev-$(aws sts get-caller-identity --query Account --output text)"

echo -e "${GREEN}✓ CloudFront Domain: ${CLOUDFRONT_DOMAIN}${NC}"
echo -e "${GREEN}✓ Load Balancer: ${LOAD_BALANCER_DNS}${NC}"

# Step 2: Update frontend API configuration
echo -e "\n${YELLOW}Step 2: Updating frontend API configuration...${NC}"
API_URL="http://${LOAD_BALANCER_DNS}"

# Create temporary config file for build
cat > "${FRONTEND_DIR}/.env.production" <<EOF
# Auto-generated during deployment - DO NOT COMMIT
REACT_APP_API_URL=${API_URL}
EXPO_PUBLIC_API_URL=${API_URL}
EOF

echo -e "${GREEN}✓ API URL set to: ${API_URL}${NC}"

# Step 3: Build frontend
echo -e "\n${YELLOW}Step 3: Building React web application...${NC}"
cd "${FRONTEND_DIR}"

# Install dependencies if needed
if [ ! -d "node_modules" ]; then
    echo "Installing dependencies..."
    npm install
fi

# Build for web
npm run build:web

echo -e "${GREEN}✓ Build complete${NC}"

# Step 4: Create S3 bucket if it doesn't exist
echo -e "\n${YELLOW}Step 4: Setting up S3 frontend bucket...${NC}"
cd - > /dev/null

if ! aws s3 ls "s3://${S3_FRONTEND_BUCKET}" 2>/dev/null; then
    echo "Creating S3 bucket..."
    aws s3 mb "s3://${S3_FRONTEND_BUCKET}" --region "${AWS_REGION}"

    # Disable Block Public Access for website hosting
    echo "Configuring public access settings..."
    aws s3api put-public-access-block \
        --bucket "${S3_FRONTEND_BUCKET}" \
        --public-access-block-configuration "BlockPublicAcls=false,IgnorePublicAcls=false,BlockPublicPolicy=false,RestrictPublicBuckets=false"

    # Enable static website hosting
    aws s3 website "s3://${S3_FRONTEND_BUCKET}" \
        --index-document index.html \
        --error-document index.html

    # Wait a moment for settings to propagate
    sleep 2

    # Set bucket policy for public read
    BUCKET_POLICY=$(cat <<EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "PublicReadGetObject",
      "Effect": "Allow",
      "Principal": "*",
      "Action": "s3:GetObject",
      "Resource": "arn:aws:s3:::${S3_FRONTEND_BUCKET}/*"
    }
  ]
}
EOF
)

    echo "${BUCKET_POLICY}" | aws s3api put-bucket-policy \
        --bucket "${S3_FRONTEND_BUCKET}" \
        --policy file:///dev/stdin

    echo -e "${GREEN}✓ Created and configured S3 bucket${NC}"
else
    echo -e "${GREEN}✓ S3 bucket already exists${NC}"
fi

# Step 5: Upload build files to S3
echo -e "\n${YELLOW}Step 5: Uploading files to S3...${NC}"
aws s3 sync "${FRONTEND_DIR}/dist" "s3://${S3_FRONTEND_BUCKET}" \
    --delete \
    --cache-control "public,max-age=31536000,immutable" \
    --exclude "index.html" \
    --exclude "asset-manifest.json" \
    --exclude "service-worker.js"

# Upload index.html and service worker without caching
aws s3 cp "${FRONTEND_DIR}/dist/index.html" "s3://${S3_FRONTEND_BUCKET}/index.html" \
    --cache-control "no-cache,no-store,must-revalidate"

if [ -f "${FRONTEND_DIR}/dist/service-worker.js" ]; then
    aws s3 cp "${FRONTEND_DIR}/dist/service-worker.js" "s3://${S3_FRONTEND_BUCKET}/service-worker.js" \
        --cache-control "no-cache,no-store,must-revalidate"
fi

echo -e "${GREEN}✓ Files uploaded to S3${NC}"

# Step 6: Get CloudFront distribution ID
echo -e "\n${YELLOW}Step 6: Invalidating CloudFront cache...${NC}"
DISTRIBUTION_ID=$(aws cloudfront list-distributions \
    --query "DistributionList.Items[?Origins.Items[?Id=='S3-${S3_FRONTEND_BUCKET}']].Id | [0]" \
    --output text)

if [ -n "$DISTRIBUTION_ID" ] && [ "$DISTRIBUTION_ID" != "None" ]; then
    # Create CloudFront invalidation
    INVALIDATION_ID=$(aws cloudfront create-invalidation \
        --distribution-id "${DISTRIBUTION_ID}" \
        --paths "/*" \
        --query 'Invalidation.Id' \
        --output text)

    echo -e "${GREEN}✓ CloudFront invalidation created: ${INVALIDATION_ID}${NC}"
    echo "  (Cache will be cleared in 1-2 minutes)"
else
    echo -e "${YELLOW}⚠ CloudFront distribution not found${NC}"
    echo "  You may need to manually configure CloudFront to point to:"
    echo "  Origin: ${S3_FRONTEND_BUCKET}.s3-website-${AWS_REGION}.amazonaws.com"
fi

# Step 7: Clean up
echo -e "\n${YELLOW}Step 7: Cleaning up...${NC}"
rm -f "${FRONTEND_DIR}/.env.production"
echo -e "${GREEN}✓ Cleanup complete${NC}"

# Final summary
echo -e "\n${GREEN}=== Frontend Deployment Complete ===${NC}"
echo -e "\nFrontend URLs:"
if [ -n "$CLOUDFRONT_DOMAIN" ]; then
    echo -e "  CloudFront: https://${CLOUDFRONT_DOMAIN}"
fi
echo -e "  S3 Website: http://${S3_FRONTEND_BUCKET}.s3-website-${AWS_REGION}.amazonaws.com"
echo -e "\nAPI Endpoint:"
echo -e "  ${API_URL}"
echo -e "\n${GREEN}✓ Application is now live!${NC}"
