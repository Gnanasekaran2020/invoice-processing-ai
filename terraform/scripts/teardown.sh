#!/usr/bin/env bash
# =============================================================
#  teardown.sh — Safely destroy all infrastructure
#  Usage: ./scripts/teardown.sh [environment]
#  WARNING: This destroys ALL resources including RDS data!
# =============================================================

set -euo pipefail

ENVIRONMENT="${1:-staging}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
TERRAFORM_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

echo "════════════════════════════════════════════════════════"
echo "  ⚠️  WARNING: INFRASTRUCTURE TEARDOWN"
echo "  Environment : $ENVIRONMENT"
echo "  This will destroy ALL AWS resources including RDS!"
echo "════════════════════════════════════════════════════════"
echo ""
read -rp "Type the environment name to confirm destruction: " CONFIRM

if [[ "$CONFIRM" != "$ENVIRONMENT" ]]; then
  echo "✗ Confirmation did not match. Aborting."
  exit 1
fi

cd "$TERRAFORM_DIR"

echo ""
echo "▶ Draining ECS services to 0..."
CLUSTER=$(terraform output -raw ecs_cluster_name 2>/dev/null || true)
API_SVC=$(terraform output -raw ecs_api_service_name 2>/dev/null || true)
UI_SVC=$(terraform output -raw ecs_ui_service_name 2>/dev/null || true)
AWS_REGION="${AWS_REGION:-us-east-1}"

if [[ -n "$CLUSTER" && -n "$API_SVC" ]]; then
  aws ecs update-service --cluster "$CLUSTER" --service "$API_SVC" --desired-count 0 --region "$AWS_REGION" > /dev/null && echo "  ✓ API service drained"
fi
if [[ -n "$CLUSTER" && -n "$UI_SVC" ]]; then
  aws ecs update-service --cluster "$CLUSTER" --service "$UI_SVC"  --desired-count 0 --region "$AWS_REGION" > /dev/null && echo "  ✓ UI service drained"
fi

echo ""
echo "▶ Running terraform destroy..."
terraform destroy \
  -var="environment=$ENVIRONMENT" \
  -var-file="environments/${ENVIRONMENT}.tfvars" \
  -auto-approve

echo ""
echo "════════════════════════════════════════════════════════"
echo "  ✓ Teardown complete for environment: $ENVIRONMENT"
echo "════════════════════════════════════════════════════════"

