# =============================================================
#  Invoice Processing System — Root Module
#  Orchestrates all sub-modules
# =============================================================

provider "aws" {
  region = var.aws_region

  default_tags {
    tags = merge({
      Project     = var.project
      Environment = var.environment
      Owner       = var.owner
      ManagedBy   = "Terraform"
      CreatedDate = formatdate("YYYY-MM-DD", timestamp())
    }, var.additional_tags)
  }
}

# ── Random suffix for globally unique names ────────────────────
resource "random_id" "suffix" {
  byte_length = 4
}

locals {
  name_prefix = "${var.project}-${var.environment}"
  bucket_name = "${var.s3_invoice_bucket_name}-${random_id.suffix.hex}"
}

# ── 1. VPC ────────────────────────────────────────────────────
module "vpc" {
  source = "./modules/vpc"

  name_prefix           = local.name_prefix
  vpc_cidr              = var.vpc_cidr
  availability_zones    = var.availability_zones
  public_subnet_cidrs   = var.public_subnet_cidrs
  private_subnet_cidrs  = var.private_subnet_cidrs
  database_subnet_cidrs = var.database_subnet_cidrs
}

# ── 2. Security Groups ────────────────────────────────────────
module "security_groups" {
  source = "./modules/security_groups"

  name_prefix = local.name_prefix
  vpc_id      = module.vpc.vpc_id
  vpc_cidr    = var.vpc_cidr
}

# ── 3. ACM Certificate ────────────────────────────────────────
module "acm" {
  source = "./modules/acm"

  domain_name     = var.domain_name
  app_subdomain   = var.app_subdomain
  api_subdomain   = var.api_subdomain
  route53_zone_id = module.route53.zone_id
}

# ── 4. Route53 ────────────────────────────────────────────────
module "route53" {
  source = "./modules/route53"

  domain_name     = var.domain_name
  app_subdomain   = var.app_subdomain
  api_subdomain   = var.api_subdomain
  alb_dns_name    = module.alb.dns_name
  alb_zone_id     = module.alb.zone_id
}

# ── 5. Application Load Balancer ──────────────────────────────
module "alb" {
  source = "./modules/alb"

  name_prefix       = local.name_prefix
  vpc_id            = module.vpc.vpc_id
  public_subnet_ids = module.vpc.public_subnet_ids
  alb_sg_id         = module.security_groups.alb_sg_id
  acm_cert_arn      = module.acm.certificate_arn
}

# ── 6. ECR Repositories ───────────────────────────────────────
module "ecr" {
  source = "./modules/ecr"

  name_prefix               = local.name_prefix
  image_tag_mutability      = var.ecr_image_tag_mutability
  image_retention_count     = var.ecr_image_retention_count
}

# ── 7. IAM Roles & Policies ───────────────────────────────────
module "iam" {
  source = "./modules/iam"

  name_prefix        = local.name_prefix
  s3_bucket_arn      = module.s3.bucket_arn
  ses_from_email     = var.ses_from_email
  lambda_function_arn = module.lambda.function_arn
  aws_region         = var.aws_region
  aws_account_id     = data.aws_caller_identity.current.account_id
}

# ── 8. RDS PostgreSQL ─────────────────────────────────────────
module "rds" {
  source = "./modules/rds"

  name_prefix              = local.name_prefix
  db_instance_class        = var.db_instance_class
  db_name                  = var.db_name
  db_username              = var.db_username
  db_password              = var.db_password
  db_multi_az              = var.db_multi_az
  db_backup_retention_days = var.db_backup_retention_days
  db_storage_gb            = var.db_storage_gb
  db_max_storage_gb        = var.db_max_storage_gb
  database_subnet_ids      = module.vpc.database_subnet_ids
  rds_sg_id                = module.security_groups.rds_sg_id
}

# ── 9. ElastiCache Redis ──────────────────────────────────────
module "elasticache" {
  source = "./modules/elasticache"

  name_prefix           = local.name_prefix
  redis_node_type       = var.redis_node_type
  redis_num_cache_nodes = var.redis_num_cache_nodes
  redis_engine_version  = var.redis_engine_version
  private_subnet_ids    = module.vpc.private_subnet_ids
  redis_sg_id           = module.security_groups.redis_sg_id
}

# ── 10. S3 Bucket ─────────────────────────────────────────────
module "s3" {
  source = "./modules/s3"

  bucket_name   = local.bucket_name
  name_prefix   = local.name_prefix
  force_destroy = var.s3_force_destroy
}

