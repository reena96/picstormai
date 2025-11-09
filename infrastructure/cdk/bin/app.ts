#!/usr/bin/env node
import 'source-map-support/register';
import * as cdk from 'aws-cdk-lib';
import { RapidPhotoStack } from '../lib/rapidphoto-stack';

const app = new cdk.App();

// Development Environment
new RapidPhotoStack(app, 'RapidPhotoStack-Dev', {
  env: {
    account: process.env.CDK_DEFAULT_ACCOUNT,
    region: process.env.CDK_DEFAULT_REGION || 'us-east-1',
  },
  environment: 'dev',
  tags: {
    Project: 'RapidPhotoUpload',
    Environment: 'Development',
    ManagedBy: 'AWS-CDK',
  },
});

// Production Environment (commented out - deploy when ready)
/*
new RapidPhotoStack(app, 'RapidPhotoStack-Prod', {
  env: {
    account: process.env.CDK_DEFAULT_ACCOUNT,
    region: 'us-east-1',
  },
  environment: 'prod',
  tags: {
    Project: 'RapidPhotoUpload',
    Environment: 'Production',
    ManagedBy: 'AWS-CDK',
  },
});
*/

app.synth();
