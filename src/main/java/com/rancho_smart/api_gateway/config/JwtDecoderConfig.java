package com.rancho_smart.api_gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;

@Configuration
public class JwtDecoderConfig {

    @Value("${KEYCLOAK_HOST}")
    private String keycloakHost;

    @Value("${KEYCLOAK_REALM}")
    private String keycloakRealm;

    @Bean
    public ReactiveJwtDecoder jwtDecoder() {
        String jwkSetUri = "http://" + keycloakHost + "/realms/" + keycloakRealm + "/protocol/openid-connect/certs";
        return NimbusReactiveJwtDecoder.withJwkSetUri(jwkSetUri).build();
    }
}
