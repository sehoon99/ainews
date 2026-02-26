variable "name" {
  description = "Name prefix for resources"
  type        = string
}

variable "vpc_id" {
  description = "VPC ID"
  type        = string
}

variable "subnet_id" {
  description = "Subnet ID for EC2 instance"
  type        = string
}

variable "instance_type" {
  description = "EC2 instance type"
  type        = string
  default     = "t3.micro"
}

variable "app_port" {
  description = "Application port"
  type        = number
  default     = 8080
}

variable "deploy_bucket" {
  description = "S3 bucket name for JAR deployment"
  type        = string
}

variable "secrets_manager_arns" {
  description = "Secrets Manager ARNs the EC2 instance can read"
  type        = list(string)
  default     = []
}

variable "allowed_cidr_blocks" {
  description = "CIDR blocks allowed to access app port"
  type        = list(string)
  default     = ["0.0.0.0/0"]
}

variable "db_secret_name" {
  description = "Secrets Manager secret name for DB credentials"
  type        = string
  default     = ""
}

variable "region" {
  description = "AWS region"
  type        = string
  default     = "ap-northeast-2"
}

variable "ses_send_policy_arn" {
  description = "IAM policy ARN for SES send permissions"
  type        = string
  default     = ""
}

variable "ses_sender_email" {
  description = "SES sender email address"
  type        = string
  default     = ""
}

variable "app_base_url" {
  description = "Application base URL for links in emails"
  type        = string
  default     = "http://localhost:8080"
}

variable "tags" {
  description = "Tags for resources"
  type        = map(string)
  default     = {}
}