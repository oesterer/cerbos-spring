package dev.cerbos.spring;

import dev.cerbos.sdk.CerbosBlockingClient;
import dev.cerbos.sdk.CerbosClientBuilder;
import dev.cerbos.sdk.CerbosClientBuilder.InvalidClientConfigurationException;
import java.time.Duration;
import java.util.Map;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.util.StringUtils;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(CerbosAuthorizationProperties.class)
public class CerbosAuthorizationAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public CerbosBlockingClient cerbosBlockingClient(CerbosAuthorizationProperties properties) {
        CerbosClientBuilder builder = new CerbosClientBuilder(properties.getTarget());
        Duration timeout = properties.getTimeout();
        if (timeout != null) {
            builder.withTimeout(timeout);
        }
        if (properties.isPlaintext()) {
            builder.withPlaintext();
        } else if (properties.isInsecure()) {
            builder.withInsecure();
        }
        if (StringUtils.hasText(properties.getPlaygroundInstance())) {
            builder.withPlaygroundInstance(properties.getPlaygroundInstance());
        }

        try {
            CerbosBlockingClient client = builder.buildBlockingClient();
            Map<String, String> headers = properties.getHeaders();
            if (headers != null && !headers.isEmpty()) {
                client = client.withHeaders(headers);
            }
            return client;
        } catch (InvalidClientConfigurationException ex) {
            throw new BeanCreationException("cerbosBlockingClient", "Failed to create Cerbos client", ex);
        }
    }

    @Bean
    @ConditionalOnMissingBean
    public CerbosEvaluationService cerbosEvaluationService(
            CerbosBlockingClient cerbosBlockingClient, CerbosAuthorizationProperties properties) {
        return new CerbosEvaluationService(cerbosBlockingClient, properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public CerbosAuthorizationRequestFactory cerbosAuthorizationRequestFactory(
            CerbosAuthorizationProperties properties) {
        return new DefaultHttpCerbosRequestFactory(properties);
    }

    @Bean(name = "cerbosAuthorizationManager")
    @ConditionalOnMissingBean(name = "cerbosAuthorizationManager")
    public AuthorizationManager<RequestAuthorizationContext> cerbosAuthorizationManager(
            CerbosEvaluationService cerbosEvaluationService,
            CerbosAuthorizationRequestFactory cerbosAuthorizationRequestFactory) {
        return new CerbosAuthorizationManager(cerbosEvaluationService, cerbosAuthorizationRequestFactory);
    }
}
