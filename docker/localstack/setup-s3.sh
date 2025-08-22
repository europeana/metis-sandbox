#!/usr/bin/env bash
set -e
pip install awscli
pip install awscli-local

echo "S3 Configuration started"
awslocal s3api create-bucket \
  --bucket metis-sandbox-thumbnails-local \
  --region eu-central-1 \
  --create-bucket-configuration LocationConstraint=eu-central-1
echo "S3 Configured"