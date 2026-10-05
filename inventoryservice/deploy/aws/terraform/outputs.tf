output "ecr_repository_uri" {
  description = "URI of the ECR repository where the container image is stored"
  value       = aws_ecr_repository.app.repository_url
}

output "ecs_cluster_name" {
  description = "Name of the ECS cluster"
  value       = aws_ecs_cluster.app.name
}

output "alb_dns_name" {
  description = "Public DNS name of the Application Load Balancer"
  value       = aws_lb.app.dns_name
}

output "rds_endpoint" {
  description = "PostgreSQL endpoint address"
  value       = aws_db_instance.inventory.endpoint
}

output "redis_primary_endpoint" {
  description = "Primary endpoint address for ElastiCache Redis"
  value       = aws_elasticache_replication_group.redis.primary_endpoint_address
}

output "mq_broker_url" {
  description = "RabbitMQ broker endpoint"
  value       = aws_mq_broker.rabbitmq.broker_instances[0].endpoints[0]
}
