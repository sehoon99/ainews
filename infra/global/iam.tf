# IAM Users
resource "aws_iam_user" "huny" {
  name = "sec_project_huny"
}

resource "aws_iam_user" "ghlee" {
  name = "sec_project_ghlee"
}

# Administrator Access Policy Attachments
resource "aws_iam_user_policy_attachment" "huny_admin" {
  user       = aws_iam_user.huny.name
  policy_arn = "arn:aws:iam::aws:policy/AdministratorAccess"
}

resource "aws_iam_user_policy_attachment" "ghlee_admin" {
  user       = aws_iam_user.ghlee.name
  policy_arn = "arn:aws:iam::aws:policy/AdministratorAccess"
}

