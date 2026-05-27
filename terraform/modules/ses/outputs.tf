output "domain_identity_arn"    { value = aws_ses_domain_identity.main.arn }
output "configuration_set_name" { value = aws_ses_configuration_set.main.name }
output "sns_topic_arn"          { value = aws_sns_topic.ses_notifications.arn }

