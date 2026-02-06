# Archive the Lambda source code
data "archive_file" "lambda" {
  type        = "zip"
  source_dir  = var.source_path
  output_path = "${path.module}/.tmp/${var.name}.zip"
}

# IAM Role for Lambda
resource "aws_iam_role" "lambda" {
  name = "${var.name}-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action = "sts:AssumeRole"
        Effect = "Allow"
        Principal = {
          Service = "lambda.amazonaws.com"
        }
      }
    ]
  })

  tags = merge(var.tags, {
    Name = "${var.name}-role"
  })
}

# Basic Lambda execution policy (CloudWatch Logs)
resource "aws_iam_role_policy_attachment" "lambda_basic" {
  role       = aws_iam_role.lambda.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole"
}

# VPC access policy (if VPC is configured)
resource "aws_iam_role_policy_attachment" "lambda_vpc" {
  count      = var.vpc_config != null ? 1 : 0
  role       = aws_iam_role.lambda.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AWSLambdaVPCAccessExecutionRole"
}

# SQS access policy
resource "aws_iam_role_policy" "sqs" {
  #count = var.sqs_arn != null ? 1 : 0
  count = var.enable_sqs != null ? 1 : 0
  name  = "${var.name}-sqs-policy"
  role  = aws_iam_role.lambda.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "sqs:ReceiveMessage",
          "sqs:DeleteMessage",
          "sqs:GetQueueAttributes"
        ]
        Resource = var.sqs_arn
      }
    ]
  })
}

# SSM Parameter Store access policy
resource "aws_iam_role_policy" "ssm" {
  count = length(var.ssm_parameter_arns) > 0 ? 1 : 0
  name  = "${var.name}-ssm-policy"
  role  = aws_iam_role.lambda.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "ssm:GetParameter",
          "ssm:GetParameters"
        ]
        Resource = var.ssm_parameter_arns
      }
    ]
  })
}

# Secrets Manager access policy
resource "aws_iam_role_policy" "secrets" {
  count = length(var.secrets_manager_arns) > 0 ? 1 : 0
  name  = "${var.name}-secrets-policy"
  role  = aws_iam_role.lambda.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "secretsmanager:GetSecretValue"
        ]
        Resource = var.secrets_manager_arns
      }
    ]
  })
}

# Lambda function
resource "aws_lambda_function" "this" {
  filename         = data.archive_file.lambda.output_path
  function_name    = var.name
  role             = aws_iam_role.lambda.arn
  handler          = var.handler
  source_code_hash = data.archive_file.lambda.output_base64sha256
  runtime          = var.runtime
  timeout          = var.timeout
  memory_size      = var.memory_size

  dynamic "vpc_config" {
    for_each = var.vpc_config != null ? [var.vpc_config] : []
    content {
      subnet_ids         = vpc_config.value.subnet_ids
      security_group_ids = vpc_config.value.security_group_ids
    }
  }

  dynamic "environment" {
    for_each = length(var.environment_variables) > 0 ? [1] : []
    content {
      variables = var.environment_variables
    }
  }

  tags = merge(var.tags, {
    Name = var.name
  })

  depends_on = [
    aws_iam_role_policy_attachment.lambda_basic,
    aws_iam_role_policy_attachment.lambda_vpc,
  ]
}

# SQS Event Source Mapping
resource "aws_lambda_event_source_mapping" "sqs" {
  #count            = var.sqs_arn != null ? 1 : 0
  count            = var.enable_sqs != null ? 1 : 0
  event_source_arn = var.sqs_arn
  function_name    = aws_lambda_function.this.arn
  batch_size       = var.sqs_batch_size
  enabled          = true
}

# Security Group for Lambda (if VPC is configured)
resource "aws_security_group" "lambda" {
  count       = var.vpc_config != null ? 1 : 0
  name        = "${var.name}-sg"
  description = "Security group for Lambda function ${var.name}"
  vpc_id      = data.aws_subnet.first[0].vpc_id

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = merge(var.tags, {
    Name = "${var.name}-sg"
  })
}

# Get VPC ID from subnet (for security group creation)
data "aws_subnet" "first" {
  count = var.vpc_config != null ? 1 : 0
  id    = var.vpc_config.subnet_ids[0]
}
