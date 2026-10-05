resource "aws_lb" "this" {

  name = "${var.project_name}-${var.environment}-alb"

  load_balancer_type = "application"

  internal = false

  security_groups = [
    var.security_group_id
  ]

  subnets = var.public_subnet_ids
}


resource "aws_lb_listener" "http" {

  load_balancer_arn = aws_lb.this.arn

  port = 80

  protocol = "HTTP"

  default_action {
    type = "fixed-response"

    fixed_response {
      content_type = "text/plain"

      message_body = "Service not found"

      status_code = "404"
    }
  }
}


resource "aws_lb_target_group" "service" {

  for_each = var.services

  name = "${var.project_name}-${var.environment}-${each.key}"

  port = each.value.container_port

  protocol = "HTTP"

  target_type = "ip"

  vpc_id = var.vpc_id

  health_check {

    enabled = true

    path = each.value.health_path

    protocol = "HTTP"

    port = "traffic-port"

    healthy_threshold = 2

    unhealthy_threshold = 3

    timeout = 5

    interval = 30

    matcher = "200-399"
  }
}


resource "aws_lb_listener_rule" "service" {

  for_each = var.services

  listener_arn = aws_lb_listener.http.arn

  priority = 100 + index(keys(var.services), each.key)

  action {

    type = "forward"

    target_group_arn = aws_lb_target_group.service[each.key].arn
  }

  condition {

    path_pattern {
      values = [
        "/${each.key}",
        "/${each.key}/*"
      ]
    }
  }
}