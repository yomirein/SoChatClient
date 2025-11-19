package com.yomirein.sochatclient.service;

import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinSession;
import com.yomirein.sochatclient.model.Request;
import com.yomirein.sochatclient.model.Response;
import lombok.Getter;
import org.apache.hc.client5.http.cookie.BasicCookieStore;
import org.apache.hc.client5.http.cookie.Cookie;
import org.apache.hc.client5.http.cookie.CookieStore;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class AuthService {

    private final String baseUrl = "http://localhost:8443/api/auth";

    public AuthService() {
        // пустой конструктор — не храним shared RestTemplate/cookieStore
    }

    public Response.Auth login(String username, String password) {
        RestTemplate rest = createRestTemplateForCurrentVaadinSession();

        Request.Auth req = new Request.Auth(username, password);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Request.Auth> entity = new HttpEntity<>(req, headers);

        ResponseEntity<Response.Auth> resp = rest.exchange(
                baseUrl + "/login",
                HttpMethod.POST,
                entity,
                Response.Auth.class
        );

        return resp.getBody();
    }
    public Response.Auth register(String username, String password) {
        RestTemplate rest = createRestTemplateForCurrentVaadinSession();

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

    public Response.Auth getCurrentUser() {
        RestTemplate rest = createRestTemplateForCurrentVaadinSession();

        Request.Auth request = new Request.Auth(null, null);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Request.Auth> entity = new HttpEntity<>(request, headers);

        ResponseEntity<Response.Auth> response = rest.exchange(
                baseUrl + "/getUser",
                HttpMethod.POST,
                entity,
                Response.Auth.class
        );

        return response.getBody();
    }

    public String logout() {
        RestTemplate rest = createRestTemplateForCurrentVaadinSession();
        ResponseEntity<String> resp = rest.postForEntity(baseUrl + "/logout", null, String.class);
        clearSessionCookieStore();

        return resp.getBody();
    }


    public boolean isTokenValid(String token) {
        RestTemplate rest = createRestTemplateForCurrentVaadinSession();

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);
            ResponseEntity<Boolean> response = rest.exchange(
                    baseUrl + "/validate",
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    Boolean.class
            );

            return Boolean.TRUE.equals(response.getBody());
        } catch (Exception e) {
            return false;
        }
    }

    public static RestTemplate createRestTemplateForCurrentVaadinSession() {
        VaadinSession vaadinSession = VaadinSession.getCurrent();
        if (vaadinSession == null) {
            return createRestTemplate(new BasicCookieStore());
        }

        CookieStore cookieStore;
        vaadinSession.lock();
        try {
            cookieStore = (CookieStore) vaadinSession.getAttribute("cookieStore");
            if (cookieStore == null) {
                cookieStore = new BasicCookieStore();
                vaadinSession.setAttribute("cookieStore", cookieStore);
            }
        } finally {
            vaadinSession.unlock();
        }
        return createRestTemplate(cookieStore);
    }

    // Создать RestTemplate из заданного CookieStore (для фоновых задач)
    public static RestTemplate createRestTemplateFromCookieStore(CookieStore cookieStore) {
        return createRestTemplate(cookieStore == null ? new BasicCookieStore() : cookieStore);
    }

    private static RestTemplate createRestTemplate(CookieStore cookieStore) {
        CloseableHttpClient client = HttpClients.custom()
                .setDefaultCookieStore(cookieStore)
                .build();
        HttpComponentsClientHttpRequestFactory requestFactory =
                new HttpComponentsClientHttpRequestFactory(client);
        return new RestTemplate(requestFactory);
    }

    private void clearSessionCookieStore() {
        VaadinSession vaadinSession = VaadinSession.getCurrent();
        if (vaadinSession == null) return;
        vaadinSession.lock();
        try {
            CookieStore cs = (CookieStore) vaadinSession.getAttribute("cookieStore");
            if (cs != null) cs.clear();
        } finally {
            vaadinSession.unlock();
        }
    }
}