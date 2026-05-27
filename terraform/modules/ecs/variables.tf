variable "name_prefix"          { type = string }
variable "aws_region"           { type = string }
variable "vpc_id"               { type = string }
variable "private_subnet_ids"   { type = list(string) }
variable "ecs_task_role_arn"    { type = string }
variable "ecs_exec_role_arn"    { type = string }
variable "api_sg_id"            { type = string }
variable "ui_sg_id"             { type = string }
variable "api_tg_arn"           { type = string }
variable "ui_tg_arn"            { type = string }
variable "alb_arn_suffix"       { type = string }
variable "api_tg_arn_suffix"    { type = string }

# Images
variable "api_image"            { type = string }
variable "ui_image"             { type = string }

# API sizing & scaling
variable "api_cpu"              { type = number }
variable "api_memory"           { type = number }
variable "api_desired_count"    { type = number }
variable "api_min_count"        { type = number }
variable "api_max_count"        { type = number }

# UI sizing
variable "ui_cpu"               { type = number }
variable "ui_memory"            { type = number }
variable "ui_desired_count"     { type = number }

# App configuration
variable "db_host"              { type = string;  sensitive = true }
variable "db_port"              { type = string }
variable "db_name"              { type = string }
variable "db_username"          { type = string;  sensitive = true }
variable "db_password"          { type = string;  sensitive = true }
variable "redis_host"           { type = string;  sensitive = true }
variable "redis_port"           { type = string }
variable "s3_bucket_name"       { type = string }
variable "ses_from_email"       { type = string }
variable "jwt_secret"           { type = string;  sensitive = true }
variable "jwt_expiration_ms"    { type = string }
variable "lambda_function_name" { type = string }

# Log groups (created by cloudwatch module)
variable "api_log_group"        { type = string }
variable "ui_log_group"         { type = string }
