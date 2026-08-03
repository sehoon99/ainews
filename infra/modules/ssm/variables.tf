variable "name" {
  description = "Parameter name"
  type        = string
}

variable "type" {
  description = "Parameter type"
  type        = string
  default     = "SecureString"
}

variable "value" {
  description = "Parameter value"
  type        = string
  sensitive   = true
}

variable "key_id" {
  description = "KMS key ID for encryption"
  type        = string
  default     = ""
}

variable "tags" {
  description = "Tags to apply to the SSM parameter"
  type        = map(string)
  default     = {}
}
