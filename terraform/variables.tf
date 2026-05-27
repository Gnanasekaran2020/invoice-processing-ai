# =============================================================
#  Invoice Processing System — Root Variables
# =============================================================

# ── General ───────────────────────────────────────────────────
variable "aws_region" {
  description = "AWS region to deploy resources"
  type        = string
  default     = "us-east-1"
}

variable "environment" {
  description = "Deployment environment (dev | staging | production)"
  type        = string
  validation {
    condition     = contains(["dev", "staging", "production"], var.environment)
    error_message = "Environment must be one of: dev, staging, production."
  }
}

variable "project" {
  description = "Project name used for resource naming and tagging"
  type        = string
  default     = "invoice-processing"
}

variable "owner" {
  description = "Owner tag applied to all resources"
  type        = string
  default     = "platform-team"
}

# ── Domain & TLS ──────────────────────────────────────────────
variable "domain_name" {
  description = "Root domain name (e.g. u2acts.com)"
  type        = string
}

variable "app_subdomain" {
  description = "Subdomain for the application (e.g. invoice)"
  type        = string
  default     = "invoice"
}

variable "api_subdomain" {
  description = "Subdomain for the API (e.g. api.invoice)"
  type        = string
  default     = "api.invoice"
}

# ── VPC ───────────────────────────────────────────────────────
variable "vpc_cidr" {
  description = "CIDR block for the VPC"
  type        = string
  default     = "10.0.0.0/16"
}

variable "availability_zones" {
  description = "List of availability zones to use"
  type        = list(string)
  default     = ["us-east-1a", "us-east-1b", "us-east-1c"]
}

variable "public_subnet_cidrs" {
  description = "CIDR blocks for public subnets"
  type        = list(string)
  default     = ["10.0.1.0/24", "10.0.2.0/24", "10.0.3.0/24"]
}

variable "private_subnet_cidrs" {
  description = "CIDR blocks for private subnets (ECS, RDS, Redis)"
  type        = list(string)
  default     = ["10.0.11.0/24", "10.0.12.0/24", "10.0.13.0/24"]
}

variable "database_subnet_cidrs" {
  description = "CIDR blocks for isolated database subnets"
  type        = list(string)
  default     = ["10.0.21.0/24", "10.0.22.0/24", "10.0.23.0/24"]
}

# ── ECR ───────────────────────────────────────────────────────
variable "ecr_image_tag_mutability" {
  description = "Image tag mutability (MUTABLE | IMMUTABLE)"
  type        = string
  default     = "IMMUTABLE"
}

variable "ecr_image_retention_count" {
  description = "Number of images to keep per ECR repo"
  type        = number
  default     = 10
}

# ── ECS / Fargate ─────────────────────────────────────────────
variable "api_image_tag" {
  description = "Docker image tag for the Spring Boot API"
  type        = string
  default     = "latest"
}

variable "ui_image_tag" {
  description = "Docker image tag for the Angular UI"
  type        = string
  default     = "latest"
}

variable "api_cpu" {
  description = "Fargate vCPU units for the API task (256 = 0.25 vCPU)"
  type        = number
  default     = 1024
}

variable "api_memory" {
  description = "Fargate memory (MB) for the API task"
  type        = number
  default     = 2048
}

variable "ui_cpu" {
  description = "Fargate vCPU units for the UI task"
  type        = number
  default     = 512
}

variable "ui_memory" {
  description = "Fargate memory (MB) for the UI task"
  type        = number
  default     = 1024
}

variable "api_desired_count" {
  description = "Desired number of API task instances"
  type        = number
  default     = 2
}

variable "ui_desired_count" {
  description = "Desired number of UI task instances"
  type        = number
  default     = 2
}

variable "api_min_count" {
  description = "Minimum API task count (auto-scaling)"
  type        = number
  default     = 1
}

variable "api_max_count" {
  description = "Maximum API task count (auto-scaling)"
  type        = number
  default     = 10
}

# ── RDS PostgreSQL ────────────────────────────────────────────
variable "db_instance_class" {
  description = "RDS instance class"
  type        = string
  default     = "db.t3.medium"
}

variable "db_name" {
  description = "Database name"
  type        = string
  default     = "invoice"
}

variable "db_username" {
  description = "Database master username"
  type        = string
  default     = "postgres"
  sensitive   = true
}

variable "db_password" {
  description = "Database master password (use Secrets Manager in production)"
  type        = string
  sensitive   = true
}

variable "db_multi_az" {
  description = "Enable Multi-AZ for RDS"
  type        = bool
  default     = true
}

variable "db_backup_retention_days" {
  description = "Number of days to retain RDS automated backups"
  type        = number
  default     = 7
}

variable "db_storage_gb" {
  description = "Allocated storage in GB for RDS"
  type        = number
  default     = 50
}

variable "db_max_storage_gb" {
  description = "Maximum storage autoscaling limit in GB"
  type        = number
  default     = 200
}

# ── ElastiCache Redis ─────────────────────────────────────────
variable "redis_node_type" {
  description = "ElastiCache Redis node type"
  type        = string
  default     = "cache.t3.micro"
}

variable "redis_num_cache_nodes" {
  description = "Number of Redis cache nodes"
  type        = number
  default     = 1
}

variable "redis_engine_version" {
  description = "Redis engine version"
  type        = string
  default     = "7.1"
}

# ── S3 ────────────────────────────────────────────────────────
variable "s3_invoice_bucket_name" {
  description = "S3 bucket name for invoice documents"
  type        = string
  default     = "u2a-invoice-documents"
}

variable "s3_force_destroy" {
  description = "Allow Terraform to destroy non-empty S3 bucket"
  type        = bool
  default     = false
}

# ── SES ───────────────────────────────────────────────────────
variable "ses_from_email" {
  description = "Verified SES sender email address"
  type        = string
  default     = "noreply@u2acts.com"
}

variable "ses_alert_email" {
  description = "Email address for infrastructure alerts"
  type        = string
}

# ── JWT ───────────────────────────────────────────────────────
variable "jwt_secret" {
  description = "JWT signing secret (min 256-bit)"
  type        = string
  sensitive   = true
}

variable "jwt_expiration_ms" {
  description = "JWT expiration in milliseconds (default 24h)"
  type        = number
  default     = 86400000
}

# ── Lambda ────────────────────────────────────────────────────
variable "lambda_function_name" {
  description = "Name of the Lambda function for invoice processing"
  type        = string
  default     = "InvoiceProcessorFunction"
}

variable "lambda_runtime" {
  description = "Lambda runtime"
  type        = string
  default     = "java21"
}

variable "lambda_memory_size" {
  description = "Lambda memory in MB"
  type        = number
  default     = 1024
}

variable "lambda_timeout" {
  description = "Lambda timeout in seconds"
  type        = number
  default     = 300
}

# ── CloudWatch / Monitoring ───────────────────────────────────
variable "log_retention_days" {
  description = "CloudWatch log group retention in days"
  type        = number
  default     = 30
}

variable "alarm_email" {
  description = "Email for CloudWatch alarm notifications"
  type        = string
}

# ── Common Tags ───────────────────────────────────────────────
variable "additional_tags" {
  description = "Additional tags applied to all resources"
  type        = map(string)
  default     = {}
}

