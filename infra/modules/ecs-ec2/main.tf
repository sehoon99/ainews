data "aws_ssm_parameter" "ecs_optimized_ami" {
  name = "/aws/service/ecs/optimized-ami/amazon-linux-2023/recommended/image_id"
}

resource "aws_ecr_repository" "app" {
  name                 = "${var.name}-backend"
  image_tag_mutability = "IMMUTABLE"

  image_scanning_configuration {
    scan_on_push = true
  }

  encryption_configuration {
    encryption_type = "AES256"
  }

  tags = merge(var.tags, { Name = "${var.name}-backend" })
}

resource "aws_ecr_lifecycle_policy" "app" {
  repository = aws_ecr_repository.app.name
  policy = jsonencode({
    rules = [{
      rulePriority = 1
      description  = "Keep the latest 20 backend images"
      selection = {
        tagStatus   = "any"
        countType   = "imageCountMoreThan"
        countNumber = 20
      }
      action = { type = "expire" }
    }]
  })
}

resource "aws_ecr_repository" "prometheus" {
  name                 = "${var.name}-prometheus"
  image_tag_mutability = "IMMUTABLE"

  image_scanning_configuration {
    scan_on_push = true
  }

  encryption_configuration {
    encryption_type = "AES256"
  }

  tags = merge(var.tags, {
    Name    = "${var.name}-prometheus"
    Service = "monitoring"
  })
}

resource "aws_ecr_repository" "grafana" {
  name                 = "${var.name}-grafana"
  image_tag_mutability = "IMMUTABLE"

  image_scanning_configuration {
    scan_on_push = true
  }

  encryption_configuration {
    encryption_type = "AES256"
  }

  tags = merge(var.tags, {
    Name    = "${var.name}-grafana"
    Service = "monitoring"
  })
}

resource "aws_ecr_lifecycle_policy" "monitoring" {
  for_each = {
    prometheus = aws_ecr_repository.prometheus.name
    grafana    = aws_ecr_repository.grafana.name
  }

  repository = each.value
  policy = jsonencode({
    rules = [{
      rulePriority = 1
      description  = "Keep the latest 20 ${each.key} images"
      selection = {
        tagStatus   = "any"
        countType   = "imageCountMoreThan"
        countNumber = 20
      }
      action = { type = "expire" }
    }]
  })
}

resource "aws_ecs_cluster" "this" {
  name = var.name

  setting {
    name  = "containerInsights"
    value = "enabled"
  }

  tags = merge(var.tags, { Name = var.name })
}

resource "aws_cloudwatch_log_group" "app" {
  name              = "/ecs/${var.name}-backend"
  retention_in_days = 30
  tags              = merge(var.tags, { Name = "${var.name}-backend" })
}

resource "aws_cloudwatch_log_group" "monitoring" {
  name              = "/ecs/${var.name}-monitoring"
  retention_in_days = 30
  tags = merge(var.tags, {
    Name    = "${var.name}-monitoring"
    Service = "monitoring"
  })
}

resource "random_password" "grafana_admin" {
  length  = 32
  special = false
}

resource "aws_secretsmanager_secret" "grafana_admin" {
  name                    = "${var.name}/grafana/admin-password"
  recovery_window_in_days = 7
  tags = merge(var.tags, {
    Name    = "${var.name}-grafana-admin"
    Service = "monitoring"
  })
}

resource "aws_secretsmanager_secret_version" "grafana_admin" {
  secret_id     = aws_secretsmanager_secret.grafana_admin.id
  secret_string = random_password.grafana_admin.result
}

resource "aws_iam_role" "container_instance" {
  name = "${var.name}-ecs-instance-role"
  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Action    = "sts:AssumeRole"
      Effect    = "Allow"
      Principal = { Service = "ec2.amazonaws.com" }
    }]
  })
  tags = merge(var.tags, { Name = "${var.name}-ecs-instance-role" })
}

