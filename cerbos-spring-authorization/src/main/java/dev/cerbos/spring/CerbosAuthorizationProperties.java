package dev.cerbos.spring;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

@ConfigurationProperties(prefix = "cerbos.authorization")
public class CerbosAuthorizationProperties {

    private String target = "localhost:3593";

    private boolean plaintext = true;

    private boolean insecure;

    private Duration timeout = Duration.ofSeconds(1);

    private String playgroundInstance;

    private String policyVersion;

    private String principalScope;

    private String resourceScope;

    private String resourceKind = "http_request";

    private Map<String, String> methodActions = createDefaultMethodActions();

    private Map<String, String> headers = new LinkedHashMap<>();

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public boolean isPlaintext() {
        return plaintext;
    }

    public void setPlaintext(boolean plaintext) {
        this.plaintext = plaintext;
    }

    public boolean isInsecure() {
        return insecure;
    }

    public void setInsecure(boolean insecure) {
        this.insecure = insecure;
    }

    public Duration getTimeout() {
        return timeout;
    }

    public void setTimeout(Duration timeout) {
        this.timeout = timeout;
    }

    public String getPlaygroundInstance() {
        return playgroundInstance;
    }

    public void setPlaygroundInstance(String playgroundInstance) {
        this.playgroundInstance = playgroundInstance;
    }

    public String getPolicyVersion() {
        return policyVersion;
    }

    public void setPolicyVersion(String policyVersion) {
        this.policyVersion = policyVersion;
    }

    public String getPrincipalScope() {
        return principalScope;
    }

    public void setPrincipalScope(String principalScope) {
        this.principalScope = principalScope;
    }

    public String getResourceScope() {
        return resourceScope;
    }

    public void setResourceScope(String resourceScope) {
        this.resourceScope = resourceScope;
    }

    public String getResourceKind() {
        return resourceKind;
    }

    public void setResourceKind(String resourceKind) {
        this.resourceKind = resourceKind;
    }

    public Map<String, String> getMethodActions() {
        return methodActions;
    }

    public void setMethodActions(Map<String, String> methodActions) {
        this.methodActions = new LinkedHashMap<>();
        if (methodActions != null) {
            methodActions.forEach((key, value) -> this.methodActions.put(key.toUpperCase(Locale.ENGLISH), value));
        }
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    public String resolveAction(String httpMethod) {
        if (!StringUtils.hasText(httpMethod)) {
            return "unknown";
        }
        return methodActions.getOrDefault(httpMethod.toUpperCase(Locale.ENGLISH), httpMethod.toLowerCase(Locale.ENGLISH));
    }

    private static Map<String, String> createDefaultMethodActions() {
        Map<String, String> defaults = new LinkedHashMap<>();
        defaults.put("GET", "read");
        defaults.put("HEAD", "read");
        defaults.put("POST", "create");
        defaults.put("PUT", "update");
        defaults.put("PATCH", "update");
        defaults.put("DELETE", "delete");
        return defaults;
    }
}
