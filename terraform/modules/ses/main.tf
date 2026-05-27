# =============================================================
#  Module: SES — Email Notifications
# =============================================================

# ── Domain Identity ───────────────────────────────────────────
resource "aws_ses_domain_identity" "main" {
  domain = var.domain_name
}

# ── DKIM Verification ─────────────────────────────────────────
resource "aws_ses_domain_dkim" "main" {
  domain = aws_ses_domain_identity.main.domain
}

resource "aws_route53_record" "ses_dkim" {
  count   = 3
  zone_id = var.route53_zone_id
  name    = "${aws_ses_domain_dkim.main.dkim_tokens[count.index]}._domainkey"
  type    = "CNAME"
  ttl     = 600
  records = ["${aws_ses_domain_dkim.main.dkim_tokens[count.index]}.dkim.amazonses.com"]
}

# ── SPF Record ────────────────────────────────────────────────
resource "aws_route53_record" "ses_spf" {
  zone_id = var.route53_zone_id
  name    = var.domain_name
  type    = "TXT"
  ttl     = 600
  records = ["v=spf1 include:amazonses.com ~all"]
}

# ── DMARC Record ──────────────────────────────────────────────
resource "aws_route53_record" "ses_dmarc" {
  zone_id = var.route53_zone_id
  name    = "_dmarc.${var.domain_name}"
  type    = "TXT"
  ttl     = 600
  records = ["v=DMARC1; p=quarantine; rua=mailto:${var.alert_email}; ruf=mailto:${var.alert_email}; fo=1"]
}

# ── SES Mail From Domain ──────────────────────────────────────
resource "aws_ses_domain_mail_from" "main" {
  domain           = aws_ses_domain_identity.main.domain
  mail_from_domain = "mail.${var.domain_name}"
}

resource "aws_route53_record" "ses_mail_from_mx" {
  zone_id = var.route53_zone_id
  name    = aws_ses_domain_mail_from.main.mail_from_domain
  type    = "MX"
  ttl     = 600
  records = ["10 feedback-smtp.${data.aws_region.current.name}.amazonses.com"]
}

# ── Email Identity for sender address ─────────────────────────
resource "aws_ses_email_identity" "from" {
  email = var.from_email
}

# ── SES Configuration Set (tracking + suppression) ───────────
resource "aws_ses_configuration_set" "main" {
  name = "${replace(var.domain_name, ".", "-")}-config-set"

  delivery_options {
    tls_policy = "Require"
  }

  reputation_metrics_enabled = true
  sending_enabled            = true
}

# ── SNS Topic for bounce/complaint notifications ──────────────
resource "aws_sns_topic" "ses_notifications" {
  name = "${replace(var.domain_name, ".", "-")}-ses-notifications"
}

resource "aws_sns_topic_subscription" "ses_alert_email" {
  topic_arn = aws_sns_topic.ses_notifications.arn
  protocol  = "email"
  endpoint  = var.alert_email
}

data "aws_region" "current" {}

