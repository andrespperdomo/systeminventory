resource "aws_cognito_user_pool_client" "m2m" {
  name         = "${var.project_name}-${var.environment}-m2m"
  user_pool_id = aws_cognito_user_pool.this.id

  generate_secret = true

  allowed_oauth_flows_user_pool_client = true

  allowed_oauth_flows = [
    "client_credentials"
  ]

  allowed_oauth_scopes = [
    for scope in var.resource_server_scopes :
    "${aws_cognito_resource_server.this.identifier}/${scope.scope}"
  ]

  supported_identity_providers = [
    "COGNITO"
  ]
}