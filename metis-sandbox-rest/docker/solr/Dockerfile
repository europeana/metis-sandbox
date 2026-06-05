FROM solr:9.10.1-slim
USER root
RUN apt-get update  \
    && apt-get install git -y  \
    && apt-get install rsync -y  \
    && apt-get install curl -y  \
    && apt-get install nano -y  \
    && apt-get clean \
    && git clone https://github.com/europeana/search
COPY solr-schema.sh /opt/solr/search
COPY searchcomponent-alias-1.0-SNAPSHOT.jar /opt/solr/lib/searchcomponent-alias-1.0-SNAPSHOT.jar
RUN chown -R solr:solr /opt/solr/
RUN chmod ug+x /opt/solr/search/solr-schema.sh
# Add entrypoint script
COPY solr-entrypoint.sh /solr-entrypoint.sh
RUN mkdir -p /var/solr/data
RUN chown -R solr:solr /var/solr
RUN chmod ug+x /solr-entrypoint.sh
RUN chown solr:solr /solr-entrypoint.sh
USER solr
EXPOSE 8983
CMD ["bash","-c","/solr-entrypoint.sh"]


