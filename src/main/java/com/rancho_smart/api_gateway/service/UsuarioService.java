package com.rancho_smart.api_gateway.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.rancho_smart.UsuarioDTO;

import reactor.core.publisher.Mono;

@Service
public class UsuarioService {

    private final WebClient webClient;

    @Value("${USUARIOS_URL}")
    private String usuariosUrl;

    
    public UsuarioService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    public Mono<UsuarioDTO> getUsuarioById(Long usuarioId) {
        return webClient.get()
            .uri("/usuarios/{id}", usuarioId)
            .retrieve()
            .bodyToMono(UsuarioDTO.class);
    }

    public Mono<UsuarioDTO> crearUsuario(UsuarioDTO usuarioRequest, String bearerToken) {
        return webClient.post()
            .uri(usuariosUrl + "/usuarios")
            .header("Authorization", "Bearer " + bearerToken)
            .body(Mono.just(usuarioRequest), Object.class)
            .retrieve()
            .bodyToMono(UsuarioDTO.class)
            .onErrorResume(WebClientResponseException.class, e -> {
                return Mono.error(new RuntimeException("Error al crear Usuario: " + e.getMessage()));
            });
    }
}