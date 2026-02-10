output "instance_id" {
  description = "EC2 instance ID"
  value       = aws_instance.app.id
}

output "security_group_id" {
  description = "Application security group ID"
  value       = aws_security_group.app.id
}

output "public_ip" {
  description = "EC2 public IP address"
  value       = aws_instance.app.public_ip
}
