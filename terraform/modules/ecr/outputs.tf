output "api_repo_url" { value = aws_ecr_repository.api.repository_url }
output "ui_repo_url"  { value = aws_ecr_repository.ui.repository_url }
output "api_repo_arn" { value = aws_ecr_repository.api.arn }
output "ui_repo_arn"  { value = aws_ecr_repository.ui.arn }

