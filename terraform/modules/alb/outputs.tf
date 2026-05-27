output "dns_name"        { value = aws_lb.main.dns_name }
output "zone_id"         { value = aws_lb.main.zone_id }
output "arn"             { value = aws_lb.main.arn }
output "arn_suffix"      { value = aws_lb.main.arn_suffix }
output "api_tg_arn"      { value = aws_lb_target_group.api.arn }
output "ui_tg_arn"       { value = aws_lb_target_group.ui.arn }
output "api_tg_arn_suffix" { value = aws_lb_target_group.api.arn_suffix }
output "https_listener_arn" { value = aws_lb_listener.https.arn }

