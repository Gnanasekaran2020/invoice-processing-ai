output "cluster_name"      { value = aws_ecs_cluster.main.name }
output "cluster_arn"       { value = aws_ecs_cluster.main.arn }
output "api_service_name"  { value = aws_ecs_service.api.name }
output "ui_service_name"   { value = aws_ecs_service.ui.name }
output "api_task_def_arn"  { value = aws_ecs_task_definition.api.arn }
output "ui_task_def_arn"   { value = aws_ecs_task_definition.ui.arn }
output "db_secret_arn"     { value = aws_secretsmanager_secret.db_password.arn }
output "jwt_secret_arn"    { value = aws_secretsmanager_secret.jwt_secret.arn }

