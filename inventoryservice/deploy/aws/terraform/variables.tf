variable "aws_region" {
  type        = string
  description = "AWS region for deployment"
  default     = "us-east-1"
}

variable "environment" {
  type        = string
  description = "Deployment environment identifier"
  default     = "prod"
}

variable "app_name" {
  type        = string
  description = "Short application name used for naming resources"
  default     = "inventory-service"
}

variable "vpc_cidr" {
  type        = string
  description = "CIDR block for the VPC"
  default     = "10.0.0.0/16"
}

variable "public_subnets" {
  type        = list(string)
  description = "CIDR blocks for public subnets"
  default     = ["10.0.1.0/24", "10.0.2.0/24"]
}

variable "private_subnets" {
  type        = list(string)
  description = "CIDR blocks for private subnets"
  default     = ["10.0.101.0/24", "10.0.102.0/24"]
}

variable "db_username" {
  type        = string
  description = "Database administrator username"
  default     = "inventory_admin"
}

variable "db_password" {
  type        = string
  description = "Database administrator password"
  sensitive   = true
}

variable "db_name" {
  type        = string
  description = "PostgreSQL database name"
  default     = "inventory_db"
}

variable "inventory_api_key" {
  type        = string
  description = "API key value used to authorize HTTP requests"
  sensitive   = true
}

variable "mq_username" {
  type        = string
  description = "RabbitMQ broker username"
  default     = "mq_user"
}

variable "mq_password" {
  type        = string
  description = "RabbitMQ broker password"
  sensitive   = true
}

variable "container_cpu" {
  type        = number
  description = "Fargate task CPU"
  default     = 512
}

variable "container_memory" {
  type        = number
  description = "Fargate task memory"
  default     = 1024
}

variable "desired_count" {
  type        = number
  description = "Number of ECS tasks to run"
  default     = 2
}

variable "container_port" {
  type        = number
  description = "HTTP port exposed by the container"
  default     = 8080
}

variable "enable_https" {
  type        = bool
  description = "Whether to attach an HTTPS listener to the ALB"
  default     = false
}

variable "certificate_arn" {
  type        = string
  description = "ACM certificate ARN for HTTPS listener"
  default     = ""
}
