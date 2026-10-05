module "elasticache" {

  source = "../../modules/elasticache"

  name = "inventory-prod"

  subnet_ids = var.private_subnet_ids

  security_group_ids = [
    var.quarkus_security_group_id
  ]

  node_type = "cache.r7g.large"

  num_cache_clusters = 2

  automatic_failover_enabled = true
  multi_az_enabled           = true

  tags = {
    Environment = "prod"
    Application = "inventory"
    ManagedBy   = "terraform"
  }
}