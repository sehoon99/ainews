data "sops_file" "secrets" {
  source_file = "../../secrets/prod.yaml"
}

locals {
  secrets = data.sops_file.secrets.data
}

module "sightengine_api_user" {
  source = "../../modules/ssm"

  name  = "/ainews/prod/sightengine_api_user"
  value = local.secrets["sightengine_api_user"]

  tags = {
    Environment = var.environment
    Team        = var.team
    Service     = "image-analysis"
    ManagedBy   = "terraform"
  }
}

module "sightengine_api_secret" {
  source = "../../modules/ssm"

  name  = "/ainews/prod/sightengine_api_secret"
  value = local.secrets["sightengine_api_secret"]

  tags = {
    Environment = var.environment
    Team        = var.team
    Service     = "image-analysis"
    ManagedBy   = "terraform"
  }
}
