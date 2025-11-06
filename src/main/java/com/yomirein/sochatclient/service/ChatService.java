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

    public List<Message> getMessages(Long chatId) {
        ResponseEntity<Message[]> response = restTemplate.getForEntity(
                baseUrl + "/" + chatId + "/allmessages",
                Message[].class
        );

        return Arrays.asList(response.getBody());
    }

    public List<Chat> getChats(Long userId) {
        ResponseEntity<Chat[]> response = restTemplate.getForEntity(
                baseUrl + "/user/chats/" + userId,
                Chat[].class
        );
        Chat[] chatsArray = response.getBody();
        if (chatsArray == null) {
            return Collections.emptyList();
        }
        return Arrays.asList(chatsArray);
    }

    public List<Message> getLastMessages(Long chatId) {
        ResponseEntity<Message[]> response = restTemplate.getForEntity(
                baseUrl + "/" + chatId + "/messages",
                Message[].class
        );

        return Arrays.asList(response.getBody());
    }

    public User getUser(Long userId) {
        ResponseEntity<User> response = restTemplate.getForEntity(
                baseUrl + "/user/" + userId,
                User.class
        );

        User user = response.getBody();
        if (user == null) {
            return null;
        }
        return user;
    }

    public Chat createChat(Long userId2) {
        String url = baseUrl + "/create?user2Id=" + userId2;

        ResponseEntity<Chat> response = restTemplate.getForEntity(
                url,
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