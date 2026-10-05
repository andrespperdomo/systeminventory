output "cluster_id" {
  description = "ECS cluster ID"
  value       = aws_ecs_cluster.this.id
}

output "cluster_name" {
  description = "ECS cluster name"
  value       = aws_ecs_cluster.this.name
}

output "execution_role_arn" {
  description = "ECS execution role ARN"
  value       = aws_iam_role.execution.arn
}

output "task_role_arn" {
  description = "ECS task role ARN"
  value       = aws_iam_role.task.arn
}

output "task_definition_arns" {
  description = "Task definition ARN per service"
  value = {
    for service, task in aws_ecs_task_definition.service :
    service => task.arn
  }
}

output "service_names" {
  description = "ECS service name per microservice"
  value = {
    for service, ecs_service in aws_ecs_service.service :
    service => ecs_service.name
  }
}
