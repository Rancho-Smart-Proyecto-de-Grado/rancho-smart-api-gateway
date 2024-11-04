package com.rancho_smart.api_gateway.service;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

import reactor.core.publisher.Mono;

import org.springframework.core.ParameterizedTypeReference;

@Service
public class KeycloakAdminService {

    @Autowired
    private RestTemplate restTemplate;

    @Value("${KEYCLOAK_HOST}")
    private String keycloakHost;

    @Value("${KEYCLOAK_REALM}")
    private String realmName;

    @Value("${KEYCLOAK_ADMIN}")
    private String adminUsername;

    @Value("${KEYCLOAK_ADMIN_PASSWORD}")
    private String adminPassword;

    @Value("${KEYCLOAK_CLIENT_ID}")
    private String clientId;

    @Value("${KEYCLOAK_CLIENT_SECRET}")
    private String clientSecret;

    @Value("${KEYCLOAK_HTTP_PORT}")
    private String keycloakPort;

    private final WebClient webClient;
    public KeycloakAdminService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    public String getAdminToken() {
        String keycloakUrl = "http://" + keycloakHost + "/realms/" + realmName + "/protocol/openid-connect/token";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        body.add("username", adminUsername);
        body.add("password", adminPassword);
        body.add("grant_type", "password");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                keycloakUrl,
                HttpMethod.POST,
                request,
                new ParameterizedTypeReference<Map<String, Object>>() {
                });

        Map<String, Object> responseBody = response.getBody();
        if (responseBody != null && responseBody.containsKey("access_token")) {
            return responseBody.get("access_token").toString();
        }

