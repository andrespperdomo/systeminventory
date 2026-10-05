module "vpc" {
  source = "./modules/vpc"

  project_name = var.project_name
  environment  = var.environment
  vpc_cidr     = var.vpc_cidr
}


module "security" {
  source = "./modules/security"

  project_name = var.project_name
  environment  = var.environment
  vpc_id       = module.vpc.vpc_id

  bastion_security_group_id = module.bastion.security_group_id

  tags = var.tags
}

resource "aws_db_subnet_group" "rds" {

  name = "${var.project_name}-${var.environment}-rds"

  subnet_ids = module.vpc.database_subnet_ids

  tags = {
    Name        = "${var.project_name}-${var.environment}-rds"
    Environment = var.environment
  }
}


module "rds" {
  for_each = var.services

  source = "./modules/rds"

  project_name = var.project_name
  environment  = var.environment

  database_name     = each.value.database_name
  database_username = each.value.database_username
  database_password = each.value.database_password

  instance_class = var.database_instance_class

  subnet_ids        = module.vpc.database_subnet_ids
  security_group_id = module.security.rds_security_group_id

  service_name = each.key

  db_subnet_group_name = aws_db_subnet_group.rds.name
}

module "elasticache" {
  source = "./modules/elasticache"

  name       = "${var.project_name}-${var.environment}"
  subnet_ids = module.vpc.private_subnet_ids

  security_group_ids = [
    module.security.redis_security_group_id
  ]

  redis_password = random_password.redis.result

  node_type = "cache.t4g.micro"

  engine_version = "7.2"

  tags = var.tags
}

module "secrets" {
  for_each = var.services

  source = "./modules/secrets"

  project_name = var.project_name
  environment  = var.environment

  service_name = each.key

  database_username = each.value.database_username
  database_password = each.value.database_password

  database_endpoint = module.rds[each.key].endpoint
  database_port     = module.rds[each.key].port
  database_name     = module.rds[each.key].database_name

  app_api_key = var.app_api_key
}
module "redis_secret" {
  source = "./modules/redis-secret"

  name = "${var.project_name}/${var.environment}/redis"

  project_name = var.project_name
  environment  = var.environment

  redis_username = "inventory"

  redis_password = random_password.redis.result

  tags = var.tags
}
module "ecr" {
  source = "./modules/ecr"

  project_name = var.project_name
  environment  = var.environment

  services = var.services
}


module "alb" {
  source = "./modules/alb"

  project_name = var.project_name
  environment  = var.environment

  vpc_id            = module.vpc.vpc_id
  public_subnet_ids = module.vpc.public_subnet_ids

  security_group_id = module.security.alb_security_group_id

  services = var.services
}

module "ecs" {
  source = "./modules/ecs"

  project_name = var.project_name
  environment  = var.environment
  aws_region   = var.aws_region

  services = var.services

  private_subnet_ids = module.vpc.private_subnet_ids

  security_group_id = module.security.ecs_security_group_id

  database_secret_arns = {
    for service, secret in module.secrets :
    service => secret.database_secret_arn
  }

  target_group_arns = module.alb.target_group_arns

  ecr_repository_urls = module.ecr.repository_urls

  quarkus_oidc_auth_server_url = module.cognito.oidc_issuer_url

  log_retention_days = var.log_retention_days

  tags = var.tags

  redis_url = "rediss://${module.elasticache.primary_endpoint_address}:6379"

  redis_secret_arn = module.redis_secret.secret_arn

  rabbitmq_host       = module.rabbitmq.host
  rabbitmq_secret_arn = module.rabbitmq.secret_arn

}


module "cognito" {
  source = "./modules/cognito"

  project_name = var.project_name
  environment  = var.environment
  aws_region   = var.aws_region

  groups = var.groups

  resource_server_scopes = var.resource_server_scopes

  temporary_password = var.cognito_temporary_password
}

module "rabbitmq" {
  source = "./modules/rabbitmq"

  project_name = var.project_name
  environment  = var.environment
  tags         = var.tags

  name = "${var.project_name}-${var.environment}-rabbitmq"

  instance_type = var.rabbitmq_instance_type

  subnet_ids = module.vpc.private_subnet_ids

  security_group_ids = [
    module.security.rabbitmq_security_group_id
  ]

  username = var.rabbitmq_username
  password = var.rabbitmq_password
}

module "connectivity_test" {
  source = "./modules/connectivity-test"

  project_name = var.project_name
  environment  = var.environment

  vpc_id = module.vpc.vpc_id

  subnet_ids = module.vpc.private_subnet_ids

  security_group_id = module.security.ecs_security_group_id

  database_endpoint   = module.rds["inventory"].endpoint
  database_port       = module.rds["inventory"].port
  database_secret_arn = module.secrets["inventory"].database_secret_arn

  execution_role_arn = module.ecs.execution_role_arn

  depends_on = [
    module.ecs
  ]
}
module "bastion" {
  source = "./modules/bastion"

  project_name = var.project_name
  environment  = var.environment

  vpc_id = module.vpc.vpc_id

  subnet_id = module.vpc.private_subnet_ids[0]
}