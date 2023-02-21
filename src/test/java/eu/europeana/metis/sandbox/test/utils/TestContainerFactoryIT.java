package eu.europeana.metis.sandbox.test.utils;

public class TestContainerFactoryIT {

  public static TestContainer getContainer(TestContainerType type) {
    switch(type) {
      case MONGO:
        return new MongoDBContainerIT();
      case POSTGRES:
        return new PostgreSQLContainerIT();
      case RABBITMQ:
        return new RabbitMQContainerIT();
      case SOLR:
        return new SolrContainerIT();
      case S3:
        return new S3ContainerIT();
      default:
        throw new IllegalArgumentException("Pass a valid container type");
    }
  }
}
