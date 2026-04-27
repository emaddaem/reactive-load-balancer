package com.reactiveloadbalancer.load_balancer.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "loadbalancer")
@Data
public class LoadBalancerProperties {

    private Health health = new Health();
    private int failureThreshold = 3;
    private Proxy proxy = new Proxy();
    private Algorithm algorithm = new Algorithm();

    @Data
    public static class Health {
        private Check check = new Check();

        @Data
        public static class Check {
            private long interval = 10000;
            private long timeout = 3000;
        }
    }

    @Data
    public static class Proxy {
        private long timeout = 5000;
    }

    @Data
    public static class Algorithm {
        private String defaultAlgorithm = "ROUND_ROBIN";
    }
}
