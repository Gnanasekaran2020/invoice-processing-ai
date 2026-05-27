output "function_arn"        { value = aws_lambda_function.invoice_processor.arn }
output "function_name"       { value = aws_lambda_function.invoice_processor.function_name }
output "alias_arn"           { value = aws_lambda_alias.live.arn }
output "dlq_arn"             { value = aws_sqs_queue.dlq.arn }
output "s3_permission_id"    { value = aws_lambda_permission.s3_invoke.id }

