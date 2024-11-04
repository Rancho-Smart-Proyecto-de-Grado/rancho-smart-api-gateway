package com.rancho_smart.api_gateway.config;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import reactor.core.publisher.Mono;

public class KeycloakJwtAuthenticationConverter implements Converter<Jwt, Mono<AbstractAuthenticationToken>> {

    
    private static final String clientId = "rancho-smart-api-gateway"; // Nombre del cliente en Keycloak

    @Override
    public Mono<AbstractAuthenticationToken> convert(Jwt jwt) {
        Collection<GrantedAuthority> authorities = extractClientRoles(jwt);
        return Mono.just(new JwtAuthenticationToken(jwt, authorities));
    }

    private Collection<GrantedAuthority> extractClientRoles(Jwt jwt) {
        Map<String, Object> resourceAccess = jwt.getClaim("resource_access");


        if (resourceAccess == null || resourceAccess.isEmpty()) {
            return Collections.emptySet(); // Si no hay roles, retorna un set vacío
        }

        // Obtener roles específicos del cliente
        Map<String, Object> clientAccess = (Map<String, Object>) resourceAccess.get(clientId);

        if (clientAccess == null || clientAccess.isEmpty()) {
            return Collections.emptySet();
        }

        @SuppressWarnings("unchecked")
        Collection<String> roles = (Collection<String>) clientAccess.get("roles");

        if (roles == null) {
            return Collections.emptySet();
        }

        // Mapear cada rol con prefijo ROLE_
        return roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .collect(Collectors.toSet());
    }
}
