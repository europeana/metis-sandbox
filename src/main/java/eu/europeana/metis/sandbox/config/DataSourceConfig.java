package eu.europeana.metis.sandbox.config;

import eu.europeana.metis.utils.CustomTruststoreAppender;
import eu.europeana.metis.utils.CustomTruststoreAppender.TrustStoreConfigurationException;
import jakarta.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.TransactionException;
import org.postgresql.util.PSQLException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.support.DefaultTransactionStatus;

/**
 * Configures the datasource, ensures that the custom trust store is appended before creating the
 * datasource
 */
@Configuration
class DataSourceConfig {

    @Value("${sandbox.truststore.path}")
    private String trustStorePath;

    @Value("${sandbox.truststore.password}")
    private String trustStorePassword;

    @Bean
    @ConfigurationProperties(prefix = "sandbox.datasource")
    public DataSource getDataSource() throws TrustStoreConfigurationException {
        appendCustomTrustStore();
        return DataSourceBuilder.create().build();
    }

    @Bean
    public JpaTransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory) {
            @Override
            protected void doCommit(DefaultTransactionStatus status) {
                try {
                    super.doCommit(status);
                } catch (JpaSystemException jpaSystemException) {
                    transformToTransactionSystemExceptionIfNeeded(jpaSystemException);
                }
            }
        };
    }

    private static void transformToTransactionSystemExceptionIfNeeded(JpaSystemException jpaSystemException) {
        Throwable firstLevelThrowable = jpaSystemException.getCause();
        // Check if the transaction was unable to commit against JDBC Connection.
        if (firstLevelThrowable instanceof TransactionException) {
            Throwable secondLevelThrowable = firstLevelThrowable.getCause();
            // Check if it could not serialize access due to read/write dependencies among transactions (SQL 40001)
            // Detail: Reason code: Canceled on identification as a pivot, during commit attempt.
            // Hint: The transaction might succeed if retried when TransactionSystemException is raised.
            if (secondLevelThrowable instanceof PSQLException
                    && "40001".equals(((PSQLException) secondLevelThrowable).getSQLState())) {
                throw new TransactionSystemException(firstLevelThrowable.getMessage(), firstLevelThrowable);
            }
        }
        throw jpaSystemException;
    }

    private void appendCustomTrustStore()
            throws TrustStoreConfigurationException {
        if (StringUtils.isNotEmpty(trustStorePath) && StringUtils.isNotEmpty(trustStorePassword)) {
            CustomTruststoreAppender.appendCustomTruststoreToDefault(trustStorePath, trustStorePassword);
        }
    }
}
