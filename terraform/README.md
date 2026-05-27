# 🏗️ Invoice Processing System — Terraform AWS Deployment

## Architecture Overview

```
Internet
   │
   ▼
Route 53 (DNS)
   │
   ▼
ACM (TLS Certificate)
   │
   ▼
Application Load Balancer  (Public Subnets)
   ├──► /api/*  ──► ECS Fargate: Spring Boot API  (Private Subnets)
   └──► /*      ──► ECS Fargate: Angular Nginx UI  (Private Subnets)
                          │
               ┌──────────┼──────────┐
               ▼          ▼          ▼
          RDS Aurora  ElastiCache   S3 Bucket
          PostgreSQL    Redis      (Invoices)
               │
               ▼
          AWS SES (Email Notifications)
          AWS Lambda (Invoice Processing)
          AWS CloudWatch (Logs & Metrics)
          AWS ECR (Container Registry)
```

## Module Structure

```
terraform/
├── main.tf                  # Root module – providers & module calls
├── variables.tf             # Input variables
├── outputs.tf               # Stack outputs
├── terraform.tfvars.example # Sample variable values
├── backend.tf               # S3 remote state
├── versions.tf              # Provider version constraints
└── modules/
    ├── vpc/                 # VPC, subnets, IGW, NAT, route tables
    ├── security_groups/     # All security groups
    ├── ecr/                 # ECR repositories (API + UI)
    ├── ecs/                 # ECS Cluster, Task Defs, Services (Fargate)
    ├── alb/                 # ALB, listeners, target groups
    ├── rds/                 # RDS PostgreSQL (Multi-AZ)
    ├── elasticache/         # Redis ElastiCache cluster
    ├── s3/                  # S3 bucket + lifecycle + encryption
    ├── ses/                 # SES domain identity + DKIM
    ├── lambda/              # Lambda function for invoice processing
    ├── iam/                 # IAM roles & policies
    ├── cloudwatch/          # Log groups, dashboards, alarms
    ├── acm/                 # ACM certificate
    └── route53/             # Route53 hosted zone & records
```

## Prerequisites

- [Terraform >= 1.7](https://developer.hashicorp.com/terraform/downloads)
- AWS CLI configured (`aws configure`)
- AWS account with sufficient permissions
- Domain name (for Route53/ACM)
- Docker (for building and pushing images)

## Quick Start

### 1. Bootstrap S3 Remote State Bucket
```bash
aws s3 mb s3://invoice-terraform-state-<account-id> --region us-east-1
aws dynamodb create-table \
  --table-name invoice-terraform-locks \
  --attribute-definitions AttributeName=LockID,AttributeType=S \
  --key-schema AttributeName=LockID,KeyType=HASH \
  --billing-mode PAY_PER_REQUEST \
  --region us-east-1
```

### 2. Configure Variables
```bash
cp terraform.tfvars.example terraform.tfvars
# Edit terraform.tfvars with your values
```

### 3. Build & Push Docker Images to ECR
```bash
# After terraform apply creates ECR repos, run:
./scripts/build_and_push.sh
```

### 4. Deploy Infrastructure
```bash
terraform init
terraform plan -out=tfplan
terraform apply tfplan
```

### 5. Tear Down
```bash
terraform destroy
```

## Environments

Use Terraform workspaces:
```bash
terraform workspace new staging
terraform workspace new production
terraform workspace select production
terraform apply -var-file=environments/production.tfvars
```

