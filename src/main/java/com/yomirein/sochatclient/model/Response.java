package com.yomirein.sochatclient.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;


public class Response {

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Getter
    public static class Auth {
        private String message;
        private String token;
        private User user;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Getter
    public static class ChatWithExtras {
        List<Chat> chats;
        List<User> users;
    }
}
