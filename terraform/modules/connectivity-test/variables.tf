variable "project_name" {
  type = string
}

variable "environment" {
  type = string
}

variable "vpc_id" {
  type = string
}

variable "subnet_ids" {
  type = list(string)
}

variable "security_group_id" {
  type = string
}

variable "database_endpoint" {
  type = string
}

variable "database_port" {
  type = number
}

variable "database_secret_arn" {
  type = string
}

variable "execution_role_arn" {
  type = string
}