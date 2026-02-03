terraform {
  required_version = ">= 1.0"

  backend "s3" {
    bucket         = "ainews-apnortheast2-tfstate"
    key            = "envs/prod/terraform.tfstate"
    region         = "ap-northeast-2"
    encrypt        = true
    dynamodb_table = "terraform-lock"
  }

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
}

provider "aws" {
  region = var.region

  default_tags {
    tags = {
      Project     = "ainews"
      Environment = "prod"
      ManagedBy   = "terraform"
    }
  }
}
