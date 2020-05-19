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

  @Value("${sandbox.preview.mongo.hosts}")
  private String[] mongoPreviewHosts;

  @Value("${sandbox.preview.mongo.ports}")
  private int[] mongoPreviewPorts;

  @Value("${sandbox.preview.mongo.authentication-db:#{null}}")
  private String mongoPreviewAuthenticationDb;

  @Value("${sandbox.preview.mongo.username:#{null}}")
  private String mongoPreviewUsername;

  @Value("${sandbox.preview.mongo.password:#{null}}")
  private String mongoPreviewPassword;

  @Value("${sandbox.preview.mongo.ssl-enable:#{null}}")
  private Boolean mongoPreviewEnableSSL;

  @Value("${sandbox.preview.mongo.db}")
  private String mongoPreviewDb;

  @Value("${sandbox.preview.solr.hosts}")
  private String[] solrPreviewHosts;

  @Value("${sandbox.preview.solr.zookeeper.hosts:#{null}}")
  private String[] zookeeperPreviewHosts;

  @Value("${sandbox.preview.solr.zookeeper.ports:#{null}}")
  private int[] zookeeperPreviewPorts;

  @Value("${sandbox.preview.solr.zookeeper.chroot:#{null}}")
  private String zookeeperPreviewChroot;

  @Value("${sandbox.preview.solr.zookeeper.default.collection:#{null}}")
  private String zookeeperPreviewDefaultCollection;

  @Value("${sandbox.preview.solr.zookeeper.timeout:#{null}}")
  private Integer zookeeperPreviewTimeoutInSecs;

  @Value("${sandbox.publish.mongo.hosts}")
  private String[] mongoPublishHosts;

  @Value("${sandbox.publish.mongo.ports}")
  private int[] mongoPublishPorts;

  @Value("${sandbox.publish.mongo.authentication-db:#{null}}")
  private String mongoPublishAuthenticationDb;

  @Value("${sandbox.publish.mongo.username:#{null}}")
  private String mongoPublishUsername;

  @Value("${sandbox.publish.mongo.password:#{null}}")
  private String mongoPublishPassword;

  @Value("${sandbox.publish.mongo.ssl-enable:#{null}}")
  private Boolean mongoPublishEnableSSL;

  @Value("${sandbox.publish.mongo.db}")
  private String mongoPublishDb;

  @Value("${sandbox.publish.solr.hosts}")
  private String[] solrPublishHosts;

  @Value("${sandbox.publish.solr.zookeeper.hosts:#{null}}")
  private String[] zookeeperPublishHosts;

  @Value("${sandbox.publish.solr.zookeeper.ports:#{null}}")
  private int[] zookeeperPublishPorts;

  @Value("${sandbox.publish.solr.zookeeper.chroot:#{null}}")
  private String zookeeperPublishChroot;

  @Value("${sandbox.publish.solr.zookeeper.default.collection:#{null}}")
  private String zookeeperPublishDefaultCollection;

  @Value("${sandbox.publish.solr.zookeeper.timeout:#{null}}")
  private Integer zookeeperPublishTimeoutInSecs;

  @Bean
  Indexer previewIndexer()
      throws URISyntaxException, SetupRelatedIndexingException, IndexerRelatedIndexingException {
    return getIndexer(mongoPreviewHosts, mongoPreviewPorts, mongoPreviewDb,
        mongoPreviewAuthenticationDb,
        mongoPreviewUsername, mongoPreviewPassword, mongoPreviewEnableSSL, solrPreviewHosts,
        zookeeperPreviewHosts, zookeeperPreviewPorts, zookeeperPreviewChroot,
        zookeeperPreviewDefaultCollection, zookeeperPreviewTimeoutInSecs);
  }

  @Bean
  Indexer publishIndexer()
      throws URISyntaxException, SetupRelatedIndexingException, IndexerRelatedIndexingException {
    return getIndexer(mongoPublishHosts, mongoPublishPorts, mongoPublishDb,
        mongoPublishAuthenticationDb,
        mongoPublishUsername, mongoPublishPassword, mongoPublishEnableSSL, solrPublishHosts,
        zookeeperPublishHosts, zookeeperPublishPorts, zookeeperPublishChroot,
        zookeeperPublishDefaultCollection, zookeeperPublishTimeoutInSecs);
  }

  //Suppress: Methods should not have too many parameters warning
  //We are okay with this method to ease configuration
  @SuppressWarnings("squid:S107")
  private Indexer getIndexer(String[] mongoHosts, int[] mongoPorts, String mongoDb,
      String mongoAuthenticationDb, String mongoUsername, String mongoPassword,
      Boolean mongoEnableSSL, String[] solrHosts, String[] zookeeperHosts, int[] zookeeperPorts,
      String zookeeperChroot, String zookeeperDefaultCollection, Integer zookeeperTimeoutInSecs)
      throws SetupRelatedIndexingException, URISyntaxException, IndexerRelatedIndexingException {
    checkArgument(isNotBlank(mongoDb), "Mongo db must be provided");
    checkArgument(isNotEmpty(mongoHosts), "Mongo hosts must be provided ");
    checkArgument(isNotEmpty(mongoPorts), "Mongo ports must be provided ");
    checkArgument(isNotEmpty(solrHosts), "Solr hosts must be provided ");

    var settings = new IndexingSettings();

    boolean enableSSL = !isNull(mongoEnableSSL) && mongoEnableSSL;

    // Set the Mongo properties
    settings.getMongoProperties().setAllProperties(mongoHosts, mongoPorts,
        mongoAuthenticationDb, mongoUsername, mongoPassword, enableSSL, null);
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
