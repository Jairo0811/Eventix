package com.jairomatias.eventix.config;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.testcontainers.containers.MSSQLServerContainer;
import org.testcontainers.utility.DockerImageName;

@Configuration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    private static final DockerImageName SQL_SERVER_IMAGE =
            DockerImageName.parse(
                    "mcr.microsoft.com/mssql/server:2022-CU20-ubuntu-22.04");

    @Bean
    @ServiceConnection
    MSSQLServerContainer<?> sqlServerContainer() {
        return new MSSQLServerContainer<>(SQL_SERVER_IMAGE)
                .acceptLicense();
    }
}
