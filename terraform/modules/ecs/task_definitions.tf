locals {

  # =========================================================
  # COMMON ENVIRONMENT VARIABLES
  # =========================================================

  common_environment = {
    QUARKUS_OIDC_AUTH_SERVER_URL = var.quarkus_oidc_auth_server_url
  }


  # =========================================================
  # ENVIRONMENT VARIABLES PER SERVICE
  # =========================================================

  service_environment = {
    for service_name, service in var.services :

    service_name => merge(

      # Common variables
      local.common_environment,

      # Service-specific variables
      service.environment,

      # HTTP
      {
        QUARKUS_HTTP_PORT = tostring(service.container_port)
      },

      # Redis
      service.redis_enabled ? {
        REDIS_URL = var.redis_url
      } : {},

      # RabbitMQ
      service.rabbitmq_enabled ? {
        RABBITMQ_HOST = var.rabbitmq_host
        RABBITMQ_PORT = "5671"
        RABBITMQ_TLS  = "true"
      } : {}
    )
  }


  # =========================================================
  # SECRETS PER SERVICE
  # =========================================================

  service_secrets = {
    for service_name, service in var.services :

    service_name => concat(

      # Database - common to all services
      [
        {
          name      = "DATABASE_USERNAME"
          valueFrom = "${var.database_secret_arns[service_name]}:database_username::"
        },
        {
          name      = "DATABASE_PASSWORD"
          valueFrom = "${var.database_secret_arns[service_name]}:database_password::"
        },
        {
          name      = "DATABASE_URL"
          valueFrom = "${var.database_secret_arns[service_name]}:database_url::"
        },
        {
          name      = "APP_API_KEY"
          valueFrom = "${var.database_secret_arns[service_name]}:app_api_key::"
        }
      ],

      # Redis - only when enabled
      service.redis_enabled ? [
        {
          name      = "REDIS_USERNAME"
          valueFrom = "${var.redis_secret_arn}:username::"
        },
        {
          name      = "REDIS_PASSWORD"
          valueFrom = "${var.redis_secret_arn}:password::"
        }
      ] : [],

      # RabbitMQ - only when enabled
      service.rabbitmq_enabled ? [
        {
          name      = "RABBITMQ_USERNAME"
          valueFrom = "${var.rabbitmq_secret_arn}:username::"
        },
        {
          name      = "RABBITMQ_PASSWORD"
          valueFrom = "${var.rabbitmq_secret_arn}:password::"
        }
      ] : []
    )
  }
}


resource "aws_ecs_task_definition" "service" {

  for_each = var.services

  family = "${var.project_name}-${var.environment}-${each.key}"

  requires_compatibilities = ["FARGATE"]

  network_mode = "awsvpc"

  cpu = each.value.cpu

  memory = each.value.memory

  execution_role_arn = aws_iam_role.execution.arn

  task_role_arn = aws_iam_role.task.arn

  container_definitions = jsonencode([
    {
      name = each.key

      image = "${var.ecr_repository_urls[each.key]}:${each.value.image}"

      essential = true

      portMappings = [
        {
          containerPort = each.value.container_port
          protocol      = "tcp"
        }
      ]

      # =====================================================
      # ENVIRONMENT
      # =====================================================

      environment = [
        for name, value in local.service_environment[each.key] : {
          name  = name
          value = value
        }
      ]

      # =====================================================
      # SECRETS
      # =====================================================

      secrets = local.service_secrets[each.key]

      # =====================================================
      # LOGGING
      # =====================================================

      logConfiguration = {
        logDriver = "awslogs"

        options = {
          "awslogs-group"         = aws_cloudwatch_log_group.service[each.key].name
          "awslogs-region"        = var.aws_region
          "awslogs-stream-prefix" = each.key
        }
      }

      # =====================================================
      # HEALTH CHECK
      # =====================================================

      healthCheck = {
        command = [
          "CMD-SHELL",
          "curl -f http://localhost:${each.value.container_port}${each.value.health_path} || exit 1"
        ]

        interval    = 30
        timeout     = 5
        retries     = 3
        startPeriod = 60
      }
    }
  ])

  tags = merge(
    var.tags,
    {
      Service = each.key
    }
  )
}