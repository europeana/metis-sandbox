#!/bin/bash
echo "Starting Solr in cloud mode (embedded ZK)..."
solr start --cloud --foreground &
SOLR_PID=$!
echo "Waiting for Solr..."
until curl -s http://localhost:8983/solr/admin/info/system >/dev/null; do
  sleep 2
done

echo "Checking if collection exists..."
if curl --fail --silent "http://localhost:8983/solr/admin/collections?action=LIST&wt=json" \
 | grep --quiet "metis_sandbox_publish_local"; then
  echo "Collection exists"
else
  echo "Creating collection..."
  solr create --name metis_sandbox_publish_local --shards 1 --replication-factor 1
  echo "Solr collection is created."
  solr stop --all
  mkdir --parents /var/solr/data/metis_sandbox_publish_local_shard1_replica_n1/conf
  cp /opt/solr/search/solr_confs/metadata/conf/query_aliases.xml /var/solr/data/metis_sandbox_publish_local_shard1_replica_n1/conf/query_aliases.xml
  bash -c "cd /opt/solr/search && ./solr-schema.sh"
fi
echo "Solr is ready."
wait $SOLR_PID

