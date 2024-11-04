package com.rancho_smart.api_gateway.controller;

import java.util.Collections;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.rancho_smart.CredencialDTO;
import com.rancho_smart.UsuarioDTO;
import com.rancho_smart.api_gateway.dto.UsuarioRegisterDTO;
import com.rancho_smart.api_gateway.service.CredencialService;
import com.rancho_smart.api_gateway.service.KeycloakAdminService;
import com.rancho_smart.api_gateway.service.UsuarioService;

import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/auth")
public class AuthenticationRESTController {

    @Autowired
    private KeycloakAdminService keycloakAdminService;

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private CredencialService credencialService;

    @PostMapping("/register")
    public Mono<ResponseEntity<UsuarioRegisterDTO>> registerUser(@RequestParam String role,
            @RequestBody UsuarioRegisterDTO userDTO) {

        System.out.println("Inicio de registro de usuario");
        System.out.println("Rol recibido: " + role);
        System.out.println("Datos del usuario recibidos: " + userDTO);

        String adminToken = keycloakAdminService.getAdminToken();
        System.out.println("Token de administrador obtenido: " + adminToken);

        return keycloakAdminService
                .createUserInKeycloak(adminToken, userDTO.getCredencial().getUsername(),
                        userDTO.getCredencial().getPassword(), role)
                .flatMap(keycloakUserId -> {
                    System.out.println("Usuario creado en Keycloak con ID: " + keycloakUserId);

                    return keycloakAdminService
                            .loginUser(userDTO.getCredencial().getUsername(), userDTO.getCredencial().getPassword())
                            .flatMap(userToken -> {
                                System.out.println("Token de usuario obtenido después de login: " + userToken);

                                // Crear el usuario en el microservicio de usuarios
                                UsuarioDTO usuarioDTO = new UsuarioDTO();
                                usuarioDTO.setNombre(userDTO.getNombre());
                                usuarioDTO.setApellido(userDTO.getApellido());
                                usuarioDTO.setEmail(userDTO.getEmail());
                                usuarioDTO.setTelefono(userDTO.getTelefono());
                                usuarioDTO.setDireccion(userDTO.getDireccion());
                                usuarioDTO.setFechaRegistro(userDTO.getFechaRegistro());
                                usuarioDTO.setFoto(userDTO.getFoto());

                                System.out.println(
                                        "Datos del usuario a enviar al microservicio de usuarios: " + usuarioDTO);

                                return usuarioService.crearUsuario(usuarioDTO, userToken)
                                        .flatMap(usuarioCreado -> {
                                            System.out.println(
                                                    "Usuario creado en el microservicio de usuarios: " + usuarioCreado);

                                            // Crear las credenciales en el microservicio de credenciales
                                            CredencialDTO credencialDTO = new CredencialDTO();
                                            credencialDTO.setIdUsuario(usuarioCreado.getIdUsuario());
                                            credencialDTO.setUsername(userDTO.getCredencial().getUsername());
                                            credencialDTO.setPassword(userDTO.getCredencial().getPassword());
                                            credencialDTO.setRol(role);

                                            System.out.println(
                                                    "Datos de credencial a enviar al microservicio de credenciales: "
                                                            + credencialDTO);

                                            return credencialService.crearCredenciales(credencialDTO, userToken)
                                                    .map(credencialCreada -> {
                                                        System.out.println(
                                                                "Credencial creada en el microservicio de credenciales: "
                                                                        + credencialCreada);

                                                        // Construir la respuesta final con Usuario y Credencial creados
                                                        UsuarioRegisterDTO usuarioRegisterDTO = new UsuarioRegisterDTO();
                                                        usuarioRegisterDTO.setIdUsuario(usuarioCreado.getIdUsuario());
                                                        usuarioRegisterDTO.setNombre(usuarioCreado.getNombre());
                                                        usuarioRegisterDTO.setApellido(usuarioCreado.getApellido());
                                                        usuarioRegisterDTO.setEmail(usuarioCreado.getEmail());
                                                        usuarioRegisterDTO.setTelefono(usuarioCreado.getTelefono());
                                                        usuarioRegisterDTO.setDireccion(usuarioCreado.getDireccion());
                                                        usuarioRegisterDTO
                                                                .setFechaRegistro(usuarioCreado.getFechaRegistro());
                                                        usuarioRegisterDTO.setFoto(usuarioCreado.getFoto());
                                                        usuarioRegisterDTO.setCredencial(credencialCreada);

                                                        System.out.println(
                                                                "Registro de usuario completo: " + usuarioRegisterDTO);
                                                        return ResponseEntity.ok(usuarioRegisterDTO);
                                                    });
                                        });
                            });
                })
                .onErrorResume(e -> {
                    System.out.println("Error durante el registro del usuario: " + e.getMessage());
                    e.printStackTrace();
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                });
    }

    @PostMapping("/login")
    public Mono<ResponseEntity<Map<String, String>>> loginUser(@RequestParam String username,
            @RequestParam String password) {
        return keycloakAdminService.loginUser(username, password)
                .map(token -> ResponseEntity.ok(Collections.singletonMap("access_token", token)))
                .onErrorResume(e -> Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Collections.singletonMap("error", "Invalid credentials"))));
    }

}
