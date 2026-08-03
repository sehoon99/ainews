output "ecr_repository_url" {
  value = aws_ecr_repository.app.repository_url
}

output "prometheus_repository_url" {
  value = aws_ecr_repository.prometheus.repository_url
}

output "grafana_repository_url" {
  value = aws_ecr_repository.grafana.repository_url
}

output "cluster_name" {
  value = aws_ecs_cluster.this.name
}

output "service_name" {
  value = aws_ecs_service.app.name
}

output "monitoring_service_name" {
  value = aws_ecs_service.monitoring.name
}

output "grafana_admin_secret_arn" {
  value     = aws_secretsmanager_secret.grafana_admin.arn
  sensitive = true
}

output "autoscaling_group_name" {
  value = aws_autoscaling_group.ecs.name
}

output "load_balancer_dns_name" {
  value = aws_lb.app.dns_name
}

output "load_balancer_arn" {
  value = aws_lb.app.arn
}
