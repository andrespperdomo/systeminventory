variable "aws_region" {
  type    = string
  default = "us-east-1"
}

variable "project_name" {
  type = string
}

variable "environment" {
  type = string
}

variable "vpc_cidr" {
  type = string
}


variable "database_instance_class" {
  type    = string
  default = "db.t3.micro"
}
variable "app_api_key" {
  description = "Application API key stored in Secrets Manager"
  type        = string
  sensitive   = true
}

variable "services" {
  type = map(object({
    image             = string
    cpu               = number
    memory            = number
    container_port    = number
    desired_count     = number
    health_path       = string
    database_name     = string
    database_username = string
    database_password = string

    redis_enabled    = optional(bool, false)
    rabbitmq_enabled = optional(bool, false)

    environment = optional(map(string), {})
  }))
}

variable "log_retention_days" {
  type    = number
  default = 7
}

variable "tags" {
  type    = map(string)
  default = {}
}

# ============================================================
# COGNITO
# ============================================================

variable "groups" {
  type = map(object({
    description = string
    precedence  = number
    role_arn    = optional(string)
  }))
}

variable "admin_password" {
  type      = string
  sensitive = true
}

variable "product_manager_password" {
  type      = string
  sensitive = true
}

variable "user_password" {
  type      = string
  sensitive = true
}

variable "cognito_temporary_password" {
  type      = string
  sensitive = true
}
variable "resource_server_scopes" {
  description = "OAuth scopes for the Cognito resource server"

  type = list(object({
    scope       = string
    description = string
  }))

  default = [
    {
      scope       = "read"
      description = "Read inventory"
    },
    {
      scope       = "write"
      description = "Create and update inventory"
    },
    {
      scope       = "delete"
      description = "Delete inventory"
    }
  ]
}

variable "rabbitmq_username" {
  description = "RabbitMQ administrator username"
  type        = string
  sensitive   = true
}

variable "rabbitmq_password" {
  description = "RabbitMQ administrator password"
  type        = string
  sensitive   = true
}
variable "rabbitmq_instance_type" {
  description = "Amazon MQ RabbitMQ broker instance type"
  type        = string
  default     = "mq.m5.large"
}
