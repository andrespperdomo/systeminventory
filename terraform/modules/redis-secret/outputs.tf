output "secret_arn" {
  value = aws_secretsmanager_secret.redis.arn
}

output "secret_name" {
  value = aws_secretsmanager_secret.redis.name
}