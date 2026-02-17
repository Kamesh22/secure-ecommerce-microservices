package com.ecommerce.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
public class RoleHeaderFilter extends OncePerRequestFilter {

    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String ROLES_HEADER = "X-User-Roles";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        try {
            String rolesHeader = request.getHeader(ROLES_HEADER);
            String userId = request.getHeader(USER_ID_HEADER);

            if (rolesHeader != null && userId != null) {

                var authorities = RoleExtractor.extractRoles(rolesHeader);

                var authentication = new UsernamePasswordAuthenticationToken(
                        userId,
                        null,
                        authorities
                );

                SecurityContextHolder.getContext().setAuthentication(authentication);

                log.debug("SecurityContext set for userId={} with roles={}", userId, rolesHeader);
            }

        } catch (Exception ex) {
            log.error("Error in RoleHeaderFilter: {}", ex.getMessage());
        }

        filterChain.doFilter(request, response);
    }
}
