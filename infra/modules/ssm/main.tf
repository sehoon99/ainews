resource "aws_ssm_parameter" "this" {
  name   = var.name
  type   = var.type
  value  = var.value
  key_id = var.key_id

  tags = merge(var.tags, {
    Name = var.name
  })
}
