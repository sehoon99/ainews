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

output "ecr_repository_url" {
  description = "Backend ECR repository URL"
  value       = module.ecs_backend.ecr_repository_url
}

output "prometheus_repository_url" {
  description = "Prometheus ECR repository URL"
  value       = module.ecs_backend.prometheus_repository_url
}

output "grafana_repository_url" {
  description = "Grafana ECR repository URL"
  value       = module.ecs_backend.grafana_repository_url
}

output "ecs_cluster_name" {
  description = "Backend ECS cluster name"
  value       = module.ecs_backend.cluster_name
}

output "ecs_service_name" {
  description = "Backend ECS service name"
  value       = module.ecs_backend.service_name
}

output "monitoring_ecs_service_name" {
  description = "Prometheus and Grafana ECS service name"
  value       = module.ecs_backend.monitoring_service_name
}

output "grafana_admin_secret_arn" {
  description = "Secrets Manager ARN containing the Grafana admin password"
  value       = module.ecs_backend.grafana_admin_secret_arn
  sensitive   = true
}

output "ecs_autoscaling_group_name" {
  description = "Spot ECS Auto Scaling Group name"
  value       = module.ecs_backend.autoscaling_group_name
}

output "backend_alb_dns_name" {
  description = "Backend ALB origin DNS name"
  value       = module.ecs_backend.load_balancer_dns_name
}

output "cloudfront_distribution_id" {
  description = "Frontend CloudFront distribution ID"
  value       = module.cloudfront.distribution_id
}

output "cloudfront_domain_name" {
  description = "Frontend CloudFront domain name"
  value       = module.cloudfront.domain_name
}

output "grafana_url" {
  description = "Grafana URL exposed through CloudFront"
  value       = "https://${module.cloudfront.domain_name}/grafana/"
}
