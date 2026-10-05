resource "aws_cloudwatch_log_group" "service" {
  for_each = var.services

  name = "/ecs/${var.project_name}/${var.environment}/${each.key}"

  retention_in_days = var.log_retention_days

  tags = merge(
    var.tags,
    {
      Service = each.key
    }
  )
}