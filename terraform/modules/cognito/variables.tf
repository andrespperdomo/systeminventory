variable "project_name" {
  type = string
}

variable "environment" {
  type = string
}

variable "groups" {
  description = "Cognito user groups"

  type = map(object({
    description = string
    precedence  = number
    role_arn    = optional(string)
  }))
}


variable "resource_server_scopes" {
  description = "OAuth scopes available to the M2M client"

  type = list(object({
    scope       = string
    description = string
  }))
}
variable "aws_region" {
  description = "AWS region"
  type        = string
}