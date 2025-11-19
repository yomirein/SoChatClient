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

    private final String baseUrl = "http://localhost:8443/chat";

    public ChatService() {}

    // --- UI-поток (использует сессионный RestTemplate автоматически) ---
    public List<Chat> getChats(Long userId) {
        RestTemplate rest = AuthService.createRestTemplateForCurrentVaadinSession();
        return getChatsUsingRest(rest, userId);
    }

    public List<Message> getMessages(Long chatId, int getLastMessageId) {
        RestTemplate rest = AuthService.createRestTemplateForCurrentVaadinSession();
        return getMessagesUsingRest(rest, chatId, getLastMessageId);
    }

    public List<Message> getAllMessages(Long chatId) {
        RestTemplate rest = AuthService.createRestTemplateForCurrentVaadinSession();
        return getAllMessagesUsingRest(rest, chatId);
    }

    public User getUser(Long userId) {
        RestTemplate rest = AuthService.createRestTemplateForCurrentVaadinSession();
        return getUserUsingRest(rest, userId);
    }

    public Chat createChat(Long userId2) {
        RestTemplate rest = AuthService.createRestTemplateForCurrentVaadinSession();
        return createChatUsingRest(rest, userId2);
    }

    // --- Фоновые версии: принимают RestTemplate (на основе snapshot cookieStore) ---
    public List<Chat> getChatsUsingRest(RestTemplate rest, Long userId) {
        ResponseEntity<Chat[]> response = rest.getForEntity(baseUrl + "/user/chats/" + userId, Chat[].class);
        Chat[] arr = response.getBody();
        return arr == null ? Collections.emptyList() : Arrays.asList(arr);
    }

    public List<Message> getMessagesUsingRest(RestTemplate rest, Long chatId, int getLastMessageId) {
        ResponseEntity<Message[]> response = rest.getForEntity(baseUrl + "/" + chatId + "/messages?messageId=" + getLastMessageId, Message[].class);
        Message[] arr = response.getBody();
        return arr == null ? Collections.emptyList() : Arrays.asList(arr);
    }

    public List<Message> getAllMessagesUsingRest(RestTemplate rest, Long chatId) {
        ResponseEntity<Message[]> response = rest.getForEntity(baseUrl + "/" + chatId + "/allmessages", Message[].class);
        Message[] arr = response.getBody();
        return arr == null ? Collections.emptyList() : Arrays.asList(arr);
    }

    public User getUserUsingRest(RestTemplate rest, Long userId) {
        ResponseEntity<User> response = rest.getForEntity(baseUrl + "/user/" + userId, User.class);
        return response.getBody();
    }

    public Chat createChatUsingRest(RestTemplate rest, Long userId2) {
        ResponseEntity<Chat> response = rest.postForEntity(baseUrl + "/create", userId2, Chat.class);
        return response.getBody();
    }
}