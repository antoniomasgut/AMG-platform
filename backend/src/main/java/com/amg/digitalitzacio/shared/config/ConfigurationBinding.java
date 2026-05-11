package com.amg.digitalitzacio.shared.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({
        JwtProperties.class,
        LoginProperties.class
})
public class ConfigurationBinding {}
