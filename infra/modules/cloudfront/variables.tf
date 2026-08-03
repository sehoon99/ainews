variable "name" {
  description = "Name prefix for CloudFront resources"
  type        = string
}

variable "web_bucket_id" {
  description = "S3 bucket ID that stores the frontend"
  type        = string
}

variable "web_bucket_arn" {
  description = "S3 bucket ARN that stores the frontend"
  type        = string
}

variable "web_bucket_regional_domain_name" {
  description = "Regional domain name of the frontend S3 bucket"
  type        = string
}

variable "api_origin_domain_name" {
  description = "Public ALB DNS name used for API requests"
  type        = string
}

variable "origin_verify_header_value" {
  description = "Shared value added to requests sent to the ALB origin"
  type        = string
  sensitive   = true
}

variable "price_class" {
  description = "CloudFront price class"
  type        = string
  default     = "PriceClass_200"
}

variable "tags" {
  description = "Tags to apply to CloudFront resources"
  type        = map(string)
  default     = {}
}
