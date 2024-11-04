package com.rancho_smart.api_gateway.controller;

import java.util.Collections;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.rancho_smart.api_gateway.dto.KeycloakUserDTO;
import com.rancho_smart.api_gateway.service.KeycloakAdminService;

@RestController
@RequestMapping("/auth")
public class AuthenticationRESTController {

    @Autowired
    private KeycloakAdminService keycloakAdminService;

    @PostMapping("/register")
    public ResponseEntity<String> registerUser(@RequestParam String role, @RequestBody KeycloakUserDTO userDTO) {
        String token = keycloakAdminService.getAdminToken();
        System.out.println();
        System.out.println();
        System.out.println();
        System.out.println();
        System.out.println();
        System.out.println();
        System.out.println();
        System.out.println();
        System.out.println();
        System.out.println();
        System.out.println();
        System.out.println();
        System.out.println();
        System.out.println();
        System.out.println();
        System.out.println();
        System.out.println();
        System.out.println(userDTO.toString());
        keycloakAdminService.createUserInKeycloak(
                token,
                userDTO.getUsername(),
                userDTO.getEmail(),
                userDTO.getFirstName(),
                userDTO.getLastName(),
                userDTO.getPassword(),
                role // Pass the role as the seventh argument
        );
        return ResponseEntity.ok("User created successfully");
    }

    @PostMapping("/login")
    public ResponseEntity<?> loginUser(@RequestParam String username, @RequestParam String password) {
        try {
            String token = keycloakAdminService.loginUser(username, password);
            return ResponseEntity.ok(Collections.singletonMap("access_token", token));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid credentials");
        }
    }
}
