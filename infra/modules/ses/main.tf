resource "aws_ses_email_identity" "sender" {
  email = var.sender_email
}

resource "aws_iam_policy" "ses_send" {
  name = "${var.name}-ses-send"

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "ses:SendEmail",
          "ses:SendRawEmail"
        ]
        Resource = "*"
      }
    ]
  })

  tags = var.tags
}