variable "name" {
  description = "SQS queue name"
  type        = string
}

variable "visibility_timeout_seconds" {
  description = "The visibility timeout for the queue (seconds)"
  type        = number
  default     = 300
}

variable "message_retention_seconds" {
  description = "The number of seconds to retain a message (1 minute to 14 days)"
  type        = number
  default     = 1209600 # 14 days
}

variable "max_receive_count" {
  description = "The number of times a message can be received before being moved to the DLQ"
  type        = number
  default     = 3
}

variable "delay_seconds" {
  description = "The time in seconds that the delivery of messages is delayed"
  type        = number
  default     = 0
}

variable "max_message_size" {
  description = "The limit of how many bytes a message can contain (up to 262144)"
  type        = number
  default     = 262144
}

variable "receive_wait_time_seconds" {
  description = "The time for which a ReceiveMessage call will wait for a message (long polling)"
  type        = number
  default     = 20
}

variable "tags" {
  description = "A map of tags to add to all resources"
  type        = map(string)
  default     = {}
}
