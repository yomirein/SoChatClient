package com.yomirein.sochatclient.service;

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

    // Хранилище cookie (автоматически сохраняет AUTH_TOKEN)

    public AuthService() {
        CookieStore cookieStore = new BasicCookieStore();

        CloseableHttpClient client = HttpClients.custom()
                .setDefaultCookieStore(cookieStore)
                .build();

        this.rest = new RestTemplate(new HttpComponentsClientHttpRequestFactory(client));
    }

    /**
     * Login пользователя
     */
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
        // cookie автоматически сохранится в cookieStore
        return response.getBody();
    }

    /**
     * Регистрация пользователя
     */
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

    /**
     * Проверка валидности токена через сервер
     * cookie отправляется автоматически
     */
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