package com.rancho_smart.api_gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .cors() // Enable CORS without disable() to let the CorsWebFilter handle it
                .and()
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(authorize -> authorize
                        .pathMatchers(HttpMethod.POST, "/keycloak-server/realms/myrealm/protocol/openid-connect/token")
                        .permitAll()
                        .pathMatchers("/personal/usuarios/", "/personal/credenciales/")
                        .hasAnyRole("GANADERO_ADMINISTRADOR", "GANADERO_EMPLEADO", "USUARIO_COMPRADOR")
                        .pathMatchers(HttpMethod.GET, "/fincas/")
                        .hasAnyRole("USUARIO_COMPRADOR", "GANADERO_ADMINISTRADOR", "GANADERO_EMPLEADO")
                        .pathMatchers("/fincas/")
                        .hasAnyRole("GANADERO_ADMINISTRADOR", "GANADERO_EMPLEADO")
                        .pathMatchers("/apareamiento/cruces/", "/apareamiento/genealogias/",
                                "/apareamiento/optimizaciones/", "/apareamiento/planificaciones/",
                                "/apareamiento/partos/")
                        .hasAnyRole("GANADERO_ADMINISTRADOR", "GANADERO_EMPLEADO")
                        .pathMatchers("/inventario/alimentos/", "/inventario/animales/", "/inventario/lotes/",
                                "/inventario/productos/", "/inventario/medicamentos/")
                        .hasAnyRole("GANADERO_ADMINISTRADOR", "GANADERO_EMPLEADO")
                        .pathMatchers("/produccion/registros-alimentacion/", "/produccion/calidades-carne/",
                                "/produccion/calidades-leche/", "/produccion/producciones-carne/",
                                "/produccion/producciones-leche/")
                        .hasAnyRole("GANADERO_ADMINISTRADOR", "GANADERO_EMPLEADO")
                        .pathMatchers("/salud/consultas/", "/salud/historiales-medicos/",
                                "/salud/procedimientos-medicos/", "/salud/tratamientos/", "/salud/vacunas/")
                        .hasAnyRole("GANADERO_ADMINISTRADOR", "GANADERO_EMPLEADO")
                        .pathMatchers("/ventas/clientes/", "/ventas/animales-venta/", "/ventas/mercado/",
                                "/ventas/ventas/", "/ventas/ventas-produccion/")
                        .hasAnyRole("GANADERO_ADMINISTRADOR", "GANADERO_EMPLEADO")
                        .pathMatchers("/auth/register", "/auth/login", "/actuator/health", "/public/")
                        .permitAll()
                        .anyExchange().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(
                                new KeycloakJwtAuthenticationConverter())))
                .build();
    }

    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.addAllowedOrigin("http://localhost:4200");
        configuration.addAllowedMethod("*");
        configuration.addAllowedHeader("*");
        configuration.setAllowCredentials(true);
        
        // Exponer el encabezado Authorization para que sea accesible en el cliente
        configuration.addExposedHeader("Authorization");
    
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration); // Aplicar configuración de CORS a todas las rutas
        return new CorsWebFilter(source);
    }
    
}
