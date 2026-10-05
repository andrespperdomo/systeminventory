output "user_pool_id" {
  value = aws_cognito_user_pool.this.id
}

output "user_pool_arn" {
  value = aws_cognito_user_pool.this.arn
}


output "user_pool_endpoint" {
  description = "Cognito User Pool endpoint"

  value = "https://cognito-idp.${var.aws_region}.amazonaws.com/${aws_cognito_user_pool.this.id}"
}

output "client_id" {
  description = "Cognito application client ID"

  value = aws_cognito_user_pool_client.m2m.id
}
output "oidc_issuer_url" {
  value = "https://cognito-idp.${var.aws_region}.amazonaws.com/${aws_cognito_user_pool.this.id}"
}

output "user_login_client_id" {
  description = "Cognito App Client ID for user login"
  value       = aws_cognito_user_pool_client.user_login.id
}


output "m2m_client_id" {
  value = aws_cognito_user_pool_client.m2m.id
}

output "m2m_client_secret" {
  value     = aws_cognito_user_pool_client.m2m.client_secret
  sensitive = true
}

