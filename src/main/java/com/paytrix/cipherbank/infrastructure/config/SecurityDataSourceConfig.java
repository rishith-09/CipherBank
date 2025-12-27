package com.paytrix.cipherbank.infrastructure.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

/**
 * Security Database Configuration
 *
 * Database: cipherbank_security
 * Contains: users, roles, user_roles, ip_whitelist
 * Purpose: Authentication, authorization, IP whitelisting
 *
 * Entity Package: com.paytrix.cipherbank.infrastructure.adapter.out.persistence.entity.security
 * Repository Package: com.paytrix.cipherbank.infrastructure.adapter.out.persistence.repository.security
 */
@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(
        basePackages = "com.paytrix.cipherbank.infrastructure.adapter.out.persistence.repository.security",
        entityManagerFactoryRef = "securityEntityManagerFactory",
        transactionManagerRef = "securityTransactionManager"
)
public class SecurityDataSourceConfig {

    @Value("${spring.jpa.security.hibernate.ddl-auto:update}")
    private String ddlAuto;

    @Value("${spring.jpa.security.hibernate.dialect:org.hibernate.dialect.MySQLDialect}")
    private String dialect;

    @Value("${spring.jpa.security.hibernate.naming.physical-strategy:org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl}")
    private String namingStrategy;

    @Value("${spring.jpa.security.show-sql:true}")
    private String showSql;

    @Value("${spring.jpa.security.format-sql:true}")
    private String formatSql;

    /**
     * Security DataSource
     * Configuration from: spring.datasource.security.*
     */
    @Bean(name = "securityDataSource")
    @ConfigurationProperties(prefix = "spring.datasource.security")
    public DataSource securityDataSource() {
        return DataSourceBuilder.create().build();
    }

    /**
     * Security EntityManagerFactory
     * Scans entities in: ...entity.security package
     */
    @Bean(name = "securityEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean securityEntityManagerFactory(
            EntityManagerFactoryBuilder builder,
            @Qualifier("securityDataSource") DataSource dataSource) {

        return builder
                .dataSource(dataSource)
                .packages("com.paytrix.cipherbank.infrastructure.adapter.out.persistence.entity.security")
                .persistenceUnit("security")
                .properties(securityJpaProperties())
                .build();
    }

    /**
     * Security TransactionManager
     */
    @Bean(name = "securityTransactionManager")
    public PlatformTransactionManager securityTransactionManager(
            @Qualifier("securityEntityManagerFactory") LocalContainerEntityManagerFactoryBean entityManagerFactory) {

        return new JpaTransactionManager(entityManagerFactory.getObject());
    }

    /**
     * JPA properties for security database
     * All values injected from application.yml - NO HARDCODING
     */
    private Map<String, Object> securityJpaProperties() {
        Map<String, Object> properties = new HashMap<>();

        properties.put("hibernate.hbm2ddl.auto", ddlAuto);
        properties.put("hibernate.dialect", dialect);
        properties.put("hibernate.physical_naming_strategy", namingStrategy);
        properties.put("hibernate.show_sql", showSql);
        properties.put("hibernate.format_sql", formatSql);

        return properties;
    }
}