resource "aws_iam_role_policy_attachment" "container_instance_ecs" {
  role       = aws_iam_role.container_instance.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonEC2ContainerServiceforEC2Role"
}

resource "aws_iam_role_policy_attachment" "container_instance_ssm" {
  role       = aws_iam_role.container_instance.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonSSMManagedInstanceCore"
}

resource "aws_iam_instance_profile" "container_instance" {
  name = "${var.name}-ecs-instance-profile"
  role = aws_iam_role.container_instance.name
}

resource "aws_iam_role" "task_execution" {
  name = "${var.name}-task-execution-role"
  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Action    = "sts:AssumeRole"
      Effect    = "Allow"
      Principal = { Service = "ecs-tasks.amazonaws.com" }
    }]
  })
  tags = merge(var.tags, { Name = "${var.name}-task-execution-role" })
}

resource "aws_iam_role_policy_attachment" "task_execution" {
  role       = aws_iam_role.task_execution.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy"
}

resource "aws_iam_role_policy" "task_execution_secrets" {
  name = "read-db-secret"
  role = aws_iam_role.task_execution.id
  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect   = "Allow"
      Action   = ["secretsmanager:GetSecretValue"]
      Resource = [var.db_secret_arn]
    }]
  })
}

resource "aws_iam_role" "monitoring_execution" {
  name = "${var.name}-monitoring-execution-role"
  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Action    = "sts:AssumeRole"
      Effect    = "Allow"
      Principal = { Service = "ecs-tasks.amazonaws.com" }
    }]
  })
  tags = merge(var.tags, {
    Name    = "${var.name}-monitoring-execution-role"
    Service = "monitoring"
  })
}

resource "aws_iam_role_policy_attachment" "monitoring_execution" {
  role       = aws_iam_role.monitoring_execution.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy"
}

resource "aws_iam_role_policy" "monitoring_execution_secret" {
  name = "read-grafana-secret"
  role = aws_iam_role.monitoring_execution.id
  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect   = "Allow"
      Action   = ["secretsmanager:GetSecretValue"]
      Resource = [aws_secretsmanager_secret.grafana_admin.arn]
    }]
  })
}

resource "aws_iam_role" "task" {
  name = "${var.name}-task-role"
  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Action    = "sts:AssumeRole"
      Effect    = "Allow"
      Principal = { Service = "ecs-tasks.amazonaws.com" }
    }]
  })
  tags = merge(var.tags, { Name = "${var.name}-task-role" })
}

resource "aws_iam_role_policy_attachment" "task_ses" {
  role       = aws_iam_role.task.name
  policy_arn = var.ses_send_policy_arn
}

