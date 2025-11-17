# Railway Deployment - Quick Start

**Time to deploy: 10 minutes** ⚡

## Prerequisites Checklist

- [ ] GitHub account
- [ ] Railway account (sign up at https://railway.app)
- [ ] AWS S3 bucket with access keys
- [ ] Repository pushed to GitHub

## 5-Step Deployment

### Step 1: Install Railway CLI (1 minute)

```bash
npm install -g @railway/cli
```

### Step 2: Run Setup Script (3 minutes)

```bash
cd railway
./setup-railway.sh
```

Follow the prompts to enter:
- Project name
- AWS credentials
- S3 bucket names

### Step 3: Setup GitHub Actions (2 minutes)

Generate Railway token via Dashboard:
1. Go to: https://railway.app/account/tokens
2. Click "Create Token"
3. Give it a name (e.g., "GitHub Actions")
4. Copy the token

Add token to GitHub:
1. Go to: https://github.com/reena96/picstormai/settings/secrets/actions
2. Click "New repository secret"
3. Name: `RAILWAY_TOKEN`
4. Value: (paste the token from Railway)

### Step 4: Deploy (1 minute)

```bash
git push origin main
```

Watch deployment at https://railway.app/dashboard

### Step 5: Verify (1 minute)

```bash
# Check status
railway status

# Get URLs
railway domain --service backend
railway domain --service frontend

# Test backend
curl https://YOUR_BACKEND_URL/actuator/health

# Test frontend
open https://YOUR_FRONTEND_URL
```

## What Just Happened?

✅ Created Railway project
✅ Added PostgreSQL database
✅ Added Redis cache
✅ Deployed backend (Spring Boot)
✅ Deployed frontend (React Native Web)
✅ Set up automatic GitHub deployments
✅ Generated public HTTPS domains

## Next Steps

### Add Custom Domain (Optional)

1. Railway Dashboard → Frontend Service → Settings → Domains
2. Add custom domain: `www.yourdomain.com`
3. Update DNS CNAME: `www.yourdomain.com` → `<railway-domain>`

### Monitor Logs

```bash
# Watch backend logs
railway logs --service backend -f

# Watch frontend logs
railway logs --service frontend -f
```

### Scale Up (When Ready)

1. Railway Dashboard → Service → Settings
2. Adjust resources (RAM/CPU)
3. Enable autoscaling

## Cost Comparison

| Service | AWS | Railway | Savings |
|---------|-----|---------|---------|
| Dev Environment | $113/mo | $20/mo | **82%** |
| Production | $478/mo | $55/mo | **88%** |

## Troubleshooting

**Build fails?**
```bash
railway logs --service backend
```

**Can't connect to database?**
```bash
railway variables --service backend | grep PG
```

**Frontend can't reach backend?**
```bash
# Check backend URL is set
railway variables --service frontend | grep REACT_APP_API_URL
```

## Support

- Railway Docs: https://docs.railway.app
- Discord: https://discord.gg/railway
- GitHub Issues: https://github.com/YOUR_USERNAME/picstormai/issues

---

**🎉 You're live on Railway!**
