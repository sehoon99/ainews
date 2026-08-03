data "aws_availability_zones" "available" {
  state = "available"
}

data "aws_ec2_managed_prefix_list" "cloudfront_origin_facing" {
  name = "com.amazonaws.global.cloudfront.origin-facing"
}

resource "random_password" "cloudfront_origin_verify" {
  length  = 32
  special = false
}

module "vpc" {
  source = "../../modules/vpc"

  name            = "${var.project_name}-${var.environment}"
  cidr            = "10.0.0.0/16"
  azs             = slice(data.aws_availability_zones.available.names, 0, 2)
  public_subnets  = ["10.0.1.0/24", "10.0.2.0/24"]
  private_subnets = ["10.0.11.0/24", "10.0.12.0/24"]
}

module "network" {
  source = "../../modules/network"

  name               = "${var.project_name}-${var.environment}"
  vpc_id             = module.vpc.vpc_id
  igw_id             = module.vpc.igw_id
  public_subnet_id   = module.vpc.public_subnet_ids[0]
  public_subnet_ids  = module.vpc.public_subnet_ids
  private_subnet_ids = module.vpc.private_subnet_ids
  enable_nat         = false
}

module "storage_image" {
  source = "../../modules/storage"

  name              = "${var.project_name}-image"
  bucket_name       = "${var.project_name}-${var.environment}-image"
  enable_versioning = true

  tags = {
    Environment = var.environment
    Team        = var.team
    Service     = "image-analysis"
    ManagedBy   = "terraform"
  }
}

module "storage_web" {
  source = "../../modules/storage"

  name              = "${var.project_name}-web"
  bucket_name       = "${var.project_name}-${var.environment}-web"
  enable_versioning = true

  tags = {
    Environment = var.environment
    Team        = var.team
    Service     = "frontend"
    ManagedBy   = "terraform"
  }
}

module "storage_deploy" {
  source = "../../modules/storage"

  name              = "${var.project_name}-deploy"
  bucket_name       = "${var.project_name}-${var.environment}-deploy"
  enable_versioning = true

  tags = {
    Environment = var.environment
    Team        = var.team
    Service     = "cicd"
    ManagedBy   = "terraform"
  }
}

module "ses" {
  source = "../../modules/ses"

  name         = "${var.project_name}-${var.environment}"
  sender_email = var.ses_sender_email

  tags = {
    Project     = var.project_name
    Environment = var.environment
  }
}

module "compute_server" {
  source = "../../modules/compute"

  name                 = "${var.project_name}-${var.environment}"
  vpc_id               = module.vpc.vpc_id
  subnet_id            = module.vpc.public_subnet_ids[0]
  deploy_bucket        = module.storage_deploy.bucket_id
  secrets_manager_arns = [module.rds.secret_arn]
  db_secret_name       = "${var.project_name}-${var.environment}-db-credentials"
  ses_send_policy_arn  = module.ses.ses_send_policy_arn
  ses_sender_email     = var.ses_sender_email
  app_base_url         = var.app_base_url

  tags = {
    Project     = var.project_name
    Environment = var.environment
  }
}

module "rds" {
  source = "../../modules/rds"

  name       = "${var.project_name}-${var.environment}"
  vpc_id     = module.vpc.vpc_id
  subnet_ids = module.vpc.public_subnet_ids

  instance_class    = "db.t3.micro"
  allocated_storage = 20
  db_name           = "ainews"
  db_username       = "admin"

  # Lambda가 VPC 밖에서 실행되므로 0.0.0.0/0 필요 (RDS publicly_accessible = true)
  allowed_cidr_blocks = ["10.0.0.0/16", "0.0.0.0/0"]

  multi_az            = false
  skip_final_snapshot = true
}

# Restored ECS-on-EC2 capacity. The legacy EC2 remains during the first cutover.
module "ecs_backend" {
  source = "../../modules/ecs-ec2"

  name                       = "${var.project_name}-${var.environment}"
  vpc_id                     = module.vpc.vpc_id
  public_subnet_ids          = module.vpc.public_subnet_ids
  cloudfront_prefix_list_id  = data.aws_ec2_managed_prefix_list.cloudfront_origin_facing.id
  image_tag                  = var.backend_image_tag
  origin_verify_header_value = random_password.cloudfront_origin_verify.result

  instance_types       = ["t3.small", "t3a.small"]
  asg_min_size         = 1
  asg_desired_capacity = 2
  asg_max_size         = 4

  service_desired_count = 2
  service_max_count     = 4

  db_host             = module.rds.address
  db_port             = module.rds.port
  db_name             = module.rds.db_name
  db_secret_arn       = module.rds.secret_arn
  ses_send_policy_arn = module.ses.ses_send_policy_arn
  ses_sender_email    = var.ses_sender_email
  app_base_url        = var.app_base_url

