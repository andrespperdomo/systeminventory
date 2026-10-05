
output "reader_endpoint_address" {
  description = "Reader endpoint for Redis/Valkey"
  value       = aws_elasticache_replication_group.this.reader_endpoint_address
}

output "port" {
  description = "Redis/Valkey port"
  value       = aws_elasticache_replication_group.this.port
}

output "replication_group_id" {
  description = "Replication group ID"
  value       = aws_elasticache_replication_group.this.id
}
output "primary_endpoint_address" {
  description = "ElastiCache primary endpoint"
  value       = aws_elasticache_replication_group.this.primary_endpoint_address
}

output "user_group_id" {
  description = "ElastiCache user group ID"
  value       = aws_elasticache_user_group.this.id
}
