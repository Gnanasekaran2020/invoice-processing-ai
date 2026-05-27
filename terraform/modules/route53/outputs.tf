output "zone_id"      { value = data.aws_route53_zone.main.zone_id }
output "app_fqdn"     { value = aws_route53_record.app.fqdn }
output "api_fqdn"     { value = aws_route53_record.api.fqdn }

