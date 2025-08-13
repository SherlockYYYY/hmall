package com.hmall.gateway.filter;

import com.hmall.common.exception.UnauthorizedException;
import com.hmall.gateway.config.AuthProperties;
import com.hmall.gateway.utils.JwtTool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
public class AuthGlobalFilter implements GlobalFilter, Ordered {

    @Autowired
    private AuthProperties authProperties;

    @Autowired
    private JwtTool jwtTool;

    private AntPathMatcher antPathMatcher = new AntPathMatcher();
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        //1.获取request对象
        ServerHttpRequest request = exchange.getRequest();
        //2.判断是否需要登录拦截
        if(isExclude( request.getPath().toString())){
            return chain.filter(exchange);
        }
        //3.获取token
        List<String> headers = request.getHeaders().get("Authorization");
        String token = null;
        //3.1判断是否为空
        if(headers != null && !headers.isEmpty()){
            token = headers.get(0);
        }
        //4.校验并解析token
        Long userId;
        try{
            userId = jwtTool.parseToken(token);
        }catch (UnauthorizedException e){
            ServerHttpResponse response = exchange.getResponse();
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return response.setComplete();
        }
        //5.如果校验通过，将用户信息设置到request对象中
        String userInfo = userId.toString();
        ServerWebExchange swe = exchange.mutate().request(builder -> builder.header("user-info", userInfo))
                .build();
        //6.放行
        return chain.filter(swe);
    }
    private boolean isExclude(String path){
        for (String excludePath : authProperties.getExcludePaths()) {
            if(antPathMatcher.match(excludePath,path)){
                return true;
            }
        }
        return false;
    }
    @Override
    public int getOrder() {
        return 0;
    }
}
