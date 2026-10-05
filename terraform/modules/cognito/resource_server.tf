resource "aws_cognito_resource_server" "this" {
  identifier = "inventory-api"
  name       = "Inventory API"

  user_pool_id = aws_cognito_user_pool.this.id

  dynamic "scope" {
    for_each = var.resource_server_scopes

    content {
      scope_name        = scope.value.scope
      scope_description = scope.value.description
    }
  }
}