resource "aws_ecs_service" "service" {

  for_each = var.services

  name = "${var.project_name}-${var.environment}-${each.key}"

  cluster = aws_ecs_cluster.this.id

  task_definition = aws_ecs_task_definition.service[each.key].arn

  desired_count = each.value.desired_count

  launch_type = "FARGATE"

  enable_execute_command = true

  deployment_minimum_healthy_percent = 50

  deployment_maximum_percent = 200

  network_configuration {

    subnets = var.private_subnet_ids

    security_groups = [
      var.security_group_id
    ]

    assign_public_ip = false
  }

  load_balancer {

    target_group_arn = var.target_group_arns[each.key]

    container_name = each.key

    container_port = each.value.container_port
  }

  depends_on = [
    aws_iam_role_policy.execution_secrets
  ]

  tags = merge(
    var.tags,
    {
      Service = each.key
    }
  )
}