variable "name_prefix"        { type = string }
variable "function_name"      { type = string }
variable "runtime"            { type = string }
variable "memory_size"        { type = number }
variable "timeout"            { type = number }
variable "lambda_role_arn"    { type = string }
variable "private_subnet_ids" { type = list(string) }
variable "lambda_sg_id"       { type = string }
variable "s3_bucket_name"     { type = string }
variable "s3_bucket_arn"      { type = string }
variable "db_host"            { type = string; sensitive = true }
variable "db_name"            { type = string }
variable "db_username"        { type = string; sensitive = true }
variable "db_password"        { type = string; sensitive = true }
variable "ses_from_email"     { type = string }
variable "log_retention_days" { type = number; default = 30 }
variable "aws_region"         { type = string }
