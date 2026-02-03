output "endpoint" {
  description = "RDS endpoint"
  value       = aws_db_instance.this.endpoint
}

output "address" {
  description = "RDS address (hostname)"
  value       = aws_db_instance.this.address
}

output "port" {
  description = "RDS port"
  value       = aws_db_instance.this.port
}

output "db_name" {
  description = "Database name"
  value       = aws_db_instance.this.db_name
}

output "security_group_id" {
  description = "RDS security group ID"
  value       = aws_security_group.this.id
}

output "secret_arn" {
  description = "Secrets Manager secret ARN"
  value       = aws_secretsmanager_secret.this.arn
}
