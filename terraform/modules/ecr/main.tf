resource "aws_ecr_repository" "service" {

  for_each = var.services

  name = "${var.project_name}-${var.environment}-${each.key}"

  image_tag_mutability = "MUTABLE"

  image_scanning_configuration {
    scan_on_push = true
  }

  force_delete = true
}