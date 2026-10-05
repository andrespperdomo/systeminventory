provider "aws" {
  region = var.aws_region
}

provider "random" {
  # Optional provider for random-generated values if needed in future modules.
}
