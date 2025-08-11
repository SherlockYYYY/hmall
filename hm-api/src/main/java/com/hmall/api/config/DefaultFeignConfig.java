package com.hmall.api.config;

import org.springframework.context.annotation.Bean;

import feign.Logger;

public class DefaultFeignConfig {
    @Bean
    public Logger.Level feignLoggerLevel() {
        return Logger.Level.FULL;
    }
}
