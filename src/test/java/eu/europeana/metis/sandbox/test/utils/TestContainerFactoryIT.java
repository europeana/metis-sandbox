package eu.europeana.metis.sandbox.test.utils;

public class TestContainerFactoryIT {

  public static TestContainer getContainer(TestContainerType type) {
    return switch (type) {
      case MONGO -> new MongoDBContainerIT();
      case POSTGRES -> new PostgreSQLContainerIT();
      case RABBITMQ -> new RabbitMQContainerIT();
      case SOLR -> new SolrContainerIT();
      case S3 -> new S3ContainerIT();
    };
  }
}