# ── 10a. S3 → Lambda notification (wired here to break circular dep) ──
resource "aws_s3_bucket_notification" "invoice_trigger" {
  bucket = module.s3.bucket_id

  lambda_function {
    lambda_function_arn = module.lambda.function_arn
    events              = ["s3:ObjectCreated:*"]
    filter_prefix       = "invoices/"
    filter_suffix       = ".pdf"
  }

  depends_on = [module.lambda]
}

# ── 11. SES ───────────────────────────────────────────────────
module "ses" {
  source = "./modules/ses"

  domain_name     = var.domain_name
  from_email      = var.ses_from_email
  alert_email     = var.ses_alert_email
  route53_zone_id = module.route53.zone_id
}

# ── 12. Lambda (Invoice Processor) ───────────────────────────
module "lambda" {
  source = "./modules/lambda"

  name_prefix          = local.name_prefix
  function_name        = var.lambda_function_name
  runtime              = var.lambda_runtime
  memory_size          = var.lambda_memory_size
  timeout              = var.lambda_timeout
  s3_bucket_name       = module.s3.bucket_name
  s3_bucket_arn        = module.s3.bucket_arn
  lambda_role_arn      = module.iam.lambda_role_arn
  private_subnet_ids   = module.vpc.private_subnet_ids
  lambda_sg_id         = module.security_groups.lambda_sg_id
  db_host              = module.rds.db_endpoint
  db_name              = var.db_name
  db_username          = var.db_username
  db_password          = var.db_password
  ses_from_email       = var.ses_from_email
  log_retention_days   = var.log_retention_days
}

# ── 13. CloudWatch Logs, Dashboards & Alarms ─────────────────
module "cloudwatch" {
  source = "./modules/cloudwatch"

  name_prefix          = local.name_prefix
  log_retention_days   = var.log_retention_days
  alarm_email          = var.alarm_email
  ecs_cluster_name     = module.ecs.cluster_name
  api_service_name     = module.ecs.api_service_name
  ui_service_name      = module.ecs.ui_service_name
  alb_arn_suffix       = module.alb.arn_suffix
  api_tg_arn_suffix    = module.alb.api_tg_arn_suffix
  rds_identifier       = module.rds.db_identifier
  redis_cluster_id     = module.elasticache.cluster_id
}

# ── 14. ECS Cluster + Services ───────────────────────────────
module "ecs" {
  source = "./modules/ecs"

  name_prefix           = local.name_prefix
  aws_region            = var.aws_region
  vpc_id                = module.vpc.vpc_id
  private_subnet_ids    = module.vpc.private_subnet_ids
  ecs_task_role_arn     = module.iam.ecs_task_role_arn
  ecs_exec_role_arn     = module.iam.ecs_exec_role_arn
  api_sg_id             = module.security_groups.api_sg_id
  ui_sg_id              = module.security_groups.ui_sg_id
  api_tg_arn            = module.alb.api_tg_arn
  ui_tg_arn             = module.alb.ui_tg_arn

  # API image
  api_image             = "${module.ecr.api_repo_url}:${var.api_image_tag}"
  api_cpu               = var.api_cpu
  api_memory            = var.api_memory
  api_desired_count     = var.api_desired_count
  api_min_count         = var.api_min_count
  api_max_count         = var.api_max_count

  # UI image
  ui_image              = "${module.ecr.ui_repo_url}:${var.ui_image_tag}"
  ui_cpu                = var.ui_cpu
  ui_memory             = var.ui_memory
  ui_desired_count      = var.ui_desired_count

  # App environment
  db_host               = module.rds.db_endpoint
  db_port               = "5432"
  db_name               = var.db_name
  db_username           = var.db_username
  db_password           = var.db_password
  redis_host            = module.elasticache.redis_endpoint
  redis_port            = "6379"
  s3_bucket_name        = module.s3.bucket_name
  aws_region            = var.aws_region
  ses_from_email        = var.ses_from_email
  jwt_secret            = var.jwt_secret
  jwt_expiration_ms     = tostring(var.jwt_expiration_ms)
  lambda_function_name  = var.lambda_function_name

  # CloudWatch log groups
  api_log_group         = module.cloudwatch.api_log_group_name
  ui_log_group          = module.cloudwatch.ui_log_group_name
}

# ── Data Sources ──────────────────────────────────────────────
data "aws_caller_identity" "current" {}
data "aws_region" "current" {}
