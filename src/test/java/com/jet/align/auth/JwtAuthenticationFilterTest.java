package com.jet.align.auth;

import com.jet.align.user.Role;
import com.jet.align.user.User;
import io.jsonwebtoken.MalformedJwtException;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JwtAuthenticationFilterTest {

    private final JwtService jwtService = mock(JwtService.class);
    private final CustomUserDetailsService userDetailsService = mock(CustomUserDetailsService.class);
    private final FilterChain chain = mock(FilterChain.class);
    private final JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService, userDetailsService);

    private final MockHttpServletRequest request = new MockHttpServletRequest();
    private final MockHttpServletResponse response = new MockHttpServletResponse();

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private Authentication currentAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    @Test
    void sin_header_Authorization_sigue_la_cadena_sin_autenticar() throws Exception {
        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
        assertThat(currentAuthentication()).isNull();
    }

    @Test
    void con_token_valido_autentica_al_usuario_cargado_por_id() throws Exception {
        UUID id = UUID.randomUUID();
        User user = User.builder().email("jhon@align.dev").role(Role.USER).build();
        request.addHeader("Authorization", "Bearer valid-token");
        when(jwtService.extractUserId("valid-token")).thenReturn(id);
        when(userDetailsService.loadUserById(id)).thenReturn(user);

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
        assertThat(currentAuthentication()).isNotNull();
        assertThat(currentAuthentication().getPrincipal()).isSameAs(user);
    }

    @Test
    void con_token_de_un_usuario_que_ya_no_existe_sigue_la_cadena_sin_autenticar_y_sin_explotar() throws Exception {
        // Antes de ampliar el catch, UsernameNotFoundException escapaba del filtro (fuera
        // del alcance de GlobalExceptionHandler) y el cliente recibía un 500 en vez de 401.
        UUID id = UUID.randomUUID();
        request.addHeader("Authorization", "Bearer orphan-token");
        when(jwtService.extractUserId("orphan-token")).thenReturn(id);
        when(userDetailsService.loadUserById(id)).thenThrow(new UsernameNotFoundException("gone"));

        assertThatCode(() -> filter.doFilterInternal(request, response, chain)).doesNotThrowAnyException();

        verify(chain).doFilter(request, response);
        assertThat(currentAuthentication()).isNull();
    }

    @Test
    void con_token_invalido_sigue_la_cadena_sin_autenticar() throws Exception {
        request.addHeader("Authorization", "Bearer bad-token");
        when(jwtService.extractUserId("bad-token")).thenThrow(new MalformedJwtException("bad"));

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
        assertThat(currentAuthentication()).isNull();
    }
}
