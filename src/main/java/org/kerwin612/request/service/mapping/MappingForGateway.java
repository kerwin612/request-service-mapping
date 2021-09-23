package org.kerwin612.request.service.mapping;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.cloud.gateway.config.LoadBalancerProperties;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.netflix.ribbon.RibbonLoadBalancerClient;
import org.springframework.cloud.netflix.ribbon.SpringClientFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;

import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR;

/**
 * Author: kerwin612
 */
@Configuration
@ConditionalOnClass(org.springframework.cloud.gateway.filter.LoadBalancerClientFilter.class)
public class MappingForGateway {

    @Bean
    @ConditionalOnBean(SpringClientFactory.class)
    public org.springframework.cloud.gateway.filter.LoadBalancerClientFilter loadBalancerClientFilter(SpringClientFactory clientFactory, LoadBalancerProperties properties) {
        return new LoadBalancerClientFilter(new RibbonLoadBalancerClient(clientFactory) {

            public ServiceInstance choose(String serviceId) {
                String[] strings = serviceId.split("<<>>");
                return super.choose(strings[0], strings[1]);
            }

        }, properties);
    }

    class LoadBalancerClientFilter extends org.springframework.cloud.gateway.filter.LoadBalancerClientFilter {

        public LoadBalancerClientFilter(LoadBalancerClient loadBalancer, LoadBalancerProperties properties) {
            super(loadBalancer, properties);
        }

        @Override
        public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
            String clientIP = RuleConfiguration.ClientIPHeaderUtil.getClientIP(exchange.getRequest());
            return super.filter(exchange.mutate().request(exchange.getRequest().mutate()
                    .header(RuleConfiguration.ClientIPHeaderUtil.getClientIPHeaderKey(), clientIP)
                    .build()).build(), chain);
        }

        protected ServiceInstance choose(ServerWebExchange exchange) {
            URI attribute = (URI) exchange.getAttribute(GATEWAY_REQUEST_URL_ATTR);
            return loadBalancer.choose(attribute.getHost() + "<<>>" + RuleConfiguration.ClientIPHeaderUtil.getClientIP(exchange.getRequest()));
        }

    }


}
