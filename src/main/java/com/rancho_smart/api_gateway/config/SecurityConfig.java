package com.rancho_smart.api_gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.CorsConfiguration;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
            .cors(cors -> cors
                .configurationSource(request -> {
                    CorsConfiguration config = new CorsConfiguration();
                    config.applyPermitDefaultValues(); // Configuración predeterminada de CORS
                    return config;
                }))
            .csrf(csrf -> csrf.disable()) // Deshabilitar CSRF para APIs REST
            .authorizeExchange(exchange -> exchange
                // Permitir el acceso al endpoint de autenticación
                .pathMatchers(HttpMethod.POST, "/keycloak-server/realms/myrealm/protocol/openid-connect/token")
                .permitAll()
                
                .pathMatchers("/personal/usuarios/**", "/personal/credenciales/**")
                .hasAnyRole("GANADERO_ADMINISTRADOR", "GANADERO_EMPLEADO", "USUARIO")

                .pathMatchers(HttpMethod.GET, "/fincas/**")
                .hasAnyRole("USUARIO", "GANADERO_ADMINISTRADOR", "GANADERO_EMPLEADO")

                .pathMatchers("/fincas/**")
                .hasAnyRole("GANADERO_ADMINISTRADOR", "GANADERO_EMPLEADO")

                .pathMatchers("/apareamiento/cruces/**", "/apareamiento/genealogias/**", "/apareamiento/optimizaciones/**", "/apareamiento/planificaciones/**", "/apareamiento/partos/**")
                .hasAnyRole("GANADERO_ADMINISTRADOR", "GANADERO_EMPLEADO")
                
                .pathMatchers("/inventario/alimentos/**", "/inventario/animales/**", "/inventario/lotes/**", "/inventario/productos/**", "/inventario/medicamentos/**")
                .hasAnyRole("GANADERO_ADMINISTRADOR", "GANADERO_EMPLEADO")

                .pathMatchers("/produccion/registros-alimentacion/**", "/produccion/calidades-carne/**", "/produccion/calidades-leche/**", "/produccion/producciones-carne/**", "/produccion/producciones-leche/**")
                .hasAnyRole("GANADERO_ADMINISTRADOR", "GANADERO_EMPLEADO")

                .pathMatchers("/salud/consultas/**", "/salud/historiales-medicos/**", "/salud/procedimientos-medicos/**", "/salud/tratamientos/**", "/salud/vacunas/**")
                .hasAnyRole("GANADERO_ADMINISTRADOR", "GANADERO_EMPLEADO")

                .pathMatchers("/ventas/clientes/**", "/ventas/animales-venta/**", "/ventas/mercado/**", "/ventas/ventas/**", "/ventas/ventas-produccion/**")
                .hasAnyRole("GANADERO_ADMINISTRADOR", "GANADERO_EMPLEADO")

                .pathMatchers("/auth/register", "/auth/login", "/actuator/health", "/public/**")
                .permitAll()
                
                .anyExchange().authenticated())
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(
                    new KeycloakJwtAuthenticationConverter()))) // Convierte los tokens JWT de Keycloak
            .build();
    }
}
