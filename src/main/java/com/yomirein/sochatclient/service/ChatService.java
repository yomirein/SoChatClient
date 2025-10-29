package com.yomirein.sochatclient.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yomirein.sochatclient.model.Chat;
import com.yomirein.sochatclient.model.Message;
import org.checkerframework.checker.units.qual.C;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class ChatService {
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper mapper = new ObjectMapper();
    private final String baseUrl = "http://localhost:8080/chat";

    public List<Message> getMessages(Long chatId, String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "/" + chatId + "/messages",
                HttpMethod.GET,
                entity,
                String.class
        );

        try {
            return mapper.readValue(response.getBody(), new TypeReference<List<Message>>() {});
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public List<Chat> getChatList(){
        List<Chat> a = new ArrayList<Chat>();
        return a;
    }
/*
    public void createChat(Long userName1, Long userName2){
        HttpHeaders headers = new HttpHeaders();
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        var body = Map.of("username", username, "password", password);


        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "/create",
                HttpMethod.GET,
                entity,
                String.class
        );
        response.getBody();
    }
*/
}