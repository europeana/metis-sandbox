#!/bin/bash
echo "Starting Solr in cloud mode (embedded ZK)..."
solr start -c -f &
SOLR_PID=$!
echo "Waiting for Solr..."
until curl -s http://localhost:8983/solr/admin/info/system >/dev/null; do
  sleep 2
done

echo "Checking if collection exists..."
if curl -fs "http://localhost:8983/solr/admin/collections?action=LIST&wt=json" \
 | grep -q "metis_sandbox_publish_local"; then
  echo "Collection exists"
else
  echo "Creating collection..."
  solr create -c metis_sandbox_publish_local -sh 1 -rf 1
  echo "Solr collection is created."
  solr stop --all
  mkdir -p /var/solr/data/metis_sandbox_publish_local_shard1_replica_n1/conf
  cp /opt/solr/search/solr_confs/metadata/conf/query_aliases.xml /var/solr/data/metis_sandbox_publish_local_shard1_replica_n1/conf/query_aliases.xml
 # solr start -f &
 # SOLR_PID=$!
  bash -c "cd /opt/solr/search && ./solr-schema.sh"
fi
echo "Solr is ready."
wait $SOLR_PID

