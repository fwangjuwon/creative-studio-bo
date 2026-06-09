package com.nsmall.ns_creative_studio_bo.common.config;

import com.zaxxer.hikari.HikariDataSource;
import java.util.HashMap;
import java.util.Map;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.datasource.LazyConnectionDataSourceProxy;

/**
 * projectName  : ns-creative-studio-bo
 * packageName  : com.nsmall.ns_creative_studio_bo.common.config
 * fileName     : DataSourceConfig
 * usage        : Postgres HA 구성에 따른 데이터소스 설정
 * author       : bbbtw21@calltech.kr
 * since        : 2026-06-08
 * history
 * ==================================================================
 * DATE             AUTHOR                      DESC.
 * ==================================================================
 * 2026-06-08      bbbtw21@calltech.kr          생성
 */
@Profile("prd")
@Configuration
public class DataSourceConfig {

    @Bean
    @ConfigurationProperties(prefix = "spring.datasource.primary")
    public DataSource primaryDataSource() {
        return DataSourceBuilder.create()
                                .type(HikariDataSource.class)
                                .build();
    }

    @Bean
    @ConfigurationProperties(prefix = "spring.datasource.secondary")
    public DataSource secondaryDataSource() {
        return DataSourceBuilder.create()
                                .type(HikariDataSource.class)
                                .build();
    }

    @Bean
    public DataSource routingDataSource(@Qualifier("primaryDataSource") DataSource primaryDataSource,
                                        @Qualifier("secondaryDataSource") DataSource secondaryDataSource) {

        RoutingDataSource routingDataSource = new RoutingDataSource();

        Map<Object, Object> dataSourceMap = new HashMap<>();
        dataSourceMap.put("primary", primaryDataSource);
        dataSourceMap.put("secondary", secondaryDataSource);

        routingDataSource.setTargetDataSources(dataSourceMap);
        routingDataSource.setDefaultTargetDataSource(primaryDataSource);

        return routingDataSource;
    }

    @Bean
    @Primary
    public DataSource dataSource(@Qualifier("routingDataSource") DataSource routingDataSource) {
        // 쿼리가 실행되는 시점에 커넥션 획득
        return new LazyConnectionDataSourceProxy(routingDataSource);
    }
}
