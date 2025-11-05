package com.yomirein.sochatclient.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yomirein.sochatclient.model.Chat;
import com.yomirein.sochatclient.model.Message;
import com.yomirein.sochatclient.model.User;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
public class ChatService {
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper mapper = new ObjectMapper();
    private final String baseUrl = "http://localhost:8080/chat";

    public List<Message> getMessages(Long chatId, String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<Message[]> response = restTemplate.exchange(
                baseUrl + "/" + chatId + "/allmessages",
                HttpMethod.GET,
                entity,
                Message[].class
        );

        return Arrays.asList(response.getBody());
    }

    public List<Chat> getChats(Long userId, String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<Chat[]> response = restTemplate.exchange(
                baseUrl + "/user/chats/" + userId,
                HttpMethod.GET,
                entity,
                Chat[].class
        );

        Chat[] chatsArray = response.getBody();
        if (chatsArray == null) {
            return Collections.emptyList();
        }
        return Arrays.asList(chatsArray);
    }

    public List<Message> getLastMessages(Long chatId, String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<Message[]> response = restTemplate.exchange(
                baseUrl + "/" + chatId + "/messages",
                HttpMethod.GET,
                entity,
                Message[].class
        );

        return Arrays.asList(response.getBody());
    }

    public User getUser(Long userId, String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<User> response = restTemplate.exchange(
                baseUrl + "/user/" + userId,
                HttpMethod.GET,
                entity,
                User.class
        );

        User user = response.getBody();
        if (user == null) {
            return null;
        }
        return user;
    }

    public Chat createChat(Long userId2, String token) {

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);

        String url = baseUrl + "/create?user2Id=" + userId2;

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<Chat> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                Chat.class
        );

        System.out.println(response.getBody().toString());

        try {
            return response.getBody();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}