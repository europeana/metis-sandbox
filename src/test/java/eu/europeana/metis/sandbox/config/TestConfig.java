package eu.europeana.metis.sandbox.config;

import static org.mockito.Mockito.mock;

import com.github.fridujo.rabbitmq.mock.MockConnectionFactory;
import eu.europeana.corelib.definitions.jibx.RDF;
import eu.europeana.indexing.Indexer;
import eu.europeana.indexing.exception.IndexingException;
import eu.europeana.metis.sandbox.service.dataset.AsyncDatasetPublishService;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.List;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

@Profile("test")
@Configuration
public class TestConfig {

  @Bean
  ConnectionFactory connectionFactory() {
    return new CachingConnectionFactory(new MockConnectionFactory());
  }

  @Bean
  @Primary
  public AsyncDatasetPublishService publishService() {
    return mock(AsyncDatasetPublishService.class);
  }

  @Bean
  Indexer indexer() {
    return new IndexerImpl();
  }

  private static class IndexerImpl implements Indexer {

    @Override
    public void indexRdf(RDF rdf, Date date, boolean b, List<String> list, boolean b1)
        throws IndexingException {

    }

    @Override
    public void indexRdfs(List<RDF> list, Date date, boolean b, List<String> list1, boolean b1)
        throws IndexingException {

    }

    @Override
    public void index(String s, Date date, boolean b, List<String> list, boolean b1)
        throws IndexingException {

    }

    @Override
    public void index(List<String> list, Date date, boolean b, List<String> list1, boolean b1)
        throws IndexingException {

    }

    @Override
    public void index(InputStream inputStream, Date date, boolean b, List<String> list, boolean b1)
        throws IndexingException {

    }

    @Override
    public void triggerFlushOfPendingChanges(boolean b) throws IndexingException {

    }

    @Override
    public boolean remove(String s) throws IndexingException {
      return false;
    }

    @Override
    public int removeAll(String s, Date date) throws IndexingException {
      return 0;
    }

    @Override
    public void close() throws IOException {

    }
  }
}
