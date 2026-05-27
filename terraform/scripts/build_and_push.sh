#!/usr/bin/env bash
# =============================================================
#  build_and_push.sh — Build Docker images & push to ECR
#  Usage: ./scripts/build_and_push.sh [environment] [image_tag]
#  Example: ./scripts/build_and_push.sh production 1.0.0
# =============================================================

set -euo pipefail

ENVIRONMENT="${1:-production}"
IMAGE_TAG="${2:-latest}"
AWS_REGION="${AWS_REGION:-us-east-1}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

echo "═══════════════════════════════════════════════════"
echo "  Invoice Processing — Build & Push to ECR"
echo "  Environment : $ENVIRONMENT"
echo "  Image Tag   : $IMAGE_TAG"
echo "  AWS Region  : $AWS_REGION"
echo "═══════════════════════════════════════════════════"

# ── Get ECR repo URLs from Terraform outputs ──────────────────
cd "$ROOT_DIR/terraform"

echo ""
echo "▶ Fetching ECR repository URLs from Terraform..."
API_REPO_URL=$(terraform output -raw ecr_api_repo_url 2>/dev/null)
UI_REPO_URL=$(terraform output -raw ecr_ui_repo_url 2>/dev/null)

if [[ -z "$API_REPO_URL" || -z "$UI_REPO_URL" ]]; then
  echo "✗ ERROR: Could not get ECR repo URLs from Terraform outputs."
  echo "  Make sure you've run 'terraform apply' first."
  exit 1
fi

AWS_ACCOUNT_ID=$(echo "$API_REPO_URL" | cut -d'.' -f1)

echo "  API Repo: $API_REPO_URL"
echo "  UI Repo : $UI_REPO_URL"

# ── Authenticate Docker to ECR ────────────────────────────────
echo ""
echo "▶ Authenticating Docker to ECR..."
aws ecr get-login-password --region "$AWS_REGION" \
  | docker login --username AWS --password-stdin "$AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com"

# ── Build & Push API (Spring Boot) ───────────────────────────
echo ""
echo "▶ Building Spring Boot API image..."
cd "$ROOT_DIR/invoice_process_sb"
docker build \
  --platform linux/amd64 \
  --build-arg BUILD_DATE="$(date -u +%Y-%m-%dT%H:%M:%SZ)" \
  --build-arg IMAGE_TAG="$IMAGE_TAG" \
  -t "${API_REPO_URL}:${IMAGE_TAG}" \
  -t "${API_REPO_URL}:latest" \
  -f Dockerfile .

echo "▶ Pushing API image..."
docker push "${API_REPO_URL}:${IMAGE_TAG}"
docker push "${API_REPO_URL}:latest"
echo "  ✓ API image pushed: ${API_REPO_URL}:${IMAGE_TAG}"

# ── Build & Push UI (Angular / Nginx) ────────────────────────
echo ""
echo "▶ Building Angular UI image..."
cd "$ROOT_DIR/invoice_processing_angular_optimized"
docker build \
  --platform linux/amd64 \
  --build-arg BUILD_DATE="$(date -u +%Y-%m-%dT%H:%M:%SZ)" \
  --build-arg IMAGE_TAG="$IMAGE_TAG" \
  -t "${UI_REPO_URL}:${IMAGE_TAG}" \
  -t "${UI_REPO_URL}:latest" \
  -f Dockerfile .

echo "▶ Pushing UI image..."
docker push "${UI_REPO_URL}:${IMAGE_TAG}"
docker push "${UI_REPO_URL}:latest"
echo "  ✓ UI image pushed: ${UI_REPO_URL}:${IMAGE_TAG}"

# ── Force ECS service redeployment ───────────────────────────
echo ""
echo "▶ Forcing ECS service redeployment..."
cd "$ROOT_DIR/terraform"
ECS_CLUSTER=$(terraform output -raw ecs_cluster_name 2>/dev/null)
API_SERVICE=$(terraform output -raw ecs_api_service_name 2>/dev/null)
UI_SERVICE=$(terraform output -raw ecs_ui_service_name 2>/dev/null)

aws ecs update-service \
  --cluster "$ECS_CLUSTER" \
  --service "$API_SERVICE" \
  --force-new-deployment \
  --region "$AWS_REGION" > /dev/null
echo "  ✓ API service redeployment triggered"

aws ecs update-service \
  --cluster "$ECS_CLUSTER" \
  --service "$UI_SERVICE" \
  --force-new-deployment \
  --region "$AWS_REGION" > /dev/null
echo "  ✓ UI service redeployment triggered"

echo ""
echo "═══════════════════════════════════════════════════"
echo "  ✓ Build & Push complete!"
echo "  Monitor deployment:"
echo "  aws ecs wait services-stable \\"
echo "    --cluster $ECS_CLUSTER \\"
echo "    --services $API_SERVICE $UI_SERVICE \\"
echo "    --region $AWS_REGION"
echo "═══════════════════════════════════════════════════"

