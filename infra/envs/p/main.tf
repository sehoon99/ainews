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

# 직접 리소스 추가는 여기 아래에 또는 별도 파일(ec2.tf 등)로
