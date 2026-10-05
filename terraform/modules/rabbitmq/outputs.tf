
output "broker_id" {
  value = aws_mq_broker.rabbitmq.id
}
output "secret_arn" {
  description = "ARN of the RabbitMQ credentials secret"
  value       = aws_secretsmanager_secret.rabbitmq.arn
  sensitive   = true
}
output "host" {
  description = "RabbitMQ broker hostname"
  value = replace(
    replace(
      aws_mq_broker.rabbitmq.instances[0].endpoints[0],
      "amqps://",
      ""
    ),
    ":5671",
    ""
  )
}

output "port" {
  description = "RabbitMQ broker TLS port"
  value       = 5671
}
