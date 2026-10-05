variable "project_name" {
  description = "Project name"
  type        = string
}

variable "environment" {
  description = "Environment name"
  type        = string
}

variable "aws_region" {
  description = "AWS region"
  type        = string
}

variable "private_subnet_ids" {
  description = "Private subnet IDs for ECS tasks"
  type        = list(string)
}

variable "security_group_id" {
  description = "Security group used by ECS tasks"
  type        = string
}

variable "database_secret_arns" {
  description = "Database secret ARN per microservice"
  type        = map(string)
}

variable "target_group_arns" {
  description = "Target group ARN per service"
  type        = map(string)
}

variable "ecr_repository_urls" {
  description = "ECR repository URL per service"
  type        = map(string)
}

variable "log_retention_days" {
  description = "CloudWatch log retention"
  type        = number
  default     = 7
}

variable "tags" {
  description = "Common resource tags"
  type        = map(string)
  default     = {}
}
variable "services" {
  description = "Microservices deployed to ECS"

  type = map(object({
    cpu            = number
    memory         = number
    container_port = number
    desired_count  = number
    health_path    = string
    image          = string

    redis_enabled    = optional(bool, false)
    rabbitmq_enabled = optional(bool, false)
    environment      = optional(map(string), {})
  }))
}
variable "quarkus_oidc_auth_server_url" {
  description = "OIDC issuer URL used by Quarkus"
  type        = string
  default     = null
}

variable "secret_arn" {
  description = "ARN of the application secret"
  type        = string
  default     = null
}
variable "redis_secret_arn" {
  description = "ARN of the Redis password secret"
  type        = string
}
variable "redis_url" {
  description = "URL of the Redis password secret"
  type        = string
}
variable "kafka_bootstrap_servers" {
  description = "Amazon MSK bootstrap broker endpoints"
  type        = string
  default     = ""
}
variable "rabbitmq_host" {
  description = "RabbitMQ broker hostname"
  type        = string
}

variable "rabbitmq_secret_arn" {
  description = "ARN of RabbitMQ credentials secret"
  type        = string
  sensitive   = true
}