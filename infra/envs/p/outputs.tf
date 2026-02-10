output "vpc_id" {
  value = module.vpc.vpc_id
}

output "public_subnet_ids" {
  value = module.vpc.public_subnet_ids
}

output "private_subnet_ids" {
  value = module.vpc.private_subnet_ids
}

output "image_bucket" {
  value = module.storage_image.bucket_id
}

output "web_bucket" {
  value = module.storage_web.bucket_id
}

output "rds_endpoint" {
  value = module.rds.endpoint
}

output "rds_secret_arn" {
  value     = module.rds.secret_arn
  sensitive = true
}

output "ec2_instance_id" {
  description = "Backend EC2 instance ID"
  value       = module.compute_server.instance_id
}

output "ec2_public_ip" {
  description = "Backend EC2 public IP"
  value       = module.compute_server.public_ip
}

output "deploy_bucket" {
  description = "Deploy S3 bucket name"
  value       = module.storage_deploy.bucket_id
}

output "news_crawler_lambda_name" {
  description = "News crawler Lambda function name"
  value       = module.lambda_news_crawler.function_name
}

output "news_crawler_lambda_arn" {
  description = "News crawler Lambda function ARN"
  value       = module.lambda_news_crawler.function_arn
}

output "eventbridge_rule_arn" {
  description = "EventBridge rule ARN for news crawler schedule"
  value       = module.eventbridge_news_crawler.rule_arn
}

output "sqs_queue_url" {
  description = "SQS queue URL for image analysis"
  value       = module.sqs_image_analysis.queue_url
}
