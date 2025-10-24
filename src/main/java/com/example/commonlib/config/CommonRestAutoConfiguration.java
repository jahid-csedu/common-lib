package com.example.commonlib.config;

import com.example.commonlib.client.CommonRestClient;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@EnableConfigurationProperties(RestClientProperties.class)
public class CommonRestAutoConfiguration {

    @Bean
    public CommonRestClient commonRestClient(RestClientProperties properties) {
        return new CommonRestClient(properties);
    }
}
