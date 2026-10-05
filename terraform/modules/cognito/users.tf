
resource "aws_cognito_user" "app_user" {
  user_pool_id = aws_cognito_user_pool.this.id

  username = "app-user@example.com"

  attributes = {
    email          = "app-user@example.com"
    email_verified = "true"
  }

  temporary_password = var.temporary_password

  lifecycle {
    ignore_changes = [
      temporary_password
    ]
  }
}

resource "aws_cognito_user_in_group" "app_user" {
  user_pool_id = aws_cognito_user_pool.this.id
  username     = aws_cognito_user.app_user.username
  group_name   = aws_cognito_user_group.this["USER"].name
}


resource "aws_cognito_user" "product_manager" {
  user_pool_id = aws_cognito_user_pool.this.id

  username = "product-manager@example.com"

  attributes = {
    email          = "product-manager@example.com"
    email_verified = "true"
  }

  temporary_password = var.temporary_password

  lifecycle {
    ignore_changes = [
      temporary_password
    ]
  }
}

resource "aws_cognito_user_in_group" "product_manager" {
  user_pool_id = aws_cognito_user_pool.this.id
  username     = aws_cognito_user.product_manager.username
  group_name   = aws_cognito_user_group.this["PRODUCT_MANAGER"].name
}


resource "aws_cognito_user" "admin" {
  user_pool_id = aws_cognito_user_pool.this.id

  username = "admin@example.com"

  attributes = {
    email          = "admin@example.com"
    email_verified = "true"
  }

  temporary_password = var.temporary_password

  lifecycle {
    ignore_changes = [
      temporary_password
    ]
  }
}

resource "aws_cognito_user_in_group" "admin" {
  user_pool_id = aws_cognito_user_pool.this.id
  username     = aws_cognito_user.admin.username
  group_name   = aws_cognito_user_group.this["ADMIN"].name
}


variable "temporary_password" {
  description = "Temporary Cognito password for development users"
  type        = string
  sensitive   = true
}

