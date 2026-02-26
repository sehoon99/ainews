variable "name" {
  description = "Name prefix for resources"
  type        = string
}

variable "sender_email" {
  description = "Sender email address for SES"
  type        = string
}

variable "tags" {
  description = "Tags for resources"
  type        = map(string)
  default     = {}
}