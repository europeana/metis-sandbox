#!/usr/bin/env bash
pip install --upgrade pip
pip uninstall awscli
pip install awscli
pip uninstall awscli-local
pip install awscli-local

export AWS_ACCESS_KEY_ID=bT3iWI27KcAQyLQCIOYT AWS_SECRET_ACCESS_KEY=pMDcycDwMnKbLvkqa2Cxb2KJVeU1u67lE7Fb1Ie
awslocal s3api create-bucket --bucket metis-sandbox-bucket
