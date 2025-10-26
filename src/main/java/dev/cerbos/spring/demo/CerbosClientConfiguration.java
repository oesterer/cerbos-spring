package dev.cerbos.spring.demo;

import dev.cerbos.sdk.CerbosBlockingClient;
import dev.cerbos.sdk.CerbosClientBuilder;
import dev.cerbos.sdk.CerbosClientBuilder.InvalidClientConfigurationException;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(CerbosProperties.class)
public class CerbosClientConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(CerbosClientConfiguration.class);

    @Bean
    @ConditionalOnMissingBean
    public CerbosBlockingClient cerbosBlockingClient(CerbosProperties properties) {
        CerbosProperties.Pdp pdp = properties.getPdp();
        CerbosClientBuilder builder = new CerbosClientBuilder(pdp.getTarget());
        Duration timeout = pdp.getTimeout();
        if (timeout != null) {
            builder.withTimeout(timeout);
        }
        if (pdp.isPlaintext()) {
            builder.withPlaintext();
        } else if (pdp.isInsecure()) {
            builder.withInsecure();
        }
        if (StringUtils.hasText(pdp.getPlaygroundInstance())) {
            builder.withPlaygroundInstance(pdp.getPlaygroundInstance());
        }
        try {
            return builder.buildBlockingClient();
        } catch (InvalidClientConfigurationException ex) {
            LOGGER.error("Failed to create Cerbos client", ex);
            throw new IllegalStateException("Failed to create Cerbos client", ex);
        }
    }

    @Bean
    public CerbosAuthorizationService cerbosAuthorizationService(
            CerbosBlockingClient cerbosBlockingClient, CerbosProperties properties) {
        return new CerbosAuthorizationService(cerbosBlockingClient, properties);
    }

    @Bean
    public CerbosAuthorizationManager cerbosAuthorizationManager(
            CerbosAuthorizationService authorizationService) {
        return new CerbosAuthorizationManager(authorizationService);
    }
}
