variable "project_name" {
  type = string
}

variable "environment" {
  type = string
}

variable "database_name" {
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

variable "instance_class" {
  type = string
}

variable "subnet_ids" {
  type = list(string)
}

variable "security_group_id" {
  type = string
}
variable "service_name" {
  type = string
}
variable "db_subnet_group_name" {
  type = string
}