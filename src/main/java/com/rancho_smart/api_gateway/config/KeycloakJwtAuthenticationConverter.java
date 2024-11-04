package com.rancho_smart.api_gateway.config;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

import reactor.core.publisher.Mono;

public class KeycloakJwtAuthenticationConverter implements Converter<Jwt, Mono<AbstractAuthenticationToken>> {

  private final JwtGrantedAuthoritiesConverter jwtGrantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();

  @Override
  public Mono<AbstractAuthenticationToken> convert(Jwt source) {
      return Mono.just(source)
              .flatMap(jwt -> {
                  Collection<GrantedAuthority> authorities = Stream.concat(
                          jwtGrantedAuthoritiesConverter.convert(jwt).stream(),
                          extractResourceRoles(jwt).stream())
                          .collect(Collectors.toSet());

                  return Mono.just(new JwtAuthenticationToken(jwt, authorities));
              });
  }

  private Collection<? extends GrantedAuthority> extractResourceRoles(Jwt jwt) {
      var resourceAccess = new HashMap<>(jwt.getClaim("realm_access"));

      @SuppressWarnings("unchecked")
      var roles = (ArrayList<String>) resourceAccess.get("roles");

      Set<SimpleGrantedAuthority> authorities = roles.stream()
                .map(role -> new SimpleGrantedAuthority(
                    // Agrega el prefijo "ROLE_" si no está presente
                    role.startsWith("ROLE_") ? role : "ROLE_" + role))
                .collect(Collectors.toSet());
                
      return authorities;
  }
}