output "instance_id" {
  description = "Bastion EC2 instance ID"
  value       = aws_instance.bastion.id
}

output "security_group_id" {
  description = "Bastion security group ID"
  value       = aws_security_group.bastion.id
}

output "private_ip" {
  description = "Bastion private IP address"
  value       = aws_instance.bastion.private_ip
}
