output "vpc_id" {
  value = module.vpc.vpc_id
}

output "public_subnet_ids" {
  value = module.vpc.public_subnet_ids
}

output "private_subnet_ids" {
  value = module.vpc.private_subnet_ids
}

output "database_subnet_ids" {
  value = module.vpc.database_subnet_ids
}

output "database_endpoints" {
  description = "RDS endpoints by service"

  value = {
    for service, rds in module.rds :
    service => rds.endpoint
  }
}

output "database_ports" {
  description = "RDS ports by service"

  value = {
    for service, rds in module.rds :
    service => rds.port
  }
}

output "database_names" {
  description = "Database names by service"

  value = {
    for service, rds in module.rds :
    service => rds.database_name
  }
}

output "database_secret_arn" {
  description = "Database Secret ARNs by service"

  value = {
    for service, secret in module.secrets :
    service => secret.database_secret_arn
  }
}

output "ecr_repository_urls" {
  value = module.ecr.repository_urls
}

output "ecs_cluster_name" {
  value = module.ecs.cluster_name
}

output "ecs_service_names" {
  value = module.ecs.service_names
}

output "alb_dns_name" {
  value = module.alb.dns_name
}

output "cognito_user_pool_id" {
  value = module.cognito.user_pool_id
}

output "cognito_user_pool_arn" {
  value = module.cognito.user_pool_arn
}

output "cognito_user_pool_endpoint" {
  value = module.cognito.user_pool_endpoint
}

output "cognito_user_login_client_id" {
  value = module.cognito.user_login_client_id
}
output "bastion_instance_id" {
  description = "Bastion EC2 instance ID"
  value       = module.bastion.instance_id
}