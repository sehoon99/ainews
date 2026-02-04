# SOPS용 KMS 키
resource "aws_kms_key" "sops" {
  description             = "KMS key for SOPS encryption"
  deletion_window_in_days = 7
  enable_key_rotation     = true

  tags = {
    Name    = "ainews-sops"
    Purpose = "sops"
  }
}

resource "aws_kms_alias" "sops" {
  name          = "alias/ainews-sops"
  target_key_id = aws_kms_key.sops.key_id
}

output "kms_sops_arn" {
  description = "KMS key ARN for SOPS"
  value       = aws_kms_key.sops.arn
}
