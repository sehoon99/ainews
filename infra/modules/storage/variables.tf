variable "name" {
  description = "Name prefix for resources"
  type        = string
}

variable "bucket_name" {
  description = "S3 bucket name"
  type        = string
}

variable "enable_versioning" {
  description = "Enable S3 versioning"
  type        = bool
  default     = true
}

variable "vpc_id" {
  description = "VPC ID"
  type        = string
  default     = ""
}

variable "private_subnet_ids" {
  description = "Private subnet IDs for DB subnet group"
  type        = list(string)
  default     = []
}

variable "app_security_group_ids" {
  description = "App security group IDs for DB access"
  type        = list(string)
  default     = []
}

variable "create_db_subnet_group" {
  description = "Create DB subnet group"
  type        = bool
  default     = false
}

variable "create_redis" {
  description = "Create Reddis"
  type        = bool
  default     = false
}
