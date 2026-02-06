output "function_name" {
  description = "Lambda function name"
  value       = aws_lambda_function.this.function_name
}

output "function_arn" {
  description = "Lambda function ARN"
  value       = aws_lambda_function.this.arn
}

output "invoke_arn" {
  description = "Lambda function invoke ARN"
  value       = aws_lambda_function.this.invoke_arn
}

output "role_arn" {
  description = "IAM role ARN for Lambda"
  value       = aws_iam_role.lambda.arn
}

output "role_name" {
  description = "IAM role name for Lambda"
  value       = aws_iam_role.lambda.name
}

output "security_group_id" {
  description = "Security group ID for Lambda (if VPC configured)"
  value       = var.vpc_config != null ? aws_security_group.lambda[0].id : null
}
