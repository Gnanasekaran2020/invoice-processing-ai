variable "name_prefix"       { type = string }
variable "log_retention_days"{ type = number }
variable "alarm_email"       { type = string }
variable "ecs_cluster_name"  { type = string }
variable "api_service_name"  { type = string }
variable "ui_service_name"   { type = string }
variable "alb_arn_suffix"    { type = string }
variable "api_tg_arn_suffix" { type = string }
variable "rds_identifier"    { type = string }
variable "redis_cluster_id"  { type = string }

