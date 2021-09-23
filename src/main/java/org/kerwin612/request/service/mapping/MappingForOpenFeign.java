package org.kerwin612.request.service.mapping;

import com.netflix.client.config.IClientConfig;
import com.netflix.loadbalancer.ILoadBalancer;
import com.netflix.loadbalancer.reactive.LoadBalancerCommand;
import feign.Request;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.cloud.client.loadbalancer.LoadBalancedRetryFactory;
import org.springframework.cloud.netflix.ribbon.ServerIntrospector;
import org.springframework.cloud.netflix.ribbon.SpringClientFactory;
import org.springframework.cloud.openfeign.ribbon.FeignLoadBalancer;
import org.springframework.cloud.openfeign.ribbon.RetryableFeignLoadBalancer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ConcurrentReferenceHashMap;

import java.util.Collection;
import java.util.Map;

/**
 * Author: kerwin612
 */
@Configuration
@ConditionalOnClass(org.springframework.cloud.openfeign.ribbon.CachingSpringLoadBalancerFactory.class)
public class MappingForOpenFeign {

    @Bean
    @Primary
    @ConditionalOnMissingClass("org.springframework.retry.support.RetryTemplate")
    public org.springframework.cloud.openfeign.ribbon.CachingSpringLoadBalancerFactory cachingLBClientFactory(
            SpringClientFactory factory) {
        return new CachingSpringLoadBalancerFactory(factory);
    }

    @Bean
    @Primary
    @ConditionalOnClass(name = "org.springframework.retry.support.RetryTemplate")
    public org.springframework.cloud.openfeign.ribbon.CachingSpringLoadBalancerFactory retryabeCachingLBClientFactory(
            SpringClientFactory factory,
            LoadBalancedRetryFactory retryFactory) {
        return new CachingSpringLoadBalancerFactory(factory, retryFactory);
    }

    class CachingSpringLoadBalancerFactory extends org.springframework.cloud.openfeign.ribbon.CachingSpringLoadBalancerFactory {

        private volatile Map<String, FeignLoadBalancer> cache = new ConcurrentReferenceHashMap<>();

        public CachingSpringLoadBalancerFactory(SpringClientFactory factory) {
            super(factory);
        }

        public CachingSpringLoadBalancerFactory(SpringClientFactory factory, LoadBalancedRetryFactory loadBalancedRetryPolicyFactory) {
            super(factory, loadBalancedRetryPolicyFactory);
        }

        @Override
        public FeignLoadBalancer create(String clientName) {
            FeignLoadBalancer client = this.cache.get(clientName);
            if (client != null) {
                return client;
            }
            IClientConfig config = this.factory.getClientConfig(clientName);
            ILoadBalancer lb = this.factory.getLoadBalancer(clientName);
            ServerIntrospector serverIntrospector = this.factory.getInstance(clientName, ServerIntrospector.class);
            client = loadBalancedRetryFactory != null ? new RetryableFeignLoadBalancer(lb, config, serverIntrospector,
                    loadBalancedRetryFactory) {
                protected void customizeLoadBalancerCommandBuilder(final FeignLoadBalancer.RibbonRequest request, final IClientConfig config,
                                                                   final LoadBalancerCommand.Builder<FeignLoadBalancer.RibbonResponse> builder) {
                    builder.withServerLocator(getClientIP(request.getRequest()));
                }
            } : new FeignLoadBalancer(lb, config, serverIntrospector) {
                protected void customizeLoadBalancerCommandBuilder(final FeignLoadBalancer.RibbonRequest request, final IClientConfig config,
                                                                   final LoadBalancerCommand.Builder<FeignLoadBalancer.RibbonResponse> builder) {
                    builder.withServerLocator(getClientIP(request.getRequest()));
                }
            };
            this.cache.put(clientName, client);
            return client;
        }

        private String getClientIP(Request request) {
            Collection<String> ips = request.headers().get(RuleConfiguration.ClientIPHeaderUtil.getClientIPHeaderKey());
            return CollectionUtils.isEmpty(ips) ? RuleConfiguration.ClientIPHeaderUtil.getClientIP(request.headers()) : ips.toArray(new String[ips.size()])[0];
        }

    }

}
