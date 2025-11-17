#!/bin/bash
set -e

echo "🚂 Railway Deployment Setup for PicStormAI"
echo "=========================================="
echo ""

# Check if Railway CLI is installed
if ! command -v railway &> /dev/null; then
    echo "❌ Railway CLI not found. Installing..."
    npm install -g @railway/cli
fi

echo "✅ Railway CLI installed"
echo ""

# Login to Railway
echo "🔐 Logging into Railway..."
railway login
echo ""

# Create new project
echo "📦 Creating Railway project..."
read -p "Enter project name (default: picstormai): " PROJECT_NAME
PROJECT_NAME=${PROJECT_NAME:-picstormai}

railway init --name "$PROJECT_NAME"
echo ""

# Link to GitHub repository
echo "🔗 Linking to GitHub repository..."
echo "Please connect your GitHub repository through the Railway dashboard:"
echo "   1. Go to https://railway.app/dashboard"
echo "   2. Select your project"
echo "   3. Click 'Settings' → 'Connect GitHub'"
echo "   4. Select your repository"
echo ""
read -p "Press ENTER when GitHub is connected..."

# Create backend service
echo "🏗️  Creating Backend Service..."
railway service create backend
railway link backend

# Add PostgreSQL
echo "🐘 Adding PostgreSQL database..."
railway add --service postgresql
echo ""

# Add Redis
echo "🔴 Adding Redis cache..."
railway add --service redis
echo ""

# Set backend environment variables
echo "🔧 Setting backend environment variables..."
echo "Please enter the following values:"
echo ""

read -p "AWS Access Key ID: " AWS_ACCESS_KEY_ID
read -p "AWS Secret Access Key: " AWS_SECRET_ACCESS_KEY
read -p "S3 Bucket Name (photos): " S3_BUCKET_NAME
read -p "S3 Thumbnail Bucket Name: " S3_THUMBNAIL_BUCKET_NAME
read -sp "JWT Secret (leave blank to generate): " JWT_SECRET
echo ""

# Generate JWT secret if not provided
if [ -z "$JWT_SECRET" ]; then
    JWT_SECRET=$(openssl rand -base64 64 | tr -d '\n')
    echo "Generated JWT Secret"
fi

# Set environment variables
railway variables set \
    SPRING_PROFILES_ACTIVE=railway \
    AWS_REGION=us-east-1 \
    AWS_ACCESS_KEY_ID="$AWS_ACCESS_KEY_ID" \
    AWS_SECRET_ACCESS_KEY="$AWS_SECRET_ACCESS_KEY" \
    S3_BUCKET_NAME="$S3_BUCKET_NAME" \
    S3_THUMBNAIL_BUCKET_NAME="$S3_THUMBNAIL_BUCKET_NAME" \
    JWT_SECRET="$JWT_SECRET"

echo "✅ Backend environment variables set"
echo ""

# Create frontend service
echo "🎨 Creating Frontend Service..."
railway service create frontend
railway link frontend

# Get backend URL for frontend
echo "📡 Getting backend URL..."
BACKEND_URL=$(railway domain --service backend)

railway variables set \
    REACT_APP_API_URL="https://$BACKEND_URL"

echo "✅ Frontend environment variables set"
echo ""

# Generate domains
echo "🌐 Generating public domains..."
railway domain --service backend
railway domain --service frontend

echo ""
echo "✅ Railway setup complete!"
echo ""
echo "📋 Next Steps:"
echo "   1. Push to GitHub: git push origin main"
echo "   2. Railway will automatically build and deploy"
echo "   3. Check deployment status: railway status"
echo "   4. View logs: railway logs"
echo ""
echo "🔗 Dashboard: https://railway.app/dashboard"
