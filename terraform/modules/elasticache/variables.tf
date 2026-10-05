variable "name" {
  type = string
}


variable "subnet_ids" {
  type = list(string)
}

variable "security_group_ids" {
  type = list(string)
}

variable "node_type" {
  type    = string
  default = "cache.t4g.micro"
}

variable "engine_version" {
  type    = string
  default = "7.2"
}

variable "num_cache_clusters" {
  type    = number
  default = 1
}

variable "tags" {
  type    = map(string)
  default = {}
}
variable "redis_password" {
  type      = string
  sensitive = true
}