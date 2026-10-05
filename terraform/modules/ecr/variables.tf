variable "project_name" {
  type = string
}

variable "environment" {
  type = string
}

variable "services" {
  type = map(object({
    image          = string
    cpu            = number
    memory         = number
    container_port = number
    desired_count  = number
    health_path    = string
  }))
}