output "api_log_group_name" { value = aws_cloudwatch_log_group.api.name }
output "ui_log_group_name"  { value = aws_cloudwatch_log_group.ui.name }
output "alarm_sns_topic_arn"{ value = aws_sns_topic.alarms.arn }
output "dashboard_name"     { value = aws_cloudwatch_dashboard.main.dashboard_name }

