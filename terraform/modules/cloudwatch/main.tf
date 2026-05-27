# =============================================================
#  Module: CloudWatch — Log Groups, Alarms, Dashboard
# =============================================================

# ── SNS Topic for Alarms ──────────────────────────────────────
resource "aws_sns_topic" "alarms" {
  name = "${var.name_prefix}-alarms"
  tags = { Name = "${var.name_prefix}-alarms" }
}

resource "aws_sns_topic_subscription" "alarm_email" {
  topic_arn = aws_sns_topic.alarms.arn
  protocol  = "email"
  endpoint  = var.alarm_email
}

# ── Log Groups ────────────────────────────────────────────────
resource "aws_cloudwatch_log_group" "api" {
  name              = "/ecs/${var.name_prefix}/api"
  retention_in_days = var.log_retention_days
  tags              = { Name = "${var.name_prefix}-api-logs" }
}

resource "aws_cloudwatch_log_group" "ui" {
  name              = "/ecs/${var.name_prefix}/ui"
  retention_in_days = var.log_retention_days
  tags              = { Name = "${var.name_prefix}-ui-logs" }
}

# ── ECS API — High CPU Alarm ──────────────────────────────────
resource "aws_cloudwatch_metric_alarm" "api_cpu_high" {
  alarm_name          = "${var.name_prefix}-api-cpu-high"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = 2
  metric_name         = "CPUUtilization"
  namespace           = "AWS/ECS"
  period              = 60
  statistic           = "Average"
  threshold           = 80
  alarm_description   = "API CPU utilization > 80%"
  alarm_actions       = [aws_sns_topic.alarms.arn]
  ok_actions          = [aws_sns_topic.alarms.arn]

  dimensions = {
    ClusterName = var.ecs_cluster_name
    ServiceName = var.api_service_name
  }
}

# ── ECS API — High Memory Alarm ───────────────────────────────
resource "aws_cloudwatch_metric_alarm" "api_memory_high" {
  alarm_name          = "${var.name_prefix}-api-memory-high"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = 2
  metric_name         = "MemoryUtilization"
  namespace           = "AWS/ECS"
  period              = 60
  statistic           = "Average"
  threshold           = 85
  alarm_description   = "API Memory utilization > 85%"
  alarm_actions       = [aws_sns_topic.alarms.arn]
  ok_actions          = [aws_sns_topic.alarms.arn]

  dimensions = {
    ClusterName = var.ecs_cluster_name
    ServiceName = var.api_service_name
  }
}

# ── ALB — 5XX Error Rate ─────────────────────────────────────
resource "aws_cloudwatch_metric_alarm" "alb_5xx" {
  alarm_name          = "${var.name_prefix}-alb-5xx-errors"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = 1
  metric_name         = "HTTPCode_Target_5XX_Count"
  namespace           = "AWS/ApplicationELB"
  period              = 60
  statistic           = "Sum"
  threshold           = 10
  alarm_description   = "ALB 5XX errors exceed threshold"
  treat_missing_data  = "notBreaching"
  alarm_actions       = [aws_sns_topic.alarms.arn]

  dimensions = {
    LoadBalancer = var.alb_arn_suffix
  }
}

# ── ALB — Target Response Time ────────────────────────────────
resource "aws_cloudwatch_metric_alarm" "alb_latency" {
  alarm_name          = "${var.name_prefix}-alb-latency"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = 2
  metric_name         = "TargetResponseTime"
  namespace           = "AWS/ApplicationELB"
  period              = 60
  statistic           = "p99"
  threshold           = 3
  alarm_description   = "ALB p99 latency > 3s"
  treat_missing_data  = "notBreaching"
  alarm_actions       = [aws_sns_topic.alarms.arn]

  dimensions = {
    LoadBalancer = var.alb_arn_suffix
    TargetGroup  = var.api_tg_arn_suffix
  }
}

# ── ALB — Unhealthy Host Count ────────────────────────────────
resource "aws_cloudwatch_metric_alarm" "api_unhealthy_hosts" {
  alarm_name          = "${var.name_prefix}-api-unhealthy-hosts"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = 1
  metric_name         = "UnHealthyHostCount"
  namespace           = "AWS/ApplicationELB"
  period              = 60
  statistic           = "Average"
  threshold           = 0
  alarm_description   = "One or more API targets are unhealthy"
  treat_missing_data  = "notBreaching"
  alarm_actions       = [aws_sns_topic.alarms.arn]

  dimensions = {
    LoadBalancer = var.alb_arn_suffix
    TargetGroup  = var.api_tg_arn_suffix
  }
}

# ── RDS — High CPU Alarm ──────────────────────────────────────
resource "aws_cloudwatch_metric_alarm" "rds_cpu_high" {
  alarm_name          = "${var.name_prefix}-rds-cpu-high"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = 2
  metric_name         = "CPUUtilization"
  namespace           = "AWS/RDS"
  period              = 60
  statistic           = "Average"
  threshold           = 80
  alarm_description   = "RDS CPU utilization > 80%"
  alarm_actions       = [aws_sns_topic.alarms.arn]

  dimensions = {
    DBInstanceIdentifier = var.rds_identifier
  }
}

