package com.yomirein.sochatclient.service;

import com.vaadin.flow.server.VaadinSession;
import com.yomirein.sochatclient.model.Request;
import com.yomirein.sochatclient.model.Response;
import lombok.Getter;
import org.apache.hc.client5.http.cookie.BasicCookieStore;
import org.apache.hc.client5.http.cookie.CookieStore;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class AuthService {

    @Getter
    private final RestTemplate rest;

    private final String baseUrl = "http://localhost:8080/api/auth";

    @Getter
    public CookieStore cookieStore = new BasicCookieStore();

    public AuthService() {
        CloseableHttpClient client = HttpClients.custom()
                .setDefaultCookieStore(cookieStore)
                .build();
        HttpComponentsClientHttpRequestFactory requestFactory =
                new HttpComponentsClientHttpRequestFactory(client);

        this.rest = new RestTemplate(requestFactory);
    }

    public Response.Auth login(String username, String password) {
        Request.Auth request = new Request.Auth(username, password);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Request.Auth> entity = new HttpEntity<>(request, headers);

        ResponseEntity<Response.Auth> response = rest.exchange(
                baseUrl + "/login",
                HttpMethod.POST,
                entity,
                Response.Auth.class
        );
        return response.getBody();
    }

    public Response.Auth register(String username, String password) {
        Request.Auth request = new Request.Auth(username, password);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Request.Auth> entity = new HttpEntity<>(request, headers);

        ResponseEntity<Response.Auth> response = rest.exchange(
                baseUrl + "/register",
                HttpMethod.POST,
                entity,
                Response.Auth.class
        );


        return response.getBody();
    }

    public String logout() {
        ResponseEntity<String> response = rest.postForEntity(
                "http://localhost:8080/api/auth/logout",
                null,
                String.class
        );


        return response.getBody();
    }

    public boolean isTokenValid(String token) {
        try {
            ResponseEntity<Boolean> response = rest.exchange(
                    "http://localhost:8080/api/auth/validate",
                    HttpMethod.GET,
                    new HttpEntity<>(new HttpHeaders() {{
                        setBearerAuth(token);
                    }}),
                    Boolean.class
            );

            return Boolean.TRUE.equals(response.getBody());
        } catch (Exception e) {
            return false;
        }
    }
}