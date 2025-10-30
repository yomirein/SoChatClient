package com.yomirein.sochatclient.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yomirein.sochatclient.model.Response;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper mapper = new ObjectMapper();
    private final String baseUrl = "http://localhost:8080/api/auth";

    @Getter
    private String token;

    public Response.Auth register(String username, String password) {
        var body = Map.of("username", username, "password", password);
        ResponseEntity<Response.Auth> response =
                restTemplate.postForEntity(baseUrl + "/register", body, Response.Auth.class);

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            this.token = response.getBody().getToken();
            return response.getBody();
        }
        return response.getBody();
    }


    public Response.Auth login(String username, String password) {
        var body = Map.of("username", username, "password", password);
        ResponseEntity<Response.Auth> response = restTemplate.postForEntity(baseUrl + "/login", body, Response.Auth.class);

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            this.token = response.getBody().getToken();
            return response.getBody();
        }
        return response.getBody();
    }

}