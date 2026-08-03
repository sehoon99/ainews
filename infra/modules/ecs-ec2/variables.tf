variable "name" {
  description = "Name prefix for ECS resources"
  type        = string
}

variable "vpc_id" {
  description = "VPC ID"
  type        = string
}

variable "public_subnet_ids" {
  description = "Public subnets used by the ALB and Spot container instances"
  type        = list(string)
}

variable "cloudfront_prefix_list_id" {
  description = "CloudFront origin-facing managed prefix list ID"
  type        = string
}

variable "container_port" {
  description = "Backend container port"
  type        = number
  default     = 8080
}

variable "image_tag" {
  description = "Immutable backend image tag"
  type        = string
}

variable "origin_verify_header_value" {
  description = "Shared value required on CloudFront origin requests"
  type        = string
  sensitive   = true
}

variable "instance_types" {
  description = "Diversified x86 instance types for Spot placement"
  type        = list(string)
  default     = ["t3.small", "t3a.small"]
}

variable "asg_min_size" {
  type    = number
  default = 1
}

variable "asg_desired_capacity" {
  type    = number
  default = 2
}

variable "asg_max_size" {
  type    = number
  default = 4
}

variable "service_desired_count" {
  type    = number
  default = 2
}

variable "service_max_count" {
  type    = number
  default = 4
}

variable "db_host" {
  type = string
}

variable "db_port" {
  type = number
}

variable "db_name" {
  type = string
}

variable "db_secret_arn" {
  type = string
}

variable "ses_send_policy_arn" {
  type = string
}

variable "ses_sender_email" {
  type = string
}

variable "app_base_url" {
  type = string
}

variable "tags" {
  description = "Tags to apply to ECS resources"
  type        = map(string)
  default     = {}
}
