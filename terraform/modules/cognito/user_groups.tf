/*resource "aws_cognito_user_in_group" "this" {
  for_each = var.user_group_memberships

  user_pool_id = aws_cognito_user.this[each.value.user].user_pool_id
  username     = aws_cognito_user.this[each.value.user].username
  group_name   = each.value.group
}*/