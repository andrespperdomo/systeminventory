resource "aws_secretsmanager_secret" "redis" {
  name        = var.name
  description = "Redis credentials for ${var.project_name}-${var.environment}"

  recovery_window_in_days = 7

  tags = merge(
    var.tags,
    {
      Name        = var.name
      Project     = var.project_name
      Environment = var.environment
      ManagedBy   = "terraform"
    }
  )
}

resource "aws_secretsmanager_secret_version" "redis" {
  secret_id = aws_secretsmanager_secret.redis.id

  secret_string = jsonencode({
    username = var.redis_username
    password = var.redis_password
  })
}