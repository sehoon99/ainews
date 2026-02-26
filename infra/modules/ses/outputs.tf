output "ses_identity_arn" {
  description = "SES email identity ARN"
  value       = aws_ses_email_identity.sender.arn
}

output "ses_send_policy_arn" {
  description = "IAM policy ARN for SES send permissions"
  value       = aws_iam_policy.ses_send.arn
}