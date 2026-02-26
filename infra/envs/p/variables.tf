variable "region" {
  description = "AWS Region"
  type        = string
  default     = "ap-northeast-2"
}

variable "environment" {
  description = "Environment"
  type        = string
  default     = "prod"
}

variable "project_name" {
  description = "Project name"
  type        = string
  default     = "ainews"
}

variable "ses_sender_email" {
  description = "SES sender email address"
  type        = string
  default     = "noreply@ainews.example.com"
}

variable "app_base_url" {
  description = "Application base URL for links in emails"
  type        = string
  default     = "http://localhost:8080"
}
