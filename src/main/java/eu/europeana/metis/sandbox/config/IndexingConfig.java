package eu.europeana.metis.sandbox.config;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.ArrayUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import eu.europeana.indexing.Indexer;
import eu.europeana.indexing.IndexerFactory;
import eu.europeana.indexing.IndexingSettings;
import eu.europeana.indexing.exception.IndexerRelatedIndexingException;
import eu.europeana.indexing.exception.SetupRelatedIndexingException;
import java.net.URI;
import java.net.URISyntaxException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class IndexingConfig {

  @Value("${sandbox.indexing.mongo.hosts}")
  private String[] mongoHosts;

  @Value("${sandbox.indexing.mongo.ports}")
  private int[] mongoPorts;

  @Value("${sandbox.indexing.mongo.authentication-db:#{null}}")
  private String mongoAuthenticationDb;

  @Value("${sandbox.indexing.mongo.username:#{null}}")
  private String mongoUsername;

  @Value("${sandbox.indexing.mongo.password:#{null}}")
  private String mongoPassword;

  @Value("${sandbox.indexing.mongo.ssl-enable:#{null}}")
  private Boolean mongoEnableSSL;

  @Value("${sandbox.indexing.mongo.db}")
  private String mongoDb;

  @Value("${sandbox.indexing.solr.hosts}")
  private String[] solrHosts;

  @Value("${sandbox.indexing.solr.zookeeper.hosts:#{null}}")
  private String[] zookeeperHosts;

  @Value("${sandbox.indexing.solr.zookeeper.ports:#{null}}")
  private int[] zookeeperPorts;

  @Value("${sandbox.indexing.solr.zookeeper.chroot:#{null}}")
  private String zookeeperChroot;

  @Value("${sandbox.indexing.solr.zookeeper.default.collection:#{null}}")
  private String zookeeperDefaultCollection;

  @Value("${sandbox.indexing.solr.zookeeper.timeout:#{null}}")
  private Integer zookeeperTimeoutInSecs;

  @Bean
  Indexer indexer()
      throws URISyntaxException, SetupRelatedIndexingException, IndexerRelatedIndexingException {
    checkArgument(isNotBlank(mongoDb), "Mongo db must be provided");
    checkArgument(isNotEmpty(mongoHosts), "Mongo hosts must be provided ");
    checkArgument(isNotEmpty(mongoPorts), "Mongo ports must be provided ");
    checkArgument(isNotEmpty(solrHosts), "Solr hosts must be provided ");

    var settings = new IndexingSettings();

    if (isNull(mongoEnableSSL)) {
      mongoEnableSSL = Boolean.FALSE;
    }

    // Set the Mongo properties
    settings.getMongoProperties().setAllProperties(mongoHosts, mongoPorts,
        mongoAuthenticationDb, mongoUsername, mongoPassword, mongoEnableSSL, null);
    settings.setMongoDatabaseName(mongoDb);

    // Set Solr properties
    for (String host : solrHosts) {
      settings.addSolrHost(new URI(host));
    }

    // Set Zookeeper properties
    if (isNotEmpty(zookeeperHosts) && isNotEmpty(zookeeperPorts)) {
      settings.getSolrProperties().setZookeeperHosts(zookeeperHosts, zookeeperPorts);
    }
    if (isNotBlank(zookeeperChroot)) {
      settings.setZookeeperChroot(zookeeperChroot);
    }
    if (isNotBlank(zookeeperDefaultCollection)) {
      settings.setZookeeperDefaultCollection(zookeeperDefaultCollection);
    }
    if (nonNull(zookeeperTimeoutInSecs)) {
      settings.setZookeeperTimeoutInSecs(zookeeperTimeoutInSecs);
    }

    return new IndexerFactory(settings).getIndexer();
  }
}
