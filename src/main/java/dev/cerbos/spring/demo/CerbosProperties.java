package dev.cerbos.spring.demo;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

@ConfigurationProperties(prefix = "cerbos")
public class CerbosProperties {

    private final Pdp pdp = new Pdp();
    private final Http http = new Http();

    public Pdp getPdp() {
        return pdp;
    }

    public Http getHttp() {
        return http;
    }

    public static class Pdp {
        private String target = "localhost:3593";
        private boolean plaintext = true;
        private boolean insecure;
        private Duration timeout = Duration.ofSeconds(1);
        private String playgroundInstance;

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
    }

    public static class Http {
        private String resourceKind = "http_request";
        private Map<String, String> methodActions = defaultActions();

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
                methodActions.forEach((method, action) ->
                        this.methodActions.put(method.toUpperCase(Locale.ENGLISH), action));
            }
        }

        public String resolveAction(String method) {
            if (!StringUtils.hasText(method)) {
                return "unknown";
            }
            return methodActions.getOrDefault(method.toUpperCase(Locale.ENGLISH), method.toLowerCase(Locale.ENGLISH));
        }

        private static Map<String, String> defaultActions() {
            Map<String, String> defaults = new LinkedHashMap<>();
            defaults.put("GET", "read");
            defaults.put("POST", "create");
            defaults.put("PUT", "update");
            defaults.put("PATCH", "update");
            defaults.put("DELETE", "delete");
            return defaults;
        }
    }
}