# ── RDS — Low Free Storage ────────────────────────────────────
resource "aws_cloudwatch_metric_alarm" "rds_low_storage" {
  alarm_name          = "${var.name_prefix}-rds-low-storage"
  comparison_operator = "LessThanThreshold"
  evaluation_periods  = 1
  metric_name         = "FreeStorageSpace"
  namespace           = "AWS/RDS"
  period              = 300
  statistic           = "Average"
  threshold           = 5368709120  # 5 GB in bytes
  alarm_description   = "RDS free storage < 5 GB"
  alarm_actions       = [aws_sns_topic.alarms.arn]

  dimensions = {
    DBInstanceIdentifier = var.rds_identifier
  }
}

# ── Redis — High CPU ──────────────────────────────────────────
resource "aws_cloudwatch_metric_alarm" "redis_cpu_high" {
  alarm_name          = "${var.name_prefix}-redis-cpu-high"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = 2
  metric_name         = "CPUUtilization"
  namespace           = "AWS/ElastiCache"
  period              = 60
  statistic           = "Average"
  threshold           = 80
  alarm_description   = "Redis CPU > 80%"
  alarm_actions       = [aws_sns_topic.alarms.arn]

  dimensions = {
    CacheClusterId = var.redis_cluster_id
  }
}

# ── CloudWatch Dashboard ──────────────────────────────────────
resource "aws_cloudwatch_dashboard" "main" {
  dashboard_name = "${var.name_prefix}-dashboard"

  dashboard_body = jsonencode({
    widgets = [
      {
        type   = "text"
        x = 0; y = 0; width = 24; height = 1
        properties = { markdown = "# Invoice Processing System — Operations Dashboard" }
      },
      {
        type = "metric"; x = 0; y = 1; width = 8; height = 6
        properties = {
          title  = "API CPU Utilization"
          metrics = [["AWS/ECS", "CPUUtilization", "ClusterName", var.ecs_cluster_name, "ServiceName", var.api_service_name]]
          period = 60; stat = "Average"; view = "timeSeries"
        }
      },
      {
        type = "metric"; x = 8; y = 1; width = 8; height = 6
        properties = {
          title  = "API Memory Utilization"
          metrics = [["AWS/ECS", "MemoryUtilization", "ClusterName", var.ecs_cluster_name, "ServiceName", var.api_service_name]]
          period = 60; stat = "Average"; view = "timeSeries"
        }
      },
      {
        type = "metric"; x = 16; y = 1; width = 8; height = 6
        properties = {
          title  = "ALB Request Count"
          metrics = [["AWS/ApplicationELB", "RequestCount", "LoadBalancer", var.alb_arn_suffix]]
          period = 60; stat = "Sum"; view = "timeSeries"
        }
      },
      {
        type = "metric"; x = 0; y = 7; width = 8; height = 6
        properties = {
          title  = "ALB 5XX Errors"
          metrics = [["AWS/ApplicationELB", "HTTPCode_Target_5XX_Count", "LoadBalancer", var.alb_arn_suffix]]
          period = 60; stat = "Sum"; view = "timeSeries"
        }
      },
      {
        type = "metric"; x = 8; y = 7; width = 8; height = 6
        properties = {
          title  = "ALB p99 Latency (s)"
          metrics = [["AWS/ApplicationELB", "TargetResponseTime", "LoadBalancer", var.alb_arn_suffix]]
          period = 60; stat = "p99"; view = "timeSeries"
        }
      },
      {
        type = "metric"; x = 16; y = 7; width = 8; height = 6
        properties = {
          title  = "RDS CPU Utilization"
          metrics = [["AWS/RDS", "CPUUtilization", "DBInstanceIdentifier", var.rds_identifier]]
          period = 60; stat = "Average"; view = "timeSeries"
        }
      },
      {
        type = "metric"; x = 0; y = 13; width = 8; height = 6
        properties = {
          title  = "RDS Free Storage (Bytes)"
          metrics = [["AWS/RDS", "FreeStorageSpace", "DBInstanceIdentifier", var.rds_identifier]]
          period = 300; stat = "Average"; view = "timeSeries"
        }
      },
      {
        type = "metric"; x = 8; y = 13; width = 8; height = 6
        properties = {
          title  = "Redis CPU Utilization"
          metrics = [["AWS/ElastiCache", "CPUUtilization", "CacheClusterId", var.redis_cluster_id]]
          period = 60; stat = "Average"; view = "timeSeries"
        }
      },
      {
        type = "log"; x = 16; y = 13; width = 8; height = 6
        properties = {
          title   = "API Error Logs"
          query   = "SOURCE '${aws_cloudwatch_log_group.api.name}' | filter @message like /ERROR/ | sort @timestamp desc | limit 20"
          region  = "us-east-1"
          view    = "table"
        }
      }
    ]
  })
}

