variable "name_prefix"              { type = string }
variable "db_instance_class"        { type = string }
variable "db_name"                  { type = string }
variable "db_username"              { type = string; sensitive = true }
variable "db_password"              { type = string; sensitive = true }
variable "db_multi_az"              { type = bool }
variable "db_backup_retention_days" { type = number }
variable "db_storage_gb"            { type = number }
variable "db_max_storage_gb"        { type = number }
variable "database_subnet_ids"      { type = list(string) }
variable "rds_sg_id"                { type = string }
variable "create_read_replica"      { type = bool; default = false }

