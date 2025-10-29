package com.yomirein.sochatclient.model;

import java.util.List;

public class Chat {
    private Long id;

    private String name;
    private boolean isGroup;
    private List<User> participants;
}
