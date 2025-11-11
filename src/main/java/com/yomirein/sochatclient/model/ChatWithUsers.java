package com.yomirein.sochatclient.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChatWithUsers {
    private Long id;
    private String name;
    private boolean isGroup;
    private List<User> participants;
}
