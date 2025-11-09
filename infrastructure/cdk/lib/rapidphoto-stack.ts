import * as cdk from 'aws-cdk-lib';
import { Construct } from 'constructs';
import * as ec2 from 'aws-cdk-lib/aws-ec2';
import * as s3 from 'aws-cdk-lib/aws-s3';
import * as rds from 'aws-cdk-lib/aws-rds';
import * as elasticache from 'aws-cdk-lib/aws-elasticache';
import * as elbv2 from 'aws-cdk-lib/aws-elasticloadbalancingv2';
import * as autoscaling from 'aws-cdk-lib/aws-autoscaling';
import * as iam from 'aws-cdk-lib/aws-iam';
import * as cloudfront from 'aws-cdk-lib/aws-cloudfront';
import * as origins from 'aws-cdk-lib/aws-cloudfront-origins';
import * as cloudwatch from 'aws-cdk-lib/aws-cloudwatch';
import * as secretsmanager from 'aws-cdk-lib/aws-secretsmanager';
import * as logs from 'aws-cdk-lib/aws-logs';

interface RapidPhotoStackProps extends cdk.StackProps {
  environment: 'dev' | 'staging' | 'prod';
}

export class RapidPhotoStack extends cdk.Stack {
  constructor(scope: Construct, id: string, props: RapidPhotoStackProps) {
    super(scope, id, props);

    const { environment } = props;

    // ========================================
    // VPC - Network Foundation (Using Default VPC)
    // ========================================
    // Using existing default VPC to avoid hitting AWS VPC limits
    const vpc = ec2.Vpc.fromLookup(this, 'DefaultVPC', {
      isDefault: true,
    });

    // ========================================
    // S3 - Photo Storage
    // ========================================
    const photoBucket = new s3.Bucket(this, 'PhotoBucket', {
      bucketName: `rapidphoto-uploads-${environment}-${this.account}`,
      versioned: true, // Enable versioning for data protection
      encryption: s3.BucketEncryption.S3_MANAGED,
      cors: [
        {
          allowedOrigins: ['*'], // TODO: Restrict to your domain in production
          allowedMethods: [
            s3.HttpMethods.GET,
            s3.HttpMethods.PUT,
            s3.HttpMethods.POST,
            s3.HttpMethods.DELETE,
          ],
          allowedHeaders: ['*'],
          exposedHeaders: ['ETag'],
          maxAge: 3000,
        },
      ],
      lifecycleRules: [
        {
          id: 'DeleteIncompleteUploads',
          enabled: true,
          abortIncompleteMultipartUploadAfter: cdk.Duration.days(7),
        },
        {
          id: 'TransitionToIA',
          enabled: environment === 'prod',
          transitions: [
            {
              storageClass: s3.StorageClass.INFREQUENT_ACCESS,
              transitionAfter: cdk.Duration.days(90), // Move old photos to cheaper storage
            },
          ],
        },
      ],
      blockPublicAccess: s3.BlockPublicAccess.BLOCK_ALL, // Security: block public access
      removalPolicy: environment === 'prod'
        ? cdk.RemovalPolicy.RETAIN
        : cdk.RemovalPolicy.DESTROY,
    });

    // Thumbnail bucket (for CDN)
    const thumbnailBucket = new s3.Bucket(this, 'ThumbnailBucket', {
      bucketName: `rapidphoto-thumbnails-${environment}-${this.account}`,
      encryption: s3.BucketEncryption.S3_MANAGED,
      publicReadAccess: false,
      blockPublicAccess: s3.BlockPublicAccess.BLOCK_ALL,
      removalPolicy: environment === 'prod'
        ? cdk.RemovalPolicy.RETAIN
        : cdk.RemovalPolicy.DESTROY,
    });

    // ========================================
    // RDS PostgreSQL - Metadata Database
    // ========================================

    // Database credentials stored in Secrets Manager
    const dbSecret = new secretsmanager.Secret(this, 'DBSecret', {
      secretName: `rapidphoto-db-credentials-${environment}`,
      generateSecretString: {
        secretStringTemplate: JSON.stringify({ username: 'rapidphoto_admin' }),
        generateStringKey: 'password',
        excludePunctuation: true,
        includeSpace: false,
        passwordLength: 32,
      },
    });

    // Security group for RDS
    const dbSecurityGroup = new ec2.SecurityGroup(this, 'DBSecurityGroup', {
      vpc,
      description: 'Security group for RapidPhoto PostgreSQL database',
      allowAllOutbound: false,
    });

    // RDS PostgreSQL instance
    const database = new rds.DatabaseInstance(this, 'PostgresDB', {
      databaseName: 'rapidphoto',
      engine: rds.DatabaseInstanceEngine.postgres({
        version: rds.PostgresEngineVersion.VER_15, // Latest 15.x version
      }),
      instanceType: environment === 'prod'
        ? ec2.InstanceType.of(ec2.InstanceClass.T3, ec2.InstanceSize.MEDIUM)
        : ec2.InstanceType.of(ec2.InstanceClass.T3, ec2.InstanceSize.SMALL),
      vpc,
      vpcSubnets: {
        subnetType: ec2.SubnetType.PUBLIC, // Default VPC only has public subnets
      },
      securityGroups: [dbSecurityGroup],
      credentials: rds.Credentials.fromSecret(dbSecret),
      allocatedStorage: environment === 'prod' ? 100 : 20,
      maxAllocatedStorage: environment === 'prod' ? 500 : 100,
      multiAz: environment === 'prod', // Multi-AZ for production
      autoMinorVersionUpgrade: true,
      backupRetention: cdk.Duration.days(environment === 'prod' ? 30 : 7),
      deleteAutomatedBackups: environment !== 'prod',
      deletionProtection: environment === 'prod',
      cloudwatchLogsExports: ['postgresql'], // Export logs to CloudWatch
      cloudwatchLogsRetention: logs.RetentionDays.ONE_MONTH,
      storageEncrypted: true,
      publiclyAccessible: false,
      removalPolicy: environment === 'prod'
        ? cdk.RemovalPolicy.SNAPSHOT
        : cdk.RemovalPolicy.DESTROY,
    });

    // ========================================
    // ElastiCache Redis - Session Cache
    // ========================================

    // Security group for Redis
    const redisSecurityGroup = new ec2.SecurityGroup(this, 'RedisSecurityGroup', {
      vpc,
      description: 'Security group for RapidPhoto Redis cache',
      allowAllOutbound: false,
    });

    // Subnet group for Redis (using public subnets from default VPC)
    const redisSubnetGroup = new elasticache.CfnSubnetGroup(this, 'RedisSubnetGroup', {
      description: 'Subnet group for RapidPhoto Redis',
      subnetIds: vpc.publicSubnets.map(subnet => subnet.subnetId),
      cacheSubnetGroupName: `rapidphoto-redis-subnet-${environment}`,
    });

    // Redis cluster
    const redisCluster = new elasticache.CfnCacheCluster(this, 'RedisCluster', {
      engine: 'redis',
      cacheNodeType: environment === 'prod' ? 'cache.t3.medium' : 'cache.t3.micro',
      numCacheNodes: 1,
      clusterName: `rapidphoto-redis-${environment}`,
      vpcSecurityGroupIds: [redisSecurityGroup.securityGroupId],
      cacheSubnetGroupName: redisSubnetGroup.cacheSubnetGroupName,
      engineVersion: '7.0',
      port: 6379,
      autoMinorVersionUpgrade: true,
    });
    redisCluster.addDependency(redisSubnetGroup);

    // ========================================
    // Application Load Balancer
    // ========================================

    const alb = new elbv2.ApplicationLoadBalancer(this, 'ALB', {
      vpc,
      internetFacing: true,
      loadBalancerName: `rapidphoto-alb-${environment}`,
      vpcSubnets: {
        subnetType: ec2.SubnetType.PUBLIC,
      },
    });

    // HTTP listener (redirect to HTTPS in production)
    const httpListener = alb.addListener('HTTPListener', {
      port: 80,
      open: true,
      defaultAction: elbv2.ListenerAction.fixedResponse(200, {
        contentType: 'text/plain',
        messageBody: 'RapidPhotoUpload API - Use HTTPS',
      }),
    });

    // Target group for Spring Boot instances
    const targetGroup = new elbv2.ApplicationTargetGroup(this, 'TargetGroup', {
      vpc,
      port: 8080, // Spring Boot default port
      protocol: elbv2.ApplicationProtocol.HTTP,
      targetType: elbv2.TargetType.INSTANCE,
      healthCheck: {
        enabled: true,
        path: '/actuator/health',
        interval: cdk.Duration.seconds(30),
        timeout: cdk.Duration.seconds(5),
        healthyThresholdCount: 2,
        unhealthyThresholdCount: 3,
        healthyHttpCodes: '200',
      },
      deregistrationDelay: cdk.Duration.seconds(30),
    });

    httpListener.addTargetGroups('DefaultTarget', {
      targetGroups: [targetGroup],
    });

    // ========================================
    // Auto Scaling Group - Spring Boot WebFlux
    // ========================================

    // IAM role for EC2 instances
    const ec2Role = new iam.Role(this, 'EC2Role', {
      assumedBy: new iam.ServicePrincipal('ec2.amazonaws.com'),
      managedPolicies: [
        iam.ManagedPolicy.fromAwsManagedPolicyName('CloudWatchAgentServerPolicy'),
        iam.ManagedPolicy.fromAwsManagedPolicyName('AmazonSSMManagedInstanceCore'), // For SSM Session Manager
      ],
    });

    // Grant S3 permissions to EC2 instances
    photoBucket.grantReadWrite(ec2Role);
    thumbnailBucket.grantReadWrite(ec2Role);

    // Grant read access to DB secret
    dbSecret.grantRead(ec2Role);

    // Security group for EC2 instances
    const ec2SecurityGroup = new ec2.SecurityGroup(this, 'EC2SecurityGroup', {
      vpc,
      description: 'Security group for RapidPhoto Spring Boot instances',
      allowAllOutbound: true,
    });

    // Allow ALB to reach EC2 instances
    ec2SecurityGroup.addIngressRule(
      ec2.Peer.securityGroupId(alb.connections.securityGroups[0].securityGroupId),
      ec2.Port.tcp(8080),
      'Allow traffic from ALB'
    );

    // Allow EC2 to reach database
    dbSecurityGroup.addIngressRule(
      ec2SecurityGroup,
      ec2.Port.tcp(5432),
      'Allow Spring Boot to access PostgreSQL'
    );

    // Allow EC2 to reach Redis
    redisSecurityGroup.addIngressRule(
      ec2SecurityGroup,
      ec2.Port.tcp(6379),
      'Allow Spring Boot to access Redis'
    );

    // User data script to install Java and run Spring Boot
    const userData = ec2.UserData.forLinux();
    userData.addCommands(
      '#!/bin/bash',
      'set -e',
      '',
      '# Update system',
      'yum update -y',
      '',
      '# Install Java 17',
      'yum install -y java-17-amazon-corretto-headless',
      '',
      '# Install CloudWatch agent',
      'wget https://s3.amazonaws.com/amazoncloudwatch-agent/amazon_linux/amd64/latest/amazon-cloudwatch-agent.rpm',
      'rpm -U ./amazon-cloudwatch-agent.rpm',
      '',
      '# Create application directory',
      'mkdir -p /opt/rapidphoto',
      'cd /opt/rapidphoto',
      '',
      '# TODO: Download Spring Boot JAR from S3 or artifact repository',
      '# aws s3 cp s3://your-artifacts-bucket/rapidphoto-backend.jar /opt/rapidphoto/app.jar',
      '',
      '# Environment variables for Spring Boot',
      `export SPRING_PROFILES_ACTIVE=${environment}`,
      `export DB_HOST=${database.dbInstanceEndpointAddress}`,
      'export DB_PORT=5432',
      'export DB_NAME=rapidphoto',
      `export DB_SECRET_ARN=${dbSecret.secretArn}`,
      `export REDIS_HOST=${redisCluster.attrRedisEndpointAddress}`,
      'export REDIS_PORT=6379',
      `export S3_BUCKET=${photoBucket.bucketName}`,
      `export S3_THUMBNAIL_BUCKET=${thumbnailBucket.bucketName}`,
      `export AWS_REGION=${this.region}`,
      '',
      '# Create systemd service',
      'cat > /etc/systemd/system/rapidphoto.service << EOF',
      '[Unit]',
      'Description=RapidPhotoUpload Spring Boot Application',
      'After=network.target',
      '',
      '[Service]',
      'Type=simple',
      'User=ec2-user',
      'WorkingDirectory=/opt/rapidphoto',
      'ExecStart=/usr/bin/java -jar /opt/rapidphoto/app.jar',
      `Environment="SPRING_PROFILES_ACTIVE=${environment}"`,
      `Environment="DB_HOST=${database.dbInstanceEndpointAddress}"`,
      'Environment="DB_PORT=5432"',
      'Environment="DB_NAME=rapidphoto"',
      `Environment="DB_SECRET_ARN=${dbSecret.secretArn}"`,
      `Environment="REDIS_HOST=${redisCluster.attrRedisEndpointAddress}"`,
      'Environment="REDIS_PORT=6379"',
      `Environment="S3_BUCKET=${photoBucket.bucketName}"`,
      `Environment="S3_THUMBNAIL_BUCKET=${thumbnailBucket.bucketName}"`,
      `Environment="AWS_REGION=${this.region}"`,
      'Restart=on-failure',
      'RestartSec=10',
      '',
      '[Install]',
      'WantedBy=multi-user.target',
      'EOF',
      '',
      '# Note: Service will fail until you deploy the Spring Boot JAR',
      '# Enable and start service (commented out until JAR is deployed)',
      '# systemctl daemon-reload',
      '# systemctl enable rapidphoto',
      '# systemctl start rapidphoto',
      '',
      '# Signal CloudFormation that instance is ready',
      'yum install -y aws-cfn-bootstrap',
      `/opt/aws/bin/cfn-signal -e $? --stack ${this.stackName} --resource AutoScalingGroup --region ${this.region}`,
    );

    // Auto Scaling Group
    const asg = new autoscaling.AutoScalingGroup(this, 'AutoScalingGroup', {
      vpc,
      instanceType: environment === 'prod'
        ? ec2.InstanceType.of(ec2.InstanceClass.T3, ec2.InstanceSize.MEDIUM)
        : ec2.InstanceType.of(ec2.InstanceClass.T3, ec2.InstanceSize.SMALL),
      machineImage: ec2.MachineImage.latestAmazonLinux2023({
        cpuType: ec2.AmazonLinuxCpuType.X86_64,
      }),
      minCapacity: environment === 'prod' ? 2 : 1,
      maxCapacity: environment === 'prod' ? 10 : 3,
      desiredCapacity: environment === 'prod' ? 2 : 1,
      vpcSubnets: {
        subnetType: ec2.SubnetType.PUBLIC, // Default VPC only has public subnets
      },
      role: ec2Role,
      securityGroup: ec2SecurityGroup,
      userData,
      healthCheck: autoscaling.HealthCheck.elb({
        grace: cdk.Duration.minutes(5),
      }),
      signals: autoscaling.Signals.waitForAll({
        timeout: cdk.Duration.minutes(10),
      }),
    });

    // Attach ASG to target group
    asg.attachToApplicationTargetGroup(targetGroup);

    // Auto scaling policies
    asg.scaleOnCpuUtilization('CPUScaling', {
      targetUtilizationPercent: 70,
    });

    asg.scaleOnRequestCount('RequestScaling', {
      targetRequestsPerMinute: 1000,
    });

    // ========================================
    // CloudFront Distribution - CDN
    // ========================================

    const distribution = new cloudfront.Distribution(this, 'CDN', {
      defaultBehavior: {
        origin: origins.S3BucketOrigin.withOriginAccessControl(thumbnailBucket),
        viewerProtocolPolicy: cloudfront.ViewerProtocolPolicy.REDIRECT_TO_HTTPS,
        allowedMethods: cloudfront.AllowedMethods.ALLOW_GET_HEAD_OPTIONS,
        cachedMethods: cloudfront.CachedMethods.CACHE_GET_HEAD_OPTIONS,
        cachePolicy: cloudfront.CachePolicy.CACHING_OPTIMIZED,
      },
      additionalBehaviors: {
        '/api/*': {
          origin: new origins.LoadBalancerV2Origin(alb, {
            protocolPolicy: cloudfront.OriginProtocolPolicy.HTTP_ONLY,
          }),
          viewerProtocolPolicy: cloudfront.ViewerProtocolPolicy.REDIRECT_TO_HTTPS,
          allowedMethods: cloudfront.AllowedMethods.ALLOW_ALL,
          cachePolicy: cloudfront.CachePolicy.CACHING_DISABLED,
          originRequestPolicy: cloudfront.OriginRequestPolicy.ALL_VIEWER,
        },
      },
      comment: `RapidPhotoUpload CDN - ${environment}`,
    });

    // ========================================
    // CloudWatch Alarms
    // ========================================

    // ALB response time alarm
    new cloudwatch.Alarm(this, 'ALBResponseTimeAlarm', {
      metric: alb.metrics.targetResponseTime(),
      threshold: 1,
      evaluationPeriods: 2,
      alarmDescription: 'ALB target response time is high (>1 second)',
    });

    // Database CPU
    new cloudwatch.Alarm(this, 'DBCPUAlarm', {
      metric: database.metricCPUUtilization(),
      threshold: 80,
      evaluationPeriods: 2,
      alarmDescription: 'Database CPU utilization is high',
    });

    // ========================================
    // Outputs
    // ========================================

    new cdk.CfnOutput(this, 'VPCId', {
      value: vpc.vpcId,
      description: 'VPC ID',
    });

    new cdk.CfnOutput(this, 'PhotoBucketName', {
      value: photoBucket.bucketName,
      description: 'S3 bucket for photo uploads',
      exportName: `RapidPhoto-PhotoBucket-${environment}`,
    });

    new cdk.CfnOutput(this, 'ThumbnailBucketName', {
      value: thumbnailBucket.bucketName,
      description: 'S3 bucket for thumbnails',
      exportName: `RapidPhoto-ThumbnailBucket-${environment}`,
    });

    new cdk.CfnOutput(this, 'DatabaseEndpoint', {
      value: database.dbInstanceEndpointAddress,
      description: 'PostgreSQL database endpoint',
      exportName: `RapidPhoto-DBEndpoint-${environment}`,
    });

    new cdk.CfnOutput(this, 'DatabaseSecretArn', {
      value: dbSecret.secretArn,
      description: 'ARN of database credentials secret',
      exportName: `RapidPhoto-DBSecret-${environment}`,
    });

    new cdk.CfnOutput(this, 'RedisEndpoint', {
      value: redisCluster.attrRedisEndpointAddress,
      description: 'Redis cache endpoint',
      exportName: `RapidPhoto-RedisEndpoint-${environment}`,
    });

    new cdk.CfnOutput(this, 'LoadBalancerDNS', {
      value: alb.loadBalancerDnsName,
      description: 'Application Load Balancer DNS name',
      exportName: `RapidPhoto-ALB-DNS-${environment}`,
    });

    new cdk.CfnOutput(this, 'CloudFrontDomain', {
      value: distribution.distributionDomainName,
      description: 'CloudFront distribution domain',
      exportName: `RapidPhoto-CloudFront-${environment}`,
    });
  }
}
