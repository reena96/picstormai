#!/bin/bash
# Setup Environment Script
# Fetches AWS resource details and prepares environment variables
# This script pulls secrets from AWS Secrets Manager (best practice)

set -e

AWS_REGION="us-east-1"
STACK_NAME="RapidPhotoStack-Dev"

echo "=== Fetching AWS Resource Information ==="

# Get CloudFormation stack outputs
echo "Retrieving stack outputs..."
OUTPUTS=$(aws cloudformation describe-stacks \
    --stack-name "${STACK_NAME}" \
    --region "${AWS_REGION}" \
    --query 'Stacks[0].Outputs' \
    --output json)

# Parse outputs
DB_ENDPOINT=$(echo "$OUTPUTS" | jq -r '.[] | select(.OutputKey=="DatabaseEndpoint") | .OutputValue')
DB_SECRET_ARN=$(echo "$OUTPUTS" | jq -r '.[] | select(.OutputKey=="DatabaseSecretArn") | .OutputValue')
REDIS_ENDPOINT=$(echo "$OUTPUTS" | jq -r '.[] | select(.OutputKey=="RedisEndpoint") | .OutputValue')
S3_PHOTO_BUCKET=$(echo "$OUTPUTS" | jq -r '.[] | select(.OutputKey=="PhotoBucketName") | .OutputValue')
S3_THUMBNAIL_BUCKET=$(echo "$OUTPUTS" | jq -r '.[] | select(.OutputKey=="ThumbnailBucketName") | .OutputValue')
CLOUDFRONT_DOMAIN=$(echo "$OUTPUTS" | jq -r '.[] | select(.OutputKey=="CloudFrontDomain") | .OutputValue')

# Get database credentials from Secrets Manager
echo "Retrieving database credentials from Secrets Manager..."
DB_SECRET=$(aws secretsmanager get-secret-value \
    --secret-id "${DB_SECRET_ARN}" \
    --region "${AWS_REGION}" \
    --query 'SecretString' \
    --output text)

DB_USERNAME=$(echo "$DB_SECRET" | jq -r '.username')
DB_PASSWORD=$(echo "$DB_SECRET" | jq -r '.password')
DB_NAME=$(echo "$DB_SECRET" | jq -r '.dbname')

# Generate JWT secret if not already generated
if [ -f ".jwt-secret" ]; then
    JWT_SECRET=$(cat .jwt-secret)
    echo "Using existing JWT secret from .jwt-secret file"
else
    echo "Generating new JWT secret..."
    JWT_SECRET=$(openssl rand -base64 64 | tr -d '\n')
    echo "$JWT_SECRET" > .jwt-secret
    chmod 600 .jwt-secret
    echo "✓ JWT secret saved to .jwt-secret (keep this file secure!)"
fi

# Create .env file for local reference (NOT for deployment)
cat > .env <<EOF
# Generated on $(date)
# DO NOT COMMIT THIS FILE - IT'S IN .gitignore

# Database
DB_HOST=${DB_ENDPOINT}
DB_PORT=5432
DB_NAME=${DB_NAME}
DB_USERNAME=${DB_USERNAME}
DB_PASSWORD=${DB_PASSWORD}

# Redis
REDIS_HOST=${REDIS_ENDPOINT}
REDIS_PORT=6379

# S3
AWS_REGION=${AWS_REGION}
S3_BUCKET=${S3_PHOTO_BUCKET}
S3_THUMBNAIL_BUCKET=${S3_THUMBNAIL_BUCKET}

# JWT
JWT_SECRET=${JWT_SECRET}

# Frontend
FRONTEND_URL=https://${CLOUDFRONT_DOMAIN}
EOF

chmod 600 .env

echo ""
echo "✓ Environment variables saved to .env"
echo ""
echo "=== AWS Resource Summary ==="
echo "Database:         ${DB_ENDPOINT}"
echo "Redis:            ${REDIS_ENDPOINT}"
echo "Photo Bucket:     ${S3_PHOTO_BUCKET}"
echo "Thumbnail Bucket: ${S3_THUMBNAIL_BUCKET}"
echo "CloudFront:       https://${CLOUDFRONT_DOMAIN}"
echo ""
echo "✓ All credentials retrieved securely from AWS Secrets Manager"
echo "✓ Ready for deployment!"
echo ""
echo "Next step: Run ./deploy-to-ec2.sh to deploy the application"
