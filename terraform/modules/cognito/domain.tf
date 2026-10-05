resource "aws_cognito_user_pool_domain" "this" {

  domain = "${var.project_name}-${var.environment}-auth-073332434767"

  user_pool_id = aws_cognito_user_pool.this.id
}