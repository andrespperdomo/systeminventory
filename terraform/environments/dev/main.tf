module "elasticache" {

  source = "../../modules/elasticache"

  name = "inventory-dev"

  subnet_ids = var.private_subnet_ids

  security_group_ids = [
    var.quarkus_security_group_id
  ]

  node_type = "cache.t4g.micro"

  num_cache_clusters = 1

  automatic_failover_enabled = false
  multi_az_enabled           = false

  tags = {
    Environment = "dev"
    Application = "inventory"
    ManagedBy   = "terraform"
  }
}