resource "aws_mq_broker" "rabbitmq" {
  broker_name = var.name

  engine_type                = "RabbitMQ"
  engine_version             = "3.13"
  auto_minor_version_upgrade = true
  host_instance_type         = var.instance_type

  publicly_accessible = false

  deployment_mode = "SINGLE_INSTANCE"

  subnet_ids = [
    var.subnet_ids[0]
  ]

  security_groups = var.security_group_ids

  user {
    username = var.username
    password = var.password
  }

  logs {
    general = true
  }

  tags = {
    Name = var.name
  }
}

resource "aws_secretsmanager_secret" "rabbitmq" {
  name = "${var.project_name}/${var.environment}/rabbitmq"

  tags = merge(
    var.tags,
    {
      Service = "rabbitmq"
    }
  )
}

resource "aws_secretsmanager_secret_version" "rabbitmq" {
  secret_id = aws_secretsmanager_secret.rabbitmq.id

  secret_string = jsonencode({
    username = var.username
    password = var.password
  })
}
