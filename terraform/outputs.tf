# =============================================================
#  Outputs — Invoice Processing System
# =============================================================

# ── URLs ──────────────────────────────────────────────────────
output "app_url" {
  description = "Angular UI application URL"
  value       = "https://${var.app_subdomain}.${var.domain_name}"
}

output "api_url" {
  description = "Spring Boot API URL"
  value       = "https://${var.api_subdomain}.${var.domain_name}"
}

output "alb_dns_name" {
  description = "ALB DNS name (use for debugging)"
  value       = module.alb.dns_name
}

# ── ECR ───────────────────────────────────────────────────────
output "ecr_api_repo_url" {
  description = "ECR repository URL for the Spring Boot API"
  value       = module.ecr.api_repo_url
}

output "ecr_ui_repo_url" {
  description = "ECR repository URL for the Angular UI"
  value       = module.ecr.ui_repo_url
}

# ── ECS ───────────────────────────────────────────────────────
output "ecs_cluster_name" {
  description = "ECS cluster name"
  value       = module.ecs.cluster_name
}

output "ecs_api_service_name" {
  description = "ECS service name for the API"
  value       = module.ecs.api_service_name
}

output "ecs_ui_service_name" {
  description = "ECS service name for the UI"
  value       = module.ecs.ui_service_name
}

# ── Database ──────────────────────────────────────────────────
output "rds_endpoint" {
  description = "RDS PostgreSQL endpoint"
  value       = module.rds.db_endpoint
  sensitive   = true
}

output "rds_db_name" {
  description = "RDS database name"
  value       = module.rds.db_name
}

# ── Redis ─────────────────────────────────────────────────────
output "redis_endpoint" {
  description = "ElastiCache Redis primary endpoint"
  value       = module.elasticache.redis_endpoint
  sensitive   = true
}

# ── S3 ────────────────────────────────────────────────────────
output "s3_bucket_name" {
  description = "S3 bucket name for invoice documents"
  value       = module.s3.bucket_name
}

output "s3_bucket_arn" {
  description = "S3 bucket ARN"
  value       = module.s3.bucket_arn
}

# ── SES ───────────────────────────────────────────────────────
output "ses_domain_identity_arn" {
  description = "SES domain identity ARN"
  value       = module.ses.domain_identity_arn
}

# ── Lambda ────────────────────────────────────────────────────
output "lambda_function_arn" {
  description = "Lambda invoice processor function ARN"
  value       = module.lambda.function_arn
}

output "lambda_function_name" {
  description = "Lambda invoice processor function name"
  value       = module.lambda.function_name
}

# ── VPC ───────────────────────────────────────────────────────
output "vpc_id" {
  description = "VPC ID"
  value       = module.vpc.vpc_id
}

output "private_subnet_ids" {
  description = "Private subnet IDs (ECS)"
  value       = module.vpc.private_subnet_ids
}

output "public_subnet_ids" {
  description = "Public subnet IDs (ALB)"
  value       = module.vpc.public_subnet_ids
}

# ── IAM ───────────────────────────────────────────────────────
output "ecs_task_role_arn" {
  description = "ECS task IAM role ARN"
  value       = module.iam.ecs_task_role_arn
}

# ── Deployment Helper Commands ────────────────────────────────
output "docker_push_api_cmd" {
  description = "Command to push API image to ECR"
  value       = <<-EOT
    aws ecr get-login-password --region ${var.aws_region} | docker login --username AWS --password-stdin ${module.ecr.api_repo_url}
    docker build -t ${module.ecr.api_repo_url}:${var.api_image_tag} ./invoice_process_sb
    docker push ${module.ecr.api_repo_url}:${var.api_image_tag}
  EOT
}

output "docker_push_ui_cmd" {
  description = "Command to push UI image to ECR"
  value       = <<-EOT
    aws ecr get-login-password --region ${var.aws_region} | docker login --username AWS --password-stdin ${module.ecr.ui_repo_url}
    docker build -t ${module.ecr.ui_repo_url}:${var.ui_image_tag} ./invoice_processing_angular_optimized
    docker push ${module.ecr.ui_repo_url}:${var.ui_image_tag}
  EOT
}

