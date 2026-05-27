# =============================================================
#  environments/production.tfvars
# =============================================================
environment = "production"

# Production-grade instances
db_instance_class        = "db.r6g.large"
db_multi_az              = true
db_backup_retention_days = 14
db_storage_gb            = 100
db_max_storage_gb        = 500

redis_node_type       = "cache.r6g.large"
redis_num_cache_nodes = 1
redis_engine_version  = "7.1"

api_cpu           = 2048
api_memory        = 4096
ui_cpu            = 512
ui_memory         = 1024
api_desired_count = 3
ui_desired_count  = 2
api_min_count     = 2
api_max_count     = 20

lambda_memory_size = 2048
lambda_timeout     = 300

log_retention_days = 90
s3_force_destroy   = false

ecr_image_tag_mutability  = "IMMUTABLE"
ecr_image_retention_count = 20

additional_tags = {
  CostCenter  = "engineering"
  Team        = "invoice-platform"
  Compliance  = "SOC2"
  DataClass   = "Confidential"
}

