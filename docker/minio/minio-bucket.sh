#!/bin/bash

mc alias set minio http://localhost:9000 sandbox metis-sandbox     # setup Minio client
mc mb minio/metis-sandbox-bucket || true                           # create a test bucket
mc admin accesskey create minio/ --access-key bT3iWI27KcAQyLQCIOYT --secret-key pMDcycDwMnKbLvkqa2Cxb2KJVeU1u67lE7Fb1Ie     # create accesskey