resource "aws_security_group" "alb" {
  name        = "${var.name}-alb-sg"
  description = "Allow API requests only from CloudFront origin-facing addresses"
  vpc_id      = var.vpc_id

  ingress {
    description     = "HTTP from CloudFront"
    from_port       = 80
    to_port         = 80
    protocol        = "tcp"
    prefix_list_ids = [var.cloudfront_prefix_list_id]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = merge(var.tags, { Name = "${var.name}-alb-sg" })
}

resource "aws_security_group" "container_instance" {
  name        = "${var.name}-ecs-instance-sg"
  description = "Allow dynamic ECS host ports from the application load balancer"
  vpc_id      = var.vpc_id

  ingress {
    description     = "Dynamic host ports from ALB"
    from_port       = 32768
    to_port         = 65535
    protocol        = "tcp"
    security_groups = [aws_security_group.alb.id]
  }

  ingress {
    description = "Prometheus scraping between ECS container instances"
    from_port   = 32768
    to_port     = 65535
    protocol    = "tcp"
    self        = true
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = merge(var.tags, { Name = "${var.name}-ecs-instance-sg" })
}

resource "aws_service_discovery_private_dns_namespace" "this" {
  name        = "ainews.local"
  description = "Private service discovery for AI News workloads"
  vpc         = var.vpc_id

  tags = merge(var.tags, {
    Name    = "${var.name}-service-discovery"
    Service = "monitoring"
  })
}

resource "aws_service_discovery_service" "backend_metrics" {
  name = "backend-metrics"

  dns_config {
    namespace_id = aws_service_discovery_private_dns_namespace.this.id

    dns_records {
      ttl  = 10
      type = "SRV"
    }

    routing_policy = "MULTIVALUE"
  }

  health_check_custom_config {
    failure_threshold = 1
  }

  tags = merge(var.tags, {
    Name    = "${var.name}-backend-metrics"
    Service = "monitoring"
  })
}

resource "aws_lb" "app" {
  name                       = substr("${var.name}-alb", 0, 32)
  internal                   = false
  load_balancer_type         = "application"
  security_groups            = [aws_security_group.alb.id]
  subnets                    = var.public_subnet_ids
  enable_deletion_protection = false
  drop_invalid_header_fields = true
  tags                       = merge(var.tags, { Name = "${var.name}-alb" })
}

resource "aws_lb_target_group" "app" {
  name        = substr("${var.name}-backend", 0, 32)
  port        = var.container_port
  protocol    = "HTTP"
  target_type = "instance"
  vpc_id      = var.vpc_id

  health_check {
    enabled             = true
    path                = "/api/keyword-ranks/top"
    protocol            = "HTTP"
    matcher             = "200-399"
    interval            = 30
    timeout             = 10
    healthy_threshold   = 2
    unhealthy_threshold = 3
  }

  deregistration_delay = 30
  tags                 = merge(var.tags, { Name = "${var.name}-backend" })
}

resource "aws_lb_target_group" "grafana" {
  name        = substr("${var.name}-grafana", 0, 32)
  port        = 3000
  protocol    = "HTTP"
  target_type = "instance"
  vpc_id      = var.vpc_id

  health_check {
    enabled             = true
    path                = "/grafana/api/health"
    protocol            = "HTTP"
    matcher             = "200"
    interval            = 30
    timeout             = 10
    healthy_threshold   = 2
    unhealthy_threshold = 3
  }

  deregistration_delay = 30
  tags = merge(var.tags, {
    Name    = "${var.name}-grafana"
    Service = "monitoring"
  })
}

resource "aws_lb_listener" "http" {
  load_balancer_arn = aws_lb.app.arn
  port              = 80
  protocol          = "HTTP"

  default_action {
    type = "fixed-response"

    fixed_response {
      content_type = "text/plain"
      message_body = "Forbidden"
      status_code  = "403"
    }
  }

  tags = merge(var.tags, { Name = "${var.name}-http" })
}

resource "aws_lb_listener_rule" "cloudfront" {
  listener_arn = aws_lb_listener.http.arn
  priority     = 1

  action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.app.arn
  }

  condition {
    http_header {
      http_header_name = "X-Origin-Verify"
      values           = [var.origin_verify_header_value]
    }
  }

  condition {
    path_pattern {
      values = ["/api/*"]
    }
  }

  tags = merge(var.tags, { Name = "${var.name}-cloudfront-origin" })
}

resource "aws_lb_listener_rule" "grafana" {
  listener_arn = aws_lb_listener.http.arn
  priority     = 2

  action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.grafana.arn
  }

  condition {
    http_header {
      http_header_name = "X-Origin-Verify"
      values           = [var.origin_verify_header_value]
    }
  }

  condition {
    path_pattern {
      values = ["/grafana", "/grafana/*"]
    }
  }

  tags = merge(var.tags, {
    Name    = "${var.name}-grafana-origin"
    Service = "monitoring"
  })
}

