package com.tuiken.mamlakat.config;

import org.neo4j.driver.Driver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.core.DatabaseSelection;
import org.springframework.data.neo4j.core.DatabaseSelectionProvider;
import org.springframework.data.neo4j.core.transaction.Neo4jBookmarkManager;
import org.springframework.data.neo4j.core.transaction.Neo4jTransactionManager;
import org.springframework.data.neo4j.repository.config.Neo4jRepositoryConfigurationExtension;

import java.util.Objects;

@Configuration
public class AppConfig {

@ConditionalOnMissingBean(name = Neo4jRepositoryConfigurationExtension.DEFAULT_TRANSACTION_MANAGER_BEAN_NAME)
@Bean(name = "transactionManager")
public Neo4jTransactionManager neo4jTransactionManager(@Autowired Driver driver,
                                                       @Autowired DatabaseSelectionProvider databaseSelectionProvider,
                                                       @Autowired(required = false) Neo4jBookmarkManager neo4jBookmarkManager) {
    return new Neo4jTransactionManager(driver, databaseSelectionProvider,
            Objects.requireNonNullElseGet(neo4jBookmarkManager, Neo4jBookmarkManager::create));
}

    @Bean
    DatabaseSelectionProvider databaseSelectionProvider() {
        return () -> DatabaseSelection.byName("neo4j");
    }
}
