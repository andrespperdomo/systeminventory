resource "aws_elasticache_subnet_group" "this" {
  name       = "${var.name}-subnet-group"
  subnet_ids = var.subnet_ids

  tags = var.tags
}

resource "aws_elasticache_user" "inventory" {
  user_id   = "${var.name}-inventory"
  user_name = "inventory"

  engine = "redis"

  access_string = "on ~* +@all"

  passwords = [
    var.redis_password
  ]
}

resource "aws_elasticache_user_group" "this" {
  engine = "redis"

  user_group_id = "${var.name}-users"

  user_ids = [
    "default",
    aws_elasticache_user.inventory.user_id
  ]
}

resource "aws_elasticache_replication_group" "this" {
  replication_group_id = var.name

  description = "Redis cache for ${var.name}"

  engine = "redis"

  node_type = var.node_type

  parameter_group_name = aws_elasticache_parameter_group.this.name

  port = 6379

  num_cache_clusters = 1

  subnet_group_name = aws_elasticache_subnet_group.this.name

  security_group_ids = var.security_group_ids

  transit_encryption_enabled = true
  at_rest_encryption_enabled = true

  user_group_ids = [
    aws_elasticache_user_group.this.id
  ]

  automatic_failover_enabled = false
  multi_az_enabled           = false

  tags = var.tags
}

resource "aws_elasticache_parameter_group" "this" {
  name   = "${var.name}-redis"
  family = "redis7"

  parameter {
    name  = "notify-keyspace-events"
    value = "Ex"
  }
}