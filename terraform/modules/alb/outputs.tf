output "dns_name" {
  value = aws_lb.this.dns_name
}

output "load_balancer_arn" {
  value = aws_lb.this.arn
}

output "target_group_arns" {

  value = {
    for service, target_group in aws_lb_target_group.service :
    service => target_group.arn
  }
}
