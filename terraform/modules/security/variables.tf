variable "project_name" {
  type = string
}

variable "environment" {
  type = string
}

variable "vpc_id" {
  type = string
}
variable "tags" {
  description = "Tags applied to security resources"
  type        = map(string)
  default     = {}
}
variable "bastion_security_group_id" {
  type = string
}