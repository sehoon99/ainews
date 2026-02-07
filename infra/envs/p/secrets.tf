# SOPS 파일 읽기
data "sops_file" "secrets" {
  source_file = "../../secrets/prod.yaml"
}

locals {
  secrets = data.sops_file.secrets.data
}

# 예시: GitHub Token을 SSM에 저장
module "github_token" {
  source = "../../modules/ssm"

  name  = "/ainews/prod/github_token"
  value = local.secrets["github_token"]
}

# 예시: DB Password를 SSM에 저장
module "db_password" {
  source = "../../modules/ssm"

  name  = "/ainews/prod/db_password"
  value = local.secrets["db_password"]
}

# Sightengine API credentials for image analysis
module "sightengine_api_user" {
  source = "../../modules/ssm"

  name  = "/ainews/prod/sightengine_api_user"
  value = local.secrets["sightengine_api_user"]
}

module "sightengine_api_secret" {
  source = "../../modules/ssm"

  name  = "/ainews/prod/sightengine_api_secret"
  value = local.secrets["sightengine_api_secret"]
}
