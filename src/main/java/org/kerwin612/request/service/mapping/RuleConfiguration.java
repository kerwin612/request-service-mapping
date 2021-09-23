package org.kerwin612.request.service.mapping;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

/**
 * Author: kerwin612
 */
@Repository
@RefreshScope
public class RuleConfiguration {

    private static final String CONFIG_NS = "request.service.mapping";

    @Autowired
    private ConfigurableEnvironment configurableEnvironment;

    @Autowired
    private DiscoveryClient discoveryClient;

    private Map<String, Object> generateConfig() {
        Map<String, Object> config = new Hashtable<>(0);
        List<String> services = discoveryClient.getServices();
        if (!CollectionUtils.isEmpty(services)) {
            for (String service : services) {
                config.put(service + ".ribbon.NFLoadBalancerRuleClassName", MappingRule.class.getName());
            }
        }
        return config;
    }

    private void reload() {
        if (configurableEnvironment == null) return;
        configurableEnvironment.getPropertySources().remove(CONFIG_NS);
        if (!this.mappingIsDisable) {
            configurableEnvironment.getPropertySources().addFirst(new MapPropertySource(CONFIG_NS, generateConfig()));
        }
    }

    @Component
    static class ClientIPHeaderUtil {

        private String clientIPHeaderKey;

        private String[] clientIPHeaders;

        private static ClientIPHeaderUtil instance;

        ClientIPHeaderUtil() {
            ClientIPHeaderUtil.instance = this;
        }

        public static String getClientIPHeaderKey() {
            return instance.clientIPHeaderKey;
        }

        public static String getClientIP(ServerHttpRequest request) {
            String clientIP = getClientIP(request.getHeaders());
            return StringUtils.isEmpty(clientIP) ? request.getRemoteAddress().getHostString() : clientIP;
        }

        public static String getClientIP(HttpHeaders headers) {
            if (headers == null) return null;
            String ip = null;
            for (int i = 0, j = instance.clientIPHeaders.length; StringUtils.isEmpty(ip) && i < j; i++) {
                ip = headers.getFirst(instance.clientIPHeaders[i]);
            }
            return (!StringUtils.isEmpty(ip) && ip.contains(",")) ? ip.split(",")[0] : ip;
        }

        public static String getClientIP(Map<String, Collection<String>> map) {
            if (map == null) return null;
            HttpHeaders headers = new HttpHeaders();
            headers.putAll((Map) map);
            return getClientIP(headers);
        }

    }

    @Value("#{${request.service.mapping.short:false}}")
    private Boolean mappingIsShort = false;

    public Boolean mappingIsShort() {
        return this.mappingIsShort;
    }

    private Boolean mappingIsDisable = false;

    @Value("#{${request.service.mapping.disable:false}}")
    private void mappingIsDisable(Boolean isDisable) {
        this.mappingIsDisable = isDisable;
        reload();
    }

    public Boolean mappingIsDisable() {
        return this.mappingIsDisable;
    }

    private Map<String, String> mappingMap = new Hashtable<>(0);

    @Value("#{${request.service.mapping:{\".*\":\".*\"}}}")
    private void mappingMap(Map<String, String> mappingMap) {
        this.mappingMap = mappingMap;
        reload();
    }

    public Map<String, String> mappingMap() {
        return this.mappingMap;
    }

    @Value("${request.service.mapping.client-ip-header:__CLIENT_IP__}")
    private void clientIPHeaderKey(String clientIPHeaderKey) {
        ClientIPHeaderUtil.instance.clientIPHeaderKey = clientIPHeaderKey;
    }


    @Value("${request.service.mapping.get-client-ip-headers:X-Forwarded-For,Proxy-Client-IP,WL-Proxy-Client-IP,HTTP_CLIENT_IP,HTTP_X_FORWARDED_FOR}")
    private void clientIPHeaders(String[] clientIPHeaders) {
        ClientIPHeaderUtil.instance.clientIPHeaders = clientIPHeaders;
    }

}
