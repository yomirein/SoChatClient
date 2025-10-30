package com.yomirein.sochatclient.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;


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
}
