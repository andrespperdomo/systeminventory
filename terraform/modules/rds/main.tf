resource "aws_db_instance" "this" {

  identifier = "${var.project_name}-${var.environment}-${var.service_name}-postgres"

  engine = "postgres"

  instance_class = var.instance_class

  allocated_storage = 20

  storage_type = "gp3"

  db_name = var.database_name

  username = var.database_username

  password = var.database_password

  port = 5432

  publicly_accessible = false

  multi_az = false

  db_subnet_group_name = var.db_subnet_group_name

  vpc_security_group_ids = [
    var.security_group_id
  ]

  backup_retention_period = 0

  skip_final_snapshot = true

  deletion_protection = false

  apply_immediately = true

  tags = {
    Name        = "${var.project_name}-${var.environment}-${var.service_name}-postgres"
    Environment = var.environment
    Service     = var.service_name
  }
}