resource "aws_launch_template" "ecs" {
  name_prefix            = "${var.name}-ecs-"
  image_id               = data.aws_ssm_parameter.ecs_optimized_ami.value
  instance_type          = var.instance_types[0]
  update_default_version = true

  iam_instance_profile {
    arn = aws_iam_instance_profile.container_instance.arn
  }

  network_interfaces {
    associate_public_ip_address = true
    security_groups             = [aws_security_group.container_instance.id]
  }

  metadata_options {
    http_endpoint               = "enabled"
    http_tokens                 = "required"
    http_put_response_hop_limit = 2
  }

  block_device_mappings {
    device_name = "/dev/xvda"
    ebs {
      volume_size           = 30
      volume_type           = "gp3"
      encrypted             = true
      delete_on_termination = true
    }
  }

  user_data = base64encode(<<-EOT
    #!/bin/bash
    cat <<'EOF' >> /etc/ecs/ecs.config
    ECS_CLUSTER=${aws_ecs_cluster.this.name}
    ECS_ENABLE_SPOT_INSTANCE_DRAINING=true
    ECS_ENABLE_CONTAINER_METADATA=true
    EOF
  EOT
  )

  tag_specifications {
    resource_type = "instance"
    tags          = merge(var.tags, { Name = "${var.name}-ecs-spot" })
  }

  tag_specifications {
    resource_type = "volume"
    tags          = merge(var.tags, { Name = "${var.name}-ecs-spot" })
  }

  tags = merge(var.tags, { Name = "${var.name}-ecs-launch-template" })
}

resource "aws_autoscaling_group" "ecs" {
  name                      = "${var.name}-ecs-spot"
  min_size                  = var.asg_min_size
  desired_capacity          = var.asg_desired_capacity
  max_size                  = var.asg_max_size
  vpc_zone_identifier       = var.public_subnet_ids
  health_check_type         = "EC2"
  health_check_grace_period = 180
  capacity_rebalance        = true

  mixed_instances_policy {
    launch_template {
      launch_template_specification {
        launch_template_id = aws_launch_template.ecs.id
        version            = "$Latest"
      }

      dynamic "override" {
        for_each = toset(var.instance_types)
        content {
          instance_type = override.value
        }
      }
    }

    instances_distribution {
      on_demand_base_capacity                  = 0
      on_demand_percentage_above_base_capacity = 0
      spot_allocation_strategy                 = "price-capacity-optimized"
    }
  }

  instance_refresh {
    strategy = "Rolling"
    preferences {
      min_healthy_percentage = 50
    }
    triggers = ["tag"]
  }

  dynamic "tag" {
    for_each = merge(var.tags, { Name = "${var.name}-ecs-spot", AmazonECSManaged = "" })
    content {
      key                 = tag.key
      value               = tag.value
      propagate_at_launch = true
    }
  }

  lifecycle {
    create_before_destroy = true
    ignore_changes        = [desired_capacity]
  }
}

resource "aws_ecs_capacity_provider" "spot" {
  name = "${var.name}-spot"

  auto_scaling_group_provider {
    auto_scaling_group_arn         = aws_autoscaling_group.ecs.arn
    managed_draining               = "ENABLED"
    managed_termination_protection = "DISABLED"

    managed_scaling {
      status                    = "ENABLED"
      target_capacity           = 80
      minimum_scaling_step_size = 1
      maximum_scaling_step_size = 1
    }
  }

  tags = merge(var.tags, { Name = "${var.name}-spot" })
}

resource "aws_ecs_cluster_capacity_providers" "this" {
  cluster_name       = aws_ecs_cluster.this.name
  capacity_providers = [aws_ecs_capacity_provider.spot.name]

  default_capacity_provider_strategy {
    capacity_provider = aws_ecs_capacity_provider.spot.name
    base              = 0
    weight            = 1
  }
}

