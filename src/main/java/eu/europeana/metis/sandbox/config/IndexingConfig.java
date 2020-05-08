package eu.europeana.metis.sandbox.config;

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

  @Value("${sandbox.indexing.mongo.authentication-db}")
  private String mongoAuthenticationDb;

  @Value("${sandbox.indexing.mongo.username}")
  private String mongoUsername;

  @Value("${sandbox.indexing.mongo.password}")
  private String mongoPassword;

  @Value("${sandbox.indexing.mongo.ssl-enable}")
  private boolean mongoEnableSSL;

  @Value("${sandbox.indexing.mongo.db}")
  private String mongoDb;

  @Value("${sandbox.indexing.solr.hosts}")
  private String[] solrHosts;

  @Value("${sandbox.indexing.solr.zookeeper.hosts}")
  private String[] zookeeperHosts;

  @Value("${sandbox.indexing.solr.zookeeper.ports}")
  private int[] zookeeperPorts;

  @Value("${sandbox.indexing.solr.zookeeper.chroot}")
  private String zookeeperChroot;

  @Value("${sandbox.indexing.solr.zookeeper.default.collection}")
  private String zookeeperDefaultCollection;

  @Value("${sandbox.indexing.solr.zookeeper.timeout}")
  private int zookeeperTimeoutInSecs;

  @Bean
  Indexer indexer()
      throws URISyntaxException, SetupRelatedIndexingException, IndexerRelatedIndexingException {
    // Create the indexing settings
    var settings = new IndexingSettings();

    // Set the Mongo properties
    settings.getMongoProperties().setAllProperties(mongoHosts, mongoPorts,
        mongoAuthenticationDb, mongoUsername, mongoPassword, mongoEnableSSL);
    settings.setMongoDatabaseName(mongoDb);

    // Set Solr properties
    for (String host : solrHosts) {
      settings.addSolrHost(new URI(host));
    }

    // Set Zookeeper properties
    if(isNotEmpty(zookeeperHosts) && isNotEmpty(zookeeperPorts)) {
      settings.getSolrProperties().setZookeeperHosts(zookeeperHosts, zookeeperPorts);
    }
    if (isNotBlank(zookeeperChroot)) {
      settings.setZookeeperChroot(zookeeperChroot);
    }
    if (isNotBlank(zookeeperDefaultCollection)) {
      settings.setZookeeperDefaultCollection(zookeeperDefaultCollection);
    }
    settings.setZookeeperTimeoutInSecs(zookeeperTimeoutInSecs);

    IndexerFactory factory = new IndexerFactory(settings);
    Indexer indexer = factory.getIndexer();
    return indexer;
  }
}
