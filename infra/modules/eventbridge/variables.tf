variable "name" {
  description = "EventBridge rule name"
  type        = string
}

variable "description" {
  description = "EventBridge rule description"
  type        = string
  default     = ""
}

variable "schedule_expression" {
  description = "Schedule expression (e.g., rate(4 hours), cron(0 */4 * * ? *))"
  type        = string
}

variable "lambda_arn" {
  description = "ARN of the Lambda function to invoke"
  type        = string
}

variable "lambda_function_name" {
  description = "Name of the Lambda function (for permission resource)"
  type        = string
}

variable "tags" {
  description = "A map of tags to add to all resources"
  type        = map(string)
  default     = {}
}
