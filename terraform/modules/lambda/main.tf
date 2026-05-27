# =============================================================
#  Module: Lambda — Invoice Processor Function
# =============================================================

# ── Placeholder ZIP (replaced by CI/CD with real JAR) ─────────
data "archive_file" "placeholder" {
  type        = "zip"
  output_path = "${path.module}/placeholder.zip"

  source {
    content  = "placeholder"
    filename = "placeholder.txt"
  }
}

resource "aws_lambda_function" "invoice_processor" {
  function_name = var.function_name
  description   = "Processes uploaded invoice PDFs, extracts data and triggers SES notifications"
  role          = var.lambda_role_arn
  runtime       = var.runtime
  handler       = "com.invoice.lambda.InvoiceProcessorHandler::handleRequest"

  # Use placeholder; replace with real JAR via CI/CD
  filename         = data.archive_file.placeholder.output_path
  source_code_hash = data.archive_file.placeholder.output_base64sha256

  memory_size = var.memory_size
  timeout     = var.timeout

  # VPC config for RDS access
  vpc_config {
    subnet_ids         = var.private_subnet_ids
    security_group_ids = [var.lambda_sg_id]
  }

  environment {
    variables = {
      DB_HOST        = var.db_host
      DB_PORT        = "5432"
      DB_NAME        = var.db_name
      DB_USER        = var.db_username
      DB_PASSWORD    = var.db_password
      S3_BUCKET_NAME = var.s3_bucket_name
      SES_FROM_EMAIL = var.ses_from_email
      AWS_REGION_APP = var.aws_region
      SPRING_PROFILES_ACTIVE = "lambda"
    }
  }

  # Reserved concurrency
  reserved_concurrent_executions = 10

  # X-Ray tracing
  tracing_config {
    mode = "Active"
  }

  # Dead Letter Queue
  dead_letter_config {
    target_arn = aws_sqs_queue.dlq.arn
  }

  layers = []

  tags = { Name = var.function_name }

  depends_on = [aws_cloudwatch_log_group.lambda]
}

# ── CloudWatch Log Group ───────────────────────────────────────
resource "aws_cloudwatch_log_group" "lambda" {
  name              = "/aws/lambda/${var.function_name}"
  retention_in_days = var.log_retention_days
  tags              = { Name = "/aws/lambda/${var.function_name}" }
}

# ── S3 Trigger Permission ─────────────────────────────────────
resource "aws_lambda_permission" "s3_invoke" {
  statement_id   = "AllowS3Invoke"
  action         = "lambda:InvokeFunction"
  function_name  = aws_lambda_function.invoice_processor.function_name
  principal      = "s3.amazonaws.com"
  source_arn     = var.s3_bucket_arn
}

# ── Dead Letter Queue (SQS) ────────────────────────────────────
resource "aws_sqs_queue" "dlq" {
  name                      = "${var.name_prefix}-lambda-dlq"
  message_retention_seconds = 1209600  # 14 days
  tags                      = { Name = "${var.name_prefix}-lambda-dlq" }
}

resource "aws_sqs_queue_policy" "dlq" {
  queue_url = aws_sqs_queue.dlq.id
  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect    = "Allow"
      Principal = { Service = "lambda.amazonaws.com" }
      Action    = "sqs:SendMessage"
      Resource  = aws_sqs_queue.dlq.arn
    }]
  })
}

# ── Lambda Auto-Scaling (Provisioned Concurrency) ─────────────
resource "aws_lambda_alias" "live" {
  name             = "live"
  description      = "Live alias for production traffic"
  function_name    = aws_lambda_function.invoice_processor.function_name
  function_version = "$LATEST"
}
