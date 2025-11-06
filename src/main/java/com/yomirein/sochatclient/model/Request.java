package com.yomirein.sochatclient.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class Request {

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Getter
    public static class Auth {
        private String username;
        private String password;
    }

}
