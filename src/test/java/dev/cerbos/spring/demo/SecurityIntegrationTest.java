package dev.cerbos.spring.demo;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.cerbos.sdk.CerbosBlockingClient;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CerbosBlockingClient cerbosBlockingClient;

    @MockBean
    private CerbosAuthorizationService authorizationService;

    @Test
    void publicEndpointsAreAccessible() throws Exception {
        mockMvc.perform(get("/public/info"))
                .andExpect(status().isOk())
                .andExpect(content().string("public"));
    }

    @Test
    void protectedResourceAllowedWhenCerbosApproves() throws Exception {
        when(authorizationService.isAllowed(any(Authentication.class), any(HttpServletRequest.class)))
                .thenReturn(true);

        mockMvc.perform(get("/documents/alpha").with(httpBasic("alice", "password")))
                .andExpect(status().isOk());
    }

    @Test
    void protectedResourceDeniedWhenCerbosRejects() throws Exception {
        when(authorizationService.isAllowed(any(Authentication.class), any(HttpServletRequest.class)))
                .thenReturn(false);

        mockMvc.perform(get("/documents/alpha").with(httpBasic("alice", "password")))
                .andExpect(status().isForbidden());
    }
}