resource "aws_ecs_task_definition" "app" {
  family                   = "${var.name}-backend"
  requires_compatibilities = ["EC2"]
  network_mode             = "bridge"
  cpu                      = "512"
  memory                   = "768"
  execution_role_arn       = aws_iam_role.task_execution.arn
  task_role_arn            = aws_iam_role.task.arn

  container_definitions = jsonencode([{
    name      = "backend"
    image     = "${aws_ecr_repository.app.repository_url}:${var.image_tag}"
    essential = true
    cpu       = 512
    memory    = 768
    portMappings = [
      {
        containerPort = var.container_port
        hostPort      = 0
        protocol      = "tcp"
      },
      {
        containerPort = 9091
        hostPort      = 0
        protocol      = "tcp"
      }
    ]
    environment = [
      { name = "SPRING_PROFILES_ACTIVE", value = "prod" },
      { name = "DB_HOST", value = var.db_host },
      { name = "DB_PORT", value = tostring(var.db_port) },
      { name = "DB_NAME", value = var.db_name },
      { name = "SES_SENDER_EMAIL", value = var.ses_sender_email },
      { name = "APP_BASE_URL", value = var.app_base_url }
    ]
    secrets = [
      { name = "DB_USERNAME", valueFrom = "${var.db_secret_arn}:username::" },
      { name = "DB_PASSWORD", valueFrom = "${var.db_secret_arn}:password::" }
    ]
    logConfiguration = {
      logDriver = "awslogs"
      options = {
        awslogs-group         = aws_cloudwatch_log_group.app.name
        awslogs-region        = data.aws_region.current.name
        awslogs-stream-prefix = "backend"
      }
    }
  }])

  tags = merge(var.tags, { Name = "${var.name}-backend" })
}

data "aws_region" "current" {}

resource "aws_ecs_service" "app" {
  name                               = "${var.name}-backend"
  cluster                            = aws_ecs_cluster.this.id
  task_definition                    = aws_ecs_task_definition.app.arn
  desired_count                      = var.service_desired_count
  deployment_minimum_healthy_percent = 50
  deployment_maximum_percent         = 200
  enable_ecs_managed_tags            = true
  propagate_tags                     = "SERVICE"
  health_check_grace_period_seconds  = 180

  capacity_provider_strategy {
    capacity_provider = aws_ecs_capacity_provider.spot.name
    weight            = 1
    base              = 0
  }

  deployment_circuit_breaker {
    enable   = true
    rollback = true
  }

  load_balancer {
    target_group_arn = aws_lb_target_group.app.arn
    container_name   = "backend"
    container_port   = var.container_port
  }

  service_registries {
    registry_arn   = aws_service_discovery_service.backend_metrics.arn
    container_name = "backend"
    container_port = 9091
  }

  ordered_placement_strategy {
    type  = "spread"
    field = "attribute:ecs.availability-zone"
  }

  placement_constraints {
    type = "distinctInstance"
  }

  depends_on = [
    aws_lb_listener_rule.cloudfront,
    aws_ecs_cluster_capacity_providers.this,
    aws_iam_role_policy_attachment.task_execution,
    aws_iam_role_policy.task_execution_secrets,
  ]

  tags = merge(var.tags, { Name = "${var.name}-backend" })
}

