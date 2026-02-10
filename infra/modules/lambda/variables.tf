variable "name" {
  description = "Lambda function name"
  type        = string
}

variable "handler" {
  description = "Lambda function handler"
  type        = string
  default     = "handler.lambda_handler"
}

variable "runtime" {
  description = "Lambda runtime"
  type        = string
  default     = "python3.11"
}

variable "timeout" {
  description = "Lambda function timeout in seconds"
  type        = number
  default     = 300
}

variable "memory_size" {
  description = "Lambda function memory size in MB"
  type        = number
  default     = 256
}

variable "source_path" {
  description = "Path to the Lambda function source code directory"
  type        = string
}

variable "sqs_arn" {
  description = "ARN of the SQS queue that triggers the Lambda"
  type        = string
  default     = null
}

variable "enable_sqs" {
  description = "SQS 트리거 및 정책 생성 여부"
  type        = bool
  default     = false # 기본값은 꺼두는 것이 안전합니다.
}

variable "sqs_batch_size" {
  description = "The maximum number of records in each batch from SQS"
  type        = number
  default     = 10
}

variable "vpc_config" {
  description = "VPC configuration for Lambda"
  type = object({
    subnet_ids         = list(string)
    security_group_ids = list(string)
  })
  default = null
}

variable "environment_variables" {
  description = "Environment variables for the Lambda function"
  type        = map(string)
  default     = {}
}

variable "ssm_parameter_arns" {
  description = "List of SSM parameter ARNs that Lambda can access"
  type        = list(string)
  default     = []
}

variable "secrets_manager_arns" {
  description = "List of Secrets Manager secret ARNs that Lambda can access"
  type        = list(string)
  default     = []
}

variable "sqs_send_arns" {
  description = "List of SQS queue ARNs that Lambda can send messages to"
  type        = list(string)
  default     = []
}

variable "tags" {
  description = "A map of tags to add to all resources"
  type        = map(string)
  default     = {}
}
