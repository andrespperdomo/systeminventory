resource "aws_security_group" "alb" {

  name = "${var.project_name}-${var.environment}-alb-sg"

  description = "Security group for Application Load Balancer"

  vpc_id = var.vpc_id
}


resource "aws_security_group" "ecs" {

  name = "${var.project_name}-${var.environment}-ecs-sg"

  description = "Security group for ECS Fargate"

  vpc_id = var.vpc_id
}


resource "aws_security_group" "rds" {

  name = "${var.project_name}-${var.environment}-rds-sg"

  description = "Security group for PostgreSQL"

  vpc_id = var.vpc_id
}


# ALB accepts HTTP traffic.
resource "aws_vpc_security_group_ingress_rule" "alb_http" {

  security_group_id = aws_security_group.alb.id

  cidr_ipv4 = "0.0.0.0/0"

  ip_protocol = "tcp"

  from_port = 80
  to_port   = 80
}


# ECS accepts traffic ONLY from the ALB.
resource "aws_vpc_security_group_ingress_rule" "ecs_from_alb" {

  security_group_id = aws_security_group.ecs.id

  referenced_security_group_id = aws_security_group.alb.id

  ip_protocol = "tcp"

  from_port = 8080
  to_port   = 8080
}


# RDS accepts PostgreSQL ONLY from ECS.
resource "aws_vpc_security_group_ingress_rule" "rds_from_ecs" {

  security_group_id = aws_security_group.rds.id

  referenced_security_group_id = aws_security_group.ecs.id

  ip_protocol = "tcp"

  from_port = 5432
  to_port   = 5432
}


# Egress is intentionally open.
# ECS needs internet access through NAT for external endpoints.
resource "aws_vpc_security_group_egress_rule" "alb_all" {

  security_group_id = aws_security_group.alb.id

  cidr_ipv4 = "0.0.0.0/0"

  ip_protocol = "-1"
}


resource "aws_vpc_security_group_egress_rule" "ecs_all" {

  security_group_id = aws_security_group.ecs.id

  cidr_ipv4 = "0.0.0.0/0"

  ip_protocol = "-1"
}


resource "aws_vpc_security_group_egress_rule" "rds_all" {

  security_group_id = aws_security_group.rds.id

  cidr_ipv4 = "0.0.0.0/0"

  ip_protocol = "-1"
}
resource "aws_security_group" "redis" {
  name        = "${var.project_name}-${var.environment}-redis"
  description = "Security group for ElastiCache"
  vpc_id      = var.vpc_id

  tags = merge(
    var.tags,
    {
      Name = "${var.project_name}-${var.environment}-redis"
    }
  )
}
resource "aws_vpc_security_group_ingress_rule" "redis_from_ecs" {
  security_group_id            = aws_security_group.redis.id
  referenced_security_group_id = aws_security_group.ecs.id

  from_port   = 6379
  to_port     = 6379
  ip_protocol = "tcp"
}
resource "aws_security_group" "rabbitmq" {
  name        = "${var.project_name}-${var.environment}-rabbitmq"
  description = "Security group for RabbitMQ"
  vpc_id      = var.vpc_id

  tags = {
    Name = "${var.project_name}-${var.environment}-rabbitmq"
  }
}
resource "aws_vpc_security_group_ingress_rule" "rabbitmq_from_ecs" {
  security_group_id            = aws_security_group.rabbitmq.id
  referenced_security_group_id = aws_security_group.ecs.id

  from_port   = 5671
  to_port     = 5671
  ip_protocol = "tcp"
}
resource "aws_security_group_rule" "rds_from_bastion" {
  type                     = "ingress"
  security_group_id        = aws_security_group.rds.id
  source_security_group_id = var.bastion_security_group_id

  from_port = 5432
  to_port   = 5432
  protocol  = "tcp"
}
