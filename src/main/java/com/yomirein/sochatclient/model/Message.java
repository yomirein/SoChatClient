package com.yomirein.sochatclient.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class Message {
    private Long id;
    private Long chatId;
    private Long senderId;
    private String content;
    private LocalDateTime timestamp;
}