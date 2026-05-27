# =============================================================
#  Remote State Backend — S3 + DynamoDB Lock
#  Create the bucket & table BEFORE running terraform init
# =============================================================
terraform {
  backend "s3" {
    bucket         = "invoice-terraform-state"          # Change to your bucket name
    key            = "invoice-processing/terraform.tfstate"
    region         = "us-east-1"
    encrypt        = true
    dynamodb_table = "invoice-terraform-locks"
  }
}

