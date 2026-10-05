resource "aws_secretsmanager_secret" "database" {

  name = "${var.project_name}-${var.environment}-${var.service_name}-database"

  description = "Database credentials for ${var.project_name}-${var.environment}-${var.service_name}"

  recovery_window_in_days = 0
}


resource "aws_secretsmanager_secret_version" "database" {

  secret_id = aws_secretsmanager_secret.database.id

  secret_string = jsonencode({
    database_username = var.database_username
    database_password = var.database_password

    database_url = "jdbc:postgresql://${var.database_endpoint}:${var.database_port}/${var.database_name}"

    app_api_key = var.app_api_key
  })
}

