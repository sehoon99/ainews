variable "name" {
  description = "Resource name prefix"
  type        = string
}

variable "domain_name" {
  description = "OpenSearch domain name"
  type        = string
}

variable "engine_version" {
  description = "OpenSearch engine version"
  type        = string
  default     = "OpenSearch_2.11"
}

variable "instance_type" {
  description = "Instance type for OpenSearch data nodes"
  type        = string
  default     = "t3.small.search"
}

variable "instance_count" {
  description = "Number of data nodes"
  type        = number
  default     = 1
}

variable "ebs_volume_size" {
  description = "EBS volume size in GB"
  type        = number
  default     = 20
}

variable "ebs_volume_type" {
  description = "EBS volume type"
  type        = string
  default     = "gp3"
}

variable "vpc_id" {
  description = "VPC ID for OpenSearch domain"
  type        = string
}

variable "subnet_ids" {
  description = "Subnet IDs for OpenSearch domain (use 1 for single-AZ)"
  type        = list(string)
}

variable "allowed_cidr_blocks" {
  description = "CIDR blocks allowed to access OpenSearch"
  type        = list(string)
  default     = ["10.0.0.0/16"]
}

variable "tags" {
  description = "Additional tags"
  type        = map(string)
  default     = {}
}
