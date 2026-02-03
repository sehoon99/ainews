# S3 Bucket
resource "aws_s3_bucket" "this" {
  bucket = var.bucket_name

  tags = {
    Name = var.bucket_name
  }
}

resource "aws_s3_bucket_versioning" "this" {
  bucket = aws_s3_bucket.this.id
  versioning_configuration {
    status = var.enable_versioning ? "Enabled" : "Disabled"
  }
}

resource "aws_s3_bucket_public_access_block" "this" {
  bucket = aws_s3_bucket.this.id

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

# DB Subnet Group (for RDS)
resource "aws_db_subnet_group" "this" {
  count      = var.create_db_subnet_group ? 1 : 0
  name       = "${var.name}-db-subnet"
  subnet_ids = var.private_subnet_ids

  tags = {
    Name = "${var.name}-db-subnet"
  }
}

# DB Security Group
resource "aws_security_group" "db" {
  count       = var.create_db_subnet_group ? 1 : 0
  name        = "${var.name}-db-sg"
  description = "Security group for database"
  vpc_id      = var.vpc_id

  ingress {
    description     = "MySQL from Application"
    from_port       = 3306
    to_port         = 3306
    protocol        = "tcp"
    security_groups = var.app_security_group_ids
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "${var.name}-db-sg"
  }
}

resource "aws_security_group" "redis" {
  count       = var.create_redis ? 1 : 0 # Redis 생성 여부 변수(선택)
  name        = "${var.name}-redis-sg"
  vpc_id      = var.vpc_id

  ingress {
    description     = "Redis from Application"
    from_port       = 6379
    to_port         = 6379
    protocol        = "tcp"
    security_groups = var.app_security_group_ids
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = { Name = "${var.name}-redis-sg" }
}