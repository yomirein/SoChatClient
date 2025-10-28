package com.yomirein.sochatclient.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper mapper = new ObjectMapper();
    private final String baseUrl = "http://localhost:8080/api/auth";

    private String token;

    public boolean register(String username, String password) {
        var body = Map.of("username", username, "password", password);
        ResponseEntity<String> response = restTemplate.postForEntity(baseUrl + "/register", body, String.class);
        return response.getStatusCode().is2xxSuccessful();
    }

    public boolean login(String username, String password) {
        var body = Map.of("username", username, "password", password);
        ResponseEntity<String> response = restTemplate.postForEntity(baseUrl + "/login", body, String.class);

        if (response.getStatusCode().is2xxSuccessful() && !response.getBody().contains("Invalid")) {
            this.token = response.getBody(); // сервер должен возвращать JWT
            return true;
        }
        return false;
    }

    public String getToken() {
        return token;
    }
}