        throw new IllegalStateException("Access token not found in the response.");
    }

    public Mono<String> createUserInKeycloak(String accessToken, String username, String password, String role) {
    if (!roleExists(accessToken, role)) {
        return Mono.error(new IllegalArgumentException("El rol especificado no existe en Keycloak: " + role));
    }

    String keycloakUrl = "http://" + keycloakHost + "/admin/realms/" + realmName + "/users";

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.set("Authorization", "Bearer " + accessToken);

    Map<String, Object> user = new HashMap<>();
    user.put("username", username);
    user.put("enabled", true);

    Map<String, String> credentials = new HashMap<>();
    credentials.put("type", "password");
    credentials.put("value", password);
    credentials.put("temporary", "false");

    user.put("credentials", Collections.singletonList(credentials));

    // Usando WebClient en lugar de RestTemplate para encadenar mejor
    return webClient.post()
        .uri(keycloakUrl)
        .headers(httpHeaders -> httpHeaders.addAll(headers))
        .bodyValue(user)
        .retrieve()
        .toBodilessEntity()
        .map(response -> {
            String locationHeader = Optional.ofNullable(response.getHeaders().getLocation())
                .map(Object::toString)
                .orElseThrow(() -> new IllegalStateException("User creation failed: Location header is missing."));
            return extractUserIdFromLocation(locationHeader);
        })
        .flatMap(userId -> {
            assignRoleToUser(accessToken, userId, role);
            return Mono.just(userId);
        })
        .onErrorResume(e -> {
            return Mono.error(new RuntimeException("Error al crear el usuario en Keycloak: " + e.getMessage()));
        });
}


    private void assignRoleToUser(String accessToken, String userId, String roleName) {
        // Primero obtenemos el ID del cliente
        String clientsUrl = "http://" + keycloakHost + "/admin/realms/" + realmName + "/clients?clientId=" + clientId;

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessToken);
        HttpEntity<Void> request = new HttpEntity<>(headers);

        try {
            // Obtener el ID del cliente
            ResponseEntity<String> clientResponse = restTemplate.exchange(clientsUrl, HttpMethod.GET, request,
                    String.class);
            String clientIdFromResponse = extractClientId(clientResponse.getBody());

            // Construir la URL para obtener el rol del cliente usando su ID
            String roleUrl = "http://" + keycloakHost + "/admin/realms/" + realmName + "/clients/"
                    + clientIdFromResponse + "/roles/" + roleName;

            // Obtener el rol
            ResponseEntity<Map<String, Object>> roleResponse = restTemplate.exchange(
                    roleUrl, HttpMethod.GET, request,
                    new ParameterizedTypeReference<Map<String, Object>>() {
                    });

            Map<String, Object> roleBody = roleResponse.getBody();
            if (roleBody != null && roleBody.containsKey("id")) {
                String roleId = roleBody.get("id").toString();

                // Construir la URL para asignar el rol al usuario
                String userRolesUrl = "http://" + keycloakHost + "/admin/realms/" + realmName + "/users/" + userId
                        + "/role-mappings/clients/" + clientIdFromResponse;

                // Crear el mapa del rol
                Map<String, Object> roleMap = new HashMap<>();
                roleMap.put("id", roleId);
                roleMap.put("name", roleName);

                // Asignar el rol al usuario
                restTemplate.postForEntity(userRolesUrl,
                        new HttpEntity<>(Collections.singletonList(roleMap), createAuthHeaders(accessToken)),
                        Void.class);
            } else {
                throw new IllegalStateException("Role ID not found in the response.");
            }
        } catch (HttpClientErrorException e) {
            System.out.println("Error Status Code: " + e.getStatusCode());
            System.out.println("Error Body: " + e.getResponseBodyAsString());
            throw new IllegalStateException("Failed to assign role to user: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("Unexpected Error: " + e.getMessage());
            throw new IllegalStateException("An unexpected error occurred while assigning role to user.", e);
        }
    }

    private HttpHeaders createAuthHeaders(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + accessToken);
        return headers;
    }

    public Mono<String> loginUser(String username, String password) {
        String keycloakUrl = "http://" + keycloakHost + "/realms/" + realmName + "/protocol/openid-connect/token";
    
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
    
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        body.add("grant_type", "password");
        body.add("username", username);
        body.add("password", password);
    
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
    
        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    keycloakUrl,
                    HttpMethod.POST,
                    request,
                    new ParameterizedTypeReference<Map<String, Object>>() {
                    });
    
            Map<String, Object> responseBody = response.getBody();
            if (responseBody != null && responseBody.containsKey("access_token")) {
                return Mono.just(responseBody.get("access_token").toString());
            } else {
                return Mono.error(new IllegalStateException("Access token not found in the response."));
            }
        } catch (Exception e) {
            return Mono.error(e);
        }
    }
    

    private String extractUserIdFromLocation(String locationHeader) {
        return locationHeader.substring(locationHeader.lastIndexOf("/") + 1);
    }

    private boolean roleExists(String accessToken, String roleName) {
        String clientsUrl = "http://" + keycloakHost + "/admin/realms/" + realmName + "/clients?clientId=" + clientId;

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessToken);

        HttpEntity<Void> request = new HttpEntity<>(headers);

        try {
            // Obtener el ID del cliente
            ResponseEntity<String> clientResponse = restTemplate.exchange(clientsUrl, HttpMethod.GET, request,
                    String.class);
            String clientIdFromResponse = extractClientId(clientResponse.getBody());

            // Construir la URL para obtener todos los roles del cliente usando su ID
            String rolesUrl = "http://" + keycloakHost + "/admin/realms/" + realmName + "/clients/"
                    + clientIdFromResponse + "/roles";

            // Obtener la lista de roles del cliente
            ResponseEntity<String> rolesResponse = restTemplate.exchange(rolesUrl, HttpMethod.GET, request,
                    String.class);

            // Analizar el JSON de la respuesta y buscar el rol deseado
            return roleExistsInResponse(rolesResponse.getBody(), roleName);
        } catch (HttpClientErrorException e) {
            System.out.println("Error Status Code: " + e.getStatusCode());
            System.out.println("Error Body: " + e.getResponseBodyAsString());
            return false;
        } catch (Exception e) {
            System.out.println("Unexpected Error: " + e.getMessage());
            return false;
        }
    }

    private String extractClientId(String responseBody) {
        // Buscar el campo "id" en el JSON manualmente
        String idKey = "\"id\":\"";
        int idStartIndex = responseBody.indexOf(idKey);

        if (idStartIndex == -1) {
            throw new IllegalArgumentException("Client ID not found in response");
        }

        // Ajustar el índice para empezar justo después de "id":"
        idStartIndex += idKey.length();
        int idEndIndex = responseBody.indexOf("\"", idStartIndex);

        // Extraer el valor del ID
        return responseBody.substring(idStartIndex, idEndIndex);
    }

    private boolean roleExistsInResponse(String responseBody, String roleName) {
        // Buscar el nombre del rol en el JSON manualmente sin dependencias
        String roleNameKey = "\"name\":\"" + roleName + "\"";
        return responseBody.contains(roleNameKey);
    }
}
