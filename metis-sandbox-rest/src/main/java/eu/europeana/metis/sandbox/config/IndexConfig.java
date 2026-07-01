package eu.europeana.metis.sandbox.config;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.ArrayUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import eu.europeana.corelib.solr.bean.impl.FullBeanImpl;
import eu.europeana.indexing.Indexer;
import eu.europeana.indexing.IndexerFactory;
import eu.europeana.indexing.IndexingSettings;
import eu.europeana.indexing.exception.SetupRelatedIndexingException;
import java.net.URI;
import java.net.URISyntaxException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class IndexConfig {

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

  @Value("${sandbox.preview.mongo.application-name:#{null}}")
  private String mongoPreviewApplicationName;

  @Value("${sandbox.preview.mongo.max-connection-pool-size:#{null}}")
  private Integer mongoPreviewMaxConnectionPoolSize;

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

  @Value("${sandbox.preview.solr.useHttp1:#{null}}")
  private Boolean solrUseHttp1;

  @Bean
  Indexer<FullBeanImpl> indexerPreview() throws URISyntaxException, SetupRelatedIndexingException {
    return getIndexer(mongoPreviewHosts, mongoPreviewPorts, mongoPreviewDb,
        mongoPreviewAuthenticationDb,
        mongoPreviewUsername, mongoPreviewPassword, mongoPreviewEnableSSL, mongoPreviewApplicationName,
        mongoPreviewMaxConnectionPoolSize, solrPreviewHosts,
        zookeeperPreviewHosts, zookeeperPreviewPorts, zookeeperPreviewChroot,
        zookeeperPreviewDefaultCollection, zookeeperPreviewTimeoutInSecs, solrUseHttp1
    );
  }

  //todo: this class should use configuration properties from metis-common-spring-properties
  //Suppress: Methods should not have too many parameters warning
  //We are okay with this method to ease configuration
  @SuppressWarnings("squid:S107")
  private Indexer<FullBeanImpl> getIndexer(String[] mongoHosts, int[] mongoPorts, String mongoDb,
      String mongoAuthenticationDb, String mongoUsername, String mongoPassword,
      Boolean mongoEnableSSL, String mongoApplicationName, Integer mongoMaxConnectionPoolSize,
      String[] solrHosts, String[] zookeeperHosts, int[] zookeeperPorts, String zookeeperChroot,
      String zookeeperDefaultCollection, Integer zookeeperTimeoutInSecs, boolean  solrUseHttp1)
      throws SetupRelatedIndexingException, URISyntaxException {
    checkArgument(isNotBlank(mongoDb), "Mongo db must be provided");
    checkArgument(isNotEmpty(mongoHosts), "Mongo hosts must be provided ");
    checkArgument(isNotEmpty(mongoPorts), "Mongo ports must be provided ");
    checkArgument(isNotEmpty(solrHosts), "Solr hosts must be provided ");

    var settings = new IndexingSettings();

    // Set the Mongo properties
    settings.getMongoProperties().setAllProperties(mongoHosts, mongoPorts,
        mongoAuthenticationDb, mongoUsername, mongoPassword, Boolean.TRUE.equals(mongoEnableSSL),
        null, mongoApplicationName);
    settings.setMongoDatabaseName(mongoDb);
    ofNullable(mongoMaxConnectionPoolSize).ifPresent(settings::setMongoMaxConnectionPoolSize);

    // Set Solr properties
    for (String host : solrHosts) {
      settings.addSolrHost(new URI(host));
    }

    // Set Zookeeper properties
    if (isNotEmpty(zookeeperHosts) && isNotEmpty(zookeeperPorts)) {
      settings.getSolrProperties().setZookeeperHosts(zookeeperHosts, zookeeperPorts);
    }
    settings.getSolrProperties().setSolrUseHttp1(solrUseHttp1);
    if (isNotBlank(zookeeperChroot)) {
      settings.setZookeeperChroot(zookeeperChroot);
    }
    if (isNotBlank(zookeeperDefaultCollection)) {
      settings.setZookeeperDefaultCollection(zookeeperDefaultCollection);
    }
    if (nonNull(zookeeperTimeoButInSecs)) {
      settings.setZookeeperTimeoutInSecs(zookeeperTimeoutInSecs);
    }

    return IndexerFactory.create(settings).getIndexer();
  }
}