  tags = {
    Environment = var.environment
    Team        = var.team
    Service     = "backend"
    ManagedBy   = "terraform"
  }
}

module "cloudfront" {
  source = "../../modules/cloudfront"

  name                            = "${var.project_name}-${var.environment}"
  web_bucket_id                   = module.storage_web.bucket_id
  web_bucket_arn                  = module.storage_web.bucket_arn
  web_bucket_regional_domain_name = module.storage_web.bucket_regional_domain_name
  api_origin_domain_name          = module.ecs_backend.load_balancer_dns_name
  origin_verify_header_value      = random_password.cloudfront_origin_verify.result

  tags = {
    Environment = var.environment
    Team        = var.team
    Service     = "frontend"
    ManagedBy   = "terraform"
  }
}

module "sqs_image_analysis" {
  source = "../../modules/sqs"

  name                       = "${var.project_name}-${var.environment}-image-analysis"
  visibility_timeout_seconds = 1800
  message_retention_seconds  = 1209600
  max_receive_count          = 3

  tags = {
    Environment = var.environment
    Team        = var.team
    Service     = "image-analysis"
    ManagedBy   = "terraform"
  }
}

module "lambda_image_analyzer" {
  source = "../../modules/lambda"

  name        = "${var.project_name}-${var.environment}-image-analyzer"
  source_path = "${path.module}/../../../lambda/image-analyzer"
  handler     = "handler.lambda_handler"
  runtime     = "python3.11"
  timeout     = 300
  memory_size = 256

  sqs_arn        = module.sqs_image_analysis.queue_arn
  enable_sqs     = true
  sqs_batch_size = 1

  environment_variables = {
    DB_HOST                    = module.rds.address
    DB_PORT                    = tostring(module.rds.port)
    DB_NAME                    = module.rds.db_name
    DB_SECRET_ARN              = module.rds.secret_arn
    SSM_SIGHTENGINE_API_USER   = module.sightengine_api_user.name
    SSM_SIGHTENGINE_API_SECRET = module.sightengine_api_secret.name
  }

  ssm_parameter_arns = [
    module.sightengine_api_user.arn,
    module.sightengine_api_secret.arn,
  ]

  secrets_manager_arns = [module.rds.secret_arn]

  tags = {
    Environment = var.environment
    Team        = var.team
    Service     = "image-analysis"
    ManagedBy   = "terraform"
  }
}

# Lambda function for news crawling
module "lambda_news_crawler" {
  source = "../../modules/lambda"

  name        = "${var.project_name}-${var.environment}-news-crawler"
  source_path = "${path.module}/../../../lambda/news-crawler"
  handler     = "handler.lambda_handler"
  runtime     = "python3.11"
  timeout     = 900 # 15분 최대
  memory_size = 512 # newspaper3k/lxml 파싱 메모리

  enable_sqs    = false
  sqs_send_arns = [module.sqs_image_analysis.queue_arn]

  environment_variables = {
    SQS_QUEUE_URL     = module.sqs_image_analysis.queue_url
    DB_SECRET_ARN     = module.rds.secret_arn
    DB_HOST           = module.rds.address
    DB_PORT           = tostring(module.rds.port)
    DB_NAME           = module.rds.db_name
    COUNT_PER_SECTION = "15"
  }

  secrets_manager_arns = [
    module.rds.secret_arn,
  ]

  tags = {
    Environment = var.environment
    Team        = var.team
    Service     = "news-crawler"
    ManagedBy   = "terraform"
  }
}

# EventBridge schedule for news crawler (every 4 hours!!)
module "eventbridge_news_crawler" {
  source = "../../modules/eventbridge"

  name                 = "${var.project_name}-${var.environment}-news-crawler-schedule"
  description          = "Trigger news crawler Lambda every 4 hours"
  schedule_expression  = "rate(4 hours)"
  lambda_arn           = module.lambda_news_crawler.function_arn
  lambda_function_name = module.lambda_news_crawler.function_name

  tags = {
    Project     = var.project_name
    Environment = var.environment
  }
}

# Bastion Host for Session Manager (RDS 접속용)
# TODO: RDS 프라이빗 전환 시 주석 해제
# module "bastion" {
#   source = "../../modules/bastion"
#
#   name      = "${var.project_name}-${var.environment}"
#   vpc_id    = module.vpc.vpc_id
#   subnet_id = module.vpc.private_subnet_ids[0]
#
#   instance_type = "t3.micro"
#
#   tags = {
#     Project     = var.project_name
#     Environment = var.environment
#   }
# }