resource "aws_ecs_task_definition" "monitoring" {
  family                   = "${var.name}-monitoring"
  requires_compatibilities = ["EC2"]
  network_mode             = "bridge"
  cpu                      = "512"
  memory                   = "768"
  execution_role_arn       = aws_iam_role.monitoring_execution.arn

  volume {
    name = "prometheus-data"
  }

  container_definitions = jsonencode([
    {
      name      = "prometheus"
      image     = "${aws_ecr_repository.prometheus.repository_url}:${var.image_tag}"
      essential = true
      cpu       = 256
      memory    = 512
      portMappings = [{
        containerPort = 9090
        hostPort      = 0
        protocol      = "tcp"
      }]
      mountPoints = [{
        sourceVolume  = "prometheus-data"
        containerPath = "/prometheus"
        readOnly      = false
      }]
      logConfiguration = {
        logDriver = "awslogs"
        options = {
          awslogs-group         = aws_cloudwatch_log_group.monitoring.name
          awslogs-region        = data.aws_region.current.name
          awslogs-stream-prefix = "prometheus"
        }
      }
    },
    {
      name      = "grafana"
      image     = "${aws_ecr_repository.grafana.repository_url}:${var.image_tag}"
      essential = true
      cpu       = 256
      memory    = 256
      portMappings = [{
        containerPort = 3000
        hostPort      = 0
        protocol      = "tcp"
      }]
      links = ["prometheus"]
      dependsOn = [{
        containerName = "prometheus"
        condition     = "START"
      }]
      environment = [
        { name = "GF_AUTH_ANONYMOUS_ENABLED", value = "false" },
        { name = "GF_USERS_ALLOW_SIGN_UP", value = "false" },
        { name = "GF_SERVER_ROOT_URL", value = "${trimsuffix(var.app_base_url, "/")}/grafana/" },
        { name = "GF_SERVER_SERVE_FROM_SUB_PATH", value = "true" }
      ]
      secrets = [{
        name      = "GF_SECURITY_ADMIN_PASSWORD"
        valueFrom = aws_secretsmanager_secret.grafana_admin.arn
      }]
      logConfiguration = {
        logDriver = "awslogs"
        options = {
          awslogs-group         = aws_cloudwatch_log_group.monitoring.name
          awslogs-region        = data.aws_region.current.name
          awslogs-stream-prefix = "grafana"
        }
      }
    }
  ])

  tags = merge(var.tags, {
    Name    = "${var.name}-monitoring"
    Service = "monitoring"
  })
}

resource "aws_ecs_service" "monitoring" {
  name                               = "${var.name}-monitoring"
  cluster                            = aws_ecs_cluster.this.id
  task_definition                    = aws_ecs_task_definition.monitoring.arn
  desired_count                      = 1
  deployment_minimum_healthy_percent = 0
  deployment_maximum_percent         = 100
  enable_ecs_managed_tags            = true
  propagate_tags                     = "SERVICE"
  health_check_grace_period_seconds  = 180

  capacity_provider_strategy {
    capacity_provider = aws_ecs_capacity_provider.spot.name
    weight            = 1
    base              = 0
  }

  deployment_circuit_breaker {
    enable   = true
    rollback = true
  }

  load_balancer {
    target_group_arn = aws_lb_target_group.grafana.arn
    container_name   = "grafana"
    container_port   = 3000
  }

  ordered_placement_strategy {
    type  = "spread"
    field = "attribute:ecs.availability-zone"
  }

  depends_on = [
    aws_lb_listener_rule.grafana,
    aws_ecs_cluster_capacity_providers.this,
    aws_iam_role_policy_attachment.monitoring_execution,
    aws_iam_role_policy.monitoring_execution_secret,
    aws_secretsmanager_secret_version.grafana_admin,
  ]

  tags = merge(var.tags, {
    Name    = "${var.name}-monitoring"
    Service = "monitoring"
  })
}

resource "aws_appautoscaling_target" "service" {
  max_capacity       = var.service_max_count
  min_capacity       = var.service_desired_count
  resource_id        = "service/${aws_ecs_cluster.this.name}/${aws_ecs_service.app.name}"
  scalable_dimension = "ecs:service:DesiredCount"
  service_namespace  = "ecs"
}

resource "aws_appautoscaling_policy" "cpu" {
  name               = "${var.name}-backend-cpu"
  policy_type        = "TargetTrackingScaling"
  resource_id        = aws_appautoscaling_target.service.resource_id
  scalable_dimension = aws_appautoscaling_target.service.scalable_dimension
  service_namespace  = aws_appautoscaling_target.service.service_namespace

  target_tracking_scaling_policy_configuration {
    target_value       = 60
    scale_in_cooldown  = 300
    scale_out_cooldown = 60

    predefined_metric_specification {
      predefined_metric_type = "ECSServiceAverageCPUUtilization"
    }
  }
}
