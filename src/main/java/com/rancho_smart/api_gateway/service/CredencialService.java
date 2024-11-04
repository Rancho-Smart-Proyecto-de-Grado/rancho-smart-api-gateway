package com.rancho_smart.api_gateway.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.rancho_smart.CredencialDTO;

import reactor.core.publisher.Mono;

@Service
public class CredencialService {
    
    private final WebClient webClient;

    @Value("${CREDENCIALES_URL}")
    private String credencialesUrl;

    public CredencialService(WebClient.Builder webClientBuilder){
        this.webClient = webClientBuilder.baseUrl(credencialesUrl).build();
    }

    public Mono<CredencialDTO> getCredencialesById(Long credencialesId) {
        return webClient.get()
            .uri("/credenciales/{id}", credencialesId)
            .retrieve()
            .bodyToMono(CredencialDTO.class);
    }

    public Mono<CredencialDTO> crearCredenciales(CredencialDTO credencialesRequest, String bearerToken) {
        return webClient.post()
            .uri(credencialesUrl + "/credenciales")
            .header("Authorization", "Bearer " + bearerToken)
            .body(Mono.just(credencialesRequest), Object.class)
            .retrieve()
            .bodyToMono(CredencialDTO.class)
            .onErrorResume(WebClientResponseException.class, e -> {
                return Mono.error(new RuntimeException("Error al crear Credenciales: " + e.getMessage()));
            });
    }
}
