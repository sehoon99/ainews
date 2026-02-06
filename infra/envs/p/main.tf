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
  enable_nat         = true
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

module "rds" {
  source = "../../modules/rds"

  name       = "${var.project_name}-${var.environment}"
  vpc_id     = module.vpc.vpc_id
  subnet_ids = module.vpc.private_subnet_ids

  instance_class    = "db.t3.micro"
  allocated_storage = 20
  db_name           = "ainews"
  db_username       = "admin"

  allowed_cidr_blocks = ["10.0.0.0/16"]

  multi_az            = false
  skip_final_snapshot = true
}

# SQS queue for image analysis
module "sqs_image_analysis" {
  source = "../../modules/sqs"

  name                       = "${var.project_name}-${var.environment}-image-analysis"
  visibility_timeout_seconds = 300 # Lambda timeout과 일치
  message_retention_seconds  = 1209600 # 14 days
  max_receive_count          = 3

  tags = {
    Project     = var.project_name
    Environment = var.environment
  }
}

# Lambda function for image analysis
module "lambda_image_analyzer" {
  source = "../../modules/lambda"

  name        = "${var.project_name}-${var.environment}-image-analyzer"
  source_path = "${path.module}/../../../lambda/image-analyzer"
  handler     = "handler.lambda_handler"
  runtime     = "python3.11"
  timeout     = 300
  memory_size = 256

  sqs_arn        = module.sqs_image_analysis.queue_arn
  enable_sqs = true
  sqs_batch_size = 1 # 이미지 분석은 하나씩 처리

  vpc_config = {
    subnet_ids         = module.vpc.private_subnet_ids
    security_group_ids = [module.rds.security_group_id]
  }

  environment_variables = {
    DB_HOST                        = module.rds.address
    DB_PORT                        = tostring(module.rds.port)
    DB_NAME                        = module.rds.db_name
    DB_SECRET_ARN                  = module.rds.secret_arn
    SSM_SIGHTENGINE_API_USER       = module.sightengine_api_user.name
    SSM_SIGHTENGINE_API_SECRET     = module.sightengine_api_secret.name
  }

  ssm_parameter_arns = [
    module.sightengine_api_user.arn,
    module.sightengine_api_secret.arn,
  ]

  secrets_manager_arns = [
    module.rds.secret_arn,
  ]

  tags = {
    Project     = var.project_name
    Environment = var.environment
  }
}
