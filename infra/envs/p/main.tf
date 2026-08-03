data "aws_availability_zones" "available" {
  state = "available"
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
}

module "storage_web" {
  source = "../../modules/storage"

  name              = "${var.project_name}-web"
  bucket_name       = "${var.project_name}-${var.environment}-web"
  enable_versioning = true
}

module "storage_deploy" {
  source = "../../modules/storage"

  name              = "${var.project_name}-deploy"
  bucket_name       = "${var.project_name}-${var.environment}-deploy"
  enable_versioning = true
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

# Lambda function for news crawling
module "lambda_news_crawler" {
  source = "../../modules/lambda"

  name        = "${var.project_name}-${var.environment}-news-crawler"
  source_path = "${path.module}/../../../lambda/news-crawler"
  handler     = "handler.lambda_handler"
  runtime     = "python3.11"
  timeout     = 900  # 15분 최대
  memory_size = 512  # newspaper3k/lxml 파싱 메모리

  enable_sqs     = false

  environment_variables = {
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
    Project     = var.project_name
    Environment = var.environment
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
