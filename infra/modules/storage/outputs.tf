output "bucket_id" {
  value = aws_s3_bucket.this.id
}

output "bucket_arn" {
  value = aws_s3_bucket.this.arn
}

output "bucket_regional_domain_name" {
  value = aws_s3_bucket.this.bucket_regional_domain_name
}

output "db_subnet_group_name" {
  value = var.create_db_subnet_group ? aws_db_subnet_group.this[0].name : null
}

output "db_security_group_id" {
  value = var.create_db_subnet_group ? aws_security_group.db[0].id : null
}
