output "alb_security_group_id" {
  value = aws_security_group.alb.id
}

output "ecs_security_group_id" {
  value = aws_security_group.ecs.id
}

output "rds_security_group_id" {
  value = aws_security_group.rds.id
}
output "redis_security_group_id" {
  description = "Security group ID for ElastiCache"
  value       = aws_security_group.redis.id
}
output "rabbitmq_security_group_id" {
  description = "Security group ID for RabbitMQ"
  value       = aws_security_group.rabbitmq.id
}
