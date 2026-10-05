# AWS Terraform Deployment for Inventory Service

This directory contains the Terraform configuration to deploy the Inventory Service to AWS using a production-grade architecture.

## Overview

The deployment provisions:

- VPC with public and private subnets
- Application Load Balancer (HTTP/HTTPS)
- ECS Fargate cluster and service
- ECR repository for container images
- RDS PostgreSQL database
- ElastiCache Redis cache
- Amazon MQ RabbitMQ broker
- CloudWatch log group
- Secrets Manager for sensitive values
- Auto-scaling policies for ECS

## Prerequisites

- AWS CLI configured with credentials and default region
- Terraform 1.5.0 or newer
- Docker installed (to build and push container image)
- Access to an AWS account with IAM permissions for VPC, ECS, ECR, RDS, ElastiCache, MQ, Secrets Manager, CloudWatch, and IAM

## File Structure

- `main.tf` — resource definitions and service wiring
- `variables.tf` — deployment variables and defaults
- `outputs.tf` — useful deployment outputs
- `providers.tf` — AWS provider configuration
- `versions.tf` — required Terraform versions

## Deploy Steps

1. Build the application image locally and tag it:

```bash
cd c:\Users\57322\OneDrive\Escritorio\TECHNICAL-TEST-GITFLOW\inventoryservice
./mvnw package -DskipTests
```

2. Authenticate Docker to ECR and push the image after Terraform creates the repository:

```bash
cd deploy/aws/terraform
terraform init
terraform plan -var="db_password=YOUR_DB_PASSWORD" -var="inventory_api_key=YOUR_API_KEY" -var="mq_password=YOUR_MQ_PASSWORD"
terraform apply -var="db_password=YOUR_DB_PASSWORD" -var="inventory_api_key=YOUR_API_KEY" -var="mq_password=YOUR_MQ_PASSWORD"
```

After apply completes, note the `ecr_repository_uri` output.

3. Tag and push the image to ECR:

```bash
aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin <ECR_URI>
docker build -t inventory-service:latest .
docker tag inventory-service:latest <ECR_URI>:latest
docker push <ECR_URI>:latest
```

4. Re-run `terraform apply` if necessary to update the ECS service and deployment after the image is pushed.

## Recommended Variable Values

Use secure values for:

- `db_password`
- `inventory_api_key`
- `mq_password`

Set environment-specific values for:

- `aws_region`
- `environment` (e.g. `staging`, `prod`)
- `container_cpu` / `container_memory`
- `desired_count`

## Security Best Practices

- Keep API keys in AWS Secrets Manager rather than code
- Use private subnets for stateful services (RDS, Redis, RabbitMQ)
- Restrict security group access to only required services
- Enable TLS encryption for Amazon MQ and Redis if supported
- Rotate credentials regularly

## Post-deployment Notes

- The Health Check path is configured as `/q/health/ready`.
- Metrics are available at the container path `/q/metrics`.
- Swagger UI can be exposed by the app at `/swagger`.
- The API key is expected on the `X-API-KEY` header.

## Optional Enhancements

- Use ACM certificate and set `enable_https = true` and `certificate_arn`
- Add an S3-backed Terraform state backend for team collaboration
- Add Secrets Manager references into ECS task definition instead of environment variables
- Add an outbox processor and event publisher lambda or service for improved event delivery
- Add CloudWatch Alarm definitions for CPU, memory, and failed deployments

## Commands

- `terraform init` — initialize the workspace
- `terraform plan` — preview infrastructure changes
- `terraform apply` — apply the deployment
- `terraform destroy` — destroy the deployed infrastructure

## Example Apply

```bash
cd deploy/aws/terraform
terraform init
terraform apply \
  -var="aws_region=us-east-1" \
  -var="environment=prod" \
  -var="db_password=YourSecureDbPassword" \
  -var="inventory_api_key=YourSecureApiKey" \
  -var="mq_password=YourSecureMqPassword"
```

## Notes

- The ECS task definition uses the `latest` image tag from ECR.
- In production, prefer immutable image tags and a CI/CD pipeline to deploy versioned containers.
- For multi-region deployment, replicate the same Terraform configuration in another workspace or use a Terraform `workspace` strategy.
