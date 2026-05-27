variable "name_prefix"           { type = string }
variable "image_tag_mutability"  { type = string; default = "IMMUTABLE" }
variable "image_retention_count" { type = number; default = 10 }

