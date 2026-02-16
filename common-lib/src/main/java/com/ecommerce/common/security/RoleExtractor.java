package com.ecommerce.common.security;

import lombok.experimental.UtilityClass;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@UtilityClass
public class RoleExtractor {

    private static final String ROLE_PREFIX = "ROLE_";
    private static final String ROLES_SEPARATOR = ",";

    /**
     * Extract roles from the X-User-Roles header value
     * Format: "ADMIN,USER" -> [ROLE_ADMIN, ROLE_USER]
     */
    public static Collection<GrantedAuthority> extractRoles(String rolesHeader) {
        List<GrantedAuthority> authorities = new ArrayList<>();

        if (rolesHeader == null || rolesHeader.trim().isEmpty()) {
            return authorities;
        }

        String[] roles = rolesHeader.split(ROLES_SEPARATOR);
        for (String role : roles) {
            String trimmedRole = role.trim();
            if (!trimmedRole.isEmpty()) {
                // Add ROLE_ prefix if not already present
                String authorityRole = trimmedRole.startsWith(ROLE_PREFIX) 
                    ? trimmedRole 
                    : ROLE_PREFIX + trimmedRole;
                authorities.add(new SimpleGrantedAuthority(authorityRole));
            }
        }

        return authorities;
    }
}
