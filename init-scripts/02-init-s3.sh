#!/bin/bash
# Initialize S3 bucket and CORS configuration for LocalStack

echo "Initializing S3 bucket..."

# Wait for LocalStack to be ready
sleep 5

# Create bucket
awslocal s3 mb s3://rapidphoto-uploads

# Configure CORS
awslocal s3api put-bucket-cors --bucket rapidphoto-uploads --cors-configuration '{
  "CORSRules": [
    {
      "AllowedOrigins": ["*"],
      "AllowedMethods": ["GET", "PUT", "POST", "DELETE", "HEAD"],
      "AllowedHeaders": ["*"],
      "ExposeHeaders": ["ETag", "x-amz-request-id"],
      "MaxAgeSeconds": 3000
    }
  ]
}'

echo "S3 bucket initialized with CORS configuration"
