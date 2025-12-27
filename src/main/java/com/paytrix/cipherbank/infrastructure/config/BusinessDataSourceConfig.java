package com.paytrix.cipherbank.infrastructure.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

/**
 * Business Database Configuration (PRIMARY)
 *
 * Database: cipherbank
 * Contains: bank_statements, bank_profiles, bank_statement_uploads
 * Purpose: Business logic, payment processing, statement parsing
 *
 * Entity Package: com.paytrix.cipherbank.infrastructure.adapter.out.persistence.entity.business
 * Repository Package: com.paytrix.cipherbank.infrastructure.adapter.out.persistence.repository.business
 *
 * @Primary - This is the default database for the application
 */
@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(
        basePackages = "com.paytrix.cipherbank.infrastructure.adapter.out.persistence.repository.business",
        entityManagerFactoryRef = "businessEntityManagerFactory",
        transactionManagerRef = "businessTransactionManager"
)
public class BusinessDataSourceConfig {

    @Value("${spring.jpa.business.hibernate.ddl-auto:update}")
    private String ddlAuto;

    @Value("${spring.jpa.business.hibernate.dialect:org.hibernate.dialect.MySQLDialect}")
    private String dialect;

    @Value("${spring.jpa.business.hibernate.naming.physical-strategy:org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl}")
    private String namingStrategy;

    @Value("${spring.jpa.business.show-sql:true}")
    private String showSql;

    @Value("${spring.jpa.business.format-sql:true}")
    private String formatSql;

    /**
     * Business DataSource (PRIMARY)
     * Configuration from: spring.datasource.business.*
     */
    @Primary
    @Bean(name = "businessDataSource")
    @ConfigurationProperties(prefix = "spring.datasource.business")
    public DataSource businessDataSource() {
        return DataSourceBuilder.create().build();
    }

    /**
     * Business EntityManagerFactory (PRIMARY)
     * Scans entities in: ...entity.business package
     */
    @Primary
    @Bean(name = "businessEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean businessEntityManagerFactory(
            EntityManagerFactoryBuilder builder,
            @Qualifier("businessDataSource") DataSource dataSource) {

        return builder
                .dataSource(dataSource)
                .packages("com.paytrix.cipherbank.infrastructure.adapter.out.persistence.entity.business")
                .persistenceUnit("business")
                .properties(businessJpaProperties())
                .build();
    }

    /**
     * Business TransactionManager (PRIMARY)
     */
    @Primary
    @Bean(name = "businessTransactionManager")
    public PlatformTransactionManager businessTransactionManager(
            @Qualifier("businessEntityManagerFactory") LocalContainerEntityManagerFactoryBean entityManagerFactory) {

        return new JpaTransactionManager(entityManagerFactory.getObject());
    }

    /**
     * JPA properties for business database
     * All values injected from application.yml - NO HARDCODING
     */
    private Map<String, Object> businessJpaProperties() {
        Map<String, Object> properties = new HashMap<>();

        properties.put("hibernate.hbm2ddl.auto", ddlAuto);
        properties.put("hibernate.dialect", dialect);
        properties.put("hibernate.physical_naming_strategy", namingStrategy);
        properties.put("hibernate.show_sql", showSql);
        properties.put("hibernate.format_sql", formatSql);

        return properties;
    }
}