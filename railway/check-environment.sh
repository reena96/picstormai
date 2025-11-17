#!/bin/bash

echo "🔍 Railway Environment Check"
echo "============================"
echo ""

# Check Railway CLI
if command -v railway &> /dev/null; then
    echo "✅ Railway CLI installed ($(railway --version))"
else
    echo "❌ Railway CLI not found"
    echo "   Install: npm install -g @railway/cli"
    exit 1
fi

# Check if logged in
if railway whoami &> /dev/null; then
    echo "✅ Logged into Railway ($(railway whoami))"
else
    echo "❌ Not logged into Railway"
    echo "   Run: railway login"
    exit 1
fi

# Check if project linked
if railway status &> /dev/null; then
    echo "✅ Project linked"
    echo ""
    echo "📊 Service Status:"
    railway status
else
    echo "⚠️  No project linked"
    echo "   Run: railway link"
fi

echo ""
echo "🌐 Domains:"
railway domain --service backend 2>/dev/null || echo "   Backend: Not configured"
railway domain --service frontend 2>/dev/null || echo "   Frontend: Not configured"

echo ""
echo "📋 Environment Variables:"
echo ""

echo "Backend:"
railway variables --service backend | grep -E "SPRING_PROFILES_ACTIVE|PGHOST|REDIS_URL|AWS_REGION|S3_BUCKET" || echo "   Not set"

echo ""
echo "Frontend:"
railway variables --service frontend | grep "REACT_APP_API_URL" || echo "   Not set"

echo ""
echo "✅ Environment check complete"
