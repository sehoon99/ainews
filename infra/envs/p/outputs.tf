output "vpc_id" {
  value = module.vpc.vpc_id
}

output "public_subnet_ids" {
  value = module.vpc.public_subnet_ids
}

output "private_subnet_ids" {
  value = module.vpc.private_subnet_ids
}

output "image_bucket" {
  value = module.storage_image.bucket_id
}

output "web_bucket" {
  value = module.storage_web.bucket_id
}

output "rds_endpoint" {
  value = module.rds.endpoint
}

output "rds_secret_arn" {
  value     = module.rds.secret_arn
  sensitive = true
}
