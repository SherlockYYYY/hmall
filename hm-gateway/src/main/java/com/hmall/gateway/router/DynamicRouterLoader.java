package com.hmall.gateway.router;

import cn.hutool.json.JSONUtil;
import com.alibaba.cloud.nacos.NacosConfigManager;
import com.alibaba.nacos.api.config.listener.Listener;
import com.alibaba.nacos.api.exception.NacosException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionWriter;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;

@Slf4j
@Component
public class DynamicRouterLoader {

    @Autowired
    NacosConfigManager nacosConfigManager;

    @Autowired
    RouteDefinitionWriter routeDefinitionWriter;

    private final String dataId = "gateway-router.json";
    private final String group = "DEFAULT_GROUP";
    private final Set<String> routeIds = new HashSet<>();

    @PostConstruct // 这个注解告诉 Spring：请执行这个方法，当这个类被实例化之后，就执行这个方法
    public void initRouterConfigListener() throws NacosException {
        // 获取配置信息
        String configInfo = nacosConfigManager.getConfigService().getConfigAndSignListener(dataId, group, 5000, new Listener() {
            @Override
            public Executor getExecutor() {
                return null;
            }

            @Override
            public void receiveConfigInfo(String s) {  //当配置变更时做的事情
                updateConfigInfo(s);
            }


        });
        updateConfigInfo(configInfo);
    }

    public void updateConfigInfo(String configInfo) {
        log.info("监听到路由配置信息：{}", configInfo);

        //解析配置文件，转为ROUTER DEFINITION
        List<RouteDefinition> routeDefinitions = JSONUtil.toList(configInfo, RouteDefinition.class);
        //删除旧的路由表
        for (String routeId : routeIds) {
            routeDefinitionWriter.delete(Mono.just(routeId)).subscribe();
        }

        routeIds.clear();
        // 1. 遍历ROUTER DEFINITION，保存到路由表中
        for (RouteDefinition routeDefinition : routeDefinitions) {
            routeDefinitionWriter.save(Mono.just(routeDefinition)).subscribe();
            routeIds.add(routeDefinition.getId());
        }
    }
}
