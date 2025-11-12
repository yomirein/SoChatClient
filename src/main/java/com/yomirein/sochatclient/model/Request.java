package com.yomirein.sochatclient.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.awt.print.Pageable;

public class Request {

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Getter
    public static class Auth {
        private String username;
        private String password;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Getter
    public static class PageableMessage {
        private int lastMessageCount;
        private Long chatId;
    }

}
