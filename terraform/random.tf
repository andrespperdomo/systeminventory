resource "random_password" "redis" {
  length  = 32
  special = false
}