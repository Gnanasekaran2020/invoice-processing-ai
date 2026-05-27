#!/usr/bin/env bash
# =============================================================
#  deploy.sh — Full infrastructure deploy + image push
#  Usage: ./scripts/deploy.sh [environment] [image_tag]
#  Example: ./scripts/deploy.sh production 1.0.0
# =============================================================

set -euo pipefail

ENVIRONMENT="${1:-staging}"
IMAGE_TAG="${2:-latest}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
TERRAFORM_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

echo "═══════════════════════════════════════════════════════"
echo "  Invoice Processing — Full Deployment"
echo "  Environment : $ENVIRONMENT"
echo "  Image Tag   : $IMAGE_TAG"
echo "═══════════════════════════════════════════════════════"

# ── Validate prerequisites ────────────────────────────────────
for cmd in terraform aws docker; do
  if ! command -v "$cmd" &>/dev/null; then
    echo "✗ ERROR: '$cmd' is required but not installed."
    exit 1
  fi
done

echo "✓ Prerequisites verified (terraform, aws, docker)"

# ── Check AWS auth ────────────────────────────────────────────
if ! aws sts get-caller-identity &>/dev/null; then
  echo "✗ ERROR: AWS CLI is not authenticated. Run 'aws configure' or set env vars."
  exit 1
fi

ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
echo "✓ AWS Account: $ACCOUNT_ID"

# ── Terraform init & apply ────────────────────────────────────
cd "$TERRAFORM_DIR"

echo ""
echo "▶ Step 1/4 — Terraform Init..."
terraform init -upgrade

echo ""
echo "▶ Step 2/4 — Terraform Plan..."
terraform plan \
  -var="environment=$ENVIRONMENT" \
  -var-file="environments/${ENVIRONMENT}.tfvars" \
  -out=tfplan

echo ""
echo "▶ Step 3/4 — Terraform Apply..."
terraform apply tfplan
rm -f tfplan

echo ""
echo "▶ Step 4/4 — Build & Push Docker images..."
bash "$SCRIPT_DIR/build_and_push.sh" "$ENVIRONMENT" "$IMAGE_TAG"

echo ""
echo "═══════════════════════════════════════════════════════"
echo "  ✓ Deployment complete!"
echo ""
APP_URL=$(terraform output -raw app_url 2>/dev/null || echo "N/A")
API_URL=$(terraform output -raw api_url 2>/dev/null || echo "N/A")
echo "  App URL : $APP_URL"
echo "  API URL : $API_URL"
echo "═══════════════════════════════════════════════════════"

