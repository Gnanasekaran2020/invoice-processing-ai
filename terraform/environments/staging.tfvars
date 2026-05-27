# =============================================================
#  environments/staging.tfvars
# =============================================================
environment = "staging"

# Smaller/cheaper instances for staging
db_instance_class     = "db.t3.small"
db_multi_az           = false
db_backup_retention_days = 3
db_storage_gb         = 20
db_max_storage_gb     = 50

redis_node_type       = "cache.t3.micro"

api_cpu           = 512
api_memory        = 1024
ui_cpu            = 256
ui_memory         = 512
api_desired_count = 1
ui_desired_count  = 1
api_min_count     = 1
api_max_count     = 3

log_retention_days = 7
s3_force_destroy   = true

