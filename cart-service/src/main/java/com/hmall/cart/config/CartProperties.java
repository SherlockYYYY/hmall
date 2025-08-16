package com.hmall.cart.config;

import io.swagger.models.auth.In;
import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties("hm.cart")
public class CartProperties {
    private Integer maxItems;

}
