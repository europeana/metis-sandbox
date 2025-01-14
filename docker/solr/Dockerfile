FROM solr:7.7.3-slim
USER 0
RUN apt-get update  \
    && apt-get install git -y  \
    && apt-get install rsync -y  \
    && apt-get install curl -y  \
    && apt-get install nano -y  \
    && apt-get clean \
    && git clone https://github.com/europeana/search
COPY solr-schema.sh /opt/solr/search
COPY europeana-collection-aliasing-solr6.6.5-0.0.1-SNAPSHOT.jar /opt/solr/contrib/lib/europeana-collection-aliasing-solr6.6.5-0.0.1-SNAPSHOT.jar
RUN chown -R solr:solr /opt/solr/search  \
    && chown -R solr:solr /opt/solr/contrib/lib \
    && chmod ug+x /opt/solr/search/solr-schema.sh
USER solr
RUN solr start -c \
    && solr create_collection -c metis_sandbox_publish_local -p 8983  \
    && solr stop \
    && mkdir -p /opt/solr/server/solr/metis_sandbox_publish_local_shard1_replica_n1/conf \
    && cp /opt/solr/search/solr_confs/metadata/conf/query_aliases.xml /opt/solr/server/solr/metis_sandbox_publish_local_shard1_replica_n1/conf/query_aliases.xml \
    && solr start -c \
    && cd /opt/solr/search && /opt/solr/search/solr-schema.sh \
    && solr stop
