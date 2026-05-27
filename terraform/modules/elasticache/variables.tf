variable "name_prefix"           { type = string }
variable "redis_node_type"       { type = string }
variable "redis_num_cache_nodes" { type = number }
variable "redis_engine_version"  { type = string }
variable "private_subnet_ids"    { type = list(string) }
variable "redis_sg_id"           { type = string }

