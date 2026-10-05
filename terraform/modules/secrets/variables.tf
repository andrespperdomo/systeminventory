variable "project_name" {
  type = string
}

variable "environment" {
  type = string
}

variable "database_username" {
  type      = string
  sensitive = true
}

variable "database_password" {
  type      = string
  sensitive = true
}

variable "database_endpoint" {
  type = string
}

variable "database_port" {
  type = number
}

variable "database_name" {
  type = string
}
variable "app_api_key" {
  type      = string
  sensitive = true
}
variable "service_name" {
  type = string
}