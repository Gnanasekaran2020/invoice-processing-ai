output "alb_sg_id"    { value = aws_security_group.alb.id }
output "api_sg_id"    { value = aws_security_group.api.id }
output "ui_sg_id"     { value = aws_security_group.ui.id }
output "rds_sg_id"    { value = aws_security_group.rds.id }
output "redis_sg_id"  { value = aws_security_group.redis.id }
output "lambda_sg_id" { value = aws_security_group.lambda.id }

