resource "aws_cloudwatch_log_group" "test" {

  name = "/ecs/${var.project_name}/${var.environment}/connectivity-test"

  retention_in_days = 1
}
resource "aws_ecs_task_definition" "test" {

  family = "${var.project_name}-${var.environment}-rds-connectivity-test"

  requires_compatibilities = [
    "FARGATE"
  ]

  network_mode = "awsvpc"

  cpu = 256

  memory = 512

  execution_role_arn = var.execution_role_arn

  container_definitions = jsonencode([{

    name = "rds-connectivity-test"

    image = "postgres:16-alpine"

    essential = true

    command = [

      "sh",
      "-c",

      "echo 'Testing ECS -> RDS connectivity'; pg_isready -h ${var.database_endpoint} -p ${var.database_port} -t 10"
    ]

    logConfiguration = {

      logDriver = "awslogs"

      options = {

        "awslogs-group" = aws_cloudwatch_log_group.test.name

        "awslogs-region" = "us-east-1"

        "awslogs-stream-prefix" = "connectivity"
      }
    }
  }])
}
resource "aws_ecs_service" "test" {

  name = "${var.project_name}-${var.environment}-rds-connectivity-test"

  cluster = "${var.project_name}-${var.environment}"

  task_definition = aws_ecs_task_definition.test.arn

  desired_count = 1

  launch_type = "FARGATE"

  network_configuration {

    subnets = var.subnet_ids

    security_groups = [
      var.security_group_id
    ]

    assign_public_ip = false
  }

  depends_on = [
    aws_ecs_task_definition.test
  ]
}