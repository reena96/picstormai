# GitHub Actions Setup

## Required Secret

Add `RAILWAY_TOKEN` to GitHub repository secrets:

1. Generate Railway token via Dashboard:
   - Go to: https://railway.app/account/tokens
   - Click "Create Token"
   - Give it a name (e.g., "GitHub Actions")
   - Copy the generated token

2. Add to GitHub:
   - Go to repository Settings → Secrets → Actions
   - Click "New repository secret"
   - Name: `RAILWAY_TOKEN`
   - Value: (paste token from step 1)
   - Click "Add secret"

## Workflow Behavior

- **On Pull Request:** Runs tests only
- **On Push to main:** Runs tests + deploys to Railway

## Manual Trigger

You can manually trigger deployment:

1. Go to Actions tab
2. Select "Deploy to Railway"
3. Click "Run workflow"
