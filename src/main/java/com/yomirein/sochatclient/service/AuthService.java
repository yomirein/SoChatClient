package com.yomirein.sochatclient.service;

import com.vaadin.flow.server.VaadinService;
import com.yomirein.sochatclient.model.Request;
import com.yomirein.sochatclient.model.Response;
import jakarta.servlet.http.Cookie;
import lombok.Getter;
import org.apache.hc.client5.http.cookie.BasicCookieStore;
import org.apache.hc.client5.http.cookie.CookieStore;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

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

        List<String> setCookieHeaders = response.getHeaders().get(HttpHeaders.SET_COOKIE);

        if (setCookieHeaders != null) {
            for (String cookieHeader : setCookieHeaders) {
                System.out.println(cookieHeader);
                List<java.net.HttpCookie> cookies = java.net.HttpCookie.parse(cookieHeader);
                for (java.net.HttpCookie cookie1 : cookies) {

                    Cookie nameCookie = getCookieByName(cookie1.getName());
                    if (nameCookie != null) {
                        System.out.println("User already does have token");
                    }
                    else {
                        nameCookie = new Cookie(cookie1.getName(), cookie1.getValue());
                        System.out.println("Stored name " + cookie1.getName() + " in cookie");

                        nameCookie.setMaxAge((int) cookie1.getMaxAge());
                        nameCookie.setSecure(cookie1.getSecure());
                        nameCookie.setPath(VaadinService.getCurrentRequest().getContextPath());
                        VaadinService.getCurrentResponse().addCookie(nameCookie);
                    }

                }
            }
        }

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

        List<String> setCookieHeaders = response.getHeaders().get(HttpHeaders.SET_COOKIE);

        if (setCookieHeaders != null) {
            for (String cookieHeader : setCookieHeaders) {
                System.out.println(cookieHeader);
                List<java.net.HttpCookie> cookies = java.net.HttpCookie.parse(cookieHeader);
                for (java.net.HttpCookie cookie1 : cookies) {

                    Cookie nameCookie = getCookieByName(cookie1.getName());
                    if (nameCookie != null) {
                        System.out.println("User already does have token");
                    }
                    else {
                        nameCookie = new Cookie(cookie1.getName(), cookie1.getValue());
                        System.out.println("Stored name " + cookie1.getName() + " in cookie");

                        nameCookie.setMaxAge((int) cookie1.getMaxAge());
                        nameCookie.setSecure(cookie1.getSecure());
                        nameCookie.setPath(VaadinService.getCurrentRequest().getContextPath());
                        VaadinService.getCurrentResponse().addCookie(nameCookie);
                    }

                }
            }
        }

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

    private Cookie getCookieByName(String name) {
        // Fetch all cookies from the request
        Cookie[] cookies = VaadinService.getCurrentRequest().getCookies();

        // Iterate to find cookie by its name
        for (Cookie cookie : cookies) {
            if (name.equals(cookie.getName())) {
                return cookie;
            }
        }

        return null;
    }